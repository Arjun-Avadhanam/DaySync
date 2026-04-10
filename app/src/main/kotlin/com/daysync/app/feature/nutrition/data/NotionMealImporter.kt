package com.daysync.app.feature.nutrition.data

import com.daysync.app.core.database.dao.FoodItemDao
import com.daysync.app.core.database.entity.FoodItemEntity
import com.daysync.app.core.sync.SyncStatus
import com.daysync.app.feature.nutrition.data.remote.NotionMealApiClient
import com.daysync.app.feature.nutrition.data.remote.NotionMealPage
import kotlin.time.Clock

data class NotionImportResult(
    val upserted: Int,
    val softDeleted: Int,
) {
    val total: Int get() = upserted
}

/**
 * Live importer for the Notion "Master Meal Library" database.
 * Fetches all rows over HTTP, upserts them into Room keyed by Notion page UUID,
 * and soft-deletes any previously-imported rows that no longer exist in Notion.
 */
class NotionMealImporter(
    private val apiClient: NotionMealApiClient,
    private val foodItemDao: FoodItemDao,
) {
    companion object {
        private const val ID_PREFIX = "notion-"
    }

    suspend fun forceImport(): NotionImportResult {
        val pages = apiClient.fetchAllMeals()
        val now = Clock.System.now()

        val entities = pages.map { it.toEntity(now) }
        val expectedIds = entities.map { it.id }.toSet()

        foodItemDao.insertAll(entities)

        val existingNotionIds = foodItemDao.getActiveNotionIds()
        val staleIds = existingNotionIds.filter { it !in expectedIds }
        var softDeleted = 0
        if (staleIds.isNotEmpty()) {
            staleIds.forEach { id ->
                val entity = foodItemDao.getById(id) ?: return@forEach
                foodItemDao.update(
                    entity.copy(
                        isDeleted = true,
                        syncStatus = SyncStatus.PENDING,
                        lastModified = now,
                    )
                )
                softDeleted++
            }
        }

        return NotionImportResult(upserted = entities.size, softDeleted = softDeleted)
    }

    private fun NotionMealPage.toEntity(now: kotlin.time.Instant): FoodItemEntity =
        FoodItemEntity(
            id = "$ID_PREFIX$pageId",
            name = name,
            category = category?.takeIf { it.isNotBlank() },
            caloriesPerUnit = calories,
            proteinPerUnit = protein,
            carbsPerUnit = carbs,
            fatPerUnit = fat,
            sugarPerUnit = sugar,
            unitType = mapUnitType(unit),
            servingDescription = serving?.takeIf { it.isNotBlank() },
            syncStatus = SyncStatus.PENDING,
            lastModified = now,
        )

    private fun mapUnitType(notionUnit: String?): String = when (notionUnit?.lowercase()) {
        "pieces" -> "PIECES"
        "items" -> "PIECES"
        "slices" -> "SLICE"
        "grams" -> "GRAMS"
        "cups" -> "CUPS"
        "tablespoons" -> "TABLESPOON"
        "ml" -> "ML"
        else -> "SERVING"
    }
}
