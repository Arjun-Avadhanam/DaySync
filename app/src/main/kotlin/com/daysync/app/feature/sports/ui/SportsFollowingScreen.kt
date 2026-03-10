package com.daysync.app.feature.sports.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.daysync.app.core.database.entity.CompetitionEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SportsFollowingScreen(
    competitions: List<CompetitionEntity>,
    followedCompetitionIds: Set<String>,
    onToggleFollowCompetition: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val grouped = competitions.groupBy { it.sportId }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Manage Following") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
        )

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            grouped.forEach { (sportId, comps) ->
                item(key = "header-$sportId") {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = sportId.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                items(comps, key = { it.id }) { competition ->
                    CompetitionFollowRow(
                        competition = competition,
                        isFollowed = competition.id in followedCompetitionIds,
                        onToggle = { onToggleFollowCompetition(competition.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CompetitionFollowRow(
    competition: CompetitionEntity,
    isFollowed: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = competition.name,
                style = MaterialTheme.typography.bodyLarge,
            )
            competition.country?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(
            checked = isFollowed,
            onCheckedChange = { onToggle() },
        )
    }
}
