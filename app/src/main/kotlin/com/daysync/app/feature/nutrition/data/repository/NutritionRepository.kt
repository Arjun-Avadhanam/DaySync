package com.daysync.app.feature.nutrition.data.repository

import com.daysync.app.feature.nutrition.domain.model.DailyNutritionInput
import com.daysync.app.feature.nutrition.domain.model.DailyNutritionSummary
import com.daysync.app.feature.nutrition.domain.model.FoodItem
import com.daysync.app.feature.nutrition.domain.model.FoodItemInput
import com.daysync.app.feature.nutrition.domain.model.MealEntryInput
import com.daysync.app.feature.nutrition.domain.model.MealEntryWithFood
import com.daysync.app.feature.nutrition.domain.model.MealTemplate
import com.daysync.app.feature.nutrition.domain.model.MealTemplateInput
import com.daysync.app.feature.nutrition.domain.model.MealTemplateWithItems
import com.daysync.app.feature.nutrition.domain.model.MealTime
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

interface NutritionRepository {

    // Food Library
    fun getAllFoodItems(): Flow<List<FoodItem>>
    fun searchFoodItems(query: String): Flow<List<FoodItem>>
    fun getFoodItemsByCategory(category: String): Flow<List<FoodItem>>
    fun getAllCategories(): Flow<List<String>>
    suspend fun getFoodItemById(id: String): FoodItem?
    suspend fun addFoodItem(input: FoodItemInput): String
    suspend fun updateFoodItem(id: String, input: FoodItemInput)
    suspend fun deleteFoodItem(id: String)

    // Meal Entries
    fun getMealEntriesWithFoodByDate(date: LocalDate): Flow<List<MealEntryWithFood>>
    suspend fun addMealEntry(input: MealEntryInput)
    suspend fun updateMealEntry(id: String, input: MealEntryInput)
    suspend fun deleteMealEntry(id: String)

    // Daily Summary
    fun getDailySummary(date: LocalDate): Flow<DailyNutritionSummary?>
    suspend fun updateDailySummaryManualInputs(date: LocalDate, input: DailyNutritionInput)
    fun getDailySummariesInRange(startDate: LocalDate, endDate: LocalDate): Flow<List<DailyNutritionSummary>>

    // Templates
    fun getAllMealTemplates(): Flow<List<MealTemplate>>
    suspend fun getMealTemplateWithItems(templateId: String): MealTemplateWithItems?
    suspend fun createMealTemplate(input: MealTemplateInput): String
    suspend fun deleteMealTemplate(id: String)
    suspend fun logMealFromTemplate(templateId: String, date: LocalDate, mealTime: MealTime, amountMultipliers: Map<String, Double>?)
}
