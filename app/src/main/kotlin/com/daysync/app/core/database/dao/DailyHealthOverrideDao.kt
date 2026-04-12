package com.daysync.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.daysync.app.core.database.entity.DailyHealthOverrideEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

@Dao
interface DailyHealthOverrideDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DailyHealthOverrideEntity)

    @Query("SELECT * FROM daily_health_overrides WHERE date = :date LIMIT 1")
    fun observe(date: LocalDate): Flow<DailyHealthOverrideEntity?>

    @Query("SELECT * FROM daily_health_overrides WHERE date = :date LIMIT 1")
    suspend fun get(date: LocalDate): DailyHealthOverrideEntity?

    @Query("DELETE FROM daily_health_overrides WHERE date = :date")
    suspend fun deleteByDate(date: LocalDate)

    @Query("SELECT * FROM daily_health_overrides WHERE syncStatus = 'PENDING'")
    suspend fun getPendingSync(): List<DailyHealthOverrideEntity>

    @Query("UPDATE daily_health_overrides SET syncStatus = 'SYNCED' WHERE date IN (:dates)")
    suspend fun markAsSynced(dates: List<LocalDate>)
}
