package com.daysync.app.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.daysync.app.core.database.entity.MediaItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: MediaItemEntity)

    @Update
    suspend fun update(entity: MediaItemEntity)

    @Delete
    suspend fun delete(entity: MediaItemEntity)

    @Query("SELECT * FROM media_items WHERE id = :id")
    suspend fun getById(id: String): MediaItemEntity?

    @Query("SELECT * FROM media_items WHERE isDeleted = 0 ORDER BY lastModified DESC")
    fun getAll(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE mediaType = :type AND isDeleted = 0 ORDER BY lastModified DESC")
    fun getByType(type: String): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE syncStatus = 'PENDING'")
    suspend fun getPendingSync(): List<MediaItemEntity>

    @Query("UPDATE media_items SET syncStatus = 'SYNCED' WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<String>)
}
