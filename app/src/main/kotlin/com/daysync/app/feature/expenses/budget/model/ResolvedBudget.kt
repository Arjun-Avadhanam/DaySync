package com.daysync.app.feature.expenses.budget.model

import kotlinx.datetime.LocalDate

enum class BudgetKind { MONTHLY, WEEKLY, CUSTOM }

data class ResolvedBudget(
    val instanceKey: String, // stable dedup key: "MONTHLY:2026-07", "WEEKLY:2026-07:2", "CUSTOM:<id>"
    val kind: BudgetKind,
    val label: String,
    val start: LocalDate,
    val end: LocalDate,
    val amount: Double,
)
