package com.daysync.app.feature.expenses.budget.model

import kotlinx.datetime.LocalDate

data class BudgetProgressItem(
    val instanceKey: String,
    val kind: BudgetKind,
    val label: String,
    val spent: Double,
    val amount: Double,
    val start: LocalDate,
    val end: LocalDate,
) {
    val remaining: Double get() = amount - spent
    val fraction: Float get() = if (amount <= 0.0) 0f else (spent / amount).toFloat()
}

data class BudgetSummary(
    val primary: BudgetProgressItem?,
    val monthly: BudgetProgressItem?,
    val all: List<BudgetProgressItem>,
)
