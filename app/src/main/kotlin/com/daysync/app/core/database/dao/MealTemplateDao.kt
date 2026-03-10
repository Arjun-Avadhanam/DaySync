package com.daysync.app.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.daysync.app.core.database.entity.MealTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MealTemplateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: MealTemplateEntity)

    @Update
    suspend fun update(entity: MealTemplateEntity)

    @Delete
    suspend fun delete(entity: MealTemplateEntity)

    @Query("SELECT * FROM meal_templates WHERE id = :id")
    suspend fun getById(id: String): MealTemplateEntity?

    @Query("SELECT * FROM meal_templates WHERE isDeleted = 0 ORDER BY name ASC")
    fun getAll(): Flow<List<MealTemplateEntity>>

    @Query("SELECT * FROM meal_templates WHERE syncStatus = 'PENDING'")
    suspend fun getPendingSync(): List<MealTemplateEntity>

    @Query("UPDATE meal_templates SET syncStatus = 'SYNCED' WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<String>)
}
