package com.daysync.app.feature.expenses.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daysync.app.feature.expenses.data.ExpenseRepository
import com.daysync.app.feature.expenses.model.Expense
import com.daysync.app.feature.expenses.model.generateExpenseId
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@HiltViewModel
class ExpenseFormViewModel @Inject constructor(
    private val repository: ExpenseRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val expenseId: String? = savedStateHandle["expenseId"]

    private val _formState = MutableStateFlow(ExpenseFormState())
    val formState: StateFlow<ExpenseFormState> = _formState

    val merchantSuggestions: StateFlow<List<String>> = repository.getMerchantNames()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        if (expenseId != null) {
            viewModelScope.launch {
                val expense = repository.getExpenseById(expenseId) ?: return@launch
                val merchantName = expense.merchantName ?: ""
                _formState.value = ExpenseFormState(
                    id = expense.id,
                    amount = expense.totalAmount.toBigDecimal().stripTrailingZeros().toPlainString(),
                    category = expense.category,
                    merchantName = merchantName,
                    title = expense.title ?: "",
                    notes = expense.notes ?: "",
                    date = expense.date,
                    isEditing = true,
                    existingPayeeRule = merchantName
                        .takeIf { it.isNotBlank() }
                        ?.let { repository.getPayeeRule(it) },
                )
            }
        }
    }

    fun updateAmount(value: String) {
        _formState.value = _formState.value.copy(amount = value)
    }

    fun updateCategory(value: String?) {
        _formState.value = _formState.value.copy(category = value)
    }

    fun updateMerchantName(value: String) {
        _formState.value = _formState.value.copy(merchantName = value)
        viewModelScope.launch {
            val rule = value.takeIf { it.isNotBlank() }?.let { repository.getPayeeRule(it) }
            // Only apply if the merchant hasn't changed again while we were looking up
            if (_formState.value.merchantName == value) {
                _formState.value = _formState.value.copy(existingPayeeRule = rule)
            }
        }
    }

    fun updateTitle(value: String) {
        _formState.value = _formState.value.copy(title = value)
    }

    fun updateNotes(value: String) {
        _formState.value = _formState.value.copy(notes = value)
    }

    fun updateDate(value: kotlinx.datetime.LocalDate) {
        _formState.value = _formState.value.copy(date = value)
    }

    fun updateSaveAsPayeeRule(value: Boolean) {
        _formState.value = _formState.value.copy(saveAsPayeeRule = value)
    }

    fun save(onSuccess: () -> Unit) {
        val state = _formState.value
        val amount = state.amount.toDoubleOrNull() ?: return
        val date = state.date
            ?: Clock.System.now().toLocalDateTime(TimeZone.of("Asia/Kolkata")).date

        _formState.value = state.copy(isSaving = true)

        viewModelScope.launch {
            val expense = Expense(
                id = state.id ?: generateExpenseId(),
                title = state.title.ifBlank { null },
                date = date,
                category = state.category,
                unitCost = amount,
                totalAmount = amount,
                notes = state.notes.ifBlank { null },
                source = if (state.isEditing) "MANUAL" else "MANUAL",
                merchantName = state.merchantName.ifBlank { null },
            )

            if (state.isEditing) {
                repository.updateExpense(expense)
            } else {
                repository.addExpense(expense)
            }

            if (state.saveAsPayeeRule &&
                state.existingPayeeRule == null &&
                state.merchantName.isNotBlank() &&
                state.category != null
            ) {
                repository.addPayeeRule(
                    payeeName = state.merchantName,
                    category = state.category,
                    defaultTitle = state.title.ifBlank { null },
                )
            }

            _formState.value = _formState.value.copy(isSaving = false)
            onSuccess()
        }
    }
}
