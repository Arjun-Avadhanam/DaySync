package com.daysync.app.feature.journal.domain

import com.daysync.app.core.database.entity.JournalEntryEntity
import com.daysync.app.core.sync.SyncStatus
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.datetime.LocalDate

data class JournalEntry(
    val id: String,
    val date: LocalDate,
    val title: String?,
    val content: String?,
    val mood: Mood?,
    val tags: List<String>,
    val isArchived: Boolean,
)

fun JournalEntryEntity.toDomain(): JournalEntry = JournalEntry(
    id = id,
    date = date,
    title = title,
    content = content,
    mood = Mood.fromInt(mood),
    tags = tags,
    isArchived = isArchived,
)

@OptIn(ExperimentalUuidApi::class)
fun JournalEntry.toEntity(): JournalEntryEntity = JournalEntryEntity(
    id = id,
    date = date,
    title = title,
    content = content,
    mood = mood?.intValue,
    tags = tags,
    isArchived = isArchived,
    syncStatus = SyncStatus.PENDING,
    lastModified = Clock.System.now(),
)

@OptIn(ExperimentalUuidApi::class)
fun newJournalEntryId(): String = Uuid.random().toString()
