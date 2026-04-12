package com.daysync.app.feature.journal.data

import com.daysync.app.core.database.dao.JournalEntryDao
import com.daysync.app.core.sync.SyncStatus
import com.daysync.app.feature.journal.domain.JournalEntry
import com.daysync.app.feature.journal.domain.toDomain
import com.daysync.app.feature.journal.domain.toEntity
import javax.inject.Inject
import kotlin.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

class JournalRepositoryImpl @Inject constructor(
    private val dao: JournalEntryDao,
) : JournalRepository {

    override fun getAllEntries(): Flow<List<JournalEntry>> =
        dao.getAll().map { entities -> entities.map { it.toDomain() } }

    override fun getArchivedEntries(): Flow<List<JournalEntry>> =
        dao.getArchivedEntries().map { entities -> entities.map { it.toDomain() } }

    override fun searchEntries(query: String): Flow<List<JournalEntry>> =
        dao.searchEntries(query).map { entities -> entities.map { it.toDomain() } }

    override fun getEntriesByDateRange(
        startDate: LocalDate,
        endDate: LocalDate,
    ): Flow<List<JournalEntry>> =
        dao.getByDateRange(startDate, endDate).map { entities -> entities.map { it.toDomain() } }

    override suspend fun getEntryById(id: String): JournalEntry? =
        dao.getById(id)?.toDomain()

    override suspend fun getEntryEntityById(id: String): com.daysync.app.core.database.entity.JournalEntryEntity? =
        dao.getById(id)

    override suspend fun saveEntry(entry: JournalEntry) {
        dao.insert(entry.toEntity())
    }

    override suspend fun deleteEntry(id: String) {
        val entity = dao.getById(id) ?: return
        dao.update(
            entity.copy(
                isDeleted = true,
                syncStatus = SyncStatus.PENDING,
                lastModified = Clock.System.now(),
            )
        )
    }

    override suspend fun toggleArchive(id: String) {
        val entity = dao.getById(id) ?: return
        dao.update(
            entity.copy(
                isArchived = !entity.isArchived,
                syncStatus = SyncStatus.PENDING,
                lastModified = Clock.System.now(),
            )
        )
    }

    override fun getAllUsedTags(): Flow<List<String>> =
        dao.getAll().map { entities ->
            entities.flatMap { it.tags }.distinct().sorted()
        }
}
