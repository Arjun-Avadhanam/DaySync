package com.daysync.app.core.sync

import android.util.Log
import com.daysync.app.core.database.dao.DailyHealthOverrideDao
import com.daysync.app.core.database.dao.DailyMealEntryDao
import com.daysync.app.core.database.dao.DailyNutritionSummaryDao
import com.daysync.app.core.database.dao.ExpenseDao
import com.daysync.app.core.database.dao.FoodItemDao
import com.daysync.app.core.database.dao.JournalEntryDao
import com.daysync.app.core.database.dao.MediaItemDao
import com.daysync.app.core.database.dao.MealTemplateDao
import com.daysync.app.core.database.dao.MealTemplateItemDao
import com.daysync.app.core.database.dao.SportEventDao
import com.daysync.app.core.database.entity.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Downloads user-entered data from Supabase and inserts into Room.
 * Used to restore after a schema migration wipe or fresh install.
 * Only restores tables with user-entered data — health metrics,
 * sports events, and followed competitions regenerate from their
 * external sources automatically.
 */
@Singleton
class SyncRestoreEngine @Inject constructor(
    private val supabaseUrl: String,
    private val supabaseKey: String,
    private val httpClient: HttpClient,
    private val foodItemDao: FoodItemDao,
    private val mealTemplateDao: MealTemplateDao,
    private val mealTemplateItemDao: MealTemplateItemDao,
    private val dailyMealEntryDao: DailyMealEntryDao,
    private val dailyNutritionSummaryDao: DailyNutritionSummaryDao,
    private val expenseDao: ExpenseDao,
    private val journalEntryDao: JournalEntryDao,
    private val mediaItemDao: MediaItemDao,
    private val dailyHealthOverrideDao: DailyHealthOverrideDao,
    private val sportEventDao: SportEventDao,
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    data class RestoreResult(
        val tablesRestored: Int = 0,
        val totalRecords: Int = 0,
        val errors: List<String> = emptyList(),
    )

    suspend fun restoreAll(): RestoreResult {
        val errors = mutableListOf<String>()
        var tables = 0
        var records = 0

        val steps = listOf<Pair<String, suspend () -> Int>>(
            "food_items" to ::restoreFoodItems,
            "meal_templates" to ::restoreMealTemplates,
            "meal_template_items" to ::restoreMealTemplateItems,
            "daily_meal_entries" to ::restoreDailyMealEntries,
            "daily_nutrition_summaries" to ::restoreNutritionSummaries,
            "expenses" to ::restoreExpenses,
            "journal_entries" to ::restoreJournalEntries,
            "media_items" to ::restoreMediaItems,
            "daily_health_overrides" to ::restoreHealthOverrides,
            "watchlist_entries" to ::restoreWatchlistEntries,
        )

        for ((name, fn) in steps) {
            try {
                val count = fn()
                if (count > 0) { tables++; records += count }
                Log.d(TAG, "Restored $name: $count records")
            } catch (e: Exception) {
                errors += "$name: ${e.message}"
                Log.e(TAG, "Failed to restore $name", e)
            }
        }

        return RestoreResult(tablesRestored = tables, totalRecords = records, errors = errors)
    }

    private suspend inline fun <reified T> fetchAll(table: String): List<T> {
        val response = httpClient.get("$supabaseUrl/rest/v1/$table") {
            header(HttpHeaders.Authorization, "Bearer $supabaseKey")
            header("apikey", supabaseKey)
            parameter("select", "*")
            parameter("is_deleted", "eq.false")
            parameter("limit", 10000)
        }
        return json.decodeFromString(response.body<String>())
    }

    private suspend fun restoreFoodItems(): Int {
        val rows = fetchAll<FoodItemRow>("food_items")
        val entities = rows.map { it.toEntity() }
        if (entities.isNotEmpty()) foodItemDao.insertAll(entities)
        return entities.size
    }

    private suspend fun restoreMealTemplates(): Int {
        val rows = fetchAll<MealTemplateRow>("meal_templates")
        val entities = rows.map { it.toEntity() }
        entities.forEach { mealTemplateDao.insert(it) }
        return entities.size
    }

    private suspend fun restoreMealTemplateItems(): Int {
        val rows = fetchAll<MealTemplateItemRow>("meal_template_items")
        val entities = rows.map { it.toEntity() }
        entities.forEach { mealTemplateItemDao.insert(it) }
        return entities.size
    }

    private suspend fun restoreDailyMealEntries(): Int {
        val rows = fetchAll<DailyMealEntryRow>("daily_meal_entries")
        val entities = rows.map { it.toEntity() }
        entities.forEach { dailyMealEntryDao.insert(it) }
        return entities.size
    }

    private suspend fun restoreNutritionSummaries(): Int {
        val rows = fetchAll<NutritionSummaryRow>("daily_nutrition_summaries")
        val entities = rows.map { it.toEntity() }
        entities.forEach { dailyNutritionSummaryDao.insert(it) }
        return entities.size
    }

    private suspend fun restoreExpenses(): Int {
        val rows = fetchAll<ExpenseRow>("expenses")
        val entities = rows.map { it.toEntity() }
        if (entities.isNotEmpty()) expenseDao.insertAll(entities)
        return entities.size
    }

    private suspend fun restoreJournalEntries(): Int {
        val rows = fetchAll<JournalRow>("journal_entries")
        val entities = rows.map { it.toEntity() }
        entities.forEach { journalEntryDao.insert(it) }
        return entities.size
    }

    private suspend fun restoreMediaItems(): Int {
        val rows = fetchAll<MediaRow>("media_items")
        val entities = rows.map { it.toEntity() }
        entities.forEach { mediaItemDao.insert(it) }
        return entities.size
    }

    private suspend fun restoreHealthOverrides(): Int {
        val rows = fetchAll<HealthOverrideRow>("daily_health_overrides")
        val entities = rows.map { it.toEntity() }
        entities.forEach { dailyHealthOverrideDao.upsert(it) }
        return entities.size
    }

    private suspend fun restoreWatchlistEntries(): Int {
        // Watchlist entries have a FK to sport_events. Skip any whose event
        // isn't present yet (sport events are re-fetched from external APIs
        // on first sport screen load, so the restore may run before that).
        val rows = fetchAll<WatchlistEntryRow>("watchlist_entries")
        var restored = 0
        for (row in rows) {
            val eventExists = sportEventDao.getEventById(row.eventId) != null
            if (!eventExists) continue
            sportEventDao.insertWatchlistEntry(row.toEntity())
            restored++
        }
        return restored
    }

    companion object {
        private const val TAG = "SyncRestore"
    }

    // ── Row DTOs (match Supabase column names) ──────────────────────

    @Serializable
    data class FoodItemRow(
        val id: String, val name: String, val category: String? = null,
        @SerialName("calories_per_unit") val caloriesPerUnit: Double,
        @SerialName("protein_per_unit") val proteinPerUnit: Double = 0.0,
        @SerialName("carbs_per_unit") val carbsPerUnit: Double = 0.0,
        @SerialName("fat_per_unit") val fatPerUnit: Double = 0.0,
        @SerialName("sugar_per_unit") val sugarPerUnit: Double = 0.0,
        @SerialName("unit_type") val unitType: String,
        @SerialName("serving_description") val servingDescription: String? = null,
        @SerialName("last_modified") val lastModified: Long,
    ) {
        fun toEntity() = FoodItemEntity(
            id = id, name = name, category = category,
            caloriesPerUnit = caloriesPerUnit, proteinPerUnit = proteinPerUnit,
            carbsPerUnit = carbsPerUnit, fatPerUnit = fatPerUnit,
            sugarPerUnit = sugarPerUnit, unitType = unitType,
            servingDescription = servingDescription,
            syncStatus = com.daysync.app.core.sync.SyncStatus.SYNCED,
            lastModified = Instant.fromEpochMilliseconds(lastModified),
        )
    }

    @Serializable
    data class ExpenseRow(
        val id: String, val title: String? = null, val item: String? = null,
        val date: String, val category: String? = null, val frequency: String? = null,
        @SerialName("unit_cost") val unitCost: Double,
        val quantity: Double = 1.0,
        @SerialName("delivery_charge") val deliveryCharge: Double = 0.0,
        @SerialName("total_amount") val totalAmount: Double,
        val notes: String? = null, val source: String = "MANUAL",
        @SerialName("merchant_name") val merchantName: String? = null,
        @SerialName("last_modified") val lastModified: Long,
    ) {
        fun toEntity() = ExpenseEntity(
            id = id, title = title, item = item,
            date = LocalDate.parse(date), category = category,
            frequency = frequency, unitCost = unitCost, quantity = quantity,
            deliveryCharge = deliveryCharge, totalAmount = totalAmount,
            notes = notes, source = source, merchantName = merchantName,
            syncStatus = SyncStatus.SYNCED,
            lastModified = Instant.fromEpochMilliseconds(lastModified),
        )
    }

    @Serializable
    data class JournalRow(
        val id: String, val date: String, val title: String? = null,
        val content: String? = null, val mood: Int? = null,
        val tags: String? = null,
        @SerialName("is_archived") val isArchived: Boolean = false,
        @SerialName("last_modified") val lastModified: Long,
    ) {
        fun toEntity() = JournalEntryEntity(
            id = id, date = LocalDate.parse(date), title = title,
            content = content, mood = mood,
            tags = if (tags != null) {
                try { Json.decodeFromString<List<String>>(tags) } catch (_: Exception) { emptyList() }
            } else emptyList(),
            isArchived = isArchived,
            syncStatus = SyncStatus.SYNCED,
            lastModified = Instant.fromEpochMilliseconds(lastModified),
        )
    }

    @Serializable
    data class MediaRow(
        val id: String, val title: String,
        @SerialName("media_type") val mediaType: String,
        val status: String = "NOT_STARTED",
        val score: Double? = null, val creators: String? = null,
        @SerialName("completed_date") val completedDate: String? = null,
        val notes: String? = null,
        @SerialName("last_modified") val lastModified: Long,
    ) {
        fun toEntity() = MediaItemEntity(
            id = id, title = title, mediaType = mediaType, status = status,
            score = score,
            creators = if (creators != null) {
                try { Json.decodeFromString<List<String>>(creators) } catch (_: Exception) { emptyList() }
            } else emptyList(),
            completedDate = completedDate?.let { try { LocalDate.parse(it) } catch (_: Exception) { null } },
            notes = notes,
            syncStatus = SyncStatus.SYNCED,
            lastModified = Instant.fromEpochMilliseconds(lastModified),
        )
    }

    @Serializable
    data class HealthOverrideRow(
        val date: String,
        @SerialName("total_calories") val totalCalories: Double? = null,
        @SerialName("weight_morning") val weightMorning: Double? = null,
        @SerialName("weight_evening") val weightEvening: Double? = null,
        @SerialName("weight_night") val weightNight: Double? = null,
        @SerialName("last_modified") val lastModified: Long,
    ) {
        fun toEntity() = DailyHealthOverrideEntity(
            date = LocalDate.parse(date),
            totalCalories = totalCalories,
            weightMorning = weightMorning,
            weightEvening = weightEvening,
            weightNight = weightNight,
            syncStatus = SyncStatus.SYNCED,
            lastModified = Instant.fromEpochMilliseconds(lastModified),
        )
    }

    @Serializable
    data class MealTemplateRow(
        val id: String, val name: String, val description: String? = null,
        @SerialName("last_modified") val lastModified: Long,
    ) {
        fun toEntity() = MealTemplateEntity(
            id = id, name = name, description = description,
            syncStatus = SyncStatus.SYNCED,
            lastModified = Instant.fromEpochMilliseconds(lastModified),
        )
    }

    @Serializable
    data class MealTemplateItemRow(
        val id: String,
        @SerialName("template_id") val templateId: String,
        @SerialName("food_id") val foodId: String,
        @SerialName("default_amount") val defaultAmount: Double,
        @SerialName("last_modified") val lastModified: Long,
    ) {
        fun toEntity() = MealTemplateItemEntity(
            id = id, templateId = templateId, foodId = foodId,
            defaultAmount = defaultAmount,
            syncStatus = SyncStatus.SYNCED,
            lastModified = Instant.fromEpochMilliseconds(lastModified),
        )
    }

    @Serializable
    data class DailyMealEntryRow(
        val id: String, val date: String,
        @SerialName("food_id") val foodId: String,
        @SerialName("meal_time") val mealTime: String,
        val amount: Double, val notes: String? = null,
        @SerialName("last_modified") val lastModified: Long,
    ) {
        fun toEntity() = DailyMealEntryEntity(
            id = id, date = LocalDate.parse(date), foodId = foodId,
            mealTime = mealTime, amount = amount, notes = notes,
            syncStatus = SyncStatus.SYNCED,
            lastModified = Instant.fromEpochMilliseconds(lastModified),
        )
    }

    @Serializable
    data class WatchlistEntryRow(
        val id: String,
        @SerialName("event_id") val eventId: String,
        @SerialName("added_at") val addedAt: Long,
        val notify: Boolean = true,
        val notes: String? = null,
        val watchnotes: String? = null,
        @SerialName("last_modified") val lastModified: Long,
    ) {
        fun toEntity() = WatchlistEntryEntity(
            id = id, eventId = eventId,
            addedAt = Instant.fromEpochMilliseconds(addedAt),
            notify = notify, notes = notes, watchnotes = watchnotes,
            syncStatus = SyncStatus.SYNCED,
            lastModified = Instant.fromEpochMilliseconds(lastModified),
        )
    }

    @Serializable
    data class NutritionSummaryRow(
        val id: String, val date: String,
        @SerialName("total_calories") val totalCalories: Double = 0.0,
        @SerialName("total_protein") val totalProtein: Double = 0.0,
        @SerialName("total_carbs") val totalCarbs: Double = 0.0,
        @SerialName("total_fat") val totalFat: Double = 0.0,
        @SerialName("total_sugar") val totalSugar: Double = 0.0,
        @SerialName("water_liters") val waterLiters: Double = 0.0,
        @SerialName("calories_burnt") val caloriesBurnt: Double = 0.0,
        val mood: String? = null, val notes: String? = null,
        @SerialName("last_modified") val lastModified: Long,
    ) {
        fun toEntity() = DailyNutritionSummaryEntity(
            id = id, date = LocalDate.parse(date),
            totalCalories = totalCalories, totalProtein = totalProtein,
            totalCarbs = totalCarbs, totalFat = totalFat,
            totalSugar = totalSugar, waterLiters = waterLiters,
            caloriesBurnt = caloriesBurnt, mood = mood, notes = notes,
            syncStatus = SyncStatus.SYNCED,
            lastModified = Instant.fromEpochMilliseconds(lastModified),
        )
    }
}
