package com.daysync.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.daysync.app.core.sync.SyncStatus
import com.daysync.app.core.sync.SyncableEntity
import kotlin.time.Clock
import kotlin.time.Instant

@Entity(tableName = "health_metrics")
data class HealthMetricEntity(
    @PrimaryKey val id: String,
    val type: String, // STEPS, HR, SPO2, HRV, FLOORS, ELEVATION, VO2MAX, SKIN_TEMP, RESTING_HR, WEIGHT
    val value: Double,
    val unit: String,
    val timestamp: Instant,
    val source: String = "health_connect",
    override val syncStatus: SyncStatus = SyncStatus.PENDING,
    override val lastModified: Instant = Clock.System.now(),
    override val isDeleted: Boolean = false,
) : SyncableEntity
