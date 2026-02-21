package com.daysync.app.feature.nutrition.ui.navigation

import kotlinx.serialization.Serializable

@Serializable data object DailyTracker

@Serializable data class MealEntry(val date: String, val mealTime: String)

@Serializable data object FoodLibrary

@Serializable data object AddFood

@Serializable data class EditFood(val foodId: String)

@Serializable data object Templates

@Serializable data class CreateTemplate(val placeholder: String = "")

@Serializable data class LogTemplate(val templateId: String, val date: String, val mealTime: String)

@Serializable data class DailySummary(val date: String)

@Serializable data object ScanLabel

@Serializable data object History
