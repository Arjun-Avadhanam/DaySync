package com.daysync.app.feature.health.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.hilt.navigation.compose.hiltViewModel
import com.daysync.app.feature.health.model.HealthPeriod
import com.daysync.app.feature.health.model.HealthUiState
import com.daysync.app.feature.health.ui.components.HealthConnectNotAvailableCard
import com.daysync.app.feature.health.ui.components.HealthSummaryCard
import com.daysync.app.feature.health.ui.components.HeartRateTrendChart
import com.daysync.app.feature.health.ui.components.PermissionCard
import com.daysync.app.feature.health.ui.components.SleepCard
import com.daysync.app.feature.health.ui.components.SleepTrendChart
import com.daysync.app.feature.health.ui.components.StepsTrendChart
import com.daysync.app.feature.health.ui.components.WorkoutList
import com.daysync.app.feature.health.ui.components.WorkoutTrendChart
import com.daysync.app.feature.nutrition.ui.components.DateNavigator
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthScreen(
    modifier: Modifier = Modifier,
    viewModel: HealthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
    ) { viewModel.onPermissionsGranted() }

    LaunchedEffect(Unit) {
        viewModel.checkAvailabilityAndLoad()
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refresh() },
        modifier = modifier.fillMaxSize(),
    ) {
        when (val state = uiState) {
            is HealthUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is HealthUiState.PermissionRequired -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    PermissionCard(
                        onRequestPermissions = {
                            permissionLauncher.launch(viewModel.healthPermissions)
                        },
                    )
                }
            }

            is HealthUiState.HealthConnectNotAvailable -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    HealthConnectNotAvailableCard()
                }
            }

            is HealthUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }

            is HealthUiState.Success -> {
                HealthDashboard(
                    state = state,
                    selectedDate = selectedDate,
                    selectedPeriod = selectedPeriod,
                    onPreviousDay = viewModel::navigateToPreviousDay,
                    onNextDay = viewModel::navigateToNextDay,
                    onToday = viewModel::navigateToToday,
                    onPeriodSelected = viewModel::onPeriodSelected,
                    onCaloriesOverride = viewModel::setCalorieOverride,
                    onWorkoutSubTypeChange = viewModel::setWorkoutSubType,
                    onWorkoutTypeSelected = viewModel::selectWorkoutType,
                    onWorkoutSubTypeFilterSelected = viewModel::selectWorkoutSubType,
                )
            }
        }
    }
}

@Composable
private fun HealthDashboard(
    state: HealthUiState.Success,
    selectedDate: LocalDate,
    selectedPeriod: HealthPeriod,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onToday: () -> Unit,
    onPeriodSelected: (HealthPeriod) -> Unit,
    onCaloriesOverride: (Double?) -> Unit,
    onWorkoutSubTypeChange: (String, String?) -> Unit,
    onWorkoutTypeSelected: (String?) -> Unit,
    onWorkoutSubTypeFilterSelected: (String?) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Date navigator (arrows + "Today" button)
        DateNavigator(
            date = selectedDate,
            onPreviousDay = onPreviousDay,
            onNextDay = onNextDay,
            onToday = onToday,
        )

        // Daily summary for the selected date
        HealthSummaryCard(
            summary = state.dailySummary,
            onCaloriesOverride = onCaloriesOverride,
        )

        // Sleep sessions for the selected date
        if (state.sleepSessions.isNotEmpty()) {
            SleepCard(sessions = state.sleepSessions)
        }

        // Workouts for the selected date
        WorkoutList(
            workouts = state.recentWorkouts,
            onSubTypeChange = onWorkoutSubTypeChange,
        )

        // Period selector (charts are independent of selectedDate)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HealthPeriod.entries.forEach { period ->
                FilterChip(
                    selected = period == selectedPeriod,
                    onClick = { onPeriodSelected(period) },
                    label = { Text(period.label) },
                )
            }
        }

        // Trend charts
        StepsTrendChart(data = state.stepsTrend)
        HeartRateTrendChart(data = state.heartRateTrend)
        SleepTrendChart(data = state.sleepTrend)
        WorkoutTrendChart(data = state.workoutTrend, title = "Workout Trend")

        // Per-type workout chart with type + sub-type filter
        WorkoutTypeSection(
            selectedType = state.selectedWorkoutType,
            selectedSubType = state.selectedWorkoutSubType,
            typeTrend = state.workoutTypeTrend,
            onTypeSelected = onWorkoutTypeSelected,
            onSubTypeSelected = onWorkoutSubTypeFilterSelected,
        )
    }
}

@Composable
private fun WorkoutTypeSection(
    selectedType: String?,
    selectedSubType: String?,
    typeTrend: List<com.daysync.app.feature.health.model.WorkoutTrendPoint>,
    onTypeSelected: (String?) -> Unit,
    onSubTypeSelected: (String?) -> Unit,
) {
    val workoutTypes = listOf(
        "EXERCISE_TYPE_RUNNING" to "Running",
        "EXERCISE_TYPE_STRENGTH_TRAINING" to "Strength training",
        "EXERCISE_TYPE_FOOTBALL_AUSTRALIAN" to "Football",
        "EXERCISE_TYPE_OTHER_WORKOUT" to "Workout (Other)",
        "EXERCISE_TYPE_WALKING" to "Walking",
        "EXERCISE_TYPE_BIKING" to "Biking",
        "EXERCISE_TYPE_HIKING" to "Hiking",
        "EXERCISE_TYPE_YOGA" to "Yoga",
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Workout by Type",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            workoutTypes.forEach { (type, label) ->
                FilterChip(
                    selected = selectedType == type,
                    onClick = {
                        onTypeSelected(if (selectedType == type) null else type)
                    },
                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                )
            }
        }

        // Sub-type chips for strength training and other workouts
        val subTypeOptions = com.daysync.app.feature.health.model.WorkoutSubTypes.optionsFor(selectedType ?: "")
        if (selectedType != null && subTypeOptions != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = selectedSubType == null,
                    onClick = { onSubTypeSelected(null) },
                    label = { Text("All", style = MaterialTheme.typography.labelSmall) },
                )
                subTypeOptions.forEach { subType ->
                    val label = when (subType) {
                        com.daysync.app.feature.health.model.WorkoutSubTypes.PUSH -> "Push"
                        com.daysync.app.feature.health.model.WorkoutSubTypes.PULL -> "Pull"
                        com.daysync.app.feature.health.model.WorkoutSubTypes.LEG_EXERCISES -> "Leg exercises"
                        com.daysync.app.feature.health.model.WorkoutSubTypes.OTHER -> "Other"
                        else -> subType
                    }
                    FilterChip(
                        selected = selectedSubType == subType,
                        onClick = {
                            onSubTypeSelected(if (selectedSubType == subType) null else subType)
                        },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }
        }

        if (selectedType != null) {
            WorkoutTrendChart(
                data = typeTrend,
                title = workoutTypes.firstOrNull { it.first == selectedType }?.second ?: "Workout",
            )
        }
    }
}
