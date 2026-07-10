package com.daysync.app.feature.expenses.budget

import com.daysync.app.core.database.dao.BudgetDao
import com.daysync.app.core.database.dao.ExpenseDao
import com.daysync.app.feature.expenses.budget.model.ResolvedBudget
import kotlinx.datetime.LocalDate
import javax.inject.Inject

class BudgetAlertEvaluator @Inject constructor(
    private val budgetDao: BudgetDao,
    private val expenseDao: ExpenseDao,
    private val alertLevels: BudgetAlertLevels,
    private val post: (ResolvedBudget, Int) -> Unit,
) {
    /** Re-evaluate every budget instance covering each changed date. */
    suspend fun onExpenseChanged(dates: Set<LocalDate>) {
        for (date in dates) evaluate(date) { rb, level -> post(rb, level) }
    }

    /** Android-free core: resolve covering budgets, decide alerts, update dedup store. */
    suspend fun evaluate(date: LocalDate, onFire: (ResolvedBudget, Int) -> Unit) {
        val budgets = budgetDao.getAllActiveList()
        val covering = BudgetResolver.coveringDate(budgets, date)
        for (rb in covering) {
            val spent = expenseDao.getTotalInRangeOnce(rb.start, rb.end)
            val last = alertLevels.getLevel(rb.instanceKey)
            val decision = BudgetThresholds.evaluate(spent, rb.amount, last)
            alertLevels.setLevel(rb.instanceKey, decision.newStoredLevel)
            decision.notifyLevel?.let { onFire(rb, it) }
        }
    }
}
