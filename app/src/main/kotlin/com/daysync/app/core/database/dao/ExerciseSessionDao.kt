package com.daysync.app.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.daysync.app.core.database.entity.ExerciseSessionEntity
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ExerciseSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<ExerciseSessionEntity>)

    @Update
    suspend fun update(entity: ExerciseSessionEntity)

    @Delete
    suspend fun delete(entity: ExerciseSessionEntity)

    @Query("SELECT * FROM exercise_sessions WHERE id = :id")
    suspend fun getById(id: String): ExerciseSessionEntity?

    @Query("SELECT * FROM exercise_sessions WHERE isDeleted = 0 ORDER BY startTime DESC")
    fun getAll(): Flow<List<ExerciseSessionEntity>>

    @Query(
        "SELECT * FROM exercise_sessions WHERE isDeleted = 0 " +
            "AND startTime >= :start AND startTime < :end ORDER BY startTime DESC"
    )
    fun getByDateRange(start: Instant, end: Instant): Flow<List<ExerciseSessionEntity>>

    @Query(
        "SELECT * FROM exercise_sessions WHERE isDeleted = 0 " +
            "ORDER BY startTime DESC LIMIT :limit"
    )
    fun getRecent(limit: Int): Flow<List<ExerciseSessionEntity>>

    @Query("SELECT * FROM exercise_sessions WHERE syncStatus = 'PENDING'")
    suspend fun getPendingSync(): List<ExerciseSessionEntity>

    @Query("SELECT * FROM exercise_sessions WHERE startTime >= :startMillis AND startTime <= :endMillis AND isDeleted = 0")
    suspend fun getByDateRange(startMillis: Long, endMillis: Long): List<ExerciseSessionEntity>

    @Query("UPDATE exercise_sessions SET syncStatus = 'SYNCED' WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<String>)
}
