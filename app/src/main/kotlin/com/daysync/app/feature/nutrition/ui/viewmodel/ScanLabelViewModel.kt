package com.daysync.app.feature.nutrition.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daysync.app.feature.nutrition.data.NutritionLabelExtractor
import com.daysync.app.feature.nutrition.data.repository.NutritionRepository
import com.daysync.app.feature.nutrition.domain.model.FoodItemInput
import com.daysync.app.feature.nutrition.domain.model.NutrientValues
import com.daysync.app.feature.nutrition.domain.model.NutritionLabelResult
import com.daysync.app.feature.nutrition.domain.model.UnitType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ExtractionState {
    data object Idle : ExtractionState
    data object Extracting : ExtractionState
    data class Success(val result: NutritionLabelResult) : ExtractionState
    data class Error(val message: String) : ExtractionState
}

sealed interface ScanLabelEvent {
    data object FoodSaved : ScanLabelEvent
    data class SaveError(val message: String) : ScanLabelEvent
}

@HiltViewModel
class ScanLabelViewModel @Inject constructor(
    private val extractor: NutritionLabelExtractor,
    private val repository: NutritionRepository,
) : ViewModel() {

    private val _imageUri = MutableStateFlow<Uri?>(null)
    val imageUri: StateFlow<Uri?> = _imageUri.asStateFlow()

    private val _extractionState = MutableStateFlow<ExtractionState>(ExtractionState.Idle)
    val extractionState: StateFlow<ExtractionState> = _extractionState.asStateFlow()

    private val _selectedUnitType = MutableStateFlow(UnitType.SERVING)
    val selectedUnitType: StateFlow<UnitType> = _selectedUnitType.asStateFlow()

    // Editable form fields
    private val _productName = MutableStateFlow("")
    val productName: StateFlow<String> = _productName.asStateFlow()

    private val _category = MutableStateFlow("")
    val category: StateFlow<String> = _category.asStateFlow()

    private val _servingDescription = MutableStateFlow("")
    val servingDescription: StateFlow<String> = _servingDescription.asStateFlow()

    private val _calories = MutableStateFlow("")
    val calories: StateFlow<String> = _calories.asStateFlow()

    private val _protein = MutableStateFlow("")
    val protein: StateFlow<String> = _protein.asStateFlow()

    private val _carbs = MutableStateFlow("")
    val carbs: StateFlow<String> = _carbs.asStateFlow()

    private val _fat = MutableStateFlow("")
    val fat: StateFlow<String> = _fat.asStateFlow()

    private val _sugar = MutableStateFlow("")
    val sugar: StateFlow<String> = _sugar.asStateFlow()

    private val _events = MutableSharedFlow<ScanLabelEvent>()
    val events = _events.asSharedFlow()

    fun setImageUri(uri: Uri) {
        _imageUri.value = uri
    }

    fun extractNutrition(context: Context) {
        val uri = _imageUri.value ?: return
        _extractionState.value = ExtractionState.Extracting

        viewModelScope.launch {
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw IllegalStateException("Cannot read image")

                val result = extractor.extractFromImage(bytes)
                _extractionState.value = ExtractionState.Success(result)

                // Pre-fill form fields
                _productName.value = result.productName
                _category.value = result.category ?: ""

                // Determine default unit type from detected unit
                val defaultUnit = when (result.detectedUnit?.lowercase()) {
                    "ml" -> UnitType.ML
                    "g" -> UnitType.GRAMS
                    else -> UnitType.GRAMS
                }
                _selectedUnitType.value = defaultUnit

                applyValuesToForm(result, defaultUnit)
            } catch (e: Exception) {
                _extractionState.value = ExtractionState.Error(
                    e.message ?: "Failed to extract nutrition data"
                )
            }
        }
    }

    fun setProductName(value: String) { _productName.value = value }
    fun setCategory(value: String) { _category.value = value }
    fun setServingDescription(value: String) { _servingDescription.value = value }
    fun setCalories(value: String) { _calories.value = value }
    fun setProtein(value: String) { _protein.value = value }
    fun setCarbs(value: String) { _carbs.value = value }
    fun setFat(value: String) { _fat.value = value }
    fun setSugar(value: String) { _sugar.value = value }

    fun setUnitType(unitType: UnitType) {
        _selectedUnitType.value = unitType
        val state = _extractionState.value
        if (state is ExtractionState.Success) {
            applyValuesToForm(state.result, unitType)
        }
    }

    private fun applyValuesToForm(result: NutritionLabelResult, unitType: UnitType) {
        // For GRAMS/ML → use per100 values; for other units → use perServing (fallback to per100)
        val values: NutrientValues?
        val servDesc: String

        if (unitType == UnitType.GRAMS || unitType == UnitType.ML) {
            values = result.per100
            val unit = if (unitType == UnitType.ML) "ml" else "g"
            servDesc = "per 100$unit"
        } else {
            values = result.perServing ?: result.per100
            servDesc = result.servingSize ?: "per serving"
        }

        _servingDescription.value = servDesc
        _calories.value = formatValue(values?.calories)
        _protein.value = formatValue(values?.proteinG)
        _carbs.value = formatValue(values?.carbsG)
        _fat.value = formatValue(values?.fatG)
        _sugar.value = formatValue(values?.sugarG)
    }

    private fun formatValue(value: Double?): String {
        if (value == null) return "0"
        return if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            "%.1f".format(value)
        }
    }

    fun saveFood() {
        viewModelScope.launch {
            try {
                val input = FoodItemInput(
                    name = _productName.value.trim(),
                    category = _category.value.trim().ifEmpty { null },
                    caloriesPerUnit = _calories.value.toDoubleOrNull() ?: 0.0,
                    proteinPerUnit = _protein.value.toDoubleOrNull() ?: 0.0,
                    carbsPerUnit = _carbs.value.toDoubleOrNull() ?: 0.0,
                    fatPerUnit = _fat.value.toDoubleOrNull() ?: 0.0,
                    sugarPerUnit = _sugar.value.toDoubleOrNull() ?: 0.0,
                    unitType = _selectedUnitType.value,
                    servingDescription = _servingDescription.value.trim().ifEmpty { null },
                )
                repository.addFoodItem(input)
                _events.emit(ScanLabelEvent.FoodSaved)
            } catch (e: Exception) {
                _events.emit(ScanLabelEvent.SaveError(e.message ?: "Failed to save"))
            }
        }
    }

    fun retry() {
        _extractionState.value = ExtractionState.Idle
        _imageUri.value = null
    }
}
