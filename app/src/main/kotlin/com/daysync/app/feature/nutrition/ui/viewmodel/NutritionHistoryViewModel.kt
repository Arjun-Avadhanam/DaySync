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

sealed interface NutritionPeriod {
    data class Preset(val range: HistoryRange) : NutritionPeriod
    data class Custom(val start: LocalDate, val end: LocalDate) : NutritionPeriod
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NutritionHistoryViewModel @Inject constructor(
    private val repository: NutritionRepository,
) : ViewModel() {

    private val _period = MutableStateFlow<NutritionPeriod>(NutritionPeriod.Preset(HistoryRange.WEEK))
    val period: StateFlow<NutritionPeriod> = _period

    val summaries: StateFlow<List<DailyNutritionSummary>> = _period.flatMapLatest { period ->
        val (start, end) = when (period) {
            is NutritionPeriod.Preset -> {
                val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
                today.minus(period.range.days, DateTimeUnit.DAY) to today
            }
            is NutritionPeriod.Custom -> period.start to period.end
        }
        repository.getDailySummariesInRange(start, end)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Backward-compatible accessor
    val selectedRange: StateFlow<HistoryRange>
        get() = MutableStateFlow(
            (_period.value as? NutritionPeriod.Preset)?.range ?: HistoryRange.WEEK
        )

    fun setRange(range: HistoryRange) {
        _period.value = NutritionPeriod.Preset(range)
    }

    fun setCustomRange(start: LocalDate, end: LocalDate) {
        _period.value = NutritionPeriod.Custom(start, end)
    }
}
