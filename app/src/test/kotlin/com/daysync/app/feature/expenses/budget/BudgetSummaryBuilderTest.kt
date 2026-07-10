package com.daysync.app.feature.expenses.budget

import com.daysync.app.feature.expenses.budget.data.BudgetSummaryBuilder
import com.daysync.app.feature.expenses.budget.model.BudgetKind
import com.daysync.app.feature.expenses.budget.model.ResolvedBudget
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class BudgetSummaryBuilderTest {
    private fun rb(key: String, kind: BudgetKind, amount: Double, start: LocalDate, end: LocalDate) =
        ResolvedBudget(key, kind, key, start, end, amount)

    @Test
    fun `primary is the smallest range and monthly is separated`() {
        val monthly = rb("MONTHLY:2026-07", BudgetKind.MONTHLY, 40000.0, LocalDate(2026, 7, 1), LocalDate(2026, 7, 31))
        val week = rb("WEEKLY:2026-07:2", BudgetKind.WEEKLY, 10000.0, LocalDate(2026, 7, 8), LocalDate(2026, 7, 14))
        val custom = rb("CUSTOM:c", BudgetKind.CUSTOM, 8000.0, LocalDate(2026, 7, 9), LocalDate(2026, 7, 11))
        val summary = BudgetSummaryBuilder.build(
            covering = listOf(monthly, week, custom),
            spentByKey = mapOf("MONTHLY:2026-07" to 22300.0, "WEEKLY:2026-07:2" to 6800.0, "CUSTOM:c" to 4800.0),
        )
        // custom is the smallest (3 days) -> primary
        assertEquals("CUSTOM:c", summary.primary!!.instanceKey)
        assertEquals(3200.0, summary.primary!!.remaining, 0.0)
        assertEquals("MONTHLY:2026-07", summary.monthly!!.instanceKey)
        assertEquals(3, summary.all.size)
    }

    @Test
    fun `no budgets yields empty summary`() {
        val summary = BudgetSummaryBuilder.build(emptyList(), emptyMap())
        assertEquals(null, summary.primary)
        assertEquals(null, summary.monthly)
        assertEquals(0, summary.all.size)
    }
}
