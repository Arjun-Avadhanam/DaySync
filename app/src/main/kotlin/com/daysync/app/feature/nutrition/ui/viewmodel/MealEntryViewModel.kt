package com.daysync.app.feature.nutrition.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daysync.app.feature.nutrition.data.repository.NutritionRepository
import com.daysync.app.feature.nutrition.domain.model.FoodItem
import com.daysync.app.feature.nutrition.domain.model.MealEntryInput
import com.daysync.app.feature.nutrition.domain.model.MealTime
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

sealed interface MealEntryEvent {
    data object Saved : MealEntryEvent
    data class Error(val message: String) : MealEntryEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MealEntryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: NutritionRepository,
) : ViewModel() {

    val date: LocalDate = savedStateHandle.get<String>("date")?.let { LocalDate.parse(it) }
        ?: kotlin.time.Clock.System.todayIn(TimeZone.currentSystemDefault())

    val mealTime: MealTime = savedStateHandle.get<String>("mealTime")?.let { MealTime.fromDbValue(it) }
        ?: MealTime.BREAKFAST

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val searchResults: StateFlow<List<FoodItem>> = _searchQuery.flatMapLatest { query ->
        if (query.isBlank()) {
            repository.getAllFoodItems()
        } else {
            repository.searchFoodItems(query)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedFood = MutableStateFlow<FoodItem?>(null)
    val selectedFood: StateFlow<FoodItem?> = _selectedFood

    private val _amount = MutableStateFlow(1.0)
    val amount: StateFlow<Double> = _amount

    private val _events = MutableSharedFlow<MealEntryEvent>()
    val events = _events.asSharedFlow()

    fun searchFood(query: String) {
        _searchQuery.value = query
    }

    fun selectFood(food: FoodItem) {
        _selectedFood.value = food
    }

    fun clearSelection() {
        _selectedFood.value = null
        _amount.value = 1.0
    }

    fun setAmount(amount: Double) {
        _amount.value = amount
    }

    fun saveMealEntry() {
        val food = _selectedFood.value ?: return
        viewModelScope.launch {
            try {
                repository.addMealEntry(
                    MealEntryInput(
                        date = date,
                        foodId = food.id,
                        mealTime = mealTime,
                        amount = _amount.value,
                    )
                )
                _events.emit(MealEntryEvent.Saved)
            } catch (e: Exception) {
                _events.emit(MealEntryEvent.Error(e.message ?: "Failed to save"))
            }
        }
    }
}
