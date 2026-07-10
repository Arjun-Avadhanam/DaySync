package com.daysync.app.feature.expenses.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daysync.app.feature.expenses.data.ExpenseRepository
import com.daysync.app.feature.expenses.model.Expense
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

sealed interface ExpensePeriod {
    data class Monthly(val year: Int, val month: Int) : ExpensePeriod
    data class Weekly(val year: Int, val month: Int, val blockIndex: Int) : ExpensePeriod
    data class Custom(val start: LocalDate, val end: LocalDate) : ExpensePeriod
}

object WeeklyNav {
    fun next(w: ExpensePeriod.Weekly): ExpensePeriod.Weekly {
        val blocks = com.daysync.app.feature.expenses.budget.model.MonthWeeks.blocksFor(w.year, w.month).size
        return if (w.blockIndex < blocks) w.copy(blockIndex = w.blockIndex + 1)
        else {
            val (y, m) = if (w.month == 12) w.year + 1 to 1 else w.year to w.month + 1
            ExpensePeriod.Weekly(y, m, 1)
        }
    }

    fun previous(w: ExpensePeriod.Weekly): ExpensePeriod.Weekly {
        return if (w.blockIndex > 1) w.copy(blockIndex = w.blockIndex - 1)
        else {
            val (y, m) = if (w.month == 1) w.year - 1 to 12 else w.year to w.month - 1
            val lastBlock = com.daysync.app.feature.expenses.budget.model.MonthWeeks.blocksFor(y, m).size
            ExpensePeriod.Weekly(y, m, lastBlock)
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ExpensesViewModel @Inject constructor(
    private val repository: ExpenseRepository,
    private val userPreferences: com.daysync.app.core.config.UserPreferences,
) : ViewModel() {

    private val today = Clock.System.now().toLocalDateTime(userPreferences.kotlinTimeZone).date

    private val _period = MutableStateFlow<ExpensePeriod>(
        ExpensePeriod.Monthly(today.year, today.monthNumber)
    )
    val period: StateFlow<ExpensePeriod> = _period

    private val dateRange = _period.flatMapLatest { period ->
        val (start, end) = when (period) {
            is ExpensePeriod.Monthly -> {
                val s = LocalDate(period.year, period.month, 1)
                val e = LocalDate(period.year, period.month, daysInMonth(period.year, period.month))
                s to e
            }
            is ExpensePeriod.Weekly -> {
                val block = com.daysync.app.feature.expenses.budget.model.MonthWeeks
                    .blocksFor(period.year, period.month)
                    .first { it.index == period.blockIndex }
                block.start to block.end
            }
            is ExpensePeriod.Custom -> period.start to period.end
        }
        kotlinx.coroutines.flow.flowOf(start to end)
    }

    val uiState: StateFlow<ExpensesListUiState> = dateRange.flatMapLatest { (start, end) ->
        val expensesFlow = repository.getExpensesInRange(start, end)
        val monthlyTotalFlow = repository.getMonthlyTotal(start, end)
        val categoryTotalsFlow = repository.getCategoryTotals(start, end)

        combine(expensesFlow, monthlyTotalFlow, categoryTotalsFlow) { expenses, total, cats ->
            val grouped = expenses.groupBy { it.date }
            val dailyTotals = grouped.mapValues { (_, list) -> list.sumOf { it.totalAmount } }
            ExpensesListUiState.Success(
                expenses = grouped,
                dailyTotals = dailyTotals,
                monthlyTotal = total,
                selectedYear = start.year,
                selectedMonth = start.monthNumber,
                categoryTotals = cats,
                isCustomRange = _period.value is ExpensePeriod.Custom,
                rangeLabel = when (val p = _period.value) {
                    is ExpensePeriod.Custom -> com.daysync.app.core.ui.formatRangeLabel(p.start, p.end)
                    is ExpensePeriod.Weekly -> {
                        val b = com.daysync.app.feature.expenses.budget.model.MonthWeeks
                            .blocksFor(p.year, p.month).first { it.index == p.blockIndex }
                        val mon = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")[p.month - 1]
                        "$mon ${b.start.dayOfMonth}–${b.end.dayOfMonth}"
                    }
                    is ExpensePeriod.Monthly -> null
                },
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExpensesListUiState.Loading)

    fun previousMonth() {
        val current = _period.value
        if (current !is ExpensePeriod.Monthly) return
        val (year, month) = if (current.month == 1) {
            current.year - 1 to 12
        } else {
            current.year to current.month - 1
        }
        _period.value = ExpensePeriod.Monthly(year, month)
    }

    fun nextMonth() {
        val current = _period.value
        if (current !is ExpensePeriod.Monthly) return
        val (year, month) = if (current.month == 12) {
            current.year + 1 to 1
        } else {
            current.year to current.month + 1
        }
        _period.value = ExpensePeriod.Monthly(year, month)
    }

    fun showWeekly() {
        val block = com.daysync.app.feature.expenses.budget.model.MonthWeeks.blockContaining(today)
        _period.value = ExpensePeriod.Weekly(today.year, today.monthNumber, block.index)
    }

    fun showMonthly() {
        _period.value = ExpensePeriod.Monthly(today.year, today.monthNumber)
    }

    fun previousWeek() {
        (_period.value as? ExpensePeriod.Weekly)?.let { _period.value = WeeklyNav.previous(it) }
    }

    fun nextWeek() {
        (_period.value as? ExpensePeriod.Weekly)?.let { _period.value = WeeklyNav.next(it) }
    }

    fun setCustomRange(start: LocalDate, end: LocalDate) {
        _period.value = ExpensePeriod.Custom(start, end)
    }

    fun resetToMonthly() {
        _period.value = ExpensePeriod.Monthly(today.year, today.monthNumber)
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            repository.deleteExpense(expense.id)
        }
    }

    private fun daysInMonth(year: Int, month: Int): Int {
        return when (month) {
            1, 3, 5, 7, 8, 10, 12 -> 31
            4, 6, 9, 11 -> 30
            2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
            else -> 30
        }
    }
}
