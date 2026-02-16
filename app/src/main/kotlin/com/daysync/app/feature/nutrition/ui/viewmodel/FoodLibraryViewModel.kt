package com.daysync.app.feature.nutrition.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daysync.app.feature.nutrition.data.repository.NutritionRepository
import com.daysync.app.feature.nutrition.domain.model.FoodItem
import com.daysync.app.feature.nutrition.domain.model.FoodItemInput
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface FoodLibraryEvent {
    data object FoodSaved : FoodLibraryEvent
    data object FoodDeleted : FoodLibraryEvent
    data class Error(val message: String) : FoodLibraryEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FoodLibraryViewModel @Inject constructor(
    private val repository: NutritionRepository,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory

    val categories: StateFlow<List<String>> = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val foodItems: StateFlow<List<FoodItem>> = combine(
        _searchQuery,
        _selectedCategory,
    ) { query, category ->
        Pair(query, category)
    }.flatMapLatest { (query, category) ->
        when {
            query.isNotBlank() -> repository.searchFoodItems(query)
            category != null -> repository.getFoodItemsByCategory(category)
            else -> repository.getAllFoodItems()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _events = MutableSharedFlow<FoodLibraryEvent>()
    val events = _events.asSharedFlow()

    fun search(query: String) {
        _searchQuery.value = query
        if (query.isNotBlank()) _selectedCategory.value = null
    }

    fun filterCategory(category: String?) {
        _selectedCategory.value = category
        if (category != null) _searchQuery.value = ""
    }

    fun addFood(input: FoodItemInput) {
        viewModelScope.launch {
            try {
                repository.addFoodItem(input)
                _events.emit(FoodLibraryEvent.FoodSaved)
            } catch (e: Exception) {
                _events.emit(FoodLibraryEvent.Error(e.message ?: "Failed to save"))
            }
        }
    }

    fun updateFood(id: String, input: FoodItemInput) {
        viewModelScope.launch {
            try {
                repository.updateFoodItem(id, input)
                _events.emit(FoodLibraryEvent.FoodSaved)
            } catch (e: Exception) {
                _events.emit(FoodLibraryEvent.Error(e.message ?: "Failed to update"))
            }
        }
    }

    fun deleteFood(id: String) {
        viewModelScope.launch {
            try {
                repository.deleteFoodItem(id)
                _events.emit(FoodLibraryEvent.FoodDeleted)
            } catch (e: Exception) {
                _events.emit(FoodLibraryEvent.Error(e.message ?: "Failed to delete"))
            }
        }
    }
}
