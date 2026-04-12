package com.daysync.app.feature.journal.ui

import com.daysync.app.feature.journal.domain.JournalEntry
import com.daysync.app.feature.journal.domain.Mood
import java.time.YearMonth
import kotlinx.datetime.LocalDate

sealed interface JournalScreenState {
    data object List : JournalScreenState
    data object Calendar : JournalScreenState
    data class Detail(val entryId: String) : JournalScreenState
    data class Editor(val entryId: String?) : JournalScreenState
}

data class JournalUiState(
    val screenState: JournalScreenState = JournalScreenState.List,
    val entries: kotlin.collections.List<JournalEntry> = emptyList(),
    val calendarEntries: Map<LocalDate, JournalEntry> = emptyMap(),
    val selectedEntry: JournalEntry? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val showArchived: Boolean = false,
    val calendarYearMonth: YearMonth = YearMonth.now(),
    // Editor fields
    val editorTitle: String = "",
    val editorContent: String = "",
    val editorMood: Mood? = null,
    val editorTags: kotlin.collections.List<String> = emptyList(),
    val editorDate: LocalDate? = null,
    val isSaving: Boolean = false,
    // Tags
    val availableTags: kotlin.collections.List<String> = emptyList(),
    val snackbarMessage: String? = null,
)
