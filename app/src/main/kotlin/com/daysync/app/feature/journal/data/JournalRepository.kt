package com.daysync.app.feature.journal.data

import com.daysync.app.core.database.entity.JournalEntryEntity
import com.daysync.app.feature.journal.domain.JournalEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

interface JournalRepository {
    fun getAllEntries(): Flow<List<JournalEntry>>
    fun getArchivedEntries(): Flow<List<JournalEntry>>
    fun searchEntries(query: String): Flow<List<JournalEntry>>
    fun getEntriesByDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<JournalEntry>>
    suspend fun getEntryById(id: String): JournalEntry?
    suspend fun getEntryEntityById(id: String): JournalEntryEntity?
    suspend fun saveEntry(entry: JournalEntry)
    suspend fun deleteEntry(id: String)
    suspend fun toggleArchive(id: String)
    fun getAllUsedTags(): Flow<List<String>>
}
