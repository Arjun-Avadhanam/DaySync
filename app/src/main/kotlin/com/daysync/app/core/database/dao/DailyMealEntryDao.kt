package com.daysync.app.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.daysync.app.core.database.entity.DailyMealEntryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

@Dao
interface DailyMealEntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DailyMealEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<DailyMealEntryEntity>)

    @Update
    suspend fun update(entity: DailyMealEntryEntity)

    @Delete
    suspend fun delete(entity: DailyMealEntryEntity)

    @Query("SELECT * FROM daily_meal_entries WHERE id = :id")
    suspend fun getById(id: String): DailyMealEntryEntity?

    @Query("SELECT * FROM daily_meal_entries WHERE date = :date AND isDeleted = 0 ORDER BY mealTime ASC")
    fun getByDate(date: LocalDate): Flow<List<DailyMealEntryEntity>>

    @Query("SELECT * FROM daily_meal_entries WHERE isDeleted = 0 ORDER BY date DESC")
    fun getAll(): Flow<List<DailyMealEntryEntity>>

    @Query("SELECT * FROM daily_meal_entries WHERE syncStatus = 'PENDING'")
    suspend fun getPendingSync(): List<DailyMealEntryEntity>

    @Query("SELECT * FROM daily_meal_entries WHERE date = :date AND mealTime = :mealTime AND isDeleted = 0 ORDER BY lastModified ASC")
    fun getByDateAndMealTime(date: LocalDate, mealTime: String): Flow<List<DailyMealEntryEntity>>

    @Query("SELECT * FROM daily_meal_entries WHERE date >= :startDate AND date <= :endDate AND isDeleted = 0 ORDER BY date ASC, mealTime ASC")
    fun getByDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<DailyMealEntryEntity>>

    @Query("SELECT * FROM daily_meal_entries WHERE date = :date AND isDeleted = 0 ORDER BY mealTime ASC")
    suspend fun getMealEntriesByDateSync(date: LocalDate): List<DailyMealEntryEntity>

    @Query("DELETE FROM daily_meal_entries WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE daily_meal_entries SET syncStatus = 'SYNCED' WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<String>)
}
