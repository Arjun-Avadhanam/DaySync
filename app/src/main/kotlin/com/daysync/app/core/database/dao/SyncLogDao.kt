package com.daysync.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.daysync.app.core.database.entity.SyncLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SyncLogEntity)

    @Query("SELECT * FROM sync_logs WHERE tableName = :tableName ORDER BY lastSyncAt DESC LIMIT 1")
    suspend fun getLastSync(tableName: String): SyncLogEntity?

    @Query("SELECT * FROM sync_logs ORDER BY lastSyncAt DESC")
    suspend fun getAll(): List<SyncLogEntity>

    @Query("SELECT * FROM sync_logs ORDER BY lastSyncAt DESC")
    fun observeAll(): Flow<List<SyncLogEntity>>

    @Query("SELECT * FROM sync_logs ORDER BY lastSyncAt DESC LIMIT 1")
    fun observeLastSync(): Flow<SyncLogEntity?>
}
