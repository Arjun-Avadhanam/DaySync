package com.daysync.app.feature.expenses.service

import com.daysync.app.core.database.dao.ExpenseDao
import com.daysync.app.core.database.entity.ExpenseEntity
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.LocalDate

class TransactionDeduplicator(
    private val expenseDao: ExpenseDao,
) {
    suspend fun findDuplicate(
        amount: Double,
        date: LocalDate,
        timestampMillis: Long,
    ): ExpenseEntity? {
        val window = 2.minutes.inWholeMilliseconds
        return expenseDao.findDuplicate(
            amount = amount,
            date = date,
            windowStart = timestampMillis - window,
            windowEnd = timestampMillis + window,
        )
    }

    suspend fun findDuplicateForCsv(
        amount: Double,
        date: LocalDate,
    ): ExpenseEntity? {
        // Wider window for CSV: entire day
        val dayStartMillis = 0L
        val dayEndMillis = Long.MAX_VALUE
        return expenseDao.findDuplicate(
            amount = amount,
            date = date,
            windowStart = dayStartMillis,
            windowEnd = dayEndMillis,
        )
    }

    fun shouldUpdateExisting(existing: ExpenseEntity, newMerchantName: String?): Boolean {
        if (newMerchantName.isNullOrBlank()) return false
        if (existing.merchantName.isNullOrBlank()) return true
        // Prefer longer/richer merchant name
        return newMerchantName.length > existing.merchantName.length
    }
}
