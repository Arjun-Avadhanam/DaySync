package com.daysync.app.feature.media.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daysync.app.core.notion.NotionMediaExporter
import com.daysync.app.feature.media.data.MediaRepository
import com.daysync.app.feature.media.data.remote.MediaMetadataResult
import com.daysync.app.feature.media.data.remote.MediaMetadataService
import com.daysync.app.feature.media.domain.MediaItem
import com.daysync.app.feature.media.domain.MediaStatus
import com.daysync.app.feature.media.domain.MediaType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ViewMode { LIST, GRID }

data class MediaCounts(
    val total: Int = 0,
    val notStarted: Int = 0,
    val inProgress: Int = 0,
    val done: Int = 0,
    val dropped: Int = 0,
)

sealed interface MediaUiState {
    data object Loading : MediaUiState
    data class Success(
        val items: List<MediaItem>,
        val typeFilter: Set<MediaType>? = null,
        val statusFilter: MediaStatus? = null,
        val searchQuery: String = "",
        val viewMode: ViewMode = ViewMode.LIST,
        val counts: MediaCounts = MediaCounts(),
    ) : MediaUiState
    data class Error(val message: String) : MediaUiState
}

sealed interface MediaScreenState {
    data object List : MediaScreenState
    data class Detail(val itemId: String) : MediaScreenState
    data class AddEdit(val itemId: String? = null) : MediaScreenState
}

@OptIn(FlowPreview::class)
@HiltViewModel
class MediaViewModel @Inject constructor(
    private val repository: MediaRepository,
    private val metadataService: MediaMetadataService,
    private val notionExporter: NotionMediaExporter,
) : ViewModel() {

    private val _typeFilter = MutableStateFlow<Set<MediaType>?>(null)
    private val _statusFilter = MutableStateFlow<MediaStatus?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _viewMode = MutableStateFlow(ViewMode.LIST)

    private val _screenState = MutableStateFlow<MediaScreenState>(MediaScreenState.List)
    val screenState: StateFlow<MediaScreenState> = _screenState.asStateFlow()

    private val _metadataResults = MutableStateFlow<List<MediaMetadataResult>>(emptyList())
    val metadataResults: StateFlow<List<MediaMetadataResult>> = _metadataResults.asStateFlow()

    private val _isSearchingMetadata = MutableStateFlow(false)
    val isSearchingMetadata: StateFlow<Boolean> = _isSearchingMetadata.asStateFlow()

    // Unique creators that have been used across all existing media items.
    // Used to power the "type-ahead" suggestions in the Add/Edit form so the
    // user doesn't have to retype names of authors/directors/etc. they've
    // already recorded.
    private val allKnownCreators: StateFlow<List<String>> = repository.getAllItems()
        .map { items ->
            items.asSequence()
                .flatMap { it.creators.asSequence() }
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinctBy { it.lowercase() }
                .sortedBy { it.lowercase() }
                .toList()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _creatorQuery = MutableStateFlow("")

    val creatorSuggestions: StateFlow<List<String>> = _creatorQuery
        .debounce(250)
        .combine(allKnownCreators) { query, all ->
            val trimmed = query.trim()
            if (trimmed.isBlank()) {
                emptyList()
            } else {
                all.asSequence()
                    .filter { it.contains(trimmed, ignoreCase = true) }
                    .filter { !it.equals(trimmed, ignoreCase = true) }
                    .take(6)
                    .toList()
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setCreatorQuery(query: String) {
        _creatorQuery.value = query
    }

    fun clearCreatorQuery() {
        _creatorQuery.value = ""
    }

    val uiState: StateFlow<MediaUiState> = combine(
        repository.getAllItems(),
        _typeFilter,
        _statusFilter,
        _searchQuery,
        _viewMode,
    ) { allItems, typeFilter, statusFilter, searchQuery, viewMode ->
        val filtered = allItems.filter { item ->
            val matchesType = typeFilter == null || item.mediaType in typeFilter
            val matchesStatus = statusFilter == null || item.status == statusFilter
            val matchesSearch = searchQuery.isBlank() ||
                item.title.contains(searchQuery, ignoreCase = true)
            matchesType && matchesStatus && matchesSearch
        }

        val effectiveViewMode = if (typeFilter != null && typeFilter.all { it.isVisualHeavy }) {
            ViewMode.GRID
        } else {
            viewMode
        }

        MediaUiState.Success(
            items = filtered,
            typeFilter = typeFilter,
            statusFilter = statusFilter,
            searchQuery = searchQuery,
            viewMode = effectiveViewMode,
            counts = MediaCounts(
                total = allItems.size,
                notStarted = allItems.count { it.status == MediaStatus.NOT_STARTED },
                inProgress = allItems.count { it.status == MediaStatus.IN_PROGRESS },
                done = allItems.count { it.status == MediaStatus.DONE },
                dropped = allItems.count { it.status == MediaStatus.DROPPED },
            ),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MediaUiState.Loading)

    fun setTypeFilter(types: Set<MediaType>?) {
        _typeFilter.value = types
    }

    fun setStatusFilter(status: MediaStatus?) {
        _statusFilter.value = status
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleViewMode() {
        _viewMode.value = when (_viewMode.value) {
            ViewMode.LIST -> ViewMode.GRID
            ViewMode.GRID -> ViewMode.LIST
        }
    }

    fun navigateTo(state: MediaScreenState) {
        _screenState.value = state
    }

    fun navigateBack() {
        _screenState.value = MediaScreenState.List
    }

    fun addItem(item: MediaItem) {
        viewModelScope.launch {
            repository.addItem(item)
            _screenState.value = MediaScreenState.List
        }
    }

    fun updateItem(item: MediaItem) {
        viewModelScope.launch {
            repository.updateItem(item)
        }
    }

    fun deleteItem(id: String) {
        viewModelScope.launch {
            repository.deleteItem(id)
            _screenState.value = MediaScreenState.List
        }
    }

    fun quickUpdateStatus(item: MediaItem, newStatus: MediaStatus) {
        viewModelScope.launch {
            repository.updateItem(item.copy(status = newStatus))
        }
    }

    fun quickUpdateScore(item: MediaItem, newScore: Double?) {
        viewModelScope.launch {
            repository.updateItem(item.copy(score = newScore))
        }
    }

    suspend fun getItemById(id: String): MediaItem? = repository.getItemById(id)

    private var searchJob: Job? = null

    fun searchMetadata(query: String, mediaType: MediaType) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(500) // Debounce: wait 500ms after last keystroke
            _isSearchingMetadata.value = true
            _metadataResults.value = metadataService.search(query, mediaType)
            _isSearchingMetadata.value = false
        }
    }

    suspend fun fetchCreators(externalId: String, mediaType: MediaType): List<String> {
        return metadataService.fetchCreators(externalId, mediaType)
    }

    fun clearMetadataResults() {
        _metadataResults.value = emptyList()
    }

    private val _notionExportStatus = MutableStateFlow<String?>(null)
    val notionExportStatus: StateFlow<String?> = _notionExportStatus.asStateFlow()

    fun exportToNotion(item: MediaItem) {
        viewModelScope.launch {
            _notionExportStatus.value = "Saving..."
            val entity = com.daysync.app.core.database.entity.MediaItemEntity(
                id = item.id,
                title = item.title,
                mediaType = item.mediaType.name,
                status = item.status.name,
                score = item.score,
                creators = item.creators,
                completedDate = item.completedDate,
                notes = item.notes,
                coverImageUrl = item.coverImageUrl,
            )
            notionExporter.export(entity).fold(
                onSuccess = { _notionExportStatus.value = "Saved to Notion" },
                onFailure = { _notionExportStatus.value = "Failed: ${it.message}" },
            )
            kotlinx.coroutines.delay(3000)
            _notionExportStatus.value = null
        }
    }
}
