package com.daysync.app.feature.sports.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.daysync.app.feature.sports.data.model.SportEventWithDetails
import com.daysync.app.feature.sports.ui.components.SportsEmptyState
import com.daysync.app.feature.sports.ui.components.SportsEventCard
import com.daysync.app.feature.sports.ui.components.SportsFilterBar
import com.daysync.app.feature.sports.ui.components.SportsTabRow
import com.daysync.app.feature.sports.ui.components.SportFilter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SportsEventListScreen(
    state: SportsUiState,
    onTabSelected: (SportsTab) -> Unit,
    onSportSelected: (String?) -> Unit,
    onEventClick: (String) -> Unit,
    onWatchlistToggle: (String) -> Unit,
    onRefresh: () -> Unit,
    onNavigate: (SportsDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sportFilters = remember {
        listOf(
            SportFilter(null, "All"),
            SportFilter("football", "Football"),
            SportFilter("basketball", "Basketball"),
            SportFilter("tennis", "Tennis"),
            SportFilter("f1", "F1"),
            SportFilter("mma", "MMA"),
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Top bar
        TopAppBar(
            title = { Text("Sports") },
            actions = {
                IconButton(onClick = { onNavigate(SportsDestination.Search) }) {
                    Icon(Icons.Default.Search, contentDescription = "Search teams")
                }
                var menuExpanded by remember { mutableStateOf(false) }
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More options")
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Manage Following") },
                        onClick = {
                            menuExpanded = false
                            onNavigate(SportsDestination.ManageFollowing)
                        },
                    )
                }
            },
        )

        // Sport filter chips
        SportsFilterBar(
            filters = sportFilters,
            selectedId = state.selectedSportId,
            onFilterSelected = onSportSelected,
        )

        // Tabs
        SportsTabRow(
            selectedTab = state.selectedTab,
            liveCount = state.liveCount,
            onTabSelected = onTabSelected,
        )

        // Loading indicator
        if (state.isLoading) {
            LinearProgressIndicator(modifier = Modifier.padding(horizontal = 16.dp))
        }

        // Error
        state.error?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }

        // Event list
        val events = when (state.selectedTab) {
            SportsTab.UPCOMING -> state.upcomingEvents
            SportsTab.LIVE -> state.liveEvents
            SportsTab.RESULTS -> state.resultEvents
            SportsTab.WATCHLIST -> state.watchlistedEvents
        }

        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            if (events.isEmpty() && !state.isLoading) {
                SportsEmptyState(tab = state.selectedTab)
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(events, key = { it.id }) { event ->
                        SportsEventCard(
                            event = event,
                            onEventClick = onEventClick,
                            onWatchlistToggle = onWatchlistToggle,
                        )
                    }
                }
            }
        }
    }
}
