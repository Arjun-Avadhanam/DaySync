package com.daysync.app.feature.sports.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daysync.app.feature.sports.data.SportsRefreshManager
import com.daysync.app.feature.sports.data.SportsRepository
import com.daysync.app.feature.sports.data.model.SportEventWithDetails
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

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

    init {
        viewModelScope.launch {
            repository.ensureSeedData()
            collectEvents(sportId = null)
            refreshData()
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
            ) { events, watchlistIds ->
                events to watchlistIds.toSet()
            }.collect { (events, watchlistIds) ->
                val enriched = events.map { repository.enrichEvent(it, watchlistIds) }
                _uiState.update { it.copy(upcomingEvents = enriched, isLoading = false) }
            }
        }

        // Collect live events
        liveJob = viewModelScope.launch {
            combine(
                repository.getLiveEvents(sportId),
                repository.getWatchlistedEventIds(),
            ) { events, watchlistIds ->
                events to watchlistIds.toSet()
            }.collect { (events, watchlistIds) ->
                val enriched = events.map { repository.enrichEvent(it, watchlistIds) }
                _uiState.update { it.copy(liveEvents = enriched, liveCount = enriched.size) }
            }
        }

        // Collect results
        resultsJob = viewModelScope.launch {
            combine(
                repository.getRecentResults(sportId),
                repository.getWatchlistedEventIds(),
            ) { events, watchlistIds ->
                events to watchlistIds.toSet()
            }.collect { (events, watchlistIds) ->
                val enriched = events.map { repository.enrichEvent(it, watchlistIds) }
                _uiState.update { it.copy(resultEvents = enriched) }
            }
        }

        // Collect watchlisted events (not sport-filtered)
        viewModelScope.launch {
            combine(
                repository.getWatchlistedEvents(),
                repository.getWatchlistedEventIds(),
            ) { events, watchlistIds ->
                events to watchlistIds.toSet()
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
        _uiState.update { it.copy(destination = SportsDestination.EventList, selectedEvent = null) }
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
}
