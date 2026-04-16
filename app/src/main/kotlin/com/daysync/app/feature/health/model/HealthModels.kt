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
    val caloriesConsumed: Double? = null,
    val weightMorning: Double? = null,
    val weightEvening: Double? = null,
    val weightNight: Double? = null,
    val avgHeartRate: Long? = null,
    val minHeartRate: Long? = null,
    val maxHeartRate: Long? = null,
    val restingHeartRate: Double? = null,
    val hrv: Double? = null,
    val spo2: Double? = null,
    val floorsClimbed: Double? = null,
    val vo2Max: Double? = null,
    val weight: Double? = null,
    val allTimeCalorieDeficit: Double? = null,
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
    val subType: String? = null,
) {
    val durationMinutes: Long
        get() {
            val durationMs = session.activeDurationMs
                ?: (session.endTime - session.startTime).inWholeMilliseconds
            return durationMs / 60_000
        }

    // Strength training + a sub-type renders as "Strength training • Push".
    // Generic Other workouts use the sub-type as the whole label since that
    // *is* what the workout was (e.g. "Leg exercises"). Everything else is
    // the straight friendly name.
    val displayType: String
        get() {
            val base = formatExerciseType(session.exerciseType)
            return when {
                subType == null -> base
                session.exerciseType == "EXERCISE_TYPE_OTHER_WORKOUT" -> formatSubType(subType)
                else -> "$base • ${formatSubType(subType)}"
            }
        }

    val paceMinPerKm: Double?
        get() {
            val dist = session.distance ?: return null
            if (dist <= 0) return null
            val mins = durationMinutes.toDouble()
            return mins / (dist / 1000.0)
        }
}

// Exposed as constants so the UI picker and the storage layer stay in sync.
object WorkoutSubTypes {
    // STRENGTH_TRAINING sub-types
    const val PUSH = "PUSH"
    const val PULL = "PULL"
    const val OTHER = "OTHER"

    // OTHER_WORKOUT sub-types
    const val LEG_EXERCISES = "LEG_EXERCISES"

    val strengthOptions: List<String> = listOf(PUSH, PULL, OTHER)
    val otherWorkoutOptions: List<String> = listOf(LEG_EXERCISES, OTHER)

    fun optionsFor(exerciseType: String): List<String>? = when (exerciseType) {
        "EXERCISE_TYPE_STRENGTH_TRAINING" -> strengthOptions
        "EXERCISE_TYPE_OTHER_WORKOUT" -> otherWorkoutOptions
        else -> null
    }
}

private fun formatSubType(subType: String): String = when (subType) {
    WorkoutSubTypes.PUSH -> "Push"
    WorkoutSubTypes.PULL -> "Pull"
    WorkoutSubTypes.OTHER -> "Other"
    WorkoutSubTypes.LEG_EXERCISES -> "Leg exercises"
    else -> subType.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
}

data class PeriodStats(
    val avgSleepMinutes: Int? = null,
    val avgWeight: Double? = null,
    val totalCalorieDeficit: Double? = null,
)

data class WorkoutTrendPoint(
    val label: String,
    val avgHr: Int,
    val calories: Int,
)

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
        val sleepSessions: List<SleepSummary> = emptyList(),
        val recentWorkouts: List<WorkoutSummary>,
        val stepsTrend: List<StepsTrendPoint>,
        val heartRateTrend: List<HeartRateTrendPoint>,
        val sleepTrend: List<SleepTrendPoint>,
        val periodStats: PeriodStats = PeriodStats(),
        val workoutTrend: List<WorkoutTrendPoint> = emptyList(),
        val workoutTypeTrend: List<WorkoutTrendPoint> = emptyList(),
        val selectedWorkoutType: String? = null,
        val selectedWorkoutSubType: String? = null,
    ) : HealthUiState
    data class Error(val message: String) : HealthUiState
}

enum class HealthPeriod(val label: String, val days: Int) {
    WEEKLY("7 Days", 7),
    MONTHLY("30 Days", 30),
}

private fun formatExerciseType(type: String): String = when (type) {
    // OHealth maps soccer to FOOTBALL_AUSTRALIAN in Health Connect's enum.
    "EXERCISE_TYPE_FOOTBALL_AUSTRALIAN" -> "Football"
    "EXERCISE_TYPE_OTHER_WORKOUT" -> "Workout"
    else -> type.removePrefix("EXERCISE_TYPE_")
        .replace("_", " ")
        .lowercase()
        .replaceFirstChar { it.uppercase() }
}
