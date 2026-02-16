package com.daysync.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.daysync.app.core.sync.SyncStatus
import com.daysync.app.core.sync.SyncableEntity
import kotlin.time.Clock
import kotlin.time.Instant

@Entity(tableName = "exercise_sessions")
data class ExerciseSessionEntity(
    @PrimaryKey val id: String,
    val exerciseType: String,
    val startTime: Instant,
    val endTime: Instant,
    val title: String? = null,
    val calories: Double? = null,
    val activeCalories: Double? = null,
    val avgHeartRate: Int? = null,
    val maxHeartRate: Int? = null,
    val minHeartRate: Int? = null,
    val distance: Double? = null,
    val elevationGain: Double? = null,
    val steps: Long? = null,
    val activeDurationMs: Long? = null,
    // Running-specific
    val avgSpeedMps: Double? = null,
    val maxSpeedMps: Double? = null,
    val avgCadenceSpm: Double? = null,
    val maxCadenceSpm: Double? = null,
    // HR zones (time in each zone, in seconds)
    val zone1Seconds: Int? = null,
    val zone2Seconds: Int? = null,
    val zone3Seconds: Int? = null,
    val zone4Seconds: Int? = null,
    val zone5Seconds: Int? = null,
    // Laps (JSON serialized)
    val laps: String? = null,
    val hasRoute: Boolean = false,
    val notes: String? = null,
    override val syncStatus: SyncStatus = SyncStatus.PENDING,
    override val lastModified: Instant = Clock.System.now(),
    override val isDeleted: Boolean = false,
) : SyncableEntity
