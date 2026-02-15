package com.daysync.app.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "competitions",
    foreignKeys = [
        ForeignKey(
            entity = SportEntity::class,
            parentColumns = ["id"],
            childColumns = ["sportId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sportId")]
)
data class CompetitionEntity(
    @PrimaryKey val id: String,
    val sportId: String,
    val name: String,
    val shortName: String? = null,
    val country: String? = null,
    val logoUrl: String? = null,
    val apiFootballId: Int? = null,
    val footballDataId: String? = null,
    val espnSlug: String? = null,
)
