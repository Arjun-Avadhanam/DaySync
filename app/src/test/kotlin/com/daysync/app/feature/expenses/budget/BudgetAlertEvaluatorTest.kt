package com.daysync.app.feature.expenses.budget

import com.daysync.app.core.database.dao.BudgetDao
import com.daysync.app.core.database.dao.ExpenseDao
import com.daysync.app.core.database.entity.BudgetEntity
import com.daysync.app.core.sync.SyncStatus
import io.mockk.coEvery
import io.mockk.mockk
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class BudgetAlertEvaluatorTest {
    private fun monthly(amount: Double) = BudgetEntity(
        id = "m", type = "MONTHLY", amount = amount, recurring = true,
        syncStatus = SyncStatus.SYNCED, lastModified = Instant.fromEpochMilliseconds(0L),
    )

    @Test
    fun `fires 75 alert once then re-arms after a delete drops spend`() = runTest {
        val budgetDao = mockk<BudgetDao>()
        val expenseDao = mockk<ExpenseDao>()
        val store = InMemoryLevels()
        coEvery { budgetDao.getAllActiveList() } returns listOf(monthly(40000.0))

        val evaluator = BudgetAlertEvaluator(budgetDao, expenseDao, store) { _, _ -> }

        // 80% -> notify 75
        coEvery { expenseDao.getTotalInRangeOnce(any(), any()) } returns 32000.0
        val fired1 = mutableListOf<Pair<String, Int>>()
        evaluator.evaluate(LocalDate(2026, 7, 10)) { rb, lvl -> fired1 += rb.instanceKey to lvl }
        assertEquals(listOf("MONTHLY:2026-07" to 75), fired1)

        // still 80% -> no notify
        val fired2 = mutableListOf<Pair<String, Int>>()
        evaluator.evaluate(LocalDate(2026, 7, 10)) { rb, lvl -> fired2 += rb.instanceKey to lvl }
        assertEquals(emptyList<Pair<String, Int>>(), fired2)

        // delete drops to 40% (below the 50% band) -> re-arm to 0, no notify
        coEvery { expenseDao.getTotalInRangeOnce(any(), any()) } returns 16000.0
        evaluator.evaluate(LocalDate(2026, 7, 10)) { _, _ -> }
        assertEquals(0, store.getLevel("MONTHLY:2026-07"))

        // climbing back to 80% re-notifies 75 (the marker had re-armed)
        coEvery { expenseDao.getTotalInRangeOnce(any(), any()) } returns 32000.0
        val fired3 = mutableListOf<Pair<String, Int>>()
        evaluator.evaluate(LocalDate(2026, 7, 10)) { rb, lvl -> fired3 += rb.instanceKey to lvl }
        assertEquals(listOf("MONTHLY:2026-07" to 75), fired3)
    }

    private class InMemoryLevels : BudgetAlertLevels {
        val m = mutableMapOf<String, Int>()
        override fun getLevel(key: String) = m[key] ?: 0
        override fun setLevel(key: String, level: Int) { m[key] = level }
        override fun keys() = m.keys.toSet()
        override fun prune(validKeys: Set<String>) { m.keys.retainAll(validKeys) }
    }
}
