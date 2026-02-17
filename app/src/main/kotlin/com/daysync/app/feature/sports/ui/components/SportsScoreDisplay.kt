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
        is ResultDetail.Unknown -> {
            Text(
                text = detail.raw,
                style = MaterialTheme.typography.bodySmall,
                modifier = modifier.padding(16.dp),
            )
        }
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

        detail.winner?.let {
            ScoreRow("Winner", it)
        }
        detail.winnerTeam?.takeIf { it.isNotBlank() }?.let {
            ScoreRow("Team", it)
        }
        detail.winnerTime?.let {
            ScoreRow("Time", it)
        }
        detail.totalLaps?.let {
            ScoreRow("Laps", it)
        }

        if (detail.fastestLapDriver != null) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            ScoreRow("Fastest Lap", "${detail.fastestLapDriver} (${detail.fastestLapTime ?: ""})")
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
