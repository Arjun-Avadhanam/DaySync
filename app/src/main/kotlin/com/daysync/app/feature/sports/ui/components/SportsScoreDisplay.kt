package com.daysync.app.feature.sports.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.daysync.app.feature.sports.data.model.ResultDetail

@Composable
fun SportsScoreDisplay(
    detail: ResultDetail?,
    modifier: Modifier = Modifier,
) {
    when (detail) {
        is ResultDetail.Football -> FootballScoreDetail(detail, modifier)
        is ResultDetail.F1 -> F1ResultDetail(detail, modifier)
        is ResultDetail.Mma -> MmaFightDetail(detail, modifier)
        is ResultDetail.Tennis -> TennisMatchDetail(detail, modifier)
        is ResultDetail.Basketball -> BasketballGameDetail(detail, modifier)
        is ResultDetail.Unknown -> {}
        null -> {}
    }
}

@Composable
private fun FootballScoreDetail(detail: ResultDetail.Football, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(16.dp)) {
        Text(
            text = "Match Details",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (detail.halftimeHome != null) {
            ScoreRow("Half Time", "${detail.halftimeHome} - ${detail.halftimeAway}")
        }
        ScoreRow("Full Time", "${detail.fulltimeHome} - ${detail.fulltimeAway}")
        if (detail.extratimeHome != null) {
            ScoreRow("Extra Time", "${detail.extratimeHome} - ${detail.extratimeAway}")
        }
        if (detail.penaltiesHome != null) {
            ScoreRow("Penalties", "${detail.penaltiesHome} - ${detail.penaltiesAway}")
        }
        detail.elapsed?.let {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$it'",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun F1ResultDetail(detail: ResultDetail.F1, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(16.dp)) {
        Text(
            text = "Race Result",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Circuit info
        detail.circuit?.let {
            val location = listOfNotNull(detail.circuitCity, detail.circuitCountry).joinToString(", ")
            ScoreRow("Circuit", if (location.isNotEmpty()) "$it, $location" else it)
        }

        detail.totalLaps?.let { ScoreRow("Laps", it) }

        // Winner
        if (detail.winner != null) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            ScoreRow("Winner", detail.winner)
            detail.winnerTeam?.takeIf { it.isNotBlank() }?.let { ScoreRow("Team", it) }
            detail.winnerTime?.let { ScoreRow("Time", it) }
        }

        // Pole position
        if (detail.poleDriver != null) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            ScoreRow("Pole Position", detail.poleDriver)
            detail.poleTeam?.takeIf { it.isNotBlank() }?.let { ScoreRow("Team", it) }
            detail.poleTime?.let { ScoreRow("Qualifying Time", it) }
        }

        // Fastest lap
        if (detail.fastestLapDriver != null) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            val lapInfo = buildString {
                append(detail.fastestLapDriver)
                if (detail.fastestLapTime != null) append(" (${detail.fastestLapTime})")
                if (detail.fastestLapNumber != null) append(" Lap ${detail.fastestLapNumber}")
            }
            ScoreRow("Fastest Lap", lapInfo)
        }

        // Stats
        if (detail.finishers != null || detail.retirements != null) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            detail.finishers?.let { ScoreRow("Finishers", "$it") }
            detail.retirements?.let { ScoreRow("Retirements/DNF", "$it") }
        }
    }
}

@Composable
private fun MmaFightDetail(detail: ResultDetail.Mma, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(16.dp)) {
        Text(
            text = "Fight Details",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(8.dp))

        detail.cardName?.let { ScoreRow("Card", it) }
        detail.weightClass?.let { ScoreRow("Weight Class", it) }
        detail.scheduledRounds?.let { ScoreRow("Rounds", "$it") }

        if (detail.isChampionship) {
            ScoreRow("Type", "Championship")
        } else if (detail.isMainEvent) {
            ScoreRow("Type", "Main Event")
        }

        if (detail.fighter1Record != null || detail.fighter2Record != null) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            detail.fighter1Record?.let { ScoreRow("Fighter 1 Record", it) }
            detail.fighter2Record?.let { ScoreRow("Fighter 2 Record", it) }
        }

        if (detail.winner != null || detail.method != null) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            detail.winner?.let { ScoreRow("Winner", it) }
            detail.method?.let { ScoreRow("Method", it) }
            if (detail.endedRound != null) {
                val roundStr = "R${detail.endedRound}" + (detail.endedTime?.let { " $it" } ?: "")
                ScoreRow("Ended", roundStr)
            }
        }

        if (detail.currentRound != null) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            val liveStr = "R${detail.currentRound}" + (detail.roundTime?.let { " $it" } ?: "")
            ScoreRow("Live", liveStr)
        }
    }
}

@Composable
private fun TennisMatchDetail(detail: ResultDetail.Tennis, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(16.dp)) {
        Text(
            text = "Match Details",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(8.dp))

        detail.tournament?.let { ScoreRow("Tournament", it) }
        if (detail.isGrandSlam) {
            ScoreRow("Type", "Grand Slam")
        }
        detail.draw?.let { ScoreRow("Draw", it) }
        detail.round?.let { ScoreRow("Round", it) }
        detail.court?.let { ScoreRow("Court", it) }

        // Player ranks
        if (detail.player1Rank != null || detail.player2Rank != null) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            detail.player1Rank?.let { ScoreRow("Player 1 Rank", "#$it") }
            detail.player2Rank?.let { ScoreRow("Player 2 Rank", "#$it") }
        }

        // Set scores
        if (detail.player1Sets.isNotEmpty()) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            val setsDisplay = detail.player1Sets.zip(detail.player2Sets).mapIndexed { i, (s1, s2) ->
                val tb = detail.tiebreaks.getOrNull(i)
                if (tb != null) "$s1-$s2 (${tb[0]}-${tb[1]})"
                else "$s1-$s2"
            }.joinToString(", ")
            ScoreRow("Score", setsDisplay)
        }

        // Winner
        if (detail.winner != null) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            ScoreRow("Winner", detail.winner)
        }

        // Live set
        detail.currentSet?.let {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            ScoreRow("Live", "Set $it")
        }
    }
}

@Composable
private fun BasketballGameDetail(detail: ResultDetail.Basketball, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(16.dp)) {
        Text(
            text = "Game Details",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Playoff info
        if (detail.isPostseason) {
            detail.playoffLabel?.let { ScoreRow("Round", it) }
            detail.seriesSummary?.let { ScoreRow("Series", it) }
        }

        // Records
        if (detail.homeRecord != null || detail.awayRecord != null) {
            detail.homeRecord?.let { ScoreRow("Home Record", it) }
            detail.awayRecord?.let { ScoreRow("Away Record", it) }
        }

        // Quarter breakdown
        if (detail.homeQuarters.isNotEmpty()) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            val labels = detail.homeQuarters.mapIndexed { i, _ ->
                if (i < 4) "Q${i + 1}" else "OT${i - 3}"
            }
            ScoreRow("", labels.joinToString("  "))
            ScoreRow("Home", detail.homeQuarters.joinToString("  ") { "$it".padStart(3) })
            ScoreRow("Away", detail.awayQuarters.joinToString("  ") { "$it".padStart(3) })
        }

        // Live info
        if (detail.currentPeriod != null) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            val periodName = when {
                detail.currentPeriod <= 4 -> "Q${detail.currentPeriod}"
                else -> "OT${detail.currentPeriod - 4}"
            }
            val liveStr = periodName + (detail.gameClock?.let { " - $it" } ?: "")
            ScoreRow("Live", liveStr)
        }

        // Venue
        detail.venue?.let {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            ScoreRow("Venue", it)
        }
    }
}

@Composable
private fun ScoreRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
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
