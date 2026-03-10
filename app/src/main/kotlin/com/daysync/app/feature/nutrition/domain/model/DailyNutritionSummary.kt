package com.daysync.app.feature.nutrition.domain.model

import kotlinx.datetime.LocalDate

data class DailyNutritionSummary(
    val id: String,
    val date: LocalDate,
    val totalCalories: Double,
    val totalProtein: Double,
    val totalCarbs: Double,
    val totalFat: Double,
    val totalSugar: Double,
    val waterLiters: Double,
    val caloriesBurnt: Double,
    val mood: String?,
    val notes: String?,
)

data class DailyNutritionInput(
    val waterLiters: Double? = null,
    val caloriesBurnt: Double? = null,
    val mood: String? = null,
    val notes: String? = null,
)
