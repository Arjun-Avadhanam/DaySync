package com.daysync.app.core.notion

import com.daysync.app.core.database.entity.FoodItemEntity
import com.daysync.app.core.notion.NotionExportClient.Companion.numberProperty
import com.daysync.app.core.notion.NotionExportClient.Companion.richTextProperty
import com.daysync.app.core.notion.NotionExportClient.Companion.selectProperty
import com.daysync.app.core.notion.NotionExportClient.Companion.titleProperty
import kotlinx.serialization.json.buildJsonObject

class NotionFoodExporter(
    private val client: NotionExportClient,
    private val databaseId: String,
) {
    suspend fun export(food: FoodItemEntity): Result<String> {
        val props = buildJsonObject {
            put("Meal Name", titleProperty(food.name))
            put("Calories per Unit", numberProperty(food.caloriesPerUnit))
            put("Protein per Unit (g)", numberProperty(food.proteinPerUnit))
            put("Carbs per Unit (g)", numberProperty(food.carbsPerUnit))
            put("Fat per Unit (g)", numberProperty(food.fatPerUnit))
            put("Sugar per Unit (g)", numberProperty(food.sugarPerUnit))
            food.category?.let { put("Food Category", selectProperty(it)) }
            put("Unit Type", selectProperty(mapUnitType(food.unitType)))
            food.servingDescription?.let { put("Serving Description", richTextProperty(it)) }
        }
        return client.createPage(databaseId, props)
    }

    suspend fun exportFromDomain(
        name: String,
        caloriesPerUnit: Double,
        proteinPerUnit: Double,
        carbsPerUnit: Double,
        fatPerUnit: Double,
        sugarPerUnit: Double,
        category: String?,
        unitType: String,
        servingDescription: String?,
    ): Result<String> {
        val props = buildJsonObject {
            put("Meal Name", titleProperty(name))
            put("Calories per Unit", numberProperty(caloriesPerUnit))
            put("Protein per Unit (g)", numberProperty(proteinPerUnit))
            put("Carbs per Unit (g)", numberProperty(carbsPerUnit))
            put("Fat per Unit (g)", numberProperty(fatPerUnit))
            put("Sugar per Unit (g)", numberProperty(sugarPerUnit))
            category?.let { put("Food Category", selectProperty(it)) }
            put("Unit Type", selectProperty(mapUnitType(unitType)))
            servingDescription?.let { put("Serving Description", richTextProperty(it)) }
        }
        return client.createPage(databaseId, props)
    }

    private fun mapUnitType(appUnit: String): String = when (appUnit) {
        "PIECES" -> "pieces"
        "SLICE" -> "slices"
        "GRAMS" -> "grams"
        "CUPS" -> "cups"
        "TABLESPOON" -> "tablespoons"
        "ML" -> "ml"
        "SERVING" -> "items"
        else -> "items"
    }
}
