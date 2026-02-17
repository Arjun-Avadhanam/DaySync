package com.daysync.app.feature.expenses.data

import com.daysync.app.core.database.dao.CategoryTotal
import com.daysync.app.core.database.entity.PayeeRuleEntity
import com.daysync.app.feature.expenses.model.Expense
import com.daysync.app.feature.expenses.service.ParsedTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

interface ExpenseRepository {
    // Reads
    fun getExpensesForDate(date: LocalDate): Flow<List<Expense>>
    fun getExpensesInRange(startDate: LocalDate, endDate: LocalDate): Flow<List<Expense>>
    fun getDailyTotal(date: LocalDate): Flow<Double>
    fun getMonthlyTotal(startDate: LocalDate, endDate: LocalDate): Flow<Double>
    fun getCategoryTotals(startDate: LocalDate, endDate: LocalDate): Flow<List<CategoryTotal>>
    fun getMerchantNames(): Flow<List<String>>
    suspend fun getExpenseById(id: String): Expense?

    // Writes
    suspend fun addExpense(expense: Expense)
    suspend fun updateExpense(expense: Expense)
    suspend fun deleteExpense(id: String)

    // Notification processing
    suspend fun processNotification(parsed: ParsedTransaction): ProcessResult

    // CSV import
    suspend fun importFromCsv(entries: List<CsvExpenseEntry>): ImportResult

    // Payee rules
    fun getPayeeRules(): Flow<List<PayeeRuleEntity>>
    suspend fun addPayeeRule(payeeName: String, category: String, defaultTitle: String?)
    suspend fun getPayeeRule(payeeName: String): PayeeRuleEntity?
    suspend fun deletePayeeRule(id: String)
}

sealed class ProcessResult {
    data class Saved(val expense: Expense) : ProcessResult()
    data class Deduplicated(val existingId: String) : ProcessResult()
    data class NeedsClassification(val expense: Expense) : ProcessResult()
}

data class ImportResult(
    val imported: Int,
    val skippedDuplicates: Int,
    val errors: Int,
)
