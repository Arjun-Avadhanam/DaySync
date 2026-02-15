package com.daysync.app.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.daysync.app.core.database.entity.DailyNutritionSummaryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

@Dao
interface DailyNutritionSummaryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DailyNutritionSummaryEntity)

    @Update
    suspend fun update(entity: DailyNutritionSummaryEntity)

    @Delete
    suspend fun delete(entity: DailyNutritionSummaryEntity)

    @Query("SELECT * FROM daily_nutrition_summaries WHERE id = :id")
    suspend fun getById(id: String): DailyNutritionSummaryEntity?

    @Query("SELECT * FROM daily_nutrition_summaries WHERE date = :date")
    suspend fun getByDate(date: LocalDate): DailyNutritionSummaryEntity?

    @Query("SELECT * FROM daily_nutrition_summaries WHERE isDeleted = 0 ORDER BY date DESC")
    fun getAll(): Flow<List<DailyNutritionSummaryEntity>>

    @Query("SELECT * FROM daily_nutrition_summaries WHERE syncStatus = 'PENDING'")
    suspend fun getPendingSync(): List<DailyNutritionSummaryEntity>
}
