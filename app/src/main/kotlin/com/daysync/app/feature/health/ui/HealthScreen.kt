package com.daysync.app.feature.health.ui

import androidx.activity.compose.rememberLauncherForActivityResult
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthScreen(
    modifier: Modifier = Modifier,
    viewModel: HealthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
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
                    selectedPeriod = selectedPeriod,
                    onPeriodSelected = viewModel::onPeriodSelected,
                )
            }
        }
    }
}

@Composable
private fun HealthDashboard(
    state: HealthUiState.Success,
    selectedPeriod: HealthPeriod,
    onPeriodSelected: (HealthPeriod) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Today's summary
        HealthSummaryCard(summary = state.dailySummary)

        // Sleep card
        state.sleepSummary?.let { SleepCard(sleep = it) }

        // Recent workouts
        WorkoutList(workouts = state.recentWorkouts)

        // Period selector
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
    }
}
