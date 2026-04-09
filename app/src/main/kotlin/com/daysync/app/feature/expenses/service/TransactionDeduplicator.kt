package com.daysync.app.feature.expenses.service

import com.daysync.app.core.database.dao.ExpenseDao
import com.daysync.app.core.database.entity.ExpenseEntity
import kotlinx.datetime.LocalDate

class TransactionDeduplicator(
    private val expenseDao: ExpenseDao,
) {
    // A notification is a duplicate iff it carries the exact same reference ID.
    suspend fun findDuplicate(referenceId: String?): ExpenseEntity? {
        if (referenceId.isNullOrBlank()) return null
        return expenseDao.findByNotes("%Ref: $referenceId%")
    }

    suspend fun findDuplicateForCsv(
        amount: Double,
        date: LocalDate,
    ): ExpenseEntity? {
        return expenseDao.findDuplicate(
            amount = amount,
            date = date,
            windowStart = 0L,
            windowEnd = Long.MAX_VALUE,
        )
    }

    fun shouldUpdateExisting(existing: ExpenseEntity, newMerchantName: String?): Boolean {
        if (newMerchantName.isNullOrBlank()) return false
        if (existing.merchantName.isNullOrBlank()) return true
        return newMerchantName.length > existing.merchantName.length
    }
}
