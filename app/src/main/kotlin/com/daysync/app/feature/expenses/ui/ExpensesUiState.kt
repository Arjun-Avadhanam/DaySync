package com.daysync.app.feature.expenses.ui

import com.daysync.app.core.database.dao.CategoryTotal
import com.daysync.app.feature.expenses.data.CsvExpenseEntry
import com.daysync.app.feature.expenses.data.ImportResult
import com.daysync.app.feature.expenses.model.Expense
import kotlinx.datetime.LocalDate

sealed interface ExpensesListUiState {
    data object Loading : ExpensesListUiState
    data class Success(
        val expenses: Map<LocalDate, List<Expense>>,
        val dailyTotals: Map<LocalDate, Double>,
        val monthlyTotal: Double,
        val selectedYear: Int,
        val selectedMonth: Int,
        val categoryTotals: List<CategoryTotal>,
    ) : ExpensesListUiState
    data class Error(val message: String) : ExpensesListUiState
}

data class ExpenseFormState(
    val id: String? = null,
    val amount: String = "",
    val category: String? = null,
    val merchantName: String = "",
    val title: String = "",
    val notes: String = "",
    val date: LocalDate? = null,
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val saveAsPayeeRule: Boolean = false,
    val merchantSuggestions: List<String> = emptyList(),
)

sealed interface CsvImportUiState {
    data object Idle : CsvImportUiState
    data class Parsing(val fileName: String) : CsvImportUiState
    data class Preview(val entries: List<CsvExpenseEntry>, val duplicateCount: Int) : CsvImportUiState
    data class Importing(val progress: Int, val total: Int) : CsvImportUiState
    data class Done(val result: ImportResult) : CsvImportUiState
    data class Error(val message: String) : CsvImportUiState
}
