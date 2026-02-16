package com.daysync.app.feature.health.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.daysync.app.feature.health.model.SleepSummary

private val DeepSleepColor = Color(0xFF1A237E)
private val LightSleepColor = Color(0xFF5C6BC0)
private val RemSleepColor = Color(0xFF7E57C2)
private val AwakeColor = Color(0xFFEF5350)

@Composable
fun SleepCard(
    sleep: SleepSummary,
    modifier: Modifier = Modifier,
) {
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
                        text = "Last Night's Sleep",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    sleep.session.score?.let { score ->
                        Text(
                            text = "Score: $score",
                            style = MaterialTheme.typography.bodySmall,
                            color = scoreColor(score),
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
                Text(
                    text = formatDuration(sleep.session.totalMinutes),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            // Stage breakdown bar
            if (sleep.session.totalMinutes > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp)),
                ) {
                    if (sleep.deepPercent > 0) {
                        Box(
                            modifier = Modifier
                                .weight(sleep.deepPercent.toFloat())
                                .height(12.dp)
                                .background(DeepSleepColor),
                        )
                    }
                    if (sleep.lightPercent > 0) {
                        Box(
                            modifier = Modifier
                                .weight(sleep.lightPercent.toFloat())
                                .height(12.dp)
                                .background(LightSleepColor),
                        )
                    }
                    if (sleep.remPercent > 0) {
                        Box(
                            modifier = Modifier
                                .weight(sleep.remPercent.toFloat())
                                .height(12.dp)
                                .background(RemSleepColor),
                        )
                    }
                    if (sleep.awakePercent > 0) {
                        Box(
                            modifier = Modifier
                                .weight(sleep.awakePercent.toFloat())
                                .height(12.dp)
                                .background(AwakeColor),
                        )
                    }
                }

                // Legend
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    SleepLegend("Deep", formatDuration(sleep.session.deepMinutes), DeepSleepColor)
                    SleepLegend("Light", formatDuration(sleep.session.lightMinutes), LightSleepColor)
                    SleepLegend("REM", formatDuration(sleep.session.remMinutes), RemSleepColor)
                    SleepLegend("Awake", formatDuration(sleep.session.awakeMinutes), AwakeColor)
                }
            }
        }
    }
}

@Composable
private fun SleepLegend(label: String, duration: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape),
        )
        Column(modifier = Modifier.padding(start = 4.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelSmall)
            Text(
                text = duration,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
            )
        }
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
