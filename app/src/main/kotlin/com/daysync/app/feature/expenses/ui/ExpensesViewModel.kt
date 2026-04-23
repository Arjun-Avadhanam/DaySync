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
    data class Custom(val start: LocalDate, val end: LocalDate) : ExpensePeriod
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ExpensesViewModel @Inject constructor(
    private val repository: ExpenseRepository,
) : ViewModel() {

    private val today = Clock.System.now().toLocalDateTime(TimeZone.of("Asia/Kolkata")).date

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
