package com.daysync.app.feature.sports.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.daysync.app.core.ui.LoadingIndicator
import com.daysync.app.feature.sports.data.StandingRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SportsStandingsScreen(
    standings: List<StandingRow>,
    isLoading: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Standings") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
        )

        if (isLoading) {
            LoadingIndicator()
            return
        }

        if (standings.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "No standings available",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return
        }

        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("#", modifier = Modifier.width(28.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text("Team", modifier = Modifier.width(160.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Text("P", modifier = Modifier.width(32.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text("W", modifier = Modifier.width(32.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text("D", modifier = Modifier.width(32.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text("L", modifier = Modifier.width(32.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text("GD", modifier = Modifier.width(36.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text("Pts", modifier = Modifier.width(36.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
        HorizontalDivider()

        LazyColumn {
            items(standings, key = { it.position }) { row ->
                StandingRowItem(row)
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}

@Composable
private fun StandingRowItem(row: StandingRow) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${row.position}",
            modifier = Modifier.width(28.dp),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Row(
            modifier = Modifier.width(160.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (row.teamLogoUrl != null) {
                AsyncImage(
                    model = row.teamLogoUrl,
                    contentDescription = row.teamName,
                    modifier = Modifier
                        .size(20.dp)
                        .clip(MaterialTheme.shapes.extraSmall),
                    contentScale = ContentScale.Fit,
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = row.teamName,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text("${row.played}", modifier = Modifier.width(32.dp), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        Text("${row.won}", modifier = Modifier.width(32.dp), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        Text("${row.draw}", modifier = Modifier.width(32.dp), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        Text("${row.lost}", modifier = Modifier.width(32.dp), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        Text(
            text = if (row.goalDifference > 0) "+${row.goalDifference}" else "${row.goalDifference}",
            modifier = Modifier.width(36.dp),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "${row.points}",
            modifier = Modifier.width(36.dp),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}
