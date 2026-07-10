package com.daysync.app.feature.expenses.budget.model

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus

data class CalendarWeek(val start: LocalDate, val end: LocalDate)

object CalendarWeeks {
    fun daysInMonth(year: Int, month: Int): Int = when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
        else -> 30
    }

    /** Monday of the ISO week containing [date]. isoDayNumber: Monday=1 … Sunday=7. */
    fun weekStart(date: LocalDate): LocalDate = date.minus(date.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)

    fun weekEnd(monday: LocalDate): LocalDate = monday.plus(6, DateTimeUnit.DAY)

    /** Every Mon–Sun week (by Monday) that overlaps [first-of-month .. last-of-month]. */
    fun weeksOverlappingMonth(year: Int, month: Int): List<CalendarWeek> {
        val first = LocalDate(year, month, 1)
        val last = LocalDate(year, month, daysInMonth(year, month))
        val weeks = mutableListOf<CalendarWeek>()
        var monday = weekStart(first)
        while (monday <= last) {
            weeks += CalendarWeek(monday, weekEnd(monday))
            monday = monday.plus(7, DateTimeUnit.DAY)
        }
        return weeks
    }
}
