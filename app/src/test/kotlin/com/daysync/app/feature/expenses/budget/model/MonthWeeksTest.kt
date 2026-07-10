package com.daysync.app.feature.expenses.budget.model

import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class MonthWeeksTest {
    @Test
    fun `31-day month has five blocks with correct bounds`() {
        val blocks = MonthWeeks.blocksFor(2026, 7) // July = 31 days
        assertEquals(5, blocks.size)
        assertEquals(WeekBlock(1, LocalDate(2026, 7, 1), LocalDate(2026, 7, 7)), blocks[0])
        assertEquals(WeekBlock(2, LocalDate(2026, 7, 8), LocalDate(2026, 7, 14)), blocks[1])
        assertEquals(WeekBlock(3, LocalDate(2026, 7, 15), LocalDate(2026, 7, 21)), blocks[2])
        assertEquals(WeekBlock(4, LocalDate(2026, 7, 22), LocalDate(2026, 7, 28)), blocks[3])
        assertEquals(WeekBlock(5, LocalDate(2026, 7, 29), LocalDate(2026, 7, 31)), blocks[4])
    }

    @Test
    fun `28-day February has four blocks and no block five`() {
        val blocks = MonthWeeks.blocksFor(2026, 2) // 2026 Feb = 28 days
        assertEquals(4, blocks.size)
        assertEquals(WeekBlock(4, LocalDate(2026, 2, 22), LocalDate(2026, 2, 28)), blocks[3])
    }

    @Test
    fun `29-day leap February has a one-day block five`() {
        val blocks = MonthWeeks.blocksFor(2028, 2) // 2028 leap = 29 days
        assertEquals(5, blocks.size)
        assertEquals(WeekBlock(5, LocalDate(2028, 2, 29), LocalDate(2028, 2, 29)), blocks[4])
    }

    @Test
    fun `blockContaining finds the block whose range holds the date`() {
        assertEquals(2, MonthWeeks.blockContaining(LocalDate(2026, 7, 10)).index)
        assertEquals(5, MonthWeeks.blockContaining(LocalDate(2026, 7, 31)).index)
        assertEquals(1, MonthWeeks.blockContaining(LocalDate(2026, 7, 1)).index)
    }
}
