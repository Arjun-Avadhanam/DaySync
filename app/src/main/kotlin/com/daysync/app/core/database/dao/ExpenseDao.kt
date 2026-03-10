package com.daysync.app.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.daysync.app.core.database.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

data class CategoryTotal(
    val category: String,
    val total: Double,
    val count: Int,
)

@Dao
interface ExpenseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ExpenseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<ExpenseEntity>)

    @Update
    suspend fun update(entity: ExpenseEntity)

    @Delete
    suspend fun delete(entity: ExpenseEntity)

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getById(id: String): ExpenseEntity?

    @Query("SELECT * FROM expenses WHERE date = :date AND isDeleted = 0 ORDER BY lastModified DESC")
    fun getByDate(date: LocalDate): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE isDeleted = 0 ORDER BY date DESC, lastModified DESC")
    fun getAll(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE syncStatus = 'PENDING'")
    suspend fun getPendingSync(): List<ExpenseEntity>

    @Query(
        "SELECT * FROM expenses WHERE date >= :startDate AND date <= :endDate " +
            "AND isDeleted = 0 ORDER BY date DESC, lastModified DESC"
    )
    fun getExpensesInRange(startDate: LocalDate, endDate: LocalDate): Flow<List<ExpenseEntity>>

    @Query(
        "SELECT category, SUM(totalAmount) as total, COUNT(*) as count " +
            "FROM expenses WHERE date >= :startDate AND date <= :endDate " +
            "AND isDeleted = 0 AND category IS NOT NULL " +
            "GROUP BY category ORDER BY total DESC"
    )
    fun getTotalByCategory(startDate: LocalDate, endDate: LocalDate): Flow<List<CategoryTotal>>

    @Query(
        "SELECT COALESCE(SUM(totalAmount), 0.0) FROM expenses " +
            "WHERE date = :date AND isDeleted = 0"
    )
    fun getDailyTotal(date: LocalDate): Flow<Double>

    @Query(
        "SELECT COALESCE(SUM(totalAmount), 0.0) FROM expenses " +
            "WHERE date >= :startDate AND date <= :endDate AND isDeleted = 0"
    )
    fun getMonthlyTotal(startDate: LocalDate, endDate: LocalDate): Flow<Double>

    @Query(
        "SELECT * FROM expenses WHERE LOWER(merchantName) LIKE '%' || LOWER(:merchantName) || '%' " +
            "AND isDeleted = 0 ORDER BY date DESC"
    )
    fun getByMerchant(merchantName: String): Flow<List<ExpenseEntity>>

    @Query(
        "SELECT * FROM expenses WHERE ABS(totalAmount - :amount) < 0.01 " +
            "AND date = :date AND lastModified >= :windowStart AND lastModified <= :windowEnd " +
            "AND isDeleted = 0 LIMIT 1"
    )
    suspend fun findDuplicate(
        amount: Double,
        date: LocalDate,
        windowStart: Long,
        windowEnd: Long,
    ): ExpenseEntity?

    @Query(
        "SELECT DISTINCT merchantName FROM expenses " +
            "WHERE merchantName IS NOT NULL AND isDeleted = 0 ORDER BY merchantName ASC"
    )
    fun getAllMerchantNames(): Flow<List<String>>

    @Query(
        "SELECT DISTINCT category FROM expenses " +
            "WHERE category IS NOT NULL AND isDeleted = 0 ORDER BY category ASC"
    )
    fun getAllUsedCategories(): Flow<List<String>>

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM expenses WHERE date >= :startDate AND date <= :endDate AND isDeleted = 0 ORDER BY date ASC")
    suspend fun getByDateRange(startDate: String, endDate: String): List<ExpenseEntity>

    @Query("SELECT category, SUM(totalAmount) as total, COUNT(*) as count FROM expenses WHERE date >= :startDate AND date <= :endDate AND isDeleted = 0 AND category IS NOT NULL GROUP BY category ORDER BY total DESC")
    suspend fun getTotalByCategoryList(startDate: String, endDate: String): List<CategoryTotal>
}
