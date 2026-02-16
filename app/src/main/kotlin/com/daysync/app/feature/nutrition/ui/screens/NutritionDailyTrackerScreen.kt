package com.daysync.app.feature.nutrition.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.daysync.app.feature.nutrition.domain.model.MealTime
import com.daysync.app.feature.nutrition.ui.components.DateNavigator
import com.daysync.app.feature.nutrition.ui.components.MacroSummaryCard
import com.daysync.app.feature.nutrition.ui.components.MealSectionCard
import com.daysync.app.feature.nutrition.ui.components.WaterTracker
import com.daysync.app.feature.nutrition.ui.viewmodel.NutritionDailyTrackerState
import com.daysync.app.feature.nutrition.ui.viewmodel.NutritionDailyTrackerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionDailyTrackerScreen(
    onAddMealEntry: (date: String, mealTime: String) -> Unit,
    onViewSummary: (date: String) -> Unit,
    onNavigateToFoodLibrary: () -> Unit,
    onNavigateToTemplates: () -> Unit,
    onNavigateToHistory: () -> Unit,
    viewModel: NutritionDailyTrackerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentDate by viewModel.currentDate.collectAsStateWithLifecycle()
    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nutrition") },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Food Library") },
                            onClick = {
                                showMenu = false
                                onNavigateToFoodLibrary()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Meal Templates") },
                            onClick = {
                                showMenu = false
                                onNavigateToTemplates()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("History & Trends") },
                            onClick = {
                                showMenu = false
                                onNavigateToHistory()
                            },
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        when (val s = state) {
            is NutritionDailyTrackerState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            is NutritionDailyTrackerState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(s.message)
                }
            }

            is NutritionDailyTrackerState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        DateNavigator(
                            date = s.date,
                            onPreviousDay = viewModel::navigateToPreviousDay,
                            onNextDay = viewModel::navigateToNextDay,
                            onToday = viewModel::navigateToToday,
                        )
                    }

                    item {
                        MacroSummaryCard(
                            totalCalories = s.totalCalories,
                            totalProtein = s.totalProtein,
                            totalCarbs = s.totalCarbs,
                            totalFat = s.totalFat,
                        )
                    }

                    MealTime.entries.forEach { mealTime ->
                        item(key = mealTime.name) {
                            MealSectionCard(
                                mealTime = mealTime,
                                entries = s.entriesByMealTime[mealTime] ?: emptyList(),
                                onAddEntry = {
                                    onAddMealEntry(currentDate.toString(), mealTime.dbValue)
                                },
                                onDeleteEntry = viewModel::deleteMealEntry,
                            )
                        }
                    }

                    item {
                        WaterTracker(
                            waterLiters = s.summary?.waterLiters ?: 0.0,
                            onUpdateWater = viewModel::updateWater,
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            androidx.compose.material3.TextButton(
                                onClick = { onViewSummary(currentDate.toString()) },
                            ) {
                                Text("View Full Summary")
                            }
                        }
                    }
                }
            }
        }
    }
}
