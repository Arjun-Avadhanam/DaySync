package com.daysync.app.feature.health.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.graphics.Color
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
    onWeightChange: (Double?, Double?, Double?) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier,
) {
    var showCalorieDialog by remember { mutableStateOf(false) }
    var showWeightDialog by remember { mutableStateOf(false) }

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

            // Calorie deficit/surplus
            val burned = summary.totalCalories
            val consumed = summary.caloriesConsumed
            if (burned != null && consumed != null) {
                val deficit = burned - consumed
                val sign = if (deficit >= 0) "+" else ""
                val label = if (deficit >= 0) "Deficit" else "Surplus"
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Calorie $label",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = "$sign${deficit.toInt()} kcal",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (deficit >= 0) {
                            Color(0xFF4CAF50) // green for deficit
                        } else {
                            Color(0xFFEF5350) // red for surplus
                        },
                    )
                }
            }

            // Weight tracking (morning / evening / night)
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showWeightDialog = true },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Weight",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                val weightText = listOfNotNull(
                    summary.weightMorning?.let { "M: ${it}kg" },
                    summary.weightEvening?.let { "E: ${it}kg" },
                    summary.weightNight?.let { "N: ${it}kg" },
                ).joinToString("  ")
                Text(
                    text = weightText.ifEmpty { "Tap to set" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
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

    if (showWeightDialog) {
        WeightDialog(
            morning = summary.weightMorning,
            evening = summary.weightEvening,
            night = summary.weightNight,
            onDismiss = { showWeightDialog = false },
            onSave = { m, e, n ->
                onWeightChange(m, e, n)
                showWeightDialog = false
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
private fun WeightDialog(
    morning: Double?,
    evening: Double?,
    night: Double?,
    onDismiss: () -> Unit,
    onSave: (Double?, Double?, Double?) -> Unit,
) {
    var morningText by remember { mutableStateOf(morning?.toString().orEmpty()) }
    var eveningText by remember { mutableStateOf(evening?.toString().orEmpty()) }
    var nightText by remember { mutableStateOf(night?.toString().orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Weight") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = morningText,
                    onValueChange = { morningText = it },
                    label = { Text("Morning (kg)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = eveningText,
                    onValueChange = { eveningText = it },
                    label = { Text("Evening (kg)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = nightText,
                    onValueChange = { nightText = it },
                    label = { Text("Night (kg)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    morningText.toDoubleOrNull(),
                    eveningText.toDoubleOrNull(),
                    nightText.toDoubleOrNull(),
                )
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
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
