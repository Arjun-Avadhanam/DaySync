package com.daysync.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.daysync.app.core.database.entity.PayeeRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PayeeRuleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PayeeRuleEntity)

    @Update
    suspend fun update(entity: PayeeRuleEntity)

    @Query("SELECT * FROM payee_rules WHERE LOWER(payeeName) = LOWER(:payeeName) LIMIT 1")
    suspend fun getByPayeeName(payeeName: String): PayeeRuleEntity?

    @Query("SELECT * FROM payee_rules ORDER BY payeeName ASC")
    fun getAll(): Flow<List<PayeeRuleEntity>>

    @Query("DELETE FROM payee_rules WHERE id = :id")
    suspend fun deleteById(id: String)
}
