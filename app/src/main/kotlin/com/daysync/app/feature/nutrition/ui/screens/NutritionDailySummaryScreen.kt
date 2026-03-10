package com.daysync.app.feature.nutrition.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.daysync.app.feature.nutrition.ui.components.MacroBreakdownChart
import com.daysync.app.feature.nutrition.ui.components.WaterTracker
import com.daysync.app.feature.nutrition.ui.viewmodel.NutritionDailyTrackerState
import com.daysync.app.feature.nutrition.ui.viewmodel.NutritionDailyTrackerViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionDailySummaryScreen(
    date: String,
    onNavigateBack: () -> Unit,
    viewModel: NutritionDailyTrackerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daily Summary") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { paddingValues ->
        when (val s = state) {
            is NutritionDailyTrackerState.Success -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    // Macro breakdown chart
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            MacroBreakdownChart(
                                protein = s.totalProtein,
                                carbs = s.totalCarbs,
                                fat = s.totalFat,
                                modifier = Modifier.size(120.dp),
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "${s.totalCalories.roundToInt()} calories",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }

                    // Individual macro bars
                    MacroProgressBar("Protein", s.totalProtein, Color(0xFF4CAF50))
                    MacroProgressBar("Carbs", s.totalCarbs, Color(0xFF2196F3))
                    MacroProgressBar("Fat", s.totalFat, Color(0xFFFF9800))
                    MacroProgressBar("Sugar", s.totalSugar, Color(0xFFE91E63))

                    // Water
                    WaterTracker(
                        waterLiters = s.summary?.waterLiters ?: 0.0,
                        onUpdateWater = viewModel::updateWater,
                    )

                    // Mood
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Mood",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                            ) {
                                val moods = listOf("Great", "Good", "Okay", "Low", "Bad")
                                moods.forEach { mood ->
                                    val isSelected = s.summary?.mood == mood
                                    androidx.compose.material3.FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.updateMood(mood) },
                                        label = { Text(mood, style = MaterialTheme.typography.labelSmall) },
                                    )
                                }
                            }
                        }
                    }

                    // Notes
                    var notes by remember(s.summary) {
                        mutableStateOf(s.summary?.notes ?: "")
                    }
                    OutlinedTextField(
                        value = notes,
                        onValueChange = {
                            notes = it
                            viewModel.updateNotes(it)
                        },
                        label = { Text("Notes") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            else -> {}
        }
    }
}

@Composable
private fun MacroProgressBar(
    label: String,
    value: Double,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "${value.roundToInt()}g",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { (value / 200.0).toFloat().coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = color,
            trackColor = color.copy(alpha = 0.15f),
        )
    }
}
