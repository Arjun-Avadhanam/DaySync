package com.daysync.app.feature.sports.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daysync.app.core.database.entity.CompetitorEntity
import com.daysync.app.feature.sports.data.SportsRefreshManager
import com.daysync.app.feature.sports.data.SportsRepository
import com.daysync.app.feature.sports.data.model.SportEventWithDetails
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class SportsViewModel @Inject constructor(
    private val repository: SportsRepository,
    private val refreshManager: SportsRefreshManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SportsUiState(isLoading = true))
    val uiState: StateFlow<SportsUiState> = _uiState

    // Track collector jobs so we can cancel them when sport filter changes
    private var upcomingJob: Job? = null
    private var liveJob: Job? = null
    private var resultsJob: Job? = null

    // Search state — kept as separate flows to allow debounced + flatMapLatest behaviour
    private val searchQueryFlow = MutableStateFlow("")
    private val searchSportFilterFlow = MutableStateFlow<String?>(null)
    private val selectedSearchTeamFlow = MutableStateFlow<CompetitorEntity?>(null)

    init {
        viewModelScope.launch {
            repository.ensureSeedData()
            collectEvents(sportId = null)
            collectSearch()
            refreshData()
        }
    }

    private fun collectSearch() {
        // Team list driven by debounced query + sport filter
        viewModelScope.launch {
            combine(
                searchQueryFlow.debounce(250),
                searchSportFilterFlow,
            ) { q, sport -> q.trim() to sport }
                .flatMapLatest { (q, sport) ->
                    if (q.length < 2) flowOf(emptyList())
                    else repository.searchCompetitors(q, sport)
                }
                .collect { results ->
                    _uiState.update { it.copy(searchResults = results) }
                }
        }

        // Events for the currently-selected team
        viewModelScope.launch {
            selectedSearchTeamFlow
                .flatMapLatest { team ->
                    if (team == null) {
                        flowOf(emptyList<SportEventWithDetails>())
                    } else {
                        combine(
                            repository.getEventsByCompetitor(team.id),
                            repository.getWatchlistedEventIds(),
                        ) { events, ids ->
                            events.map { repository.enrichEvent(it, ids.toSet()) }
                        }
                    }
                }
                .collect { enriched ->
                    _uiState.update { it.copy(selectedSearchTeamEvents = enriched) }
                }
        }
    }

    private fun collectEvents(sportId: String?) {
        // Cancel previous collectors
        upcomingJob?.cancel()
        liveJob?.cancel()
        resultsJob?.cancel()

        // Collect upcoming events
        upcomingJob = viewModelScope.launch {
            combine(
                repository.getUpcomingEvents(sportId),
                repository.getWatchlistedEventIds(),
                repository.getFollowedCompetitions(),
            ) { events, watchlistIds, followed ->
                Triple(events, watchlistIds.toSet(), followed.map { it.competitionId }.toSet())
            }.collect { (events, watchlistIds, followedIds) ->
                val filtered = events.filter { it.competitionId in followedIds }
                val enriched = filtered.map { repository.enrichEvent(it, watchlistIds) }
                _uiState.update { it.copy(upcomingEvents = enriched, isLoading = false) }
            }
        }

        // Collect live events
        liveJob = viewModelScope.launch {
            combine(
                repository.getLiveEvents(sportId),
                repository.getWatchlistedEventIds(),
                repository.getFollowedCompetitions(),
            ) { events, watchlistIds, followed ->
                Triple(events, watchlistIds.toSet(), followed.map { it.competitionId }.toSet())
            }.collect { (events, watchlistIds, followedIds) ->
                val filtered = events.filter { it.competitionId in followedIds }
                val enriched = filtered.map { repository.enrichEvent(it, watchlistIds) }
                _uiState.update { it.copy(liveEvents = enriched, liveCount = enriched.size) }
            }
        }

        // Collect results
        resultsJob = viewModelScope.launch {
            combine(
                repository.getRecentResults(sportId),
                repository.getWatchlistedEventIds(),
                repository.getFollowedCompetitions(),
            ) { events, watchlistIds, followed ->
                Triple(events, watchlistIds.toSet(), followed.map { it.competitionId }.toSet())
            }.collect { (events, watchlistIds, followedIds) ->
                val filtered = events.filter { it.competitionId in followedIds }
                val enriched = filtered.map { repository.enrichEvent(it, watchlistIds) }
                _uiState.update { it.copy(resultEvents = enriched) }
            }
        }

        // Collect watchlisted events (filtered by selected sport)
        viewModelScope.launch {
            combine(
                repository.getWatchlistedEvents(),
                repository.getWatchlistedEventIds(),
            ) { events, watchlistIds ->
                val filtered = if (sportId != null) events.filter { it.sportId == sportId } else events
                filtered to watchlistIds.toSet()
            }.collect { (events, watchlistIds) ->
                val enriched = events.map { repository.enrichEvent(it, watchlistIds) }
                _uiState.update { it.copy(watchlistedEvents = enriched) }
            }
        }

        // Collect competitions and followed state
        viewModelScope.launch {
            repository.getAllCompetitions().collect { comps ->
                _uiState.update { it.copy(allCompetitions = comps) }
            }
        }
        viewModelScope.launch {
            repository.getFollowedCompetitions().collect { followed ->
                _uiState.update { it.copy(followedCompetitionIds = followed.map { f -> f.competitionId }.toSet()) }
            }
        }
    }

    fun selectTab(tab: SportsTab) {
        _uiState.update { it.copy(selectedTab = tab) }
        // Start/stop live polling based on tab
        if (tab == SportsTab.LIVE) {
            refreshManager.startPolling(viewModelScope)
        } else {
            refreshManager.stopPolling()
        }
    }

    override fun onCleared() {
        super.onCleared()
        refreshManager.stopPolling()
    }

    fun selectSport(sportId: String?) {
        _uiState.update { it.copy(selectedSportId = sportId) }
        collectEvents(sportId)
    }

    fun toggleWatchlist(eventId: String) {
        viewModelScope.launch {
            repository.toggleWatchlist(eventId)
        }
    }

    fun updateWatchnotes(eventId: String, watchnotes: String?) {
        viewModelScope.launch {
            repository.updateWatchnotes(eventId, watchnotes)
        }
    }

    fun navigateTo(destination: SportsDestination) {
        _uiState.update { it.copy(destination = destination) }
        when (destination) {
            is SportsDestination.EventDetail -> loadEventDetail(destination.eventId)
            is SportsDestination.CompetitionFixtures -> loadCompetitionFixtures(destination.competitionId)
            is SportsDestination.Standings -> loadStandings(destination.competitionCode)
            else -> {}
        }
    }

    fun navigateBack() {
        _uiState.update {
            it.copy(
                destination = SportsDestination.EventList,
                selectedEvent = null,
                selectedEventWatchnotes = null,
            )
        }
    }

    private fun loadEventDetail(eventId: String) {
        viewModelScope.launch {
            val event = repository.getEventById(eventId) ?: return@launch
            val watchlistIds = repository.getWatchlistedEventIds()
            watchlistIds.collect { ids ->
                val enriched = repository.enrichEvent(event, ids.toSet())
                _uiState.update { it.copy(selectedEvent = enriched) }
                return@collect
            }
        }
        // Observe watchnotes for this event so UI updates as user types
        viewModelScope.launch {
            repository.observeWatchnotes(eventId).collect { notes ->
                _uiState.update { it.copy(selectedEventWatchnotes = notes) }
            }
        }
        // Load participants (F1 drivers, etc.) with competitor names
        viewModelScope.launch {
            val participants = repository.getParticipantsByEvent(eventId)
            _uiState.update { it.copy(eventParticipants = participants) }
        }
        viewModelScope.launch {
            val participants = repository.getParticipantsByEvent(eventId)
            val competitorIds = participants.map { it.competitorId }.distinct()
            val names = mutableMapOf<String, String>()
            for (id in competitorIds) {
                repository.getCompetitorName(id)?.let { names[id] = it }
            }
            _uiState.update { it.copy(competitorNames = names) }
        }
    }

    private fun loadCompetitionFixtures(competitionId: String) {
        viewModelScope.launch {
            combine(
                repository.getEventsByCompetition(competitionId),
                repository.getWatchlistedEventIds(),
            ) { events, watchlistIds ->
                events to watchlistIds.toSet()
            }.collect { (events, watchlistIds) ->
                val enriched = events.map { repository.enrichEvent(it, watchlistIds) }
                _uiState.update { it.copy(competitionEvents = enriched) }
            }
        }
    }

    private fun loadStandings(competitionCode: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val standings = repository.getStandings(competitionCode)
            _uiState.update { it.copy(standings = standings, isLoading = false) }
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null) }
            try {
                repository.refreshAllSports()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            } finally {
                _uiState.update { it.copy(isRefreshing = false, isLoading = false) }
            }
        }
    }

    // Following
    fun toggleFollowCompetition(competitionId: String) {
        viewModelScope.launch { repository.toggleFollowCompetition(competitionId) }
    }

    fun toggleFollowCompetitor(competitorId: String) {
        viewModelScope.launch { repository.toggleFollowCompetitor(competitorId) }
    }

    // Search
    fun setSearchQuery(query: String) {
        searchQueryFlow.value = query
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setSearchSportFilter(sportId: String?) {
        searchSportFilterFlow.value = sportId
        _uiState.update { it.copy(searchSportFilter = sportId) }
    }

    fun selectSearchTeam(team: CompetitorEntity) {
        selectedSearchTeamFlow.value = team
        _uiState.update { it.copy(selectedSearchTeam = team) }
    }

    fun clearSearchTeam() {
        selectedSearchTeamFlow.value = null
        _uiState.update { it.copy(selectedSearchTeam = null, selectedSearchTeamEvents = emptyList()) }
    }
}
