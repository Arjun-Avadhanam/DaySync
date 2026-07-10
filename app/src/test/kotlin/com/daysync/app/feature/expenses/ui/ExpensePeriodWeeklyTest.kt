package com.daysync.app.feature.expenses.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class ExpensePeriodWeeklyTest {
    @Test
    fun `nextWeek advances block and rolls into next month`() {
        // July 2026 has 5 blocks; from block 5 -> August block 1
        assertEquals(ExpensePeriod.Weekly(2026, 8, 1), WeeklyNav.next(ExpensePeriod.Weekly(2026, 7, 5)))
        assertEquals(ExpensePeriod.Weekly(2026, 7, 3), WeeklyNav.next(ExpensePeriod.Weekly(2026, 7, 2)))
    }

    @Test
    fun `previousWeek rolls into previous month's last block`() {
        // From August block 1 -> July block 5 (July has 5 blocks)
        assertEquals(ExpensePeriod.Weekly(2026, 7, 5), WeeklyNav.previous(ExpensePeriod.Weekly(2026, 8, 1)))
    }

    @Test
    fun `previousWeek into February lands on its last block (4)`() {
        // March block 1 -> February block 4 (2026 Feb has 4 blocks)
        assertEquals(ExpensePeriod.Weekly(2026, 2, 4), WeeklyNav.previous(ExpensePeriod.Weekly(2026, 3, 1)))
    }
}
