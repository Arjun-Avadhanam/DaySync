package com.daysync.app.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.daysync.app.core.database.entity.FoodItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: FoodItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<FoodItemEntity>)

    @Update
    suspend fun update(entity: FoodItemEntity)

    @Delete
    suspend fun delete(entity: FoodItemEntity)

    @Query("SELECT * FROM food_items WHERE id = :id")
    suspend fun getById(id: String): FoodItemEntity?

    @Query("SELECT * FROM food_items WHERE isDeleted = 0 ORDER BY name ASC")
    fun getAll(): Flow<List<FoodItemEntity>>

    @Query("SELECT * FROM food_items WHERE syncStatus = 'PENDING'")
    suspend fun getPendingSync(): List<FoodItemEntity>

    @Query("SELECT * FROM food_items WHERE isDeleted = 0 AND name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchByName(query: String): Flow<List<FoodItemEntity>>

    @Query("SELECT * FROM food_items WHERE isDeleted = 0 AND category = :category ORDER BY name ASC")
    fun getByCategory(category: String): Flow<List<FoodItemEntity>>

    @Query("SELECT DISTINCT category FROM food_items WHERE isDeleted = 0 AND category IS NOT NULL ORDER BY category ASC")
    fun getAllCategories(): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM food_items WHERE isDeleted = 0")
    fun getCount(): Flow<Int>
}
