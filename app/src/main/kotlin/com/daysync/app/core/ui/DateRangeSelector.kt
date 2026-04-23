package com.daysync.app.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

data class PeriodPreset(val label: String, val days: Int)

/**
 * A row of period preset chips (e.g. "7 Days", "30 Days") plus a "Custom"
 * chip that opens a Material 3 DateRangePicker dialog.
 *
 * @param presets List of preset periods to show as chips
 * @param selectedPresetIndex Index of the currently selected preset, or -1 if custom range is active
 * @param onPresetSelected Called when a preset chip is tapped, with its index
 * @param onCustomRangeSelected Called when the user confirms a custom date range
 * @param customRangeLabel Label to show on the Custom chip when a range is active (e.g. "Apr 1–15")
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangeSelector(
    presets: List<PeriodPreset>,
    selectedPresetIndex: Int,
    onPresetSelected: (Int) -> Unit,
    onCustomRangeSelected: (LocalDate, LocalDate) -> Unit,
    customRangeLabel: String? = null,
    modifier: Modifier = Modifier,
) {
    var showPicker by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        presets.forEachIndexed { index, preset ->
            FilterChip(
                selected = index == selectedPresetIndex,
                onClick = { onPresetSelected(index) },
                label = { Text(preset.label) },
            )
        }
        FilterChip(
            selected = selectedPresetIndex == -1,
            onClick = { showPicker = true },
            label = { Text(customRangeLabel ?: "Custom") },
        )
    }

    if (showPicker) {
        val pickerState = rememberDateRangePickerState()

        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val startMillis = pickerState.selectedStartDateMillis
                        val endMillis = pickerState.selectedEndDateMillis
                        if (startMillis != null && endMillis != null) {
                            val tz = TimeZone.of("Asia/Kolkata")
                            val start = Instant.fromEpochMilliseconds(startMillis)
                                .toLocalDateTime(tz).date
                            val end = Instant.fromEpochMilliseconds(endMillis)
                                .toLocalDateTime(tz).date
                            onCustomRangeSelected(start, end)
                        }
                        showPicker = false
                    },
                    enabled = pickerState.selectedStartDateMillis != null &&
                        pickerState.selectedEndDateMillis != null,
                ) { Text("Apply") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Cancel") }
            },
        ) {
            DateRangePicker(
                state = pickerState,
                title = { Text("Select date range", modifier = Modifier.fillMaxWidth()) },
            )
        }
    }
}

/**
 * Formats a date range as a short label, e.g. "Apr 1 – 15" or "Mar 25 – Apr 3".
 */
fun formatRangeLabel(start: LocalDate, end: LocalDate): String {
    val startMonth = start.month.name.take(3).lowercase()
        .replaceFirstChar { it.uppercase() }
    val endMonth = end.month.name.take(3).lowercase()
        .replaceFirstChar { it.uppercase() }
    return if (start.month == end.month && start.year == end.year) {
        "$startMonth ${start.dayOfMonth}–${end.dayOfMonth}"
    } else {
        "$startMonth ${start.dayOfMonth} – $endMonth ${end.dayOfMonth}"
    }
}
