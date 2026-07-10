package com.daysync.app.feature.expenses.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daysync.app.core.config.UserPreferences
import com.daysync.app.core.database.entity.BudgetEntity
import com.daysync.app.feature.expenses.budget.data.BudgetRepository
import com.daysync.app.feature.expenses.budget.model.MonthWeeks
import com.daysync.app.feature.expenses.budget.model.WeekBlock
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

@HiltViewModel
class BudgetSetupViewModel @Inject constructor(
    private val repository: BudgetRepository,
    userPreferences: UserPreferences,
) : ViewModel() {
    val today: LocalDate = Clock.System.now().toLocalDateTime(userPreferences.kotlinTimeZone).date

    val budgets: StateFlow<List<BudgetEntity>> =
        repository.observeActiveBudgets()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun blocksFor(year: Int, month: Int): List<WeekBlock> = MonthWeeks.blocksFor(year, month)

    fun setMonthly(amount: Double?) = viewModelScope.launch {
        if (amount == null || amount <= 0.0) repository.clearRecurringMonthly() else repository.setRecurringMonthly(amount)
    }

    fun setFlatWeekly(amount: Double?) = viewModelScope.launch {
        if (amount == null || amount <= 0.0) repository.clearRecurringFlatWeekly() else repository.setRecurringFlatWeekly(amount)
    }

    fun setVaryingWeekly(year: Int, month: Int, amounts: Map<Int, Double>, repeat: Boolean) = viewModelScope.launch {
        repository.setVaryingWeekly(year, month, amounts, repeat)
    }

    fun addCustom(year: Int, month: Int, start: LocalDate, end: LocalDate, amount: Double, label: String?) = viewModelScope.launch {
        repository.addCustomBudget(year, month, start, end, amount, label)
    }

    fun updateCustom(id: String, start: LocalDate, end: LocalDate, amount: Double, label: String?) = viewModelScope.launch {
        repository.updateCustomBudget(id, start, end, amount, label)
    }

    fun deleteBudget(id: String) = viewModelScope.launch { repository.deleteBudget(id) }
}
