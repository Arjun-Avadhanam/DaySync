package com.daysync.app.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.daysync.app.core.sync.SyncStatus
import com.daysync.app.core.sync.SyncableEntity
import kotlin.time.Clock
import kotlin.time.Instant

@Entity(
    tableName = "watchlist_entries",
    foreignKeys = [
        ForeignKey(entity = SportEventEntity::class, parentColumns = ["id"], childColumns = ["eventId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index(value = ["eventId"], unique = true)]
)
data class WatchlistEntryEntity(
    @PrimaryKey val id: String,
    val eventId: String,
    val addedAt: Instant = Clock.System.now(),
    val notify: Boolean = true,
    val notes: String? = null,
    override val syncStatus: SyncStatus = SyncStatus.PENDING,
    override val lastModified: Instant = Clock.System.now(),
    override val isDeleted: Boolean = false,
) : SyncableEntity
