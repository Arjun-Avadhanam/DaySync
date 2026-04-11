package com.daysync.app.feature.health.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.daysync.app.feature.health.model.HealthDailySummary
import java.text.NumberFormat

@Composable
fun HealthSummaryCard(
    summary: HealthDailySummary,
    onCaloriesOverride: (Double?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showCalorieDialog by remember { mutableStateOf(false) }

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
                    label = if (summary.totalCaloriesOverridden) "Calories (manual)" else "Calories",
                    value = summary.totalCalories?.let { "${it.toInt()} kcal" } ?: "tap to set",
                    italic = summary.totalCaloriesOverridden,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showCalorieDialog = true },
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

    if (showCalorieDialog) {
        CalorieOverrideDialog(
            currentValue = summary.totalCalories,
            isOverridden = summary.totalCaloriesOverridden,
            onDismiss = { showCalorieDialog = false },
            onSave = { newValue ->
                onCaloriesOverride(newValue)
                showCalorieDialog = false
            },
            onClear = {
                onCaloriesOverride(null)
                showCalorieDialog = false
            },
        )
    }
}

@Composable
private fun CalorieOverrideDialog(
    currentValue: Double?,
    isOverridden: Boolean,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit,
    onClear: () -> Unit,
) {
    var text by remember { mutableStateOf(currentValue?.toInt()?.toString().orEmpty()) }
    val parsed = text.toIntOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Override today's calories") },
        text = {
            Column {
                Text(
                    "Enter the value you'd like to record for today. Leave empty or clear to fall back to Health Connect's reading.",
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.filter { ch -> ch.isDigit() }.take(6) },
                    label = { Text("Calories (kcal)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { parsed?.let { onSave(it.toDouble()) } },
                enabled = parsed != null && parsed > 0,
            ) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (isOverridden) {
                    TextButton(onClick = onClear) { Text("Clear") }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    italic: Boolean = false,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
        )
    }
}
