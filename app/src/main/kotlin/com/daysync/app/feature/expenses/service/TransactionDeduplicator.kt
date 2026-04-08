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
        referenceId: String? = null,
    ): ExpenseEntity? {
        // First check by reference number (exact match, most reliable)
        if (!referenceId.isNullOrBlank()) {
            expenseDao.findByNotes("Ref: $referenceId")?.let { return it }
        }

        // Fallback: amount + date + time window
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
        return newMerchantName.length > existing.merchantName.length
    }
}
