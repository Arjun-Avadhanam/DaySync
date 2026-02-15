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
    tableName = "followed_competitors",
    foreignKeys = [
        ForeignKey(entity = CompetitorEntity::class, parentColumns = ["id"], childColumns = ["competitorId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index(value = ["competitorId"], unique = true)]
)
data class FollowedCompetitorEntity(
    @PrimaryKey val id: String,
    val competitorId: String,
    val addedAt: Instant = Clock.System.now(),
    override val syncStatus: SyncStatus = SyncStatus.PENDING,
    override val lastModified: Instant = Clock.System.now(),
    override val isDeleted: Boolean = false,
) : SyncableEntity
