package com.daysync.app.feature.nutrition.domain.model

import kotlinx.datetime.LocalDate

data class MealEntry(
    val id: String,
    val date: LocalDate,
    val foodId: String,
    val mealTime: MealTime,
    val amount: Double,
    val notes: String?,
)

data class MealEntryWithFood(
    val entry: MealEntry,
    val food: FoodItem,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val sugar: Double,
) {
    companion object {
        fun from(entry: MealEntry, food: FoodItem): MealEntryWithFood =
            MealEntryWithFood(
                entry = entry,
                food = food,
                calories = entry.amount * food.caloriesPerUnit,
                protein = entry.amount * food.proteinPerUnit,
                carbs = entry.amount * food.carbsPerUnit,
                fat = entry.amount * food.fatPerUnit,
                sugar = entry.amount * food.sugarPerUnit,
            )
    }
}

data class MealEntryInput(
    val date: LocalDate,
    val foodId: String,
    val mealTime: MealTime,
    val amount: Double,
    val notes: String? = null,
)
