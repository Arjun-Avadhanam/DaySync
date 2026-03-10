package com.daysync.app.feature.nutrition.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daysync.app.feature.nutrition.data.repository.NutritionRepository
import com.daysync.app.feature.nutrition.domain.model.DailyNutritionSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlin.time.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn

enum class HistoryRange(val label: String, val days: Int) {
    WEEK("7 days", 7),
    MONTH("30 days", 30),
    THREE_MONTHS("90 days", 90),
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NutritionHistoryViewModel @Inject constructor(
    private val repository: NutritionRepository,
) : ViewModel() {

    private val _selectedRange = MutableStateFlow(HistoryRange.WEEK)
    val selectedRange: StateFlow<HistoryRange> = _selectedRange

    val summaries: StateFlow<List<DailyNutritionSummary>> = _selectedRange.flatMapLatest { range ->
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val startDate = today.minus(range.days, DateTimeUnit.DAY)
        repository.getDailySummariesInRange(startDate, today)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setRange(range: HistoryRange) {
        _selectedRange.value = range
    }
}
