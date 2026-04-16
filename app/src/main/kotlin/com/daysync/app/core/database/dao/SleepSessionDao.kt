package com.daysync.app.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.daysync.app.core.database.entity.SleepSessionEntity
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow

@Dao
interface SleepSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SleepSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<SleepSessionEntity>)

    @Update
    suspend fun update(entity: SleepSessionEntity)

    @Delete
    suspend fun delete(entity: SleepSessionEntity)

    @Query("SELECT * FROM sleep_sessions WHERE id = :id")
    suspend fun getById(id: String): SleepSessionEntity?

    @Query("SELECT * FROM sleep_sessions WHERE isDeleted = 0 ORDER BY startTime DESC")
    fun getAll(): Flow<List<SleepSessionEntity>>

    @Query(
        "SELECT * FROM sleep_sessions WHERE isDeleted = 0 " +
            "AND endTime >= :start AND endTime < :end ORDER BY endTime DESC"
    )
    fun getByDateRange(start: Instant, end: Instant): Flow<List<SleepSessionEntity>>

    @Query(
        "SELECT * FROM sleep_sessions WHERE isDeleted = 0 " +
            "ORDER BY startTime DESC LIMIT 1"
    )
    fun getLatest(): Flow<SleepSessionEntity?>

    @Query("SELECT * FROM sleep_sessions WHERE syncStatus = 'PENDING'")
    suspend fun getPendingSync(): List<SleepSessionEntity>

    @Query("SELECT * FROM sleep_sessions WHERE endTime >= :startMillis AND endTime <= :endMillis AND isDeleted = 0")
    suspend fun getByDateRange(startMillis: Long, endMillis: Long): List<SleepSessionEntity>

    @Query("UPDATE sleep_sessions SET syncStatus = 'SYNCED' WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<String>)
}
