package com.daysync.app.feature.expenses.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daysync.app.feature.expenses.data.CsvExpenseParser
import com.daysync.app.feature.expenses.data.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class CsvImportViewModel @Inject constructor(
    private val repository: ExpenseRepository,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow<CsvImportUiState>(CsvImportUiState.Idle)
    val uiState: StateFlow<CsvImportUiState> = _uiState

    fun parseFile(uri: Uri, fileName: String) {
        _uiState.value = CsvImportUiState.Parsing(fileName)

        viewModelScope.launch {
            try {
                val entries = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        CsvExpenseParser.parseHdfcCsv(stream)
                    } ?: emptyList()
                }

                val debitEntries = entries.filter { it.debitAmount != null && it.debitAmount > 0 }

                if (debitEntries.isEmpty()) {
                    _uiState.value = CsvImportUiState.Error("No debit transactions found in file")
                    return@launch
                }

                _uiState.value = CsvImportUiState.Preview(
                    entries = debitEntries,
                    duplicateCount = 0,
                )
            } catch (e: Exception) {
                _uiState.value = CsvImportUiState.Error("Failed to parse CSV: ${e.message}")
            }
        }
    }

    fun confirmImport() {
        val state = _uiState.value
        if (state !is CsvImportUiState.Preview) return

        _uiState.value = CsvImportUiState.Importing(0, state.entries.size)

        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    repository.importFromCsv(state.entries)
                }
                _uiState.value = CsvImportUiState.Done(result)
            } catch (e: Exception) {
                _uiState.value = CsvImportUiState.Error("Import failed: ${e.message}")
            }
        }
    }

    fun reset() {
        _uiState.value = CsvImportUiState.Idle
    }
}
