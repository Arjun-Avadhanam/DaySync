package com.daysync.app.feature.expenses.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daysync.app.feature.expenses.data.ExpenseRepository
import com.daysync.app.feature.expenses.model.Expense
import com.daysync.app.feature.expenses.model.ExpenseCategory
import com.daysync.app.feature.expenses.service.GeminiReceiptService
import com.daysync.app.feature.expenses.service.MlKitReceiptParser
import com.daysync.app.feature.expenses.service.ReceiptData
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate

sealed interface ReceiptScanUiState {
    data object Idle : ReceiptScanUiState
    data object Processing : ReceiptScanUiState
    data class Preview(
        val imageUri: Uri,
        val receiptData: ReceiptData,
        val amount: String,
        val merchantName: String,
        val category: String?,
        val date: LocalDate?,
        val notes: String,
    ) : ReceiptScanUiState
    data object Saving : ReceiptScanUiState
    data class Done(val expense: Expense) : ReceiptScanUiState
    data class Error(val message: String) : ReceiptScanUiState
}

@HiltViewModel
class ReceiptScanViewModel @Inject constructor(
    private val repository: ExpenseRepository,
    private val geminiService: GeminiReceiptService,
    private val mlKitParser: MlKitReceiptParser,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ReceiptScanUiState>(ReceiptScanUiState.Idle)
    val uiState: StateFlow<ReceiptScanUiState> = _uiState

    fun processImage(uri: Uri) {
        _uiState.value = ReceiptScanUiState.Processing

        viewModelScope.launch {
            try {
                val imageBytes = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                } ?: throw IllegalStateException("Could not read image")

                val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"

                val receiptData = try {
                    geminiService.extractFromImage(imageBytes, mimeType)
                } catch (_: Exception) {
                    // Fallback to ML Kit offline OCR
                    withContext(Dispatchers.IO) {
                        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                            ?: throw IllegalStateException("Could not decode image")
                        mlKitParser.extractFromBitmap(bitmap)
                    } ?: throw IllegalStateException("Could not extract data from receipt")
                }

                val parsedDate = receiptData.date?.let {
                    runCatching { LocalDate.parse(it) }.getOrNull()
                }

                val lineItemsSummary = receiptData.lineItems?.joinToString(", ") { item ->
                    "${item.name} ₹${item.totalPrice}"
                } ?: ""

                _uiState.value = ReceiptScanUiState.Preview(
                    imageUri = uri,
                    receiptData = receiptData,
                    amount = receiptData.totalAmount.toString(),
                    merchantName = receiptData.merchantName ?: "",
                    category = receiptData.category?.let { cat ->
                        if (ExpenseCategory.fromCategoryString(cat) != null) cat else null
                    } ?: ExpenseCategory.suggestFromMerchant(receiptData.merchantName, null),
                    date = parsedDate,
                    notes = if (lineItemsSummary.isNotEmpty()) "Items: $lineItemsSummary" else "",
                )
            } catch (e: Exception) {
                _uiState.value = ReceiptScanUiState.Error(
                    e.message ?: "Failed to process receipt"
                )
            }
        }
    }

    fun updateAmount(value: String) {
        val state = _uiState.value as? ReceiptScanUiState.Preview ?: return
        _uiState.value = state.copy(amount = value)
    }

    fun updateMerchantName(value: String) {
        val state = _uiState.value as? ReceiptScanUiState.Preview ?: return
        _uiState.value = state.copy(merchantName = value)
    }

    fun updateCategory(value: String?) {
        val state = _uiState.value as? ReceiptScanUiState.Preview ?: return
        _uiState.value = state.copy(category = value)
    }

    fun updateDate(date: LocalDate) {
        val state = _uiState.value as? ReceiptScanUiState.Preview ?: return
        _uiState.value = state.copy(date = date)
    }

    fun updateNotes(value: String) {
        val state = _uiState.value as? ReceiptScanUiState.Preview ?: return
        _uiState.value = state.copy(notes = value)
    }

    fun save(onSuccess: () -> Unit) {
        val state = _uiState.value as? ReceiptScanUiState.Preview ?: return
        val amount = state.amount.toDoubleOrNull() ?: return

        _uiState.value = ReceiptScanUiState.Saving

        viewModelScope.launch {
            try {
                val receiptData = state.receiptData.copy(
                    merchantName = state.merchantName.ifBlank { null },
                    totalAmount = amount,
                    category = state.category,
                )

                val expense = withContext(Dispatchers.IO) {
                    repository.saveFromReceipt(receiptData, state.date)
                }
                _uiState.value = ReceiptScanUiState.Done(expense)
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = ReceiptScanUiState.Error(
                    e.message ?: "Failed to save expense"
                )
            }
        }
    }

    fun reset() {
        _uiState.value = ReceiptScanUiState.Idle
    }
}
