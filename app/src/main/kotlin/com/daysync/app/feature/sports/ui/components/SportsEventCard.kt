package com.daysync.app.feature.sports.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.daysync.app.feature.sports.data.model.ResultDetail
import com.daysync.app.feature.sports.data.model.SportEventWithDetails
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

@Composable
fun SportsEventCard(
    event: SportEventWithDetails,
    onEventClick: (String) -> Unit,
    onWatchlistToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onEventClick(event.id) },
        colors = CardDefaults.cardColors(
            containerColor = if (event.status == "LIVE")
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Competition + round + watchlist star
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = event.competitionShortName ?: event.competitionName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    event.round?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (event.status == "LIVE") {
                        SportsLiveBadge()
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    IconButton(
                        onClick = { onWatchlistToggle(event.id) },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = if (event.isWatchlisted) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = if (event.isWatchlisted) "Remove from watchlist" else "Add to watchlist",
                            tint = if (event.isWatchlisted) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Team matchup
            if (event.homeCompetitorName != null && event.awayCompetitorName != null) {
                TeamMatchupRow(event)
            } else {
                // Generic event name (F1, Tennis, MMA)
                Text(
                    text = event.eventName ?: "Event",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Date/time
            Text(
                text = formatEventTime(event),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TeamMatchupRow(event: SportEventWithDetails) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Home team
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
        ) {
            Text(
                text = event.homeCompetitorName ?: "",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Spacer(modifier = Modifier.width(8.dp))
            if (event.homeCompetitorLogo != null) {
                AsyncImage(
                    model = event.homeCompetitorLogo,
                    contentDescription = event.homeCompetitorName,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(MaterialTheme.shapes.extraSmall),
                    contentScale = ContentScale.Fit,
                )
            }
        }

        // Score or "vs" — for MMA show winner indicator instead of numeric scores
        val mmaDetail = if (event.sportId == "mma") {
            ResultDetail.parse(event.resultDetail, "mma") as? ResultDetail.Mma
        } else null

        val scoreText = when {
            mmaDetail != null && event.status == "COMPLETED" -> {
                when (mmaDetail.winner) {
                    event.homeCompetitorName -> "W - L"
                    event.awayCompetitorName -> "L - W"
                    else -> "vs"
                }
            }
            event.status == "COMPLETED" || event.status == "LIVE" -> "${event.homeScore ?: 0} - ${event.awayScore ?: 0}"
            else -> "vs"
        }
        Text(
            text = scoreText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp),
            color = if (event.status == "LIVE") MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurface,
        )

        // Away team
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (event.awayCompetitorLogo != null) {
                AsyncImage(
                    model = event.awayCompetitorLogo,
                    contentDescription = event.awayCompetitorName,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(MaterialTheme.shapes.extraSmall),
                    contentScale = ContentScale.Fit,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = event.awayCompetitorName ?: "",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
        }
    }
}

private fun formatEventTime(event: SportEventWithDetails): String {
    if (event.status == "LIVE") return "In Progress"
    if (event.status == "COMPLETED") {
        val tz = TimeZone.of("Asia/Kolkata")
        val local = event.scheduledAt.toLocalDateTime(tz)
        return "Completed - ${local.date.day} ${local.month.name.take(3)} ${local.year}"
    }

    val now = Clock.System.now()
    val diff = event.scheduledAt - now
    val tz = TimeZone.of("Asia/Kolkata")
    val local = event.scheduledAt.toLocalDateTime(tz)
    val timeStr = "%02d:%02d".format(local.hour, local.minute)

    return when {
        diff < 0.days -> "Started"
        diff < 1.days -> "Today $timeStr"
        diff < 2.days -> "Tomorrow $timeStr"
        diff < 7.days -> "${local.dayOfWeek.name.take(3)} $timeStr"
        else -> "${local.date.day} ${local.month.name.take(3)} $timeStr"
    }
}
