package com.daysync.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.daysync.app.core.sync.SyncStatus
import com.daysync.app.core.sync.SyncableEntity
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

@Entity(tableName = "media_items")
data class MediaItemEntity(
    @PrimaryKey val id: String,
    val title: String,
    val mediaType: String, // BOOK, ARTICLE, TV_SERIES, FILM, PODCAST, MANGA, ANIME, COMIC, MUSIC, MOVIE, GAME
    val status: String = "NOT_STARTED", // NOT_STARTED, IN_PROGRESS, DONE, DROPPED
    val score: Double? = null, // 0.5-10 scale, 0.5 increments
    val creators: List<String> = emptyList(),
    val completedDate: LocalDate? = null,
    val notes: String? = null,
    override val syncStatus: SyncStatus = SyncStatus.PENDING,
    override val lastModified: Instant = Clock.System.now(),
    override val isDeleted: Boolean = false,
) : SyncableEntity
