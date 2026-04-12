package com.daysync.app.core.sync

import android.util.Log
import com.daysync.app.core.database.dao.DailyMealEntryDao
import com.daysync.app.core.database.dao.DailyNutritionSummaryDao
import com.daysync.app.core.database.dao.ExerciseSessionDao
import com.daysync.app.core.database.dao.ExpenseDao
import com.daysync.app.core.database.dao.FoodItemDao
import com.daysync.app.core.database.dao.HealthMetricDao
import com.daysync.app.core.database.dao.JournalEntryDao
import com.daysync.app.core.database.dao.MealTemplateDao
import com.daysync.app.core.database.dao.MealTemplateItemDao
import com.daysync.app.core.database.dao.MediaItemDao
import com.daysync.app.core.database.dao.SleepSessionDao
import com.daysync.app.core.database.dao.SportEventDao
import com.daysync.app.core.database.dao.SyncLogDao
import com.daysync.app.core.database.entity.SyncLogEntity
import com.daysync.app.core.sync.dto.CompetitionDto
import com.daysync.app.core.sync.dto.CompetitorDto
import com.daysync.app.core.sync.dto.DailyMealEntryDto
import com.daysync.app.core.sync.dto.DailyNutritionSummaryDto
import com.daysync.app.core.sync.dto.EventParticipantDto
import com.daysync.app.core.sync.dto.ExerciseSessionDto
import com.daysync.app.core.sync.dto.ExpenseDto
import com.daysync.app.core.sync.dto.FollowedCompetitionDto
import com.daysync.app.core.sync.dto.FollowedCompetitorDto
import com.daysync.app.core.sync.dto.FoodItemDto
import com.daysync.app.core.sync.dto.HealthMetricDto
import com.daysync.app.core.sync.dto.JournalEntryDto
import com.daysync.app.core.sync.dto.MealTemplateDto
import com.daysync.app.core.sync.dto.MealTemplateItemDto
import com.daysync.app.core.sync.dto.MediaItemDto
import com.daysync.app.core.sync.dto.SleepSessionDto
import com.daysync.app.core.sync.dto.SportDto
import com.daysync.app.core.sync.dto.SportEventDto
import com.daysync.app.core.sync.dto.SyncLogDto
import com.daysync.app.core.sync.dto.VenueDto
import com.daysync.app.core.sync.dto.WatchlistEntryDto
import com.daysync.app.core.sync.mapper.toDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private const val TAG = "DaySyncEngine"
private const val UPSERT_CHUNK_SIZE = 500
private const val SQLITE_VAR_LIMIT = 900

@Singleton
class DaySyncEngine @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val healthMetricDao: HealthMetricDao,
    private val sleepSessionDao: SleepSessionDao,
    private val exerciseSessionDao: ExerciseSessionDao,
    private val foodItemDao: FoodItemDao,
    private val mealTemplateDao: MealTemplateDao,
    private val mealTemplateItemDao: MealTemplateItemDao,
    private val dailyMealEntryDao: DailyMealEntryDao,
    private val dailyNutritionSummaryDao: DailyNutritionSummaryDao,
    private val expenseDao: ExpenseDao,
    private val sportEventDao: SportEventDao,
    private val journalEntryDao: JournalEntryDao,
    private val mediaItemDao: MediaItemDao,
    private val dailyHealthOverrideDao: com.daysync.app.core.database.dao.DailyHealthOverrideDao,
    private val syncLogDao: SyncLogDao,
) : SyncEngine {

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    override val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    @OptIn(ExperimentalTime::class)
    override suspend fun syncAll(): Result<Unit> {
        _syncState.value = SyncState.Syncing("Starting...", 0, TOTAL_TABLES)

        var successCount = 0
        var failureCount = 0
        var step = 0

        // Sync order respects FK dependencies:
        // 1. Independent tables (health, food_items, meal_templates, expenses)
        // 2. Dependent tables (meal_template_items, daily_meal_entries, daily_nutrition_summaries)
        // 3. Sports chain (events -> watchlist -> followed)
        // 4. Journal, Media

        val syncSteps: List<Pair<String, suspend () -> Result<Unit>>> = listOf(
            "health_metrics" to ::syncHealthMetrics,
            "sleep_sessions" to ::syncSleepSessions,
            "exercise_sessions" to ::syncExerciseSessions,
            "food_items" to ::syncFoodItems,
            "meal_templates" to ::syncMealTemplates,
            "meal_template_items" to ::syncMealTemplateItems,
            "daily_meal_entries" to ::syncDailyMealEntries,
            "daily_nutrition_summaries" to ::syncDailyNutritionSummaries,
            "expenses" to ::syncExpenses,
            "sport_events" to ::syncSportEvents,
            "watchlist_entries" to ::syncWatchlistEntries,
            "followed_competitors" to ::syncFollowedCompetitors,
            "followed_competitions" to ::syncFollowedCompetitions,
            "journal_entries" to ::syncJournalEntries,
            "media_items" to ::syncMediaItems,
            "daily_health_overrides" to ::syncDailyHealthOverrides,
        )

        for ((tableName, syncFn) in syncSteps) {
            step++
            _syncState.value = SyncState.Syncing(tableName, step, TOTAL_TABLES)
            val result = syncFn()
            if (result.isSuccess) successCount++ else failureCount++
        }

        // Reference data backup (non-critical)
        syncReferenceData()

        // Log the overall sync
        syncLogDao.insert(
            SyncLogEntity(
                id = UUID.randomUUID().toString(),
                tableName = "_overall",
                lastSyncAt = Clock.System.now(),
                recordCount = successCount,
                status = when {
                    failureCount == 0 -> "SUCCESS"
                    successCount == 0 -> "FAILED"
                    else -> "PARTIAL"
                },
            )
        )

        return if (failureCount == 0) {
            _syncState.value = SyncState.Completed(successCount, failureCount)
            Result.success(Unit)
        } else if (successCount > 0) {
            _syncState.value = SyncState.Completed(successCount, failureCount)
            Result.success(Unit) // Partial success is still success at worker level
        } else {
            _syncState.value = SyncState.Failed("All $failureCount tables failed to sync")
            Result.failure(SyncException("All tables failed to sync"))
        }
    }

    // --- Individual typed sync functions ---

    @OptIn(ExperimentalTime::class)
    private suspend fun syncHealthMetrics(): Result<Unit> = runCatching {
        val pending = healthMetricDao.getPendingSync()
        if (pending.isEmpty()) { logSync("health_metrics", 0, "SUCCESS"); return@runCatching }
        val dtos = pending.map { it.toDto() }
        upsertChunked("health_metrics", dtos)
        markSyncedChunked(pending.map { it.id }) { healthMetricDao.markAsSynced(it) }
        logSync("health_metrics", pending.size, "SUCCESS")
    }.onFailure { logSync("health_metrics", 0, "FAILED"); Log.e(TAG, "syncHealthMetrics failed", it) }

    @OptIn(ExperimentalTime::class)
    private suspend fun syncSleepSessions(): Result<Unit> = runCatching {
        val pending = sleepSessionDao.getPendingSync()
        if (pending.isEmpty()) { logSync("sleep_sessions", 0, "SUCCESS"); return@runCatching }
        val dtos = pending.map { it.toDto() }
        upsertChunked("sleep_sessions", dtos)
        markSyncedChunked(pending.map { it.id }) { sleepSessionDao.markAsSynced(it) }
        logSync("sleep_sessions", pending.size, "SUCCESS")
    }.onFailure { logSync("sleep_sessions", 0, "FAILED"); Log.e(TAG, "syncSleepSessions failed", it) }

    @OptIn(ExperimentalTime::class)
    private suspend fun syncExerciseSessions(): Result<Unit> = runCatching {
        val pending = exerciseSessionDao.getPendingSync()
        if (pending.isEmpty()) { logSync("exercise_sessions", 0, "SUCCESS"); return@runCatching }
        val dtos = pending.map { it.toDto() }
        upsertChunked("exercise_sessions", dtos)
        markSyncedChunked(pending.map { it.id }) { exerciseSessionDao.markAsSynced(it) }
        logSync("exercise_sessions", pending.size, "SUCCESS")
    }.onFailure { logSync("exercise_sessions", 0, "FAILED"); Log.e(TAG, "syncExerciseSessions failed", it) }

    @OptIn(ExperimentalTime::class)
    private suspend fun syncFoodItems(): Result<Unit> = runCatching {
        val pending = foodItemDao.getPendingSync()
        if (pending.isEmpty()) { logSync("food_items", 0, "SUCCESS"); return@runCatching }
        val dtos = pending.map { it.toDto() }
        upsertChunked("food_items", dtos)
        markSyncedChunked(pending.map { it.id }) { foodItemDao.markAsSynced(it) }
        logSync("food_items", pending.size, "SUCCESS")
    }.onFailure { logSync("food_items", 0, "FAILED"); Log.e(TAG, "syncFoodItems failed", it) }

    @OptIn(ExperimentalTime::class)
    private suspend fun syncMealTemplates(): Result<Unit> = runCatching {
        val pending = mealTemplateDao.getPendingSync()
        if (pending.isEmpty()) { logSync("meal_templates", 0, "SUCCESS"); return@runCatching }
        val dtos = pending.map { it.toDto() }
        upsertChunked("meal_templates", dtos)
        markSyncedChunked(pending.map { it.id }) { mealTemplateDao.markAsSynced(it) }
        logSync("meal_templates", pending.size, "SUCCESS")
    }.onFailure { logSync("meal_templates", 0, "FAILED"); Log.e(TAG, "syncMealTemplates failed", it) }

    @OptIn(ExperimentalTime::class)
    private suspend fun syncMealTemplateItems(): Result<Unit> = runCatching {
        val pending = mealTemplateItemDao.getPendingSync()
        if (pending.isEmpty()) { logSync("meal_template_items", 0, "SUCCESS"); return@runCatching }
        val dtos = pending.map { it.toDto() }
        upsertChunked("meal_template_items", dtos)
        markSyncedChunked(pending.map { it.id }) { mealTemplateItemDao.markAsSynced(it) }
        logSync("meal_template_items", pending.size, "SUCCESS")
    }.onFailure { logSync("meal_template_items", 0, "FAILED"); Log.e(TAG, "syncMealTemplateItems failed", it) }

    @OptIn(ExperimentalTime::class)
    private suspend fun syncDailyMealEntries(): Result<Unit> = runCatching {
        val pending = dailyMealEntryDao.getPendingSync()
        if (pending.isEmpty()) { logSync("daily_meal_entries", 0, "SUCCESS"); return@runCatching }
        val dtos = pending.map { it.toDto() }
        upsertChunked("daily_meal_entries", dtos)
        markSyncedChunked(pending.map { it.id }) { dailyMealEntryDao.markAsSynced(it) }
        logSync("daily_meal_entries", pending.size, "SUCCESS")
    }.onFailure { logSync("daily_meal_entries", 0, "FAILED"); Log.e(TAG, "syncDailyMealEntries failed", it) }

    @OptIn(ExperimentalTime::class)
    private suspend fun syncDailyNutritionSummaries(): Result<Unit> = runCatching {
        val pending = dailyNutritionSummaryDao.getPendingSync()
        if (pending.isEmpty()) { logSync("daily_nutrition_summaries", 0, "SUCCESS"); return@runCatching }
        val dtos = pending.map { it.toDto() }
        upsertChunked("daily_nutrition_summaries", dtos)
        markSyncedChunked(pending.map { it.id }) { dailyNutritionSummaryDao.markAsSynced(it) }
        logSync("daily_nutrition_summaries", pending.size, "SUCCESS")
    }.onFailure { logSync("daily_nutrition_summaries", 0, "FAILED"); Log.e(TAG, "syncDailyNutritionSummaries failed", it) }

    @OptIn(ExperimentalTime::class)
    private suspend fun syncExpenses(): Result<Unit> = runCatching {
        val pending = expenseDao.getPendingSync()
        if (pending.isEmpty()) { logSync("expenses", 0, "SUCCESS"); return@runCatching }
        val dtos = pending.map { it.toDto() }
        upsertChunked("expenses", dtos)
        markSyncedChunked(pending.map { it.id }) { expenseDao.markAsSynced(it) }
        logSync("expenses", pending.size, "SUCCESS")
    }.onFailure { logSync("expenses", 0, "FAILED"); Log.e(TAG, "syncExpenses failed", it) }

    @OptIn(ExperimentalTime::class)
    private suspend fun syncSportEvents(): Result<Unit> = runCatching {
        val pending = sportEventDao.getPendingSyncEvents()
        if (pending.isEmpty()) { logSync("sport_events", 0, "SUCCESS"); return@runCatching }
        val dtos = pending.map { it.toDto() }
        upsertChunked("sport_events", dtos)
        markSyncedChunked(pending.map { it.id }) { sportEventDao.markEventsSynced(it) }
        logSync("sport_events", pending.size, "SUCCESS")
    }.onFailure { logSync("sport_events", 0, "FAILED"); Log.e(TAG, "syncSportEvents failed", it) }

    @OptIn(ExperimentalTime::class)
    private suspend fun syncWatchlistEntries(): Result<Unit> = runCatching {
        val pending = sportEventDao.getPendingSyncWatchlist()
        if (pending.isEmpty()) { logSync("watchlist_entries", 0, "SUCCESS"); return@runCatching }
        val dtos = pending.map { it.toDto() }
        upsertChunked("watchlist_entries", dtos)
        markSyncedChunked(pending.map { it.id }) { sportEventDao.markWatchlistSynced(it) }
        logSync("watchlist_entries", pending.size, "SUCCESS")
    }.onFailure { logSync("watchlist_entries", 0, "FAILED"); Log.e(TAG, "syncWatchlistEntries failed", it) }

    @OptIn(ExperimentalTime::class)
    private suspend fun syncFollowedCompetitors(): Result<Unit> = runCatching {
        val pending = sportEventDao.getPendingSyncFollowedCompetitors()
        if (pending.isEmpty()) { logSync("followed_competitors", 0, "SUCCESS"); return@runCatching }
        val dtos = pending.map { it.toDto() }
        upsertChunked("followed_competitors", dtos)
        markSyncedChunked(pending.map { it.id }) { sportEventDao.markFollowedCompetitorsSynced(it) }
        logSync("followed_competitors", pending.size, "SUCCESS")
    }.onFailure { logSync("followed_competitors", 0, "FAILED"); Log.e(TAG, "syncFollowedCompetitors failed", it) }

    @OptIn(ExperimentalTime::class)
    private suspend fun syncFollowedCompetitions(): Result<Unit> = runCatching {
        val pending = sportEventDao.getPendingSyncFollowedCompetitions()
        if (pending.isEmpty()) { logSync("followed_competitions", 0, "SUCCESS"); return@runCatching }
        val dtos = pending.map { it.toDto() }
        upsertChunked("followed_competitions", dtos)
        markSyncedChunked(pending.map { it.id }) { sportEventDao.markFollowedCompetitionsSynced(it) }
        logSync("followed_competitions", pending.size, "SUCCESS")
    }.onFailure { logSync("followed_competitions", 0, "FAILED"); Log.e(TAG, "syncFollowedCompetitions failed", it) }

    @OptIn(ExperimentalTime::class)
    private suspend fun syncJournalEntries(): Result<Unit> = runCatching {
        val pending = journalEntryDao.getPendingSync()
        if (pending.isEmpty()) { logSync("journal_entries", 0, "SUCCESS"); return@runCatching }
        val dtos = pending.map { it.toDto() }
        upsertChunked("journal_entries", dtos)
        markSyncedChunked(pending.map { it.id }) { journalEntryDao.markAsSynced(it) }
        logSync("journal_entries", pending.size, "SUCCESS")
    }.onFailure { logSync("journal_entries", 0, "FAILED"); Log.e(TAG, "syncJournalEntries failed", it) }

    @OptIn(ExperimentalTime::class)
    private suspend fun syncMediaItems(): Result<Unit> = runCatching {
        val pending = mediaItemDao.getPendingSync()
        if (pending.isEmpty()) { logSync("media_items", 0, "SUCCESS"); return@runCatching }
        val dtos = pending.map { it.toDto() }
        upsertChunked("media_items", dtos)
        markSyncedChunked(pending.map { it.id }) { mediaItemDao.markAsSynced(it) }
        logSync("media_items", pending.size, "SUCCESS")
    }.onFailure { logSync("media_items", 0, "FAILED"); Log.e(TAG, "syncMediaItems failed", it) }

    private suspend fun syncDailyHealthOverrides(): Result<Unit> = runCatching {
        val pending = dailyHealthOverrideDao.getPendingSync()
        if (pending.isEmpty()) { logSync("daily_health_overrides", 0, "SUCCESS"); return@runCatching }
        val dtos = pending.map { it.toDto() }
        upsertChunked("daily_health_overrides", dtos)
        dailyHealthOverrideDao.markAsSynced(pending.map { it.date })
        logSync("daily_health_overrides", pending.size, "SUCCESS")
    }.onFailure { logSync("daily_health_overrides", 0, "FAILED"); Log.e(TAG, "syncDailyHealthOverrides failed", it) }

    // --- Reference data backup (non-syncable tables) ---

    private suspend fun syncReferenceData() {
        try {
            val sports = sportEventDao.getAllSportsList()
            if (sports.isNotEmpty()) {
                upsertChunked("sports", sports.map { it.toDto() })
            }

            val competitions = sportEventDao.getAllCompetitionsList()
            if (competitions.isNotEmpty()) {
                upsertChunked("competitions", competitions.map { it.toDto() })
            }

            val competitors = sportEventDao.getAllCompetitorsList()
            if (competitors.isNotEmpty()) {
                upsertChunked("competitors", competitors.map { it.toDto() })
            }

            val venues = sportEventDao.getAllVenuesList()
            if (venues.isNotEmpty()) {
                upsertChunked("venues", venues.map { it.toDto() })
            }

            val participants = sportEventDao.getAllParticipantsList()
            if (participants.isNotEmpty()) {
                upsertChunked("event_participants", participants.map { it.toDto() })
            }
        } catch (e: Exception) {
            Log.w(TAG, "Reference data backup failed (non-critical)", e)
        }
    }

    // --- Helpers ---

    private suspend inline fun <reified T : Any> upsertChunked(table: String, dtos: List<T>) {
        for (chunk in dtos.chunked(UPSERT_CHUNK_SIZE)) {
            supabaseClient.postgrest.from(table).upsert(chunk)
        }
    }

    private suspend fun markSyncedChunked(ids: List<String>, markFn: suspend (List<String>) -> Unit) {
        for (chunk in ids.chunked(SQLITE_VAR_LIMIT)) {
            markFn(chunk)
        }
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun logSync(tableName: String, count: Int, status: String) {
        try {
            syncLogDao.insert(
                SyncLogEntity(
                    id = UUID.randomUUID().toString(),
                    tableName = tableName,
                    lastSyncAt = Clock.System.now(),
                    recordCount = count,
                    status = status,
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log sync for $tableName", e)
        }
    }

    companion object {
        private const val TOTAL_TABLES = 16
    }
}

class SyncException(message: String) : Exception(message)
