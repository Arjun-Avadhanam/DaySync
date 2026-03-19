package com.daysync.app.feature.health.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daysync.app.feature.health.data.HealthConnectManager
import com.daysync.app.feature.health.data.HealthRepository
import com.daysync.app.feature.health.model.HealthDailySummary
import com.daysync.app.feature.health.model.HealthPeriod
import com.daysync.app.feature.health.model.HealthUiState
import com.daysync.app.feature.health.model.HeartRateTrendPoint
import com.daysync.app.feature.health.model.SleepSummary
import com.daysync.app.feature.health.model.SleepTrendPoint
import com.daysync.app.feature.health.model.StepsTrendPoint
import com.daysync.app.feature.health.model.WorkoutSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import kotlin.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@HiltViewModel
class HealthViewModel @Inject constructor(
    private val healthConnectManager: HealthConnectManager,
    private val healthRepository: HealthRepository,
) : ViewModel() {

    val healthPermissions: Set<String> get() = healthConnectManager.permissions

    private val _uiState = MutableStateFlow<HealthUiState>(HealthUiState.Loading)
    val uiState: StateFlow<HealthUiState> = _uiState.asStateFlow()

    private val _selectedPeriod = MutableStateFlow(HealthPeriod.WEEKLY)
    val selectedPeriod: StateFlow<HealthPeriod> = _selectedPeriod.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun checkAvailabilityAndLoad() {
        viewModelScope.launch {
            if (!healthConnectManager.isAvailable()) {
                _uiState.value = HealthUiState.HealthConnectNotAvailable
                return@launch
            }
            if (!healthConnectManager.hasAllPermissions()) {
                _uiState.value = HealthUiState.PermissionRequired
                return@launch
            }
            syncAndLoad()
        }
    }

    fun onPermissionsGranted() {
        viewModelScope.launch { syncAndLoad() }
    }

    fun onPeriodSelected(period: HealthPeriod) {
        _selectedPeriod.value = period
        viewModelScope.launch { loadData() }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            syncAndLoad()
            _isRefreshing.value = false
        }
    }

    private suspend fun syncAndLoad() {
        _uiState.value = HealthUiState.Loading
        try {
            // Sync today's data from Health Connect to Room
            val todayRange = todayRange()
            healthRepository.syncHealthData(todayRange.first, todayRange.second)
            loadData()
        } catch (e: Exception) {
            _uiState.value = HealthUiState.Error(e.message ?: "Failed to sync health data")
        }
    }

    private suspend fun loadData() {
        try {
            val period = _selectedPeriod.value
            val (periodStart, periodEnd) = periodRange(period)
            val (todayStart, todayEnd) = todayRange()

            // Today's summary from metrics
            val todayMetrics = healthRepository.getMetricsByDateRange(todayStart, todayEnd).first()
            val dailySummary = buildDailySummary(todayMetrics)

            // Latest sleep
            val latestSleep = healthRepository.getLatestSleep().first()
            val sleepSummary = latestSleep?.let { SleepSummary(it) }

            // Recent workouts
            val recentWorkouts = healthRepository.getRecentWorkouts(5).first()
                .map { WorkoutSummary(it) }

            // Trend data for selected period
            val stepsTrend = buildStepsTrend(periodStart, periodEnd, period)
            val heartRateTrend = buildHeartRateTrend(periodStart, periodEnd, period)
            val sleepTrend = buildSleepTrend(periodStart, periodEnd, period)

            _uiState.value = HealthUiState.Success(
                dailySummary = dailySummary,
                sleepSummary = sleepSummary,
                recentWorkouts = recentWorkouts,
                stepsTrend = stepsTrend,
                heartRateTrend = heartRateTrend,
                sleepTrend = sleepTrend,
            )
        } catch (e: Exception) {
            _uiState.value = HealthUiState.Error(e.message ?: "Failed to load health data")
        }
    }

    private fun buildDailySummary(
        metrics: List<com.daysync.app.core.database.entity.HealthMetricEntity>,
    ): HealthDailySummary {
        val byType = metrics.associateBy { it.type }
        val debug = "Metrics found: ${metrics.size}, types: ${metrics.map { it.type }.distinct()}"
        return HealthDailySummary(
            steps = byType["STEPS"]?.value?.toLong(),
            totalCalories = byType["TOTAL_CALORIES"]?.value,
            activeCalories = byType["ACTIVE_CALORIES"]?.value,
            avgHeartRate = byType["HR_AVG"]?.value?.toLong(),
            minHeartRate = byType["HR_MIN"]?.value?.toLong(),
            maxHeartRate = byType["HR_MAX"]?.value?.toLong(),
            restingHeartRate = byType["RESTING_HR"]?.value,
            hrv = byType["HRV"]?.value,
            spo2 = byType["SPO2"]?.value,
            floorsClimbed = byType["FLOORS"]?.value,
            vo2Max = byType["VO2MAX"]?.value,
            weight = byType["WEIGHT"]?.value,
            debugInfo = debug,
        )
    }

    private suspend fun buildStepsTrend(
        start: Instant,
        end: Instant,
        period: HealthPeriod,
    ): List<StepsTrendPoint> {
        val metrics = healthRepository.getMetricsByTypeAndDateRange("STEPS", start, end).first()
        if (metrics.isEmpty()) return emptyList()

        return metrics.reversed().map { metric ->
            StepsTrendPoint(
                label = formatLabel(metric.timestamp, period),
                steps = metric.value.toLong(),
            )
        }
    }

    private suspend fun buildHeartRateTrend(
        start: Instant,
        end: Instant,
        period: HealthPeriod,
    ): List<HeartRateTrendPoint> {
        val avgMetrics = healthRepository.getMetricsByTypeAndDateRange("HR_AVG", start, end).first()
        val maxMetrics = healthRepository.getMetricsByTypeAndDateRange("HR_MAX", start, end).first()
            .associateBy { formatLabel(it.timestamp, period) }
        val minMetrics = healthRepository.getMetricsByTypeAndDateRange("HR_MIN", start, end).first()
            .associateBy { formatLabel(it.timestamp, period) }

        return avgMetrics.reversed().map { avg ->
            val label = formatLabel(avg.timestamp, period)
            HeartRateTrendPoint(
                label = label,
                avg = avg.value.toLong(),
                min = minMetrics[label]?.value?.toLong() ?: avg.value.toLong(),
                max = maxMetrics[label]?.value?.toLong() ?: avg.value.toLong(),
            )
        }
    }

    private suspend fun buildSleepTrend(
        start: Instant,
        end: Instant,
        period: HealthPeriod,
    ): List<SleepTrendPoint> {
        val sessions = healthRepository.getSleepSessions(start, end).first()
        return sessions.reversed().map { session ->
            SleepTrendPoint(
                label = formatLabel(session.startTime, period),
                totalMinutes = session.totalMinutes,
                deepMinutes = session.deepMinutes,
                lightMinutes = session.lightMinutes,
                remMinutes = session.remMinutes,
                awakeMinutes = session.awakeMinutes,
            )
        }
    }

    private fun todayRange(): Pair<Instant, Instant> {
        val zone = ZoneId.of("Asia/Kolkata")
        val todayStart = LocalDate.now(zone).atStartOfDay(zone).toInstant()
        val todayEnd = LocalDate.now(zone).atTime(LocalTime.MAX).atZone(zone).toInstant()
        return Instant.fromEpochMilliseconds(todayStart.toEpochMilli()) to
            Instant.fromEpochMilliseconds(todayEnd.toEpochMilli())
    }

    private fun periodRange(period: HealthPeriod): Pair<Instant, Instant> {
        val zone = ZoneId.of("Asia/Kolkata")
        val todayEnd = LocalDate.now(zone).atTime(LocalTime.MAX).atZone(zone).toInstant()
        val periodStart = LocalDate.now(zone).minusDays(period.days.toLong() - 1)
            .atStartOfDay(zone).toInstant()
        return Instant.fromEpochMilliseconds(periodStart.toEpochMilli()) to
            Instant.fromEpochMilliseconds(todayEnd.toEpochMilli())
    }

    private fun formatLabel(instant: Instant, period: HealthPeriod): String {
        val zone = ZoneId.of("Asia/Kolkata")
        val dateTime = java.time.Instant.ofEpochMilli(instant.toEpochMilliseconds())
            .atZone(zone).toLocalDateTime()
        return when (period) {
            HealthPeriod.DAILY -> dateTime.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
            HealthPeriod.WEEKLY -> dateTime.format(java.time.format.DateTimeFormatter.ofPattern("EEE"))
            HealthPeriod.MONTHLY -> dateTime.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM"))
        }
    }
}
