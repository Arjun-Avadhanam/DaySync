package com.daysync.app.feature.health.model

import com.daysync.app.core.database.entity.ExerciseSessionEntity
import com.daysync.app.core.database.entity.SleepSessionEntity

data class HeartRateZoneConfig(
    val zone1Ceiling: Int = 96,
    val zone2Ceiling: Int = 124,
    val zone3Ceiling: Int = 145,
    val zone4Ceiling: Int = 166,
    val zone5Ceiling: Int = 180,
) {
    val zoneNames: List<String> = listOf("Warmup", "Fat Burning", "Endurance", "Anaerobic", "Threshold")

    fun zoneForBpm(bpm: Long): Int = when {
        bpm < zone1Ceiling -> 0
        bpm < zone2Ceiling -> 1
        bpm < zone3Ceiling -> 2
        bpm < zone4Ceiling -> 3
        bpm < zone5Ceiling -> 4
        else -> 5
    }
}

data class HealthDailySummary(
    val steps: Long? = null,
    val totalCalories: Double? = null,
    val activeCalories: Double? = null,
    val avgHeartRate: Long? = null,
    val minHeartRate: Long? = null,
    val maxHeartRate: Long? = null,
    val restingHeartRate: Double? = null,
    val hrv: Double? = null,
    val spo2: Double? = null,
    val floorsClimbed: Double? = null,
    val vo2Max: Double? = null,
    val weight: Double? = null,
    val debugInfo: String? = null, // Temporary: remove after diagnosing calories issue
)

data class SleepSummary(
    val session: SleepSessionEntity,
) {
    val totalHours: Double get() = session.totalMinutes / 60.0
    val deepPercent: Int get() = if (session.totalMinutes > 0) (session.deepMinutes * 100) / session.totalMinutes else 0
    val lightPercent: Int get() = if (session.totalMinutes > 0) (session.lightMinutes * 100) / session.totalMinutes else 0
    val remPercent: Int get() = if (session.totalMinutes > 0) (session.remMinutes * 100) / session.totalMinutes else 0
    val awakePercent: Int get() = if (session.totalMinutes > 0) (session.awakeMinutes * 100) / session.totalMinutes else 0
}

data class WorkoutSummary(
    val session: ExerciseSessionEntity,
) {
    val durationMinutes: Long
        get() {
            val durationMs = session.activeDurationMs
                ?: (session.endTime - session.startTime).inWholeMilliseconds
            return durationMs / 60_000
        }

    val displayType: String get() = formatExerciseType(session.exerciseType)

    val paceMinPerKm: Double?
        get() {
            val dist = session.distance ?: return null
            if (dist <= 0) return null
            val mins = durationMinutes.toDouble()
            return mins / (dist / 1000.0)
        }
}

data class StepsTrendPoint(
    val label: String,
    val steps: Long,
)

data class HeartRateTrendPoint(
    val label: String,
    val avg: Long,
    val min: Long,
    val max: Long,
)

data class SleepTrendPoint(
    val label: String,
    val totalMinutes: Int,
    val deepMinutes: Int,
    val lightMinutes: Int,
    val remMinutes: Int,
    val awakeMinutes: Int,
)

sealed interface HealthUiState {
    data object Loading : HealthUiState
    data object PermissionRequired : HealthUiState
    data object HealthConnectNotAvailable : HealthUiState
    data class Success(
        val dailySummary: HealthDailySummary,
        val sleepSummary: SleepSummary?,
        val recentWorkouts: List<WorkoutSummary>,
        val stepsTrend: List<StepsTrendPoint>,
        val heartRateTrend: List<HeartRateTrendPoint>,
        val sleepTrend: List<SleepTrendPoint>,
    ) : HealthUiState
    data class Error(val message: String) : HealthUiState
}

enum class HealthPeriod(val label: String, val days: Int) {
    DAILY("Today", 1),
    WEEKLY("7 Days", 7),
    MONTHLY("30 Days", 30),
}

private fun formatExerciseType(type: String): String =
    type.removePrefix("EXERCISE_TYPE_")
        .replace("_", " ")
        .lowercase()
        .replaceFirstChar { it.uppercase() }
