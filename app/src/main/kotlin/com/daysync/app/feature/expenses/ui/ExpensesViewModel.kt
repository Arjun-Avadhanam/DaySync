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

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ExpensesViewModel @Inject constructor(
    private val repository: ExpenseRepository,
) : ViewModel() {

    private val today = Clock.System.now().toLocalDateTime(TimeZone.of("Asia/Kolkata")).date
    private val _selectedYear = MutableStateFlow(today.year)
    private val _selectedMonth = MutableStateFlow(today.monthNumber)

    private val dateRange = combine(_selectedYear, _selectedMonth) { year, month ->
        val start = LocalDate(year, month, 1)
        val end = LocalDate(year, month, daysInMonth(year, month))
        start to end
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
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExpensesListUiState.Loading)

    fun previousMonth() {
        val year = _selectedYear.value
        val month = _selectedMonth.value
        if (month == 1) {
            _selectedYear.value = year - 1
            _selectedMonth.value = 12
        } else {
            _selectedMonth.value = month - 1
        }
    }

    fun nextMonth() {
        val year = _selectedYear.value
        val month = _selectedMonth.value
        if (month == 12) {
            _selectedYear.value = year + 1
            _selectedMonth.value = 1
        } else {
            _selectedMonth.value = month + 1
        }
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
