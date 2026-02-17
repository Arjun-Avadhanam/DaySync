package com.daysync.app.feature.sports.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.daysync.app.feature.sports.data.model.SportEventWithDetails
import com.daysync.app.feature.sports.ui.components.SportsEventCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SportsCompetitionScreen(
    competitionId: String,
    events: List<SportEventWithDetails>,
    onBack: () -> Unit,
    onEventClick: (String) -> Unit,
    onWatchlistToggle: (String) -> Unit,
    onNavigate: (SportsDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val competitionName = events.firstOrNull()?.competitionName ?: "Competition"

    // Determine if standings are available (football competitions with footballDataId)
    val footballDataCode = when (competitionId) {
        "football-pl" -> "PL"
        "football-cl" -> "CL"
        "football-sa" -> "SA"
        "football-laliga" -> "PD"
        else -> null
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(competitionName) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                if (footballDataCode != null) {
                    IconButton(onClick = {
                        onNavigate(SportsDestination.Standings(competitionId, footballDataCode))
                    }) {
                        Icon(Icons.Default.Leaderboard, contentDescription = "Standings")
                    }
                }
            },
        )

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
