package com.daysync.app.feature.health.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.daysync.app.feature.health.model.WorkoutSubTypes
import com.daysync.app.feature.health.model.WorkoutSummary
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun WorkoutList(
    workouts: List<WorkoutSummary>,
    onSubTypeChange: (sessionId: String, subType: String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (workouts.isEmpty()) return

    var pickerFor by remember { mutableStateOf<WorkoutSummary?>(null) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Recent Workouts",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            workouts.forEachIndexed { index, workout ->
                if (index > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
                val hasPicker = WorkoutSubTypes.optionsFor(workout.session.exerciseType) != null
                WorkoutItem(
                    workout = workout,
                    onClick = if (hasPicker) {
                        { pickerFor = workout }
                    } else {
                        null
                    },
                )
            }
        }
    }

    pickerFor?.let { workout ->
        val options = WorkoutSubTypes.optionsFor(workout.session.exerciseType).orEmpty()
        WorkoutSubTypePickerDialog(
            currentSelection = workout.subType,
            options = options,
            onDismiss = { pickerFor = null },
            onSelect = { raw ->
                onSubTypeChange(workout.session.id, raw)
                pickerFor = null
            },
        )
    }
}

@Composable
private fun WorkoutItem(
    workout: WorkoutSummary,
    onClick: (() -> Unit)?,
) {
    val dateFormatter = DateTimeFormatter.ofPattern("EEE, d MMM")
    val zone = ZoneId.of("Asia/Kolkata")
    val date = java.time.Instant.ofEpochMilli(workout.session.startTime.toEpochMilliseconds())
        .atZone(zone).toLocalDate()

    val baseModifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    val rowModifier = if (onClick != null) baseModifier.clickable { onClick() } else baseModifier

    Row(
        modifier = rowModifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = workout.displayType,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = date.format(dateFormatter),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${workout.durationMinutes} min",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                workout.session.calories?.let {
                    Text(
                        text = "${it.toInt()} kcal",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                workout.session.avgHeartRate?.let {
                    Text(
                        text = "$it bpm",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                workout.session.maxHeartRate?.let {
                    Text(
                        text = "max $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                workout.session.distance?.let {
                    if (it > 0) {
                        Text(
                            text = "%.1f km".format(it / 1000),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkoutSubTypePickerDialog(
    currentSelection: String?,
    options: List<String>,
    onDismiss: () -> Unit,
    onSelect: (String?) -> Unit,
) {
    var selected by remember { mutableStateOf(currentSelection) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sub-type") },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selected == option,
                                onClick = { selected = option },
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selected == option,
                            onClick = { selected = option },
                        )
                        Text(
                            text = prettySubType(option),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSelect(selected) }) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (currentSelection != null) {
                    TextButton(onClick = { onSelect(null) }) { Text("Clear") }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}

private fun prettySubType(raw: String): String = when (raw) {
    WorkoutSubTypes.PUSH -> "Push"
    WorkoutSubTypes.PULL -> "Pull"
    WorkoutSubTypes.OTHER -> "Other"
    WorkoutSubTypes.LEG_EXERCISES -> "Leg exercises"
    else -> raw.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
}
