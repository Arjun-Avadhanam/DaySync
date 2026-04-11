package com.daysync.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.daysync.app.core.sync.SyncStatus
import com.daysync.app.core.sync.SyncableEntity
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

/**
 * Per-day manual overrides for health summary fields that the user wants to
 * correct after the fact — primarily total calories, which OHealth writes
 * incorrectly (see health section analysis). A row exists only for days the
 * user has explicitly overridden at least one field.
 */
@Entity(tableName = "daily_health_overrides")
data class DailyHealthOverrideEntity(
    @PrimaryKey val date: LocalDate,
    val totalCalories: Double? = null,
    override val syncStatus: SyncStatus = SyncStatus.PENDING,
    override val lastModified: Instant = Clock.System.now(),
    override val isDeleted: Boolean = false,
) : SyncableEntity
