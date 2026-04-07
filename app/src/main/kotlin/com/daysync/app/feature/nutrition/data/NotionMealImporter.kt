package com.daysync.app.feature.nutrition.data

import android.content.Context
import android.util.Log
import com.daysync.app.core.database.dao.FoodItemDao
import com.daysync.app.core.database.entity.FoodItemEntity
import com.daysync.app.core.sync.SyncStatus
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.UUID

/**
 * One-time importer for the Notion "Master Meal Library" database.
 * Reads from assets/notion_meal_library.json and inserts into Room.
 */
class NotionMealImporter(
    private val context: Context,
    private val foodItemDao: FoodItemDao,
) {
    companion object {
        private const val TAG = "NotionMealImporter"
        private const val ASSET_FILE = "notion_meal_library.json"
        private const val PREFS_NAME = "notion_import"
        private const val KEY_IMPORTED = "meal_library_imported"
    }

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /**
     * Import meals from Notion JSON asset if not already imported.
     * Returns the number of items imported, or 0 if already done.
     */
    suspend fun importIfNeeded(): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_IMPORTED, false)) {
            Log.d(TAG, "Meal library already imported, skipping")
            return 0
        }

        return try {
            val count = doImport()
            prefs.edit().putBoolean(KEY_IMPORTED, true).apply()
            Log.i(TAG, "Successfully imported $count meals from Notion")
            count
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import meal library", e)
            0
        }
    }

    /**
     * Force re-import (ignores the "already imported" flag).
     */
    suspend fun forceImport(): Int {
        val count = doImport()
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_IMPORTED, true).apply()
        return count
    }

    private suspend fun doImport(): Int {
        val jsonText = context.assets.open(ASSET_FILE).bufferedReader().readText()
        val meals = json.decodeFromString<List<NotionMeal>>(jsonText)

        val entities = meals.map { meal ->
            FoodItemEntity(
                id = "notion-${UUID.nameUUIDFromBytes(meal.name.toByteArray())}",
                name = meal.name,
                category = meal.category.ifBlank { null },
                caloriesPerUnit = meal.calories,
                proteinPerUnit = meal.protein,
                carbsPerUnit = meal.carbs,
                fatPerUnit = meal.fat,
                sugarPerUnit = meal.sugar,
                unitType = mapUnitType(meal.unit),
                servingDescription = meal.serving.ifBlank { null },
                syncStatus = SyncStatus.SYNCED, // Already from external source
            )
        }

        foodItemDao.insertAll(entities)
        return entities.size
    }

    private fun mapUnitType(notionUnit: String): String = when (notionUnit.lowercase()) {
        "pieces" -> "PIECES"
        "items" -> "PIECES"
        "slices" -> "SLICE"
        "grams" -> "GRAMS"
        "cups" -> "CUPS"
        "tablespoons" -> "TABLESPOON"
        "ml" -> "ML"
        else -> "SERVING"
    }

    @Serializable
    private data class NotionMeal(
        val name: String,
        val calories: Double = 0.0,
        val protein: Double = 0.0,
        val carbs: Double = 0.0,
        val fat: Double = 0.0,
        val sugar: Double = 0.0,
        val category: String = "",
        val unit: String = "",
        val serving: String = "",
    )
}
