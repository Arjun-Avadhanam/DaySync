package com.daysync.app.feature.expenses.budget.model

import kotlinx.datetime.LocalDate

data class WeekBlock(val index: Int, val start: LocalDate, val end: LocalDate)

object MonthWeeks {
    fun daysInMonth(year: Int, month: Int): Int = when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
        else -> 30
    }

    fun blocksFor(year: Int, month: Int): List<WeekBlock> {
        val n = daysInMonth(year, month)
        val startDays = listOf(1, 8, 15, 22, 29).filter { it <= n }
        return startDays.mapIndexed { i, startDay ->
            val endDay = minOf(startDay + 6, n)
            WeekBlock(i + 1, LocalDate(year, month, startDay), LocalDate(year, month, endDay))
        }
    }

    fun blockContaining(date: LocalDate): WeekBlock =
        blocksFor(date.year, date.monthNumber).first { date >= it.start && date <= it.end }
}
