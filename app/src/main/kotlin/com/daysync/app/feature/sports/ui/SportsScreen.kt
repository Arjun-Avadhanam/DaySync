package com.daysync.app.feature.sports.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SportsScreen(
    modifier: Modifier = Modifier,
    viewModel: SportsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Handle back navigation for sub-destinations
    if (state.destination != SportsDestination.EventList) {
        BackHandler { viewModel.navigateBack() }
    }

    when (val destination = state.destination) {
        is SportsDestination.EventList -> {
            SportsEventListScreen(
                state = state,
                onTabSelected = viewModel::selectTab,
                onSportSelected = viewModel::selectSport,
                onEventClick = { viewModel.navigateTo(SportsDestination.EventDetail(it)) },
                onWatchlistToggle = viewModel::toggleWatchlist,
                onRefresh = viewModel::refreshData,
                onNavigate = viewModel::navigateTo,
                modifier = modifier,
            )
        }
        is SportsDestination.EventDetail -> {
            SportsEventDetailScreen(
                event = state.selectedEvent,
                participants = state.eventParticipants,
                competitorNames = state.competitorNames,
                onBack = viewModel::navigateBack,
                onWatchlistToggle = viewModel::toggleWatchlist,
                modifier = modifier,
            )
        }
        is SportsDestination.CompetitionFixtures -> {
            SportsCompetitionScreen(
                competitionId = destination.competitionId,
                events = state.competitionEvents,
                onBack = viewModel::navigateBack,
                onEventClick = { viewModel.navigateTo(SportsDestination.EventDetail(it)) },
                onWatchlistToggle = viewModel::toggleWatchlist,
                onNavigate = viewModel::navigateTo,
                modifier = modifier,
            )
        }
        is SportsDestination.ManageFollowing -> {
            SportsFollowingScreen(
                competitions = state.allCompetitions,
                followedCompetitionIds = state.followedCompetitionIds,
                onToggleFollowCompetition = viewModel::toggleFollowCompetition,
                onBack = viewModel::navigateBack,
                modifier = modifier,
            )
        }
        is SportsDestination.Search -> {
            SportsSearchScreen(
                state = state,
                onQueryChange = viewModel::setSearchQuery,
                onSportFilterChange = viewModel::setSearchSportFilter,
                onTeamSelected = viewModel::selectSearchTeam,
                onClearSelectedTeam = viewModel::clearSearchTeam,
                onEventClick = { viewModel.navigateTo(SportsDestination.EventDetail(it)) },
                onWatchlistToggle = viewModel::toggleWatchlist,
                onBack = viewModel::navigateBack,
                modifier = modifier,
            )
        }
        is SportsDestination.Standings -> {
            SportsStandingsScreen(
                standings = state.standings,
                isLoading = state.isLoading,
                onBack = viewModel::navigateBack,
                modifier = modifier,
            )
        }
    }
}
