package com.daysync.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.daysync.app.core.sync.SyncStatus
import com.daysync.app.core.sync.SyncableEntity
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * User-set metadata attached to a specific exercise session — currently just
 * a sub-type classification (Push/Pull/Other for strength training, Leg
 * exercises/Other for generic workouts). Keyed by the Health Connect session
 * id so it stays attached if the session is re-imported.
 */
@Entity(tableName = "workout_metadata")
data class WorkoutMetadataEntity(
    @PrimaryKey val sessionId: String,
    val subType: String? = null,
    override val syncStatus: SyncStatus = SyncStatus.PENDING,
    override val lastModified: Instant = Clock.System.now(),
    override val isDeleted: Boolean = false,
) : SyncableEntity
