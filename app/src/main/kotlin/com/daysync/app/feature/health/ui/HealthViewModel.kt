package com.daysync.app.feature.health.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daysync.app.feature.health.data.HealthConnectManager
import com.daysync.app.feature.health.data.HealthRepository
import com.daysync.app.feature.health.model.HealthDailySummary
import com.daysync.app.feature.health.model.HealthPeriod
import com.daysync.app.feature.health.model.HealthUiState
import com.daysync.app.feature.health.model.HeartRateTrendPoint
import com.daysync.app.feature.health.model.PeriodStats
import com.daysync.app.feature.health.model.SleepSummary
import com.daysync.app.feature.health.model.SleepTrendPoint
import com.daysync.app.feature.health.model.StepsTrendPoint
import com.daysync.app.feature.health.model.WorkoutSummary
import com.daysync.app.feature.health.model.WorkoutTrendPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import kotlin.time.Instant
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate as KLocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

private val IST = ZoneId.of("Asia/Kolkata")

@HiltViewModel
class HealthViewModel @Inject constructor(
    private val healthConnectManager: HealthConnectManager,
    private val healthRepository: HealthRepository,
) : ViewModel() {

    val healthPermissions: Set<String> get() = healthConnectManager.permissions

    private val _uiState = MutableStateFlow<HealthUiState>(HealthUiState.Loading)
    val uiState: StateFlow<HealthUiState> = _uiState.asStateFlow()

    private val _selectedDate = MutableStateFlow(todayKLocalDate())
    val selectedDate: StateFlow<KLocalDate> = _selectedDate.asStateFlow()

    private val _selectedPeriod = MutableStateFlow(HealthPeriod.WEEKLY)
    val selectedPeriod: StateFlow<HealthPeriod> = _selectedPeriod.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var overrideJob: Job? = null
    private var subTypeJob: Job? = null

    // ── Lifecycle ────────────────────────────────────────────────────────

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

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            syncAndLoad()
            _isRefreshing.value = false
        }
    }

    // ── Date navigation ──────────────────────────────────────────────────

    fun navigateToPreviousDay() {
        _selectedDate.value = _selectedDate.value.minus(1, DateTimeUnit.DAY)
        viewModelScope.launch { loadData() }
    }

    fun navigateToNextDay() {
        _selectedDate.value = _selectedDate.value.plus(1, DateTimeUnit.DAY)
        viewModelScope.launch { loadData() }
    }

    fun navigateToToday() {
        _selectedDate.value = todayKLocalDate()
        viewModelScope.launch { loadData() }
    }

    fun onPeriodSelected(period: HealthPeriod) {
        _selectedPeriod.value = period
        viewModelScope.launch { loadData() }
    }

    // ── Sync + load ──────────────────────────────────────────────────────

    private suspend fun syncAndLoad() {
        _uiState.value = HealthUiState.Loading
        try {
            // Backfill the last 7 days from Health Connect so navigating to
            // past dates has data even if the app wasn't open on those days.
            val today = LocalDate.now(IST)
            val weekAgo = today.minusDays(6)
            val syncStart = weekAgo.atStartOfDay(IST).toInstant()
            val syncEnd = today.atTime(LocalTime.MAX).atZone(IST).toInstant()
            healthRepository.syncHealthData(
                Instant.fromEpochMilliseconds(syncStart.toEpochMilli()),
                Instant.fromEpochMilliseconds(syncEnd.toEpochMilli()),
            )
            loadData()
        } catch (e: Exception) {
            _uiState.value = HealthUiState.Error(e.message ?: "Failed to sync health data")
        }
    }

    private suspend fun loadData() {
        try {
            val date = _selectedDate.value
            val period = _selectedPeriod.value
            val (dayStart, dayEnd) = dateRange(date)
            val (periodStart, periodEnd) = periodRange(period)

            // Daily summary for the selected date
            val dayMetrics = healthRepository.getMetricsByDateRange(dayStart, dayEnd).first()
            val currentOverride = healthRepository.observeDailyOverride(date).first()
            val caloriesConsumed = healthRepository.getCaloriesConsumed(date)
            val dailySummary = buildDailySummary(dayMetrics, currentOverride, caloriesConsumed)

            // Sleep sessions for the selected date
            val sleepSessions = healthRepository.getSleepSessions(dayStart, dayEnd).first()
                .map { SleepSummary(it) }

            // Workouts for the selected date only
            val daySessions = healthRepository.getExerciseSessions(dayStart, dayEnd).first()
            val subTypes = if (daySessions.isEmpty()) {
                emptyMap()
            } else {
                healthRepository.observeWorkoutMetadata(daySessions.map { it.id }).first()
            }
            val dayWorkouts = daySessions.map { session ->
                WorkoutSummary(session = session, subType = subTypes[session.id])
            }

            // Trend charts (always use period range, independent of selectedDate)
            val stepsTrend = buildStepsTrend(periodStart, periodEnd, period)
            val heartRateTrend = buildHeartRateTrend(periodStart, periodEnd, period)
            val sleepTrend = buildSleepTrend(periodStart, periodEnd, period)
            val workoutTrend = buildWorkoutTrend(periodStart, periodEnd, period)

            // Periodic stats (above charts)
            val periodStats = buildPeriodStats(periodStart, periodEnd)

            // Per-type workout trend (if a type was previously selected)
            val prevType = (_uiState.value as? HealthUiState.Success)?.selectedWorkoutType
            val prevSubType = (_uiState.value as? HealthUiState.Success)?.selectedWorkoutSubType
            val workoutTypeTrend = if (prevType != null) {
                buildWorkoutTypeTrend(prevType, prevSubType)
            } else {
                emptyList()
            }

            _uiState.value = HealthUiState.Success(
                dailySummary = dailySummary,
                sleepSessions = sleepSessions,
                recentWorkouts = dayWorkouts,
                stepsTrend = stepsTrend,
                heartRateTrend = heartRateTrend,
                sleepTrend = sleepTrend,
                periodStats = periodStats,
                workoutTrend = workoutTrend,
                workoutTypeTrend = workoutTypeTrend,
                selectedWorkoutType = prevType,
                selectedWorkoutSubType = prevSubType,
            )

            // Reactive observers for dialog edits
            overrideJob?.cancel()
            overrideJob = viewModelScope.launch {
                healthRepository.observeDailyOverride(date).collect { override ->
                    applyOverride(override)
                }
            }
            subTypeJob?.cancel()
            if (daySessions.isNotEmpty()) {
                subTypeJob = viewModelScope.launch {
                    healthRepository.observeWorkoutMetadata(daySessions.map { it.id })
                        .collect { applySubTypes(it) }
                }
            }
        } catch (e: Exception) {
            _uiState.value = HealthUiState.Error(e.message ?: "Failed to load health data")
        }
    }

    // ── Calorie override ─────────────────────────────────────────────────

    private fun applyOverride(override: com.daysync.app.core.database.entity.DailyHealthOverrideEntity?) {
        val current = _uiState.value as? HealthUiState.Success ?: return
        _uiState.update {
            (it as? HealthUiState.Success)?.copy(
                dailySummary = current.dailySummary.copy(
                    totalCalories = override?.totalCalories,
                    weightMorning = override?.weightMorning,
                    weightEvening = override?.weightEvening,
                    weightNight = override?.weightNight,
                ),
            ) ?: it
        }
    }

    fun setCalorieOverride(totalCalories: Double?) {
        viewModelScope.launch {
            healthRepository.setCalorieOverride(_selectedDate.value, totalCalories)
        }
    }

    fun setWeight(morning: Double?, evening: Double?, night: Double?) {
        viewModelScope.launch {
            healthRepository.setWeight(_selectedDate.value, morning, evening, night)
        }
    }

    // ── Workout sub-type ─────────────────────────────────────────────────

    private fun applySubTypes(subTypes: Map<String, String?>) {
        val current = _uiState.value as? HealthUiState.Success ?: return
        val updated = current.recentWorkouts.map { summary ->
            val newSubType = subTypes[summary.session.id]
            if (newSubType == summary.subType) summary
            else summary.copy(subType = newSubType)
        }
        if (updated == current.recentWorkouts) return
        _uiState.update {
            (it as? HealthUiState.Success)?.copy(recentWorkouts = updated) ?: it
        }
    }

    fun setWorkoutSubType(sessionId: String, subType: String?) {
        viewModelScope.launch {
            healthRepository.setWorkoutSubType(sessionId, subType)
        }
    }

    // ── Workout type chart ──────────────────────────────────────────────

    fun selectWorkoutType(exerciseType: String?) {
        viewModelScope.launch {
            try {
                val trend = if (exerciseType != null) buildWorkoutTypeTrend(exerciseType, null) else emptyList()
                _uiState.update {
                    (it as? HealthUiState.Success)?.copy(
                        selectedWorkoutType = exerciseType,
                        selectedWorkoutSubType = null,
                        workoutTypeTrend = trend,
                    ) ?: it
                }
            } catch (e: Exception) {
                _uiState.value = HealthUiState.Error(
                    "Workout type filter failed: ${e::class.simpleName}: ${e.message}"
                )
            }
        }
    }

    fun selectWorkoutSubType(subType: String?) {
        viewModelScope.launch {
            try {
                val currentType = (_uiState.value as? HealthUiState.Success)?.selectedWorkoutType ?: return@launch
                val trend = buildWorkoutTypeTrend(currentType, subType)
                _uiState.update {
                    (it as? HealthUiState.Success)?.copy(
                        selectedWorkoutSubType = subType,
                        workoutTypeTrend = trend,
                    ) ?: it
                }
            } catch (e: Exception) {
                // Surface the error so we can diagnose without logcat
                _uiState.value = HealthUiState.Error(
                    "Sub-type filter failed: ${e::class.simpleName}: ${e.message}"
                )
            }
        }
    }

    // ── Summary builder ──────────────────────────────────────────────────

    private fun buildDailySummary(
        metrics: List<com.daysync.app.core.database.entity.HealthMetricEntity>,
        override: com.daysync.app.core.database.entity.DailyHealthOverrideEntity?,
        caloriesConsumed: Double?,
    ): HealthDailySummary {
        val byType = metrics.associateBy { it.type }
        // Calories are always manual — HC total is unreliable (OHealth
        // doesn't write BMR during sleep). HC metrics are kept in Room
        // for reference but not used in the daily summary.
        return HealthDailySummary(
            steps = byType["STEPS"]?.value?.toLong(),
            totalCalories = override?.totalCalories,
            caloriesConsumed = caloriesConsumed,
            weightMorning = override?.weightMorning,
            weightEvening = override?.weightEvening,
            weightNight = override?.weightNight,
            avgHeartRate = byType["HR_AVG"]?.value?.toLong(),
            minHeartRate = byType["HR_MIN"]?.value?.toLong(),
            maxHeartRate = byType["HR_MAX"]?.value?.toLong(),
            restingHeartRate = byType["RESTING_HR"]?.value,
            hrv = byType["HRV"]?.value,
            spo2 = byType["SPO2"]?.value,
            floorsClimbed = byType["FLOORS"]?.value,
            vo2Max = byType["VO2MAX"]?.value,
            weight = byType["WEIGHT"]?.value,
        )
    }

    // ── Trend builders ───────────────────────────────────────────────────

    private suspend fun buildStepsTrend(
        start: Instant, end: Instant, period: HealthPeriod,
    ): List<StepsTrendPoint> {
        val metrics = healthRepository.getMetricsByTypeAndDateRange("STEPS", start, end).first()
        if (metrics.isEmpty()) return emptyList()
        return metrics.reversed()
            .groupBy { formatLabel(it.timestamp, period) }
            .map { (label, group) ->
                StepsTrendPoint(label = label, steps = group.last().value.toLong())
            }
    }

    private suspend fun buildHeartRateTrend(
        start: Instant, end: Instant, period: HealthPeriod,
    ): List<HeartRateTrendPoint> {
        val avgMetrics = healthRepository.getMetricsByTypeAndDateRange("HR_AVG", start, end).first()
        val maxMetrics = healthRepository.getMetricsByTypeAndDateRange("HR_MAX", start, end).first()
            .associateBy { formatLabel(it.timestamp, period) }
        val minMetrics = healthRepository.getMetricsByTypeAndDateRange("HR_MIN", start, end).first()
            .associateBy { formatLabel(it.timestamp, period) }
        return avgMetrics.reversed()
            .groupBy { formatLabel(it.timestamp, period) }
            .map { (label, group) ->
                val avg = group.last()
                HeartRateTrendPoint(
                    label = label,
                    avg = avg.value.toLong(),
                    min = minMetrics[label]?.value?.toLong() ?: avg.value.toLong(),
                    max = maxMetrics[label]?.value?.toLong() ?: avg.value.toLong(),
                )
            }
    }

    private suspend fun buildSleepTrend(
        start: Instant, end: Instant, period: HealthPeriod,
    ): List<SleepTrendPoint> {
        val sessions = healthRepository.getSleepSessions(start, end).first()
        return sessions.reversed()
            .groupBy { formatLabel(it.startTime, period) }
            .map { (label, group) ->
                SleepTrendPoint(
                    label = label,
                    totalMinutes = group.sumOf { it.totalMinutes },
                    deepMinutes = group.sumOf { it.deepMinutes },
                    lightMinutes = group.sumOf { it.lightMinutes },
                    remMinutes = group.sumOf { it.remMinutes },
                    awakeMinutes = group.sumOf { it.awakeMinutes },
                )
            }
    }

    private suspend fun buildPeriodStats(start: Instant, end: Instant): PeriodStats {
        // Average sleep — sum sessions per day, then average across days
        val sleepSessions = healthRepository.getSleepSessions(start, end).first()
        val zone = java.time.ZoneId.of("Asia/Kolkata")
        val dailySleepTotals = sleepSessions
            .groupBy { java.time.Instant.ofEpochMilli(it.startTime.toEpochMilliseconds()).atZone(zone).toLocalDate() }
            .map { (_, sessions) -> sessions.sumOf { it.totalMinutes } }
        val avgSleep = if (dailySleepTotals.isNotEmpty()) {
            dailySleepTotals.average().toInt()
        } else null

        // Iterate days for weight and calorie stats from daily overrides
        val startDate = java.time.Instant.ofEpochMilli(start.toEpochMilliseconds()).atZone(zone).toLocalDate()
        val endDate = java.time.Instant.ofEpochMilli(end.toEpochMilliseconds()).atZone(zone).toLocalDate()
        val weights = mutableListOf<Double>()
        var totalBurned = 0.0
        var totalConsumed = 0.0
        var cursor = startDate
        while (!cursor.isAfter(endDate)) {
            val kDate = kotlinx.datetime.LocalDate(cursor.year, cursor.monthValue, cursor.dayOfMonth)
            val override = healthRepository.observeDailyOverride(kDate).first()
            // Weight
            val dayWeights = listOfNotNull(override?.weightMorning, override?.weightEvening, override?.weightNight)
            if (dayWeights.isNotEmpty()) weights.add(dayWeights.average())
            // Manual calories burned
            override?.totalCalories?.let { totalBurned += it }
            // Calories consumed from nutrition
            healthRepository.getCaloriesConsumed(kDate)?.let { totalConsumed += it }
            cursor = cursor.plusDays(1)
        }
        val avgWeight = if (weights.isNotEmpty()) weights.average() else null
        val totalDeficit = if (totalBurned > 0 || totalConsumed > 0) totalBurned - totalConsumed else null

        return PeriodStats(
            avgSleepMinutes = avgSleep,
            avgWeight = avgWeight?.let { Math.round(it * 10.0) / 10.0 },
            totalCalorieDeficit = totalDeficit,
        )
    }

    private suspend fun buildWorkoutTrend(
        start: Instant, end: Instant, period: HealthPeriod,
    ): List<WorkoutTrendPoint> {
        val sessions = healthRepository.getExerciseSessions(start, end).first()
        if (sessions.isEmpty()) return emptyList()
        return sessions.reversed()
            .groupBy { formatLabel(it.startTime, period) }
            .map { (label, group) ->
                WorkoutTrendPoint(
                    label = label,
                    avgHr = group.mapNotNull { it.avgHeartRate }.average().toInt().takeIf { it > 0 } ?: 0,
                    calories = group.mapNotNull { it.calories }.sumOf { it.toInt() },
                )
            }
    }

    private suspend fun buildWorkoutTypeTrend(
        exerciseType: String,
        subTypeFilter: String?,
    ): List<WorkoutTrendPoint> {
        var sessions = healthRepository.getWorkoutsByExerciseType(exerciseType).first()
        if (sessions.isEmpty()) return emptyList()

        // Filter by sub-type if selected (requires joining with WorkoutMetadata)
        if (subTypeFilter != null) {
            val metadata = healthRepository.observeWorkoutMetadata(sessions.map { it.id }).first()
            sessions = sessions.filter { metadata[it.id] == subTypeFilter }
        }

        return sessions.map { session ->
            val dateLabel = java.time.Instant.ofEpochMilli(session.startTime.toEpochMilliseconds())
                .atZone(IST).toLocalDate()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM"))
            WorkoutTrendPoint(
                label = dateLabel,
                avgHr = session.avgHeartRate ?: 0,
                calories = session.calories?.toInt() ?: 0,
            )
        }
    }

    // ── Date/time helpers ────────────────────────────────────────────────

    private fun dateRange(date: KLocalDate): Pair<Instant, Instant> {
        val jDate = LocalDate.of(date.year, date.monthNumber, date.dayOfMonth)
        val start = jDate.atStartOfDay(IST).toInstant()
        val end = jDate.atTime(LocalTime.MAX).atZone(IST).toInstant()
        return Instant.fromEpochMilliseconds(start.toEpochMilli()) to
            Instant.fromEpochMilliseconds(end.toEpochMilli())
    }

    private fun periodRange(period: HealthPeriod): Pair<Instant, Instant> {
        val todayEnd = LocalDate.now(IST).atTime(LocalTime.MAX).atZone(IST).toInstant()
        val periodStart = LocalDate.now(IST).minusDays(period.days.toLong() - 1)
            .atStartOfDay(IST).toInstant()
        return Instant.fromEpochMilliseconds(periodStart.toEpochMilli()) to
            Instant.fromEpochMilliseconds(todayEnd.toEpochMilli())
    }

    private fun todayKLocalDate(): KLocalDate {
        val today = LocalDate.now(IST)
        return KLocalDate(today.year, today.monthValue, today.dayOfMonth)
    }

    private fun formatLabel(instant: Instant, period: HealthPeriod): String {
        val dateTime = java.time.Instant.ofEpochMilli(instant.toEpochMilliseconds())
            .atZone(IST).toLocalDateTime()
        return when (period) {
            HealthPeriod.WEEKLY -> dateTime.format(java.time.format.DateTimeFormatter.ofPattern("EEE"))
            HealthPeriod.MONTHLY -> dateTime.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM"))
        }
    }
}
