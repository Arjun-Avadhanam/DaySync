package com.daysync.app.feature.expenses.budget.model

import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class CalendarWeeksTest {
    @Test
    fun `weekStart returns the Monday of the containing week`() {
        // Jul 2026: Jul 6 is Monday, Jul 12 is Sunday
        assertEquals(LocalDate(2026, 7, 6), CalendarWeeks.weekStart(LocalDate(2026, 7, 6)))  // Monday -> itself
        assertEquals(LocalDate(2026, 7, 6), CalendarWeeks.weekStart(LocalDate(2026, 7, 10))) // Friday
        assertEquals(LocalDate(2026, 7, 6), CalendarWeeks.weekStart(LocalDate(2026, 7, 12))) // Sunday
        assertEquals(LocalDate(2026, 6, 29), CalendarWeeks.weekStart(LocalDate(2026, 7, 1)))  // Wed -> prior Monday
    }

    @Test
    fun `weekEnd is the Sunday six days after the Monday`() {
        assertEquals(LocalDate(2026, 7, 12), CalendarWeeks.weekEnd(LocalDate(2026, 7, 6)))
        // Crosses a month boundary
        assertEquals(LocalDate(2026, 8, 2), CalendarWeeks.weekEnd(LocalDate(2026, 7, 27)))
    }

    @Test
    fun `weeksOverlappingMonth covers all Mon-Sun weeks touching the month`() {
        val weeks = CalendarWeeks.weeksOverlappingMonth(2026, 7)
        assertEquals(
            listOf(
                LocalDate(2026, 6, 29), LocalDate(2026, 7, 6), LocalDate(2026, 7, 13),
                LocalDate(2026, 7, 20), LocalDate(2026, 7, 27),
            ),
            weeks.map { it.start },
        )
        assertEquals(LocalDate(2026, 7, 5), weeks.first().end)   // partial leading week
        assertEquals(LocalDate(2026, 8, 2), weeks.last().end)    // trailing week spills into August
    }
}
