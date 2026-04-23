package com.daysync.app.feature.health.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.daysync.app.feature.health.model.SleepSummary
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun SleepCard(
    sessions: List<SleepSummary>,
    modifier: Modifier = Modifier,
) {
    if (sessions.isEmpty()) return
    val zone = com.daysync.app.core.config.UserPreferences(
        androidx.compose.ui.platform.LocalContext.current
    ).javaZoneId

    val totalMinutes = sessions.sumOf { it.session.totalMinutes }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = if (sessions.size == 1) "Last Night's Sleep" else "Sleep Sessions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    sessions.firstOrNull()?.session?.score?.let { score ->
                        Text(
                            text = "Score: $score",
                            style = MaterialTheme.typography.bodySmall,
                            color = scoreColor(score),
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
                Text(
                    text = formatDuration(totalMinutes),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            // Individual session rows when there are multiple
            if (sessions.size > 1) {
                sessions.forEachIndexed { index, sleep ->
                    if (index > 0) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 6.dp),
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                    }
                    SleepSessionRow(sleep)
                }
            } else {
                // Single session — show time window below the header
                val session = sessions.first().session
                val start = java.time.Instant.ofEpochMilli(session.startTime.toEpochMilliseconds())
                    .atZone(zone).toLocalTime()
                val end = java.time.Instant.ofEpochMilli(session.endTime.toEpochMilliseconds())
                    .atZone(zone).toLocalTime()
                Text(
                    text = "${start.format(timeFormatter)} – ${end.format(timeFormatter)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun SleepSessionRow(sleep: SleepSummary) {
    val session = sleep.session
    val zone = com.daysync.app.core.config.UserPreferences(
        androidx.compose.ui.platform.LocalContext.current
    ).javaZoneId
    val start = java.time.Instant.ofEpochMilli(session.startTime.toEpochMilliseconds())
        .atZone(zone).toLocalTime()
    val end = java.time.Instant.ofEpochMilli(session.endTime.toEpochMilliseconds())
        .atZone(zone).toLocalTime()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${start.format(timeFormatter)} – ${end.format(timeFormatter)}",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = formatDuration(session.totalMinutes),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun scoreColor(score: Int): Color = when {
    score >= 80 -> Color(0xFF4CAF50)
    score >= 60 -> Color(0xFFFFC107)
    else -> Color(0xFFEF5350)
}

private fun formatDuration(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}
