package com.daysync.app.feature.nutrition.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.daysync.app.feature.nutrition.ui.viewmodel.HistoryRange
import com.daysync.app.feature.nutrition.ui.viewmodel.NutritionHistoryViewModel
import com.daysync.app.feature.nutrition.ui.viewmodel.NutritionPeriod
import com.daysync.app.feature.nutrition.ui.util.fmtNutrition

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionHistoryScreen(
    onNavigateBack: () -> Unit,
    viewModel: NutritionHistoryViewModel = hiltViewModel(),
) {
    val period by viewModel.period.collectAsStateWithLifecycle()
    val summaries by viewModel.summaries.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History & Trends") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Range selector
            com.daysync.app.core.ui.DateRangeSelector(
                presets = HistoryRange.entries.map {
                    com.daysync.app.core.ui.PeriodPreset(it.label, it.days)
                },
                selectedPresetIndex = when (val p = period) {
                    is NutritionPeriod.Preset -> p.range.ordinal
                    is NutritionPeriod.Custom -> -1
                },
                onPresetSelected = { index -> viewModel.setRange(HistoryRange.entries[index]) },
                onCustomRangeSelected = { start, end -> viewModel.setCustomRange(start, end) },
                customRangeLabel = (period as? NutritionPeriod.Custom)?.let {
                    com.daysync.app.core.ui.formatRangeLabel(it.start, it.end)
                },
            )

            if (summaries.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "No data for this period",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    )
                }
            } else {
                // Averages card
                val avgCalories = summaries.map { it.totalCalories }.average()
                val avgProtein = summaries.map { it.totalProtein }.average()
                val avgCarbs = summaries.map { it.totalCarbs }.average()
                val avgFat = summaries.map { it.totalFat }.average()
                val avgWater = summaries.map { it.waterLiters }.average()

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Daily Averages",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            AverageItem("Calories", "${avgCalories.fmtNutrition()}")
                            AverageItem("Protein", "${avgProtein.fmtNutrition()}g")
                            AverageItem("Carbs", "${avgCarbs.fmtNutrition()}g")
                            AverageItem("Fat", "${avgFat.fmtNutrition()}g")
                            AverageItem("Water", String.format("%.2fL", avgWater))
                        }
                    }
                }

                // Daily summary cards
                Text(
                    text = "Daily Breakdown",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )

                summaries.reversed().forEach { summary ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(
                                    text = formatHistoryDate(summary.date.toString()),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                )
                                Text(
                                    text = "P: ${summary.totalProtein.fmtNutrition()}g  C: ${summary.totalCarbs.fmtNutrition()}g  F: ${summary.totalFat.fmtNutrition()}g",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                )
                            }
                            Text(
                                text = "${summary.totalCalories.fmtNutrition()} cal",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AverageItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
        )
    }
}

private fun formatHistoryDate(isoDate: String): String {
    val parts = isoDate.split("-")
    if (parts.size != 3) return isoDate
    val months = arrayOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )
    val monthIndex = parts[1].toIntOrNull()?.minus(1) ?: return isoDate
    return "${parts[2].toInt()} ${months[monthIndex]}"
}
