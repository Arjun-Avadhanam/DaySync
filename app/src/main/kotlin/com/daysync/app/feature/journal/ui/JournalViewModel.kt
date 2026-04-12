package com.daysync.app.feature.journal.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daysync.app.feature.journal.data.JournalRepository
import com.daysync.app.feature.journal.domain.JournalEntry
import com.daysync.app.feature.journal.domain.JournalTag
import com.daysync.app.feature.journal.domain.Mood
import com.daysync.app.feature.journal.domain.newJournalEntryId
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.YearMonth
import javax.inject.Inject
import kotlin.time.Clock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class JournalViewModel @Inject constructor(
    private val repository: JournalRepository,
    private val notionExporter: com.daysync.app.core.notion.NotionJournalExporter,
) : ViewModel() {

    private val _uiState = MutableStateFlow(JournalUiState())

    private val _listMode = MutableStateFlow(ListMode.ALL)
    private val _searchQuery = MutableStateFlow("")

    private val entries = combine(_listMode, _searchQuery) { mode, query ->
        Pair(mode, query)
    }.flatMapLatest { (mode, query) ->
        when {
            query.isNotBlank() -> repository.searchEntries(query)
            mode == ListMode.ARCHIVED -> repository.getArchivedEntries()
            else -> repository.getAllEntries()
        }
    }

    private val availableTags = repository.getAllUsedTags()

    val uiState: StateFlow<JournalUiState> = combine(
        _uiState,
        entries,
        availableTags,
    ) { state, entryList, tags ->
        val allTags = (JournalTag.PREDEFINED + tags).distinct()
        state.copy(
            entries = entryList,
            availableTags = allTags,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = JournalUiState(isLoading = true),
    )

    // Navigation

    fun navigateToList() {
        _uiState.update {
            it.copy(
                screenState = JournalScreenState.List,
                isSearchActive = false,
                searchQuery = "",
            )
        }
        _searchQuery.value = ""
    }

    fun navigateToCalendar() {
        _uiState.update {
            it.copy(
                screenState = JournalScreenState.Calendar,
                calendarYearMonth = YearMonth.now(),
            )
        }
        loadCalendarEntries(YearMonth.now())
    }

    fun navigateToDetail(entryId: String) {
        viewModelScope.launch {
            val entry = repository.getEntryById(entryId)
            _uiState.update {
                it.copy(
                    screenState = JournalScreenState.Detail(entryId),
                    selectedEntry = entry,
                )
            }
        }
    }

    fun navigateToEditor(entryId: String? = null, prefillDate: LocalDate? = null) {
        viewModelScope.launch {
            val entry = entryId?.let { repository.getEntryById(it) }
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            _uiState.update {
                it.copy(
                    screenState = JournalScreenState.Editor(entryId),
                    editorTitle = entry?.title.orEmpty(),
                    editorContent = entry?.content.orEmpty(),
                    editorMood = entry?.mood,
                    editorTags = entry?.tags.orEmpty(),
                    editorDate = entry?.date ?: prefillDate ?: today,
                )
            }
        }
    }

    /**
     * Returns true if back press was consumed (navigated within journal sub-screens).
     */
    fun handleBackPress(): Boolean {
        val current = _uiState.value.screenState
        return when (current) {
            is JournalScreenState.List -> false
            is JournalScreenState.Calendar -> {
                navigateToList()
                true
            }
            is JournalScreenState.Detail -> {
                navigateToList()
                true
            }
            is JournalScreenState.Editor -> {
                val returnTo = current.entryId
                if (returnTo != null) {
                    navigateToDetail(returnTo)
                } else {
                    navigateToList()
                }
                true
            }
        }
    }

    // Search

    fun toggleSearch() {
        _uiState.update {
            val newActive = !it.isSearchActive
            if (!newActive) {
                _searchQuery.value = ""
            }
            it.copy(
                isSearchActive = newActive,
                searchQuery = if (newActive) it.searchQuery else "",
            )
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        _searchQuery.value = query
    }

    // Archive filter

    fun toggleShowArchived() {
        val newShow = _listMode.value != ListMode.ARCHIVED
        _listMode.value = if (newShow) ListMode.ARCHIVED else ListMode.ALL
        _uiState.update { it.copy(showArchived = newShow) }
    }

    // Editor actions

    fun updateEditorTitle(title: String) {
        _uiState.update { it.copy(editorTitle = title) }
    }

    fun updateEditorContent(content: String) {
        _uiState.update { it.copy(editorContent = content) }
    }

    fun updateEditorMood(mood: Mood?) {
        _uiState.update {
            it.copy(editorMood = if (it.editorMood == mood) null else mood)
        }
    }

    fun toggleEditorTag(tag: String) {
        _uiState.update {
            val newTags = if (tag in it.editorTags) {
                it.editorTags - tag
            } else {
                it.editorTags + tag
            }
            it.copy(editorTags = newTags)
        }
    }

    fun addCustomTag(tag: String) {
        val trimmed = tag.trim()
        if (trimmed.isBlank()) return
        _uiState.update {
            if (trimmed in it.editorTags) return@update it
            it.copy(editorTags = it.editorTags + trimmed)
        }
    }

    fun updateEditorDate(date: LocalDate) {
        _uiState.update { it.copy(editorDate = date) }
    }

    fun saveEntry() {
        val state = _uiState.value
        val title = state.editorTitle.trim()
        val content = state.editorContent.trim()
        if (title.isEmpty() && content.isEmpty()) return

        val editorState = state.screenState as? JournalScreenState.Editor ?: return

        _uiState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            val entry = JournalEntry(
                id = editorState.entryId ?: newJournalEntryId(),
                date = state.editorDate
                    ?: Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date,
                title = title.ifEmpty { null },
                content = content.ifEmpty { null },
                mood = state.editorMood,
                tags = state.editorTags,
                isArchived = false,
            )
            repository.saveEntry(entry)
            _uiState.update { it.copy(isSaving = false) }
            navigateToDetail(entry.id)
        }
    }

    // Detail actions

    fun deleteEntry(id: String) {
        viewModelScope.launch {
            repository.deleteEntry(id)
            navigateToList()
        }
    }

    fun toggleArchive(id: String) {
        viewModelScope.launch {
            repository.toggleArchive(id)
            navigateToList()
        }
    }

    // Calendar

    fun changeCalendarMonth(yearMonth: YearMonth) {
        _uiState.update { it.copy(calendarYearMonth = yearMonth) }
        loadCalendarEntries(yearMonth)
    }

    private fun loadCalendarEntries(yearMonth: YearMonth) {
        viewModelScope.launch {
            val start = LocalDate(yearMonth.year, yearMonth.monthValue, 1)
            val end = LocalDate(yearMonth.year, yearMonth.monthValue, yearMonth.lengthOfMonth())
            repository.getEntriesByDateRange(start, end).collect { entries ->
                val map = entries.associateBy { it.date }
                _uiState.update { it.copy(calendarEntries = map) }
            }
        }
    }

    fun isEditorDirty(): Boolean {
        val state = _uiState.value
        val editorState = state.screenState as? JournalScreenState.Editor ?: return false

        return if (editorState.entryId != null) {
            // Editing existing: check if anything changed from selected entry
            val original = state.selectedEntry ?: return state.editorTitle.isNotBlank() ||
                state.editorContent.isNotBlank()
            state.editorTitle != (original.title.orEmpty()) ||
                state.editorContent != (original.content.orEmpty()) ||
                state.editorMood != original.mood ||
                state.editorTags != original.tags ||
                state.editorDate != original.date
        } else {
            // New entry: dirty if anything is filled
            state.editorTitle.isNotBlank() || state.editorContent.isNotBlank() ||
                state.editorMood != null || state.editorTags.isNotEmpty()
        }
    }

    fun exportToNotion(entryId: String) {
        viewModelScope.launch {
            try {
                val entity = repository.getEntryEntityById(entryId) ?: return@launch
                notionExporter.export(entity).fold(
                    onSuccess = {
                        _uiState.update { s -> s.copy(snackbarMessage = "Saved to Notion") }
                    },
                    onFailure = { err ->
                        _uiState.update { s -> s.copy(snackbarMessage = "Notion export failed: ${err.message}") }
                    },
                )
            } catch (e: Exception) {
                _uiState.update { s -> s.copy(snackbarMessage = "Notion export failed: ${e.message}") }
            }
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    private enum class ListMode { ALL, ARCHIVED }
}
