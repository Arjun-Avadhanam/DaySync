package com.daysync.app.feature.health.data

import com.daysync.app.core.database.entity.ExerciseSessionEntity
import com.daysync.app.core.database.entity.HealthMetricEntity
import com.daysync.app.core.database.entity.SleepSessionEntity
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow

interface HealthRepository {
    suspend fun syncHealthData(start: Instant, end: Instant)
    fun getMetricsByDateRange(start: Instant, end: Instant): Flow<List<HealthMetricEntity>>
    fun getMetricsByTypeAndDateRange(type: String, start: Instant, end: Instant): Flow<List<HealthMetricEntity>>
    fun getLatestMetricByType(type: String): Flow<HealthMetricEntity?>
    fun getSleepSessions(start: Instant, end: Instant): Flow<List<SleepSessionEntity>>
    fun getLatestSleep(): Flow<SleepSessionEntity?>
    fun getExerciseSessions(start: Instant, end: Instant): Flow<List<ExerciseSessionEntity>>
    fun getRecentWorkouts(limit: Int): Flow<List<ExerciseSessionEntity>>
}
