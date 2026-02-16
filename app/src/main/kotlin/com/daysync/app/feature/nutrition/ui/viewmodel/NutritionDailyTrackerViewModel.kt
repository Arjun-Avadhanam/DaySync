package com.daysync.app.feature.nutrition.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daysync.app.feature.nutrition.data.repository.NutritionRepository
import com.daysync.app.feature.nutrition.domain.model.DailyNutritionInput
import com.daysync.app.feature.nutrition.domain.model.DailyNutritionSummary
import com.daysync.app.feature.nutrition.domain.model.MealEntryWithFood
import com.daysync.app.feature.nutrition.domain.model.MealTime
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

sealed interface NutritionDailyTrackerState {
    data object Loading : NutritionDailyTrackerState
    data class Success(
        val date: LocalDate,
        val entriesByMealTime: Map<MealTime, List<MealEntryWithFood>>,
        val summary: DailyNutritionSummary?,
        val totalCalories: Double,
        val totalProtein: Double,
        val totalCarbs: Double,
        val totalFat: Double,
        val totalSugar: Double,
    ) : NutritionDailyTrackerState
    data class Error(val message: String) : NutritionDailyTrackerState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NutritionDailyTrackerViewModel @Inject constructor(
    private val repository: NutritionRepository,
) : ViewModel() {

    private val _currentDate = MutableStateFlow(Clock.System.todayIn(TimeZone.currentSystemDefault()))
    val currentDate: StateFlow<LocalDate> = _currentDate

    val state: StateFlow<NutritionDailyTrackerState> = _currentDate.flatMapLatest { date ->
        combine(
            repository.getMealEntriesWithFoodByDate(date),
            repository.getDailySummary(date),
        ) { entries, summary ->
            val grouped = MealTime.entries.associateWith { mealTime ->
                entries.filter { it.entry.mealTime == mealTime }
            }
            val totalCalories = entries.sumOf { it.calories }
            val totalProtein = entries.sumOf { it.protein }
            val totalCarbs = entries.sumOf { it.carbs }
            val totalFat = entries.sumOf { it.fat }
            val totalSugar = entries.sumOf { it.sugar }

            NutritionDailyTrackerState.Success(
                date = date,
                entriesByMealTime = grouped,
                summary = summary,
                totalCalories = totalCalories,
                totalProtein = totalProtein,
                totalCarbs = totalCarbs,
                totalFat = totalFat,
                totalSugar = totalSugar,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NutritionDailyTrackerState.Loading)

    fun navigateToPreviousDay() {
        _currentDate.value = _currentDate.value.let {
            LocalDate.fromEpochDays(it.toEpochDays() - 1)
        }
    }

    fun navigateToNextDay() {
        _currentDate.value = _currentDate.value.let {
            LocalDate.fromEpochDays(it.toEpochDays() + 1)
        }
    }

    fun navigateToToday() {
        _currentDate.value = Clock.System.todayIn(TimeZone.currentSystemDefault())
    }

    fun deleteMealEntry(id: String) {
        viewModelScope.launch {
            repository.deleteMealEntry(id)
        }
    }

    fun updateWater(liters: Double) {
        viewModelScope.launch {
            repository.updateDailySummaryManualInputs(
                date = _currentDate.value,
                input = DailyNutritionInput(waterLiters = liters),
            )
        }
    }

    fun updateMood(mood: String) {
        viewModelScope.launch {
            repository.updateDailySummaryManualInputs(
                date = _currentDate.value,
                input = DailyNutritionInput(mood = mood),
            )
        }
    }

    fun updateNotes(notes: String) {
        viewModelScope.launch {
            repository.updateDailySummaryManualInputs(
                date = _currentDate.value,
                input = DailyNutritionInput(notes = notes),
            )
        }
    }
}
