package com.daysync.app.feature.sports.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.daysync.app.core.database.entity.CompetitorEntity
import com.daysync.app.feature.sports.data.model.SportEventWithDetails
import com.daysync.app.feature.sports.ui.components.SportFilter
import com.daysync.app.feature.sports.ui.components.SportsEventCard
import com.daysync.app.feature.sports.ui.components.SportsFilterBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SportsSearchScreen(
    state: SportsUiState,
    onQueryChange: (String) -> Unit,
    onSportFilterChange: (String?) -> Unit,
    onTeamSelected: (CompetitorEntity) -> Unit,
    onClearSelectedTeam: () -> Unit,
    onEventClick: (String) -> Unit,
    onWatchlistToggle: (String) -> Unit,
    onBack: () -> Unit,
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
    val selectedTeam = state.selectedSearchTeam

    // System back: drop down a level (team detail → search list) before
    // letting the parent BackHandler take us out of the screen entirely.
    BackHandler(enabled = selectedTeam != null) {
        onClearSelectedTeam()
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(if (selectedTeam != null) selectedTeam.name else "Search teams")
            },
            navigationIcon = {
                IconButton(onClick = {
                    if (selectedTeam != null) onClearSelectedTeam() else onBack()
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
        )

        if (selectedTeam == null) {
            // Search input + filter chips + team list
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search teams, players, drivers…") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
            )

            SportsFilterBar(
                filters = sportFilters,
                selectedId = state.searchSportFilter,
                onFilterSelected = onSportFilterChange,
            )

            HorizontalDivider()

            when {
                state.searchQuery.length < 2 -> {
                    HintBox("Type at least 2 characters to search")
                }
                state.searchResults.isEmpty() -> {
                    HintBox("No teams matching \"${state.searchQuery}\"")
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp),
                    ) {
                        items(state.searchResults, key = { it.id }) { team ->
                            TeamRow(team = team, onClick = { onTeamSelected(team) })
                        }
                    }
                }
            }
        } else {
            // Selected team header + events list
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (selectedTeam.logoUrl != null) {
                    AsyncImage(
                        model = selectedTeam.logoUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(MaterialTheme.shapes.small),
                        contentScale = ContentScale.Fit,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Column {
                    Text(selectedTeam.name, style = MaterialTheme.typography.titleMedium)
                    selectedTeam.country?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            HorizontalDivider()

            val events = state.selectedSearchTeamEvents
            if (events.isEmpty()) {
                HintBox("No matches in the database for this team yet. Pull to refresh on the main feed to fetch new events.")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp),
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

@Composable
private fun TeamRow(team: CompetitorEntity, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (team.logoUrl != null) {
            AsyncImage(
                model = team.logoUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(36.dp)
                    .clip(MaterialTheme.shapes.small),
                contentScale = ContentScale.Fit,
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(team.name, style = MaterialTheme.typography.bodyLarge)
            val sublabel = listOfNotNull(
                team.sportId.replaceFirstChar { it.uppercase() },
                team.country,
            ).joinToString(" • ")
            if (sublabel.isNotBlank()) {
                Text(
                    sublabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun HintBox(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
