package com.daysync.app.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.daysync.app.core.database.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Upsert
    suspend fun upsert(entity: BudgetEntity)

    @Upsert
    suspend fun upsertAll(entities: List<BudgetEntity>)

    @Query("SELECT * FROM budgets WHERE isDeleted = 0")
    fun getAllActive(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE isDeleted = 0")
    suspend fun getAllActiveList(): List<BudgetEntity>

    @Query("SELECT * FROM budgets WHERE id = :id")
    suspend fun getById(id: String): BudgetEntity?

    @Query("UPDATE budgets SET isDeleted = 1, syncStatus = 'PENDING', lastModified = :nowMillis WHERE id = :id")
    suspend fun softDelete(id: String, nowMillis: Long)

    @Query("SELECT * FROM budgets WHERE syncStatus = 'PENDING'")
    suspend fun getPendingSync(): List<BudgetEntity>

    @Query("UPDATE budgets SET syncStatus = 'SYNCED' WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<String>)

    // Hard-deletes for replacing weekly patterns (these rows are transient config,
    // superseded wholesale; soft-delete would leave stale resolved rows).
    @Query("DELETE FROM budgets WHERE type = 'WEEKLY' AND recurring = 1 AND weekBlock IS NOT NULL")
    suspend fun deleteRecurringWeeklyBlocks()

    @Query("DELETE FROM budgets WHERE type = 'WEEKLY' AND recurring = 0 AND yearMonth = :yearMonth")
    suspend fun deletePerMonthWeeklyBlocks(yearMonth: String)
}
