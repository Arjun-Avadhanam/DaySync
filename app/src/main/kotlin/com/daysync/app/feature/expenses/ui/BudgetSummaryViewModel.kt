package com.daysync.app.feature.expenses.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daysync.app.core.config.UserPreferences
import com.daysync.app.feature.expenses.budget.data.BudgetRepository
import com.daysync.app.feature.expenses.budget.model.BudgetSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlin.time.Clock
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

@HiltViewModel
class BudgetSummaryViewModel @Inject constructor(
    budgetRepository: BudgetRepository,
    userPreferences: UserPreferences,
) : ViewModel() {
    private val today = Clock.System.now().toLocalDateTime(userPreferences.kotlinTimeZone).date

    val summary: StateFlow<BudgetSummary?> =
        budgetRepository.observeSummaryForDate(today)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
