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
    fun `weekly resolution precedence per-month override then pattern then flat`() {
        val budgets = listOf(
            budget("flat", "WEEKLY", 10000.0, recurring = true),                 // flat cap
            budget("pat2", "WEEKLY", 12000.0, recurring = true, weekBlock = 2),   // recurring pattern block 2
            budget("ovr2", "WEEKLY", 15000.0, recurring = false, yearMonth = "2026-07", weekBlock = 2),
        )
        val blocks = BudgetResolver.weeklyBlocksFor(budgets, 2026, 7)
        // block 1 -> flat, block 2 -> per-month override, block 3 -> flat
        assertEquals(10000.0, blocks.first { it.instanceKey == "WEEKLY:2026-07:1" }.amount, 0.0)
        assertEquals(15000.0, blocks.first { it.instanceKey == "WEEKLY:2026-07:2" }.amount, 0.0)
        assertEquals(10000.0, blocks.first { it.instanceKey == "WEEKLY:2026-07:3" }.amount, 0.0)
    }

    @Test
    fun `coveringDate returns monthly plus containing block plus containing customs`() {
        val budgets = listOf(
            budget("m", "MONTHLY", 40000.0, recurring = true),
            budget("w", "WEEKLY", 10000.0, recurring = true),
            budget("c", "CUSTOM", 8000.0, recurring = false, yearMonth = "2026-07",
                start = LocalDate(2026, 7, 5), end = LocalDate(2026, 7, 12), label = "Trip"),
        )
        val covering = BudgetResolver.coveringDate(budgets, LocalDate(2026, 7, 10))
        val keys = covering.map { it.instanceKey }.toSet()
        assertEquals(setOf("MONTHLY:2026-07", "WEEKLY:2026-07:2", "CUSTOM:c"), keys)
    }

    @Test
    fun `no monthly budget yields null`() {
        assertNull(BudgetResolver.monthlyFor(emptyList(), 2026, 7))
    }
}
