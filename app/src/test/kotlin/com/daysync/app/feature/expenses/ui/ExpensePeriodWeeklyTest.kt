package com.daysync.app.feature.expenses.ui

import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class ExpensePeriodWeeklyTest {
    @Test
    fun `nextWeek advances by seven days across a month boundary`() {
        assertEquals(
            ExpensePeriod.Weekly(LocalDate(2026, 8, 3)),
            WeeklyNav.next(ExpensePeriod.Weekly(LocalDate(2026, 7, 27))),
        )
    }

    @Test
    fun `previousWeek goes back seven days across a month boundary`() {
        assertEquals(
            ExpensePeriod.Weekly(LocalDate(2026, 7, 27)),
            WeeklyNav.previous(ExpensePeriod.Weekly(LocalDate(2026, 8, 3))),
        )
    }
}
