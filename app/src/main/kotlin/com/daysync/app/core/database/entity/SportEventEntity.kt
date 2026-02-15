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
    tableName = "sport_events",
    foreignKeys = [
        ForeignKey(entity = SportEntity::class, parentColumns = ["id"], childColumns = ["sportId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = CompetitionEntity::class, parentColumns = ["id"], childColumns = ["competitionId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = VenueEntity::class, parentColumns = ["id"], childColumns = ["venueId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = CompetitorEntity::class, parentColumns = ["id"], childColumns = ["homeCompetitorId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = CompetitorEntity::class, parentColumns = ["id"], childColumns = ["awayCompetitorId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [
        Index("sportId"), Index("competitionId"), Index("venueId"),
        Index("homeCompetitorId"), Index("awayCompetitorId"),
        Index("scheduledAt"), Index("status"),
    ]
)
data class SportEventEntity(
    @PrimaryKey val id: String,
    val sportId: String,
    val competitionId: String,
    val venueId: String? = null,
    val scheduledAt: Instant,
    val status: String = "SCHEDULED", // SCHEDULED, LIVE, COMPLETED, POSTPONED, CANCELLED
    val homeCompetitorId: String? = null,
    val awayCompetitorId: String? = null,
    val homeScore: Int? = null,
    val awayScore: Int? = null,
    val eventName: String? = null,
    val round: String? = null,
    val season: String? = null,
    val resultDetail: String? = null, // JSON string for sport-specific data
    val lastUpdated: Instant = Clock.System.now(),
    val dataSource: String? = null,
    override val syncStatus: SyncStatus = SyncStatus.PENDING,
    override val lastModified: Instant = Clock.System.now(),
    override val isDeleted: Boolean = false,
) : SyncableEntity
