package com.daysync.app.feature.health.data

import com.daysync.app.core.database.dao.ExerciseSessionDao
import com.daysync.app.core.database.dao.HealthMetricDao
import com.daysync.app.core.database.dao.SleepSessionDao
import com.daysync.app.core.database.entity.ExerciseSessionEntity
import com.daysync.app.core.database.entity.HealthMetricEntity
import com.daysync.app.core.database.entity.SleepSessionEntity
import com.daysync.app.feature.health.model.HeartRateZoneConfig
import java.time.Instant as JavaInstant
import javax.inject.Inject
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow

class HealthRepositoryImpl @Inject constructor(
    private val healthConnectManager: HealthConnectManager,
    private val healthMetricDao: HealthMetricDao,
    private val sleepSessionDao: SleepSessionDao,
    private val exerciseSessionDao: ExerciseSessionDao,
    private val zoneConfig: HeartRateZoneConfig,
) : HealthRepository {

    override suspend fun syncHealthData(start: Instant, end: Instant) {
        val javaStart = JavaInstant.ofEpochMilli(start.toEpochMilliseconds())
        val javaEnd = JavaInstant.ofEpochMilli(end.toEpochMilliseconds())

        // Read and store daily metrics
        val metrics = healthConnectManager.readDailyMetrics(javaStart, javaEnd)
        if (metrics.isNotEmpty()) {
            healthMetricDao.insertAll(metrics)
        }

        // Read and store sleep sessions
        val sleepSessions = healthConnectManager.readSleepSessions(javaStart, javaEnd)
        if (sleepSessions.isNotEmpty()) {
            sleepSessionDao.insertAll(sleepSessions)
        }

        // Read and store enriched exercise sessions
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
}
