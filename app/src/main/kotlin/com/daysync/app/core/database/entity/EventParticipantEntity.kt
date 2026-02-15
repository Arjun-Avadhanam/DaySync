package com.daysync.app.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "event_participants",
    foreignKeys = [
        ForeignKey(entity = SportEventEntity::class, parentColumns = ["id"], childColumns = ["eventId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = CompetitorEntity::class, parentColumns = ["id"], childColumns = ["competitorId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("eventId"), Index("competitorId")]
)
data class EventParticipantEntity(
    @PrimaryKey val id: String,
    val eventId: String,
    val competitorId: String,
    val position: Int? = null,
    val score: String? = null,
    val status: String? = null,
    val isWinner: Boolean = false,
    val detail: String? = null, // JSON string for sport-specific data
)
