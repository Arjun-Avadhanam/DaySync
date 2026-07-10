package com.daysync.app.feature.expenses.budget.data

import com.daysync.app.core.database.entity.BudgetEntity
import com.daysync.app.feature.expenses.budget.model.BudgetSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

interface BudgetRepository {
    /** All active budget rows (for the setup screen). */
    fun observeActiveBudgets(): Flow<List<BudgetEntity>>

    /** Today-anchored money-left summary for the banner. */
    fun observeSummaryForDate(date: LocalDate): Flow<BudgetSummary>

    suspend fun setRecurringMonthly(amount: Double)
    suspend fun clearRecurringMonthly()
    suspend fun setRecurringFlatWeekly(amount: Double)
    suspend fun clearRecurringFlatWeekly()

    /** Replace the weekly split for a month. If repeatEveryMonth, also install as recurring pattern. */
    suspend fun setVaryingWeekly(year: Int, month: Int, amounts: Map<Int, Double>, repeatEveryMonth: Boolean)

    suspend fun addCustomBudget(year: Int, month: Int, start: LocalDate, end: LocalDate, amount: Double, label: String?)
    suspend fun updateCustomBudget(id: String, start: LocalDate, end: LocalDate, amount: Double, label: String?)
    suspend fun deleteBudget(id: String)
}
