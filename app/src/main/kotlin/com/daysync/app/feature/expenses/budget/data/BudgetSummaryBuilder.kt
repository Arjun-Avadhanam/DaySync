package com.daysync.app.feature.expenses.budget.data

import com.daysync.app.feature.expenses.budget.model.BudgetKind
import com.daysync.app.feature.expenses.budget.model.BudgetProgressItem
import com.daysync.app.feature.expenses.budget.model.BudgetSummary
import com.daysync.app.feature.expenses.budget.model.ResolvedBudget

object BudgetSummaryBuilder {
    private fun ResolvedBudget.days(): Long = end.toEpochDays() - start.toEpochDays() + 1

    fun build(covering: List<ResolvedBudget>, spentByKey: Map<String, Double>): BudgetSummary {
        val items = covering.map { rb ->
            BudgetProgressItem(
                instanceKey = rb.instanceKey,
                kind = rb.kind,
                label = rb.label,
                spent = spentByKey[rb.instanceKey] ?: 0.0,
                amount = rb.amount,
                start = rb.start,
                end = rb.end,
            )
        }
        val monthly = items.firstOrNull { it.kind == BudgetKind.MONTHLY }
        // primary = smallest range that is NOT the monthly; fall back to monthly
        val primary = covering
            .filter { it.kind != BudgetKind.MONTHLY }
            .minWithOrNull(compareBy({ it.days() }, { it.end }, { it.instanceKey }))
            ?.let { chosen -> items.first { it.instanceKey == chosen.instanceKey } }
            ?: monthly
        return BudgetSummary(primary = primary, monthly = monthly, all = items)
    }
}
