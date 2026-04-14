package com.daysync.app.feature.dashboard.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daysync.app.core.database.dao.DailyNutritionSummaryDao
import com.daysync.app.core.database.dao.ExpenseDao
import com.daysync.app.core.database.dao.HealthMetricDao
import com.daysync.app.core.database.dao.JournalEntryDao
import com.daysync.app.core.database.dao.MediaItemDao
import com.daysync.app.core.database.dao.SleepSessionDao
import com.daysync.app.core.database.dao.SportEventDao
import com.daysync.app.core.sync.SyncEngine
import com.daysync.app.core.sync.SyncRestoreEngine
import com.daysync.app.core.sync.SyncState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import javax.inject.Inject

data class HomeSummary(
    val steps: Long? = null,
    val calories: Double? = null,
    val sleepMinutes: Int? = null,
    val caloriesConsumed: Double? = null,
    val proteinConsumed: Double? = null,
    val monthlyExpenses: Double = 0.0,
    val upcomingMatches: Int = 0,
    val journalEntries: Int = 0,
    val mediaInProgress: Int = 0,
    val mediaDone: Int = 0,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val healthMetricDao: HealthMetricDao,
    private val sleepSessionDao: SleepSessionDao,
    private val nutritionSummaryDao: DailyNutritionSummaryDao,
    private val expenseDao: ExpenseDao,
    private val sportEventDao: SportEventDao,
    private val journalEntryDao: JournalEntryDao,
    private val mediaItemDao: MediaItemDao,
    private val syncEngine: SyncEngine,
    private val restoreEngine: SyncRestoreEngine,
) : ViewModel() {

    private val _summary = MutableStateFlow(HomeSummary())
    val summary: StateFlow<HomeSummary> = _summary.asStateFlow()

    val syncState: StateFlow<SyncState> = syncEngine.syncState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SyncState.Idle)

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    init {
        loadSummary()
    }

    fun loadSummary() {
        viewModelScope.launch {
            try {
                val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
                val monthStart = LocalDate(today.year, today.monthNumber, 1)

                // Health
                val steps = healthMetricDao.getLatestByType("STEPS").first()?.value?.toLong()
                val calories = healthMetricDao.getLatestByType("TOTAL_CALORIES").first()?.value
                val latestSleep = sleepSessionDao.getLatest().first()
                val sleepMinutes = latestSleep?.totalMinutes

                // Nutrition
                val nutritionToday = nutritionSummaryDao.getByDate(today)
                val caloriesConsumed = nutritionToday?.totalCalories?.takeIf { it > 0 }
                val proteinConsumed = nutritionToday?.totalProtein?.takeIf { it > 0 }

                // Expenses
                val monthlyExpenses = expenseDao.getMonthlyTotal(monthStart, today).first()

                // Sports
                val upcomingMatches = sportEventDao.getUpcomingEvents().first().size

                // Journal
                val journalEntries = journalEntryDao.getAll().first().size

                // Media
                val allMedia = mediaItemDao.getAll().first()
                val mediaInProgress = allMedia.count { it.status == "IN_PROGRESS" }
                val mediaDone = allMedia.count { it.status == "DONE" }

                _summary.value = HomeSummary(
                    steps = steps,
                    calories = calories,
                    sleepMinutes = sleepMinutes,
                    caloriesConsumed = caloriesConsumed,
                    proteinConsumed = proteinConsumed,
                    monthlyExpenses = monthlyExpenses,
                    upcomingMatches = upcomingMatches,
                    journalEntries = journalEntries,
                    mediaInProgress = mediaInProgress,
                    mediaDone = mediaDone,
                )
            } catch (_: Exception) {
                // Summary is best-effort; don't crash if a query fails
            }
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            _isSyncing.value = true
            syncEngine.syncAll()
            _isSyncing.value = false
            loadSummary()
        }
    }

    private val _isRestoring = MutableStateFlow(false)
    val isRestoring: StateFlow<Boolean> = _isRestoring.asStateFlow()

    private val _restoreMessage = MutableStateFlow<String?>(null)
    val restoreMessage: StateFlow<String?> = _restoreMessage.asStateFlow()

    fun restoreFromCloud() {
        viewModelScope.launch {
            _isRestoring.value = true
            try {
                val result = restoreEngine.restoreAll()
                val msg = if (result.errors.isEmpty()) {
                    "Restored ${result.totalRecords} records from ${result.tablesRestored} tables"
                } else {
                    "Restored ${result.totalRecords} records (${result.errors.size} errors)"
                }
                _restoreMessage.value = msg
            } catch (e: Exception) {
                _restoreMessage.value = "Restore failed: ${e.message}"
            } finally {
                _isRestoring.value = false
                loadSummary()
            }
        }
    }

    fun clearRestoreMessage() {
        _restoreMessage.value = null
    }
}
