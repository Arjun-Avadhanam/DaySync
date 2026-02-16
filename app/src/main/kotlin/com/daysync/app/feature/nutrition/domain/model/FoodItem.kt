package com.daysync.app.feature.nutrition.domain.model

data class FoodItem(
    val id: String,
    val name: String,
    val category: String?,
    val caloriesPerUnit: Double,
    val proteinPerUnit: Double,
    val carbsPerUnit: Double,
    val fatPerUnit: Double,
    val sugarPerUnit: Double,
    val unitType: UnitType,
    val servingDescription: String?,
)

data class FoodItemInput(
    val name: String,
    val category: String?,
    val caloriesPerUnit: Double,
    val proteinPerUnit: Double = 0.0,
    val carbsPerUnit: Double = 0.0,
    val fatPerUnit: Double = 0.0,
    val sugarPerUnit: Double = 0.0,
    val unitType: UnitType = UnitType.SERVING,
    val servingDescription: String? = null,
)
