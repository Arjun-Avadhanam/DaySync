package com.daysync.app.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.daysync.app.core.sync.SyncStatus
import com.daysync.app.core.sync.SyncableEntity
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

@Entity(
    tableName = "journal_entries",
    indices = [Index("date")]
)
data class JournalEntryEntity(
    @PrimaryKey val id: String,
    val date: LocalDate,
    val title: String? = null,
    val content: String? = null,
    val mood: Int? = null, // 1-10 scale
    val tags: List<String> = emptyList(),
    val isArchived: Boolean = false,
    override val syncStatus: SyncStatus = SyncStatus.PENDING,
    override val lastModified: Instant = Clock.System.now(),
    override val isDeleted: Boolean = false,
) : SyncableEntity
