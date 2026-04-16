package com.daysync.app.feature.sports.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.daysync.app.core.database.entity.EventParticipantEntity
import com.daysync.app.core.ui.LoadingIndicator
import com.daysync.app.feature.sports.data.model.ResultDetail
import com.daysync.app.feature.sports.data.model.SportEventWithDetails
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import com.daysync.app.feature.sports.ui.components.SportsLiveBadge
import com.daysync.app.feature.sports.ui.components.SportsScoreDisplay
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private val detailJson = Json { ignoreUnknownKeys = true }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SportsEventDetailScreen(
    event: SportEventWithDetails?,
    participants: List<EventParticipantEntity> = emptyList(),
    competitorNames: Map<String, String> = emptyMap(),
    watchnotes: String? = null,
    onBack: () -> Unit,
    onWatchlistToggle: (String) -> Unit,
    onWatchnotesChange: (String, String?) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(event?.competitionName ?: "Event Details") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                if (event != null) {
                    IconButton(onClick = { onWatchlistToggle(event.id) }) {
                        Icon(
                            imageVector = if (event.isWatchlisted) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Toggle watchlist",
                            tint = if (event.isWatchlisted) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
        )

        if (event == null) {
            LoadingIndicator()
            return
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            // Competition + status header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = event.competitionName,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    event.round?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (event.status == "LIVE") {
                    SportsLiveBadge()
                } else {
                    StatusChip(event.status)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Score display
            if (event.homeCompetitorName != null && event.awayCompetitorName != null) {
                TeamMatchupDetail(event)
            } else {
                // Generic event (F1, MMA, Tennis)
                Text(
                    text = event.eventName ?: "Event",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Date/time
            val tz = TimeZone.of("Asia/Kolkata")
            val local = event.scheduledAt.toLocalDateTime(tz)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    InfoRow("Date", "${local.date.day} ${local.month.name.take(3)} ${local.year}")
                    InfoRow("Time", "%02d:%02d IST".format(local.hour, local.minute))
                    event.season?.let {
                        val label = when (event.sportId) {
                            "mma" -> "Card"
                            "tennis" -> "Tournament"
                            "f1" -> "Season"
                            else -> "Season"
                        }
                        InfoRow(label, it)
                    }
                    if (event.sportId == "f1") {
                        event.round?.let { InfoRow("Round", it) }
                    }
                    event.dataSource?.let { InfoRow("Source", it) }
                }
            }

            // Sport-specific result breakdown
            val resultDetail = ResultDetail.parse(event.resultDetail, event.sportId)
            if (resultDetail != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                ) {
                    SportsScoreDisplay(detail = resultDetail)
                }
            }

            // Participants grid (F1 drivers, etc.)
            val raceParticipants = participants
                .filter { it.status != "qualifying" }
                .sortedBy { it.position ?: Int.MAX_VALUE }
            if (raceParticipants.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (event.status == "COMPLETED") "Race Results" else "Grid",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        raceParticipants.forEach { p ->
                            val detail = p.detail?.let {
                                try { detailJson.decodeFromString<JsonObject>(it) } catch (_: Exception) { null }
                            }
                            val constructor = detail?.get("constructor")?.jsonPrimitive?.content ?: ""
                            val grid = detail?.get("grid")?.jsonPrimitive?.content
                            val laps = detail?.get("laps")?.jsonPrimitive?.content
                            val points = detail?.get("points")?.jsonPrimitive?.content

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                // Position
                                Text(
                                    text = "P${p.position ?: "-"}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (p.isWinner) FontWeight.Bold else FontWeight.Normal,
                                    color = if (p.isWinner) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.width(36.dp),
                                )
                                // Driver + constructor
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = competitorNames[p.competitorId] ?: p.competitorId,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    if (constructor.isNotBlank()) {
                                        Text(
                                            text = constructor,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                // Time/status + grid
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = p.score ?: p.status ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                    )
                                    if (grid != null && event.status == "COMPLETED") {
                                        Text(
                                            text = "Grid: $grid",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    if (points != null && points != "0") {
                                        Text(
                                            text = "${points}pts",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Watchnotes — user-editable match notes, synced to cloud
            Spacer(modifier = Modifier.height(16.dp))
            WatchnotesSection(
                eventId = event.id,
                currentNotes = watchnotes,
                onSave = onWatchnotesChange,
            )
        }
    }
}

@Composable
private fun WatchnotesSection(
    eventId: String,
    currentNotes: String?,
    onSave: (String, String?) -> Unit,
) {
    // Local draft state — only commits to repo on blur/explicit save to avoid
    // thrashing Room on every keystroke. Re-sync when source of truth changes.
    var draft by remember(eventId, currentNotes) {
        mutableStateOf(currentNotes.orEmpty())
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Watchnotes",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = draft,
                onValueChange = { new ->
                    // Soft-cap at 1000 chars to keep sync payloads sane
                    draft = if (new.length > 1000) new.take(1000) else new
                },
                placeholder = { Text("Add your thoughts on this match…") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 10,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${draft.length}/1000",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                androidx.compose.material3.TextButton(
                    onClick = { onSave(eventId, draft) },
                    enabled = draft != (currentNotes.orEmpty()),
                ) { Text("Save") }
            }
        }
    }
}

@Composable
private fun TeamMatchupDetail(event: SportEventWithDetails) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Home
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f),
            ) {
                if (event.homeCompetitorLogo != null) {
                    AsyncImage(
                        model = event.homeCompetitorLogo,
                        contentDescription = event.homeCompetitorName,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(MaterialTheme.shapes.small),
                        contentScale = ContentScale.Fit,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Text(
                    text = event.homeCompetitorName ?: "",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                )
            }

            // Score — individual sports show W/L or set scores
            val detailForScore = if (event.sportId in listOf("mma", "tennis")) {
                ResultDetail.parse(event.resultDetail, event.sportId)
            } else null

            val scoreText = when {
                detailForScore is ResultDetail.Mma && event.status == "COMPLETED" -> {
                    when (detailForScore.winner) {
                        event.homeCompetitorName -> "W\n-\nL"
                        event.awayCompetitorName -> "L\n-\nW"
                        else -> "vs"
                    }
                }
                detailForScore is ResultDetail.Tennis && (event.status == "COMPLETED" || event.status == "LIVE") -> {
                    if (detailForScore.player1Sets.isNotEmpty()) {
                        detailForScore.player1Sets.zip(detailForScore.player2Sets)
                            .joinToString("\n") { (s1, s2) -> "$s1-$s2" }
                    } else "vs"
                }
                event.status == "COMPLETED" || event.status == "LIVE" -> "${event.homeScore ?: 0}\n-\n${event.awayScore ?: 0}"
                else -> "vs"
            }
            Text(
                text = scoreText,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = if (event.status == "LIVE") MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            // Away
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f),
            ) {
                if (event.awayCompetitorLogo != null) {
                    AsyncImage(
                        model = event.awayCompetitorLogo,
                        contentDescription = event.awayCompetitorName,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(MaterialTheme.shapes.small),
                        contentScale = ContentScale.Fit,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Text(
                    text = event.awayCompetitorName ?: "",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun StatusChip(status: String) {
    val (text, color) = when (status) {
        "COMPLETED" -> "FT" to MaterialTheme.colorScheme.onSurfaceVariant
        "SCHEDULED" -> "Scheduled" to MaterialTheme.colorScheme.primary
        "POSTPONED" -> "Postponed" to MaterialTheme.colorScheme.error
        "CANCELLED" -> "Cancelled" to MaterialTheme.colorScheme.error
        else -> status to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}
