package com.daysync.app.feature.expenses.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daysync.app.core.database.entity.PayeeRuleEntity
import com.daysync.app.feature.expenses.data.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class PayeeRulesViewModel @Inject constructor(
    private val repository: ExpenseRepository,
) : ViewModel() {

    val rules: StateFlow<List<PayeeRuleEntity>> = repository.getPayeeRules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addRule(payeeName: String, category: String, defaultTitle: String?) {
        viewModelScope.launch {
            repository.addPayeeRule(payeeName, category, defaultTitle)
        }
    }

    fun deleteRule(id: String) {
        viewModelScope.launch {
            repository.deletePayeeRule(id)
        }
    }
}
