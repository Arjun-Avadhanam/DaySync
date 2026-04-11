package com.daysync.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.daysync.app.core.database.entity.WorkoutMetadataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutMetadataDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: WorkoutMetadataEntity)

    @Query("SELECT * FROM workout_metadata WHERE sessionId IN (:sessionIds)")
    fun observeBySessionIds(sessionIds: List<String>): Flow<List<WorkoutMetadataEntity>>

    @Query("SELECT * FROM workout_metadata WHERE sessionId = :sessionId LIMIT 1")
    suspend fun get(sessionId: String): WorkoutMetadataEntity?

    @Query("DELETE FROM workout_metadata WHERE sessionId = :sessionId")
    suspend fun deleteBySessionId(sessionId: String)
}
