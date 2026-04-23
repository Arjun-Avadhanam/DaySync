package com.daysync.app.feature.health.data

import com.daysync.app.core.database.dao.DailyHealthOverrideDao
import com.daysync.app.core.database.dao.DailyNutritionSummaryDao
import com.daysync.app.core.database.dao.ExerciseSessionDao
import com.daysync.app.core.database.dao.HealthMetricDao
import com.daysync.app.core.database.dao.SleepSessionDao
import com.daysync.app.core.database.dao.WorkoutMetadataDao
import com.daysync.app.core.database.entity.DailyHealthOverrideEntity
import com.daysync.app.core.database.entity.ExerciseSessionEntity
import com.daysync.app.core.database.entity.HealthMetricEntity
import com.daysync.app.core.database.entity.SleepSessionEntity
import com.daysync.app.core.database.entity.WorkoutMetadataEntity
import com.daysync.app.core.sync.SyncStatus
import com.daysync.app.feature.health.model.HeartRateZoneConfig
import java.time.Instant as JavaInstant
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

class HealthRepositoryImpl @Inject constructor(
    private val healthConnectManager: HealthConnectManager,
    private val healthMetricDao: HealthMetricDao,
    private val sleepSessionDao: SleepSessionDao,
    private val exerciseSessionDao: ExerciseSessionDao,
    private val dailyHealthOverrideDao: DailyHealthOverrideDao,
    private val workoutMetadataDao: WorkoutMetadataDao,
    private val nutritionSummaryDao: DailyNutritionSummaryDao,
    private val zoneConfig: HeartRateZoneConfig,
    private val userPreferences: com.daysync.app.core.config.UserPreferences,
) : HealthRepository {

    override suspend fun syncHealthData(start: Instant, end: Instant) {
        val javaStart = JavaInstant.ofEpochMilli(start.toEpochMilliseconds())
        val javaEnd = JavaInstant.ofEpochMilli(end.toEpochMilliseconds())

        // Metrics use HC's aggregate API which collapses the entire time range
        // into one value. To get per-day data we must query each day separately.
        val zone = userPreferences.javaZoneId
        var cursor = javaStart.atZone(zone).toLocalDate()
        val lastDay = javaEnd.atZone(zone).toLocalDate()
        while (!cursor.isAfter(lastDay)) {
            val dayStart = cursor.atStartOfDay(zone).toInstant()
            val dayEnd = cursor.atTime(java.time.LocalTime.MAX).atZone(zone).toInstant()
            val dayMetrics = healthConnectManager.readDailyMetrics(dayStart, dayEnd)
            if (dayMetrics.isNotEmpty()) {
                healthMetricDao.insertAll(dayMetrics)
            }
            cursor = cursor.plusDays(1)
        }

        // Sleep and exercise are interval records with their own timestamps,
        // so the full window query works correctly.
        val sleepSessions = healthConnectManager.readSleepSessions(javaStart, javaEnd)
        if (sleepSessions.isNotEmpty()) {
            sleepSessionDao.insertAll(sleepSessions)
        }

        val exerciseSessions = healthConnectManager.readExerciseSessions(
            javaStart,
            javaEnd,
            zoneConfig,
        )
        if (exerciseSessions.isNotEmpty()) {
            exerciseSessionDao.insertAll(exerciseSessions)
        }
    }

    override fun getMetricsByDateRange(
        start: Instant,
        end: Instant,
    ): Flow<List<HealthMetricEntity>> = healthMetricDao.getByDateRange(start, end)

    override fun getMetricsByTypeAndDateRange(
        type: String,
        start: Instant,
        end: Instant,
    ): Flow<List<HealthMetricEntity>> = healthMetricDao.getByTypeAndDateRange(type, start, end)

    override fun getLatestMetricByType(type: String): Flow<HealthMetricEntity?> =
        healthMetricDao.getLatestByType(type)

    override fun getSleepSessions(
        start: Instant,
        end: Instant,
    ): Flow<List<SleepSessionEntity>> = sleepSessionDao.getByDateRange(start, end)

    override fun getLatestSleep(): Flow<SleepSessionEntity?> = sleepSessionDao.getLatest()

    override fun getExerciseSessions(
        start: Instant,
        end: Instant,
    ): Flow<List<ExerciseSessionEntity>> = exerciseSessionDao.getByDateRange(start, end)

    override fun getRecentWorkouts(limit: Int): Flow<List<ExerciseSessionEntity>> =
        exerciseSessionDao.getRecent(limit)

    override fun observeDailyOverride(date: LocalDate): Flow<DailyHealthOverrideEntity?> =
        dailyHealthOverrideDao.observe(date)

    override suspend fun setCalorieOverride(date: LocalDate, totalCalories: Double?) {
        val existing = dailyHealthOverrideDao.get(date)
        if (totalCalories == null && existing == null) return
        if (totalCalories == null && existing != null) {
            // Clear only calories, keep weight if present
            val hasWeight = existing.weightMorning != null || existing.weightEvening != null || existing.weightNight != null
            if (hasWeight) {
                dailyHealthOverrideDao.upsert(existing.copy(
                    totalCalories = null,
                    syncStatus = SyncStatus.PENDING,
                    lastModified = Clock.System.now(),
                ))
            } else {
                dailyHealthOverrideDao.deleteByDate(date)
            }
            return
        }
        // Merge with existing to preserve weight fields
        val entity = (existing ?: DailyHealthOverrideEntity(date = date)).copy(
            totalCalories = totalCalories,
            syncStatus = SyncStatus.PENDING,
            lastModified = Clock.System.now(),
        )
        dailyHealthOverrideDao.upsert(entity)
    }

    override suspend fun setWeight(date: LocalDate, morning: Double?, evening: Double?, night: Double?) {
        val existing = dailyHealthOverrideDao.get(date)
        val entity = (existing ?: DailyHealthOverrideEntity(date = date)).copy(
            weightMorning = morning,
            weightEvening = evening,
            weightNight = night,
            syncStatus = SyncStatus.PENDING,
            lastModified = Clock.System.now(),
        )
        dailyHealthOverrideDao.upsert(entity)
    }

    override fun getWorkoutsByExerciseType(exerciseType: String): Flow<List<ExerciseSessionEntity>> =
        exerciseSessionDao.getByExerciseType(exerciseType)

    override fun observeWorkoutMetadata(sessionIds: List<String>): Flow<Map<String, String?>> =
        workoutMetadataDao.observeBySessionIds(sessionIds)
            .map { rows -> rows.associate { it.sessionId to it.subType } }

    override suspend fun setWorkoutSubType(sessionId: String, subType: String?) {
        if (subType == null) {
            workoutMetadataDao.deleteBySessionId(sessionId)
            return
        }
        workoutMetadataDao.upsert(
            WorkoutMetadataEntity(
                sessionId = sessionId,
                subType = subType,
                syncStatus = SyncStatus.PENDING,
                lastModified = Clock.System.now(),
            )
        )
    }

    override suspend fun getCaloriesConsumed(date: LocalDate): Double? {
        return nutritionSummaryDao.getByDate(date)?.totalCalories?.takeIf { it > 0 }
    }

    override suspend fun getAllTimeCalorieDeficit(): Double {
        val baseline = com.daysync.app.BuildConfig.CALORIE_DEFICIT_BASELINE.toDouble()
        var delta = 0.0
        for (override in dailyHealthOverrideDao.getAllWithCalories()) {
            val burned = override.totalCalories ?: continue
            val consumed = getCaloriesConsumed(override.date) ?: 0.0
            delta += (burned - consumed)
        }
        return baseline + delta
    }
}
