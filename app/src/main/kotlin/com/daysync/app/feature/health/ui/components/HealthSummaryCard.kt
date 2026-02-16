package com.daysync.app.feature.health.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.daysync.app.feature.health.model.HealthDailySummary
import java.text.NumberFormat

@Composable
fun HealthSummaryCard(
    summary: HealthDailySummary,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Today's Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                MetricItem(
                    label = "Steps",
                    value = summary.steps?.let { NumberFormat.getIntegerInstance().format(it) } ?: "--",
                    modifier = Modifier.weight(1f),
                )
                MetricItem(
                    label = "Calories",
                    value = summary.activeCalories?.let { "${it.toInt()} kcal" } ?: "--",
                    modifier = Modifier.weight(1f),
                )
                MetricItem(
                    label = "Avg HR",
                    value = summary.avgHeartRate?.let { "$it bpm" } ?: "--",
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                MetricItem(
                    label = "Resting HR",
                    value = summary.restingHeartRate?.let { "${it.toInt()} bpm" } ?: "--",
                    modifier = Modifier.weight(1f),
                )
                MetricItem(
                    label = "SpO2",
                    value = summary.spo2?.let { "${it.toInt()}%" } ?: "--",
                    modifier = Modifier.weight(1f),
                )
                MetricItem(
                    label = "Floors",
                    value = summary.floorsClimbed?.let { "${it.toInt()}" } ?: "--",
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
        )
    }
}
