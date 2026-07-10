package com.daysync.app.feature.expenses.budget

import com.daysync.app.core.database.entity.BudgetEntity
import com.daysync.app.core.sync.SyncStatus
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BudgetResolverTest {
    private fun budget(
        id: String, type: String, amount: Double, recurring: Boolean,
        yearMonth: String? = null, weekBlock: Int? = null,
        start: LocalDate? = null, end: LocalDate? = null, label: String? = null,
    ) = BudgetEntity(
        id = id, type = type, amount = amount, recurring = recurring,
        yearMonth = yearMonth, weekBlock = weekBlock, startDate = start, endDate = end, label = label,
        syncStatus = SyncStatus.SYNCED, lastModified = Instant.fromEpochMilliseconds(0L),
    )

    @Test
    fun `monthly override wins over recurring monthly`() {
        val budgets = listOf(
            budget("r", "MONTHLY", 40000.0, recurring = true),
            budget("o", "MONTHLY", 50000.0, recurring = false, yearMonth = "2026-07"),
        )
        val r = BudgetResolver.monthlyFor(budgets, 2026, 7)!!
        assertEquals(50000.0, r.amount, 0.0)
        assertEquals("MONTHLY:2026-07", r.instanceKey)
        assertEquals(LocalDate(2026, 7, 1), r.start)
        assertEquals(LocalDate(2026, 7, 31), r.end)
    }

    @Test
    fun `recurring monthly used when no override`() {
        val budgets = listOf(budget("r", "MONTHLY", 40000.0, recurring = true))
        assertEquals(40000.0, BudgetResolver.monthlyFor(budgets, 2026, 8)!!.amount, 0.0)
    }

    @Test
    fun `per-week override wins over recurring flat cap`() {
        val budgets = listOf(
            budget("flat", "WEEKLY", 10000.0, recurring = true),
            budget("ovr", "WEEKLY", 15000.0, recurring = false,
                start = LocalDate(2026, 7, 6), end = LocalDate(2026, 7, 12)),
        )
        // week of Mon Jul 6
        assertEquals(15000.0, BudgetResolver.weeklyForWeek(budgets, LocalDate(2026, 7, 6))!!.amount, 0.0)
        assertEquals("WEEKLY:2026-07-06", BudgetResolver.weeklyForWeek(budgets, LocalDate(2026, 7, 6))!!.instanceKey)
        // a different week falls back to the flat cap
        assertEquals(10000.0, BudgetResolver.weeklyForWeek(budgets, LocalDate(2026, 7, 13))!!.amount, 0.0)
    }

    @Test
    fun `weeklyForWeek is null when neither override nor flat cap exists`() {
        assertNull(BudgetResolver.weeklyForWeek(emptyList(), LocalDate(2026, 7, 6)))
    }

    @Test
    fun `coveringDate resolves the calendar week containing the date`() {
        val budgets = listOf(
            budget("m", "MONTHLY", 40000.0, recurring = true),
            budget("w", "WEEKLY", 10000.0, recurring = true),
            budget("c", "CUSTOM", 8000.0, recurring = false, yearMonth = "2026-07",
                start = LocalDate(2026, 7, 5), end = LocalDate(2026, 7, 12), label = "Trip"),
        )
        // Jul 10 is in the Mon Jul 6 week
        val keys = BudgetResolver.coveringDate(budgets, LocalDate(2026, 7, 10)).map { it.instanceKey }.toSet()
        assertEquals(setOf("MONTHLY:2026-07", "WEEKLY:2026-07-06", "CUSTOM:c"), keys)
    }

    @Test
    fun `no monthly budget yields null`() {
        assertNull(BudgetResolver.monthlyFor(emptyList(), 2026, 7))
    }
}
