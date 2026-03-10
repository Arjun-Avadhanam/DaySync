package com.daysync.app.feature.nutrition.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daysync.app.feature.nutrition.data.repository.NutritionRepository
import com.daysync.app.feature.nutrition.domain.model.FoodItem
import com.daysync.app.feature.nutrition.domain.model.MealTemplate
import com.daysync.app.feature.nutrition.domain.model.MealTemplateInput
import com.daysync.app.feature.nutrition.domain.model.MealTemplateItemInput
import com.daysync.app.feature.nutrition.domain.model.MealTemplateWithItems
import com.daysync.app.feature.nutrition.domain.model.MealTime
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

sealed interface MealTemplateEvent {
    data object Created : MealTemplateEvent
    data object Deleted : MealTemplateEvent
    data object Logged : MealTemplateEvent
    data class Error(val message: String) : MealTemplateEvent
}

@HiltViewModel
class MealTemplatesViewModel @Inject constructor(
    private val repository: NutritionRepository,
) : ViewModel() {

    val templates: StateFlow<List<MealTemplate>> = repository.getAllMealTemplates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allFoodItems: StateFlow<List<FoodItem>> = repository.getAllFoodItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedTemplate = MutableStateFlow<MealTemplateWithItems?>(null)
    val selectedTemplate: StateFlow<MealTemplateWithItems?> = _selectedTemplate

    private val _events = MutableSharedFlow<MealTemplateEvent>()
    val events = _events.asSharedFlow()

    fun loadTemplateWithItems(templateId: String) {
        viewModelScope.launch {
            _selectedTemplate.value = repository.getMealTemplateWithItems(templateId)
        }
    }

    fun createTemplate(name: String, description: String?, items: List<MealTemplateItemInput>) {
        viewModelScope.launch {
            try {
                repository.createMealTemplate(
                    MealTemplateInput(name = name, description = description, items = items)
                )
                _events.emit(MealTemplateEvent.Created)
            } catch (e: Exception) {
                _events.emit(MealTemplateEvent.Error(e.message ?: "Failed to create"))
            }
        }
    }

    fun deleteTemplate(id: String) {
        viewModelScope.launch {
            try {
                repository.deleteMealTemplate(id)
                _events.emit(MealTemplateEvent.Deleted)
            } catch (e: Exception) {
                _events.emit(MealTemplateEvent.Error(e.message ?: "Failed to delete"))
            }
        }
    }

    fun logTemplate(
        templateId: String,
        date: LocalDate,
        mealTime: MealTime,
        amountMultipliers: Map<String, Double>?,
    ) {
        viewModelScope.launch {
            try {
                repository.logMealFromTemplate(templateId, date, mealTime, amountMultipliers)
                _events.emit(MealTemplateEvent.Logged)
            } catch (e: Exception) {
                _events.emit(MealTemplateEvent.Error(e.message ?: "Failed to log"))
            }
        }
    }
}
