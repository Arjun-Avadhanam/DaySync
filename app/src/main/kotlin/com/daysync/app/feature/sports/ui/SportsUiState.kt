package com.daysync.app.feature.sports.ui

import com.daysync.app.core.database.entity.CompetitionEntity
import com.daysync.app.feature.sports.data.StandingRow
import com.daysync.app.feature.sports.data.model.SportEventWithDetails

enum class SportsTab { UPCOMING, LIVE, RESULTS, WATCHLIST }

sealed interface SportsDestination {
    data object EventList : SportsDestination
    data class EventDetail(val eventId: String) : SportsDestination
    data class CompetitionFixtures(val competitionId: String) : SportsDestination
    data object ManageFollowing : SportsDestination
    data class Standings(val competitionId: String, val competitionCode: String) : SportsDestination
}

data class SportsUiState(
    val selectedTab: SportsTab = SportsTab.UPCOMING,
    val selectedSportId: String? = null,
    val destination: SportsDestination = SportsDestination.EventList,
    val upcomingEvents: List<SportEventWithDetails> = emptyList(),
    val liveEvents: List<SportEventWithDetails> = emptyList(),
    val resultEvents: List<SportEventWithDetails> = emptyList(),
    val watchlistedEvents: List<SportEventWithDetails> = emptyList(),
    val competitionEvents: List<SportEventWithDetails> = emptyList(),
    val selectedEvent: SportEventWithDetails? = null,
    val standings: List<StandingRow> = emptyList(),
    val allCompetitions: List<CompetitionEntity> = emptyList(),
    val followedCompetitionIds: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val liveCount: Int = 0,
)
