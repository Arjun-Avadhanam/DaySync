package com.daysync.app.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.daysync.app.core.database.entity.HealthMetricEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthMetricDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: HealthMetricEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<HealthMetricEntity>)

    @Update
    suspend fun update(entity: HealthMetricEntity)

    @Delete
    suspend fun delete(entity: HealthMetricEntity)

    @Query("SELECT * FROM health_metrics WHERE id = :id")
    suspend fun getById(id: String): HealthMetricEntity?

    @Query("SELECT * FROM health_metrics WHERE isDeleted = 0 ORDER BY timestamp DESC")
    fun getAll(): Flow<List<HealthMetricEntity>>

    @Query("SELECT * FROM health_metrics WHERE syncStatus = 'PENDING'")
    suspend fun getPendingSync(): List<HealthMetricEntity>
}
