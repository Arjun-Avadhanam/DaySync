package com.daysync.app.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.daysync.app.core.database.entity.JournalEntryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

@Dao
interface JournalEntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: JournalEntryEntity)

    @Update
    suspend fun update(entity: JournalEntryEntity)

    @Delete
    suspend fun delete(entity: JournalEntryEntity)

    @Query("SELECT * FROM journal_entries WHERE id = :id")
    suspend fun getById(id: String): JournalEntryEntity?

    @Query("SELECT * FROM journal_entries WHERE date = :date AND isDeleted = 0")
    suspend fun getByDate(date: LocalDate): JournalEntryEntity?

    @Query("SELECT * FROM journal_entries WHERE isDeleted = 0 AND isArchived = 0 ORDER BY date DESC")
    fun getAll(): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries WHERE syncStatus = 'PENDING'")
    suspend fun getPendingSync(): List<JournalEntryEntity>

    @Query("UPDATE journal_entries SET syncStatus = 'SYNCED' WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<String>)
}
