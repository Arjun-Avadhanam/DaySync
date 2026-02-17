package com.daysync.app.feature.sports.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daysync.app.feature.sports.data.SportsRefreshManager
import com.daysync.app.feature.sports.data.SportsRepository
import com.daysync.app.feature.sports.data.model.SportEventWithDetails
import dagger.hilt.android.lifecycle.HiltViewModel
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

    init {
        viewModelScope.launch {
            repository.ensureSeedData()
            collectEvents()
            refreshData()
        }
    }

    private fun collectEvents() {
        // Collect upcoming events
        viewModelScope.launch {
            combine(
                repository.getUpcomingEvents(),
                repository.getWatchlistedEventIds(),
            ) { events, watchlistIds ->
                events to watchlistIds.toSet()
            }.collect { (events, watchlistIds) ->
                val enriched = events.map { repository.enrichEvent(it, watchlistIds) }
                _uiState.update { it.copy(upcomingEvents = enriched, isLoading = false) }
            }
        }

        // Collect live events
        viewModelScope.launch {
            combine(
                repository.getLiveEvents(),
                repository.getWatchlistedEventIds(),
            ) { events, watchlistIds ->
                events to watchlistIds.toSet()
            }.collect { (events, watchlistIds) ->
                val enriched = events.map { repository.enrichEvent(it, watchlistIds) }
                _uiState.update { it.copy(liveEvents = enriched, liveCount = enriched.size) }
            }
        }

        // Collect results
        viewModelScope.launch {
            combine(
                repository.getRecentResults(),
                repository.getWatchlistedEventIds(),
            ) { events, watchlistIds ->
                events to watchlistIds.toSet()
            }.collect { (events, watchlistIds) ->
                val enriched = events.map { repository.enrichEvent(it, watchlistIds) }
                _uiState.update { it.copy(resultEvents = enriched) }
            }
        }

        // Collect watchlisted events
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
        // Re-collect filtered events
        viewModelScope.launch {
            combine(
                repository.getUpcomingEvents(sportId),
                repository.getWatchlistedEventIds(),
            ) { events, watchlistIds ->
                events to watchlistIds.toSet()
            }.collect { (events, watchlistIds) ->
                val enriched = events.map { repository.enrichEvent(it, watchlistIds) }
                _uiState.update { it.copy(upcomingEvents = enriched) }
            }
        }
        viewModelScope.launch {
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
            // Collect once for the initial value
            watchlistIds.collect { ids ->
                val enriched = repository.enrichEvent(event, ids.toSet())
                _uiState.update { it.copy(selectedEvent = enriched) }
                return@collect
            }
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
