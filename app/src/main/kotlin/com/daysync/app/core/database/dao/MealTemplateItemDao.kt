package com.daysync.app.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.daysync.app.core.database.entity.MealTemplateItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MealTemplateItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: MealTemplateItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<MealTemplateItemEntity>)

    @Update
    suspend fun update(entity: MealTemplateItemEntity)

    @Delete
    suspend fun delete(entity: MealTemplateItemEntity)

    @Query("SELECT * FROM meal_template_items WHERE templateId = :templateId")
    fun getByTemplateId(templateId: String): Flow<List<MealTemplateItemEntity>>

    @Query("SELECT * FROM meal_template_items WHERE syncStatus = 'PENDING'")
    suspend fun getPendingSync(): List<MealTemplateItemEntity>
}
