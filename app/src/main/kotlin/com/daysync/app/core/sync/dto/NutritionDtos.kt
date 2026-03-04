package com.daysync.app.core.sync.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FoodItemDto(
    val id: String,
    val name: String,
    val category: String?,
    @SerialName("calories_per_unit") val caloriesPerUnit: Double,
    @SerialName("protein_per_unit") val proteinPerUnit: Double,
    @SerialName("carbs_per_unit") val carbsPerUnit: Double,
    @SerialName("fat_per_unit") val fatPerUnit: Double,
    @SerialName("sugar_per_unit") val sugarPerUnit: Double,
    @SerialName("unit_type") val unitType: String,
    @SerialName("serving_description") val servingDescription: String?,
    @SerialName("last_modified") val lastModified: Long,
    @SerialName("is_deleted") val isDeleted: Boolean,
)

@Serializable
data class MealTemplateDto(
    val id: String,
    val name: String,
    val description: String?,
    @SerialName("last_modified") val lastModified: Long,
    @SerialName("is_deleted") val isDeleted: Boolean,
)

@Serializable
data class MealTemplateItemDto(
    val id: String,
    @SerialName("template_id") val templateId: String,
    @SerialName("food_id") val foodId: String,
    @SerialName("default_amount") val defaultAmount: Double,
    @SerialName("last_modified") val lastModified: Long,
    @SerialName("is_deleted") val isDeleted: Boolean,
)

@Serializable
data class DailyMealEntryDto(
    val id: String,
    val date: String,
    @SerialName("food_id") val foodId: String,
    @SerialName("meal_time") val mealTime: String,
    val amount: Double,
    val notes: String?,
    @SerialName("last_modified") val lastModified: Long,
    @SerialName("is_deleted") val isDeleted: Boolean,
)

@Serializable
data class DailyNutritionSummaryDto(
    val id: String,
    val date: String,
    @SerialName("total_calories") val totalCalories: Double,
    @SerialName("total_protein") val totalProtein: Double,
    @SerialName("total_carbs") val totalCarbs: Double,
    @SerialName("total_fat") val totalFat: Double,
    @SerialName("total_sugar") val totalSugar: Double,
    @SerialName("water_liters") val waterLiters: Double,
    @SerialName("calories_burnt") val caloriesBurnt: Double,
    val mood: String?,
    val notes: String?,
    @SerialName("last_modified") val lastModified: Long,
    @SerialName("is_deleted") val isDeleted: Boolean,
)
