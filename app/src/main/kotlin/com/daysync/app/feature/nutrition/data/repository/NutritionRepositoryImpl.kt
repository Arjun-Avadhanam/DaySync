package com.daysync.app.feature.nutrition.data.repository

import com.daysync.app.core.database.dao.DailyMealEntryDao
import com.daysync.app.core.database.dao.DailyNutritionSummaryDao
import com.daysync.app.core.database.dao.FoodItemDao
import com.daysync.app.core.database.dao.MealTemplateDao
import com.daysync.app.core.database.dao.MealTemplateItemDao
import com.daysync.app.core.database.entity.DailyMealEntryEntity
import com.daysync.app.core.database.entity.DailyNutritionSummaryEntity
import com.daysync.app.core.sync.SyncStatus
import com.daysync.app.feature.nutrition.domain.mapper.toDomain
import com.daysync.app.feature.nutrition.domain.mapper.toEntity
import com.daysync.app.feature.nutrition.domain.model.DailyNutritionInput
import com.daysync.app.feature.nutrition.domain.model.DailyNutritionSummary
import com.daysync.app.feature.nutrition.domain.model.FoodItem
import com.daysync.app.feature.nutrition.domain.model.FoodItemInput
import com.daysync.app.feature.nutrition.domain.model.MealEntryInput
import com.daysync.app.feature.nutrition.domain.model.MealEntryWithFood
import com.daysync.app.feature.nutrition.domain.model.MealTemplate
import com.daysync.app.feature.nutrition.domain.model.MealTemplateInput
import com.daysync.app.feature.nutrition.domain.model.MealTemplateItemWithFood
import com.daysync.app.feature.nutrition.domain.model.MealTemplateWithItems
import com.daysync.app.feature.nutrition.domain.model.MealTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

@Singleton
class NutritionRepositoryImpl @Inject constructor(
    private val foodItemDao: FoodItemDao,
    private val mealTemplateDao: MealTemplateDao,
    private val mealTemplateItemDao: MealTemplateItemDao,
    private val dailyMealEntryDao: DailyMealEntryDao,
    private val dailyNutritionSummaryDao: DailyNutritionSummaryDao,
) : NutritionRepository {

    // -- Food Library --

    override fun getAllFoodItems(): Flow<List<FoodItem>> =
        foodItemDao.getAll().map { entities -> entities.map { it.toDomain() } }

    override fun searchFoodItems(query: String): Flow<List<FoodItem>> =
        foodItemDao.searchByName(query).map { entities -> entities.map { it.toDomain() } }

    override fun getFoodItemsByCategory(category: String): Flow<List<FoodItem>> =
        foodItemDao.getByCategory(category).map { entities -> entities.map { it.toDomain() } }

    override fun getAllCategories(): Flow<List<String>> =
        foodItemDao.getAllCategories()

    override suspend fun getFoodItemById(id: String): FoodItem? =
        foodItemDao.getById(id)?.toDomain()

    override suspend fun addFoodItem(input: FoodItemInput): String {
        val entity = input.toEntity()
        foodItemDao.insert(entity)
        return entity.id
    }

    override suspend fun updateFoodItem(id: String, input: FoodItemInput) {
        val existing = foodItemDao.getById(id) ?: return
        val updated = existing.copy(
            name = input.name,
            category = input.category,
            caloriesPerUnit = input.caloriesPerUnit,
            proteinPerUnit = input.proteinPerUnit,
            carbsPerUnit = input.carbsPerUnit,
            fatPerUnit = input.fatPerUnit,
            sugarPerUnit = input.sugarPerUnit,
            unitType = input.unitType.name,
            servingDescription = input.servingDescription,
            syncStatus = SyncStatus.PENDING,
            lastModified = Clock.System.now(),
        )
        foodItemDao.update(updated)
    }

    override suspend fun deleteFoodItem(id: String) {
        val existing = foodItemDao.getById(id) ?: return
        val softDeleted = existing.copy(
            isDeleted = true,
            syncStatus = SyncStatus.PENDING,
            lastModified = Clock.System.now(),
        )
        foodItemDao.update(softDeleted)
    }

    // -- Meal Entries --

    override fun getMealEntriesWithFoodByDate(date: LocalDate): Flow<List<MealEntryWithFood>> =
        combine(
            dailyMealEntryDao.getByDate(date),
            foodItemDao.getAll(),
        ) { entries, foods ->
            val foodMap = foods.associateBy { it.id }
            entries.mapNotNull { entry ->
                val food = foodMap[entry.foodId] ?: return@mapNotNull null
                MealEntryWithFood.from(entry.toDomain(), food.toDomain())
            }
        }

    override suspend fun addMealEntry(input: MealEntryInput) {
        val entity = input.toEntity()
        dailyMealEntryDao.insert(entity)
        recalculateDailySummary(input.date)
    }

    override suspend fun updateMealEntry(id: String, input: MealEntryInput) {
        val existing = dailyMealEntryDao.getById(id) ?: return
        val updated = existing.copy(
            date = input.date,
            foodId = input.foodId,
            mealTime = input.mealTime.dbValue,
            amount = input.amount,
            notes = input.notes,
            syncStatus = SyncStatus.PENDING,
            lastModified = Clock.System.now(),
        )
        dailyMealEntryDao.update(updated)
        recalculateDailySummary(input.date)
        // If date changed, recalculate old date too
        if (existing.date != input.date) {
            recalculateDailySummary(existing.date)
        }
    }

    override suspend fun deleteMealEntry(id: String) {
        val entry = dailyMealEntryDao.getById(id) ?: return
        dailyMealEntryDao.deleteById(id)
        recalculateDailySummary(entry.date)
    }

    // -- Daily Summary --

    override fun getDailySummary(date: LocalDate): Flow<DailyNutritionSummary?> =
        dailyNutritionSummaryDao.getAll().map { summaries ->
            summaries.firstOrNull { it.date == date }?.toDomain()
        }

    override suspend fun updateDailySummaryManualInputs(date: LocalDate, input: DailyNutritionInput) {
        val existing = dailyNutritionSummaryDao.getByDate(date)
        if (existing != null) {
            val updated = existing.copy(
                waterLiters = input.waterLiters ?: existing.waterLiters,
                caloriesBurnt = input.caloriesBurnt ?: existing.caloriesBurnt,
                mood = input.mood ?: existing.mood,
                notes = input.notes ?: existing.notes,
                syncStatus = SyncStatus.PENDING,
                lastModified = Clock.System.now(),
            )
            dailyNutritionSummaryDao.update(updated)
        } else {
            val newSummary = DailyNutritionSummaryEntity(
                id = UUID.randomUUID().toString(),
                date = date,
                waterLiters = input.waterLiters ?: 0.0,
                caloriesBurnt = input.caloriesBurnt ?: 0.0,
                mood = input.mood,
                notes = input.notes,
                syncStatus = SyncStatus.PENDING,
                lastModified = Clock.System.now(),
            )
            dailyNutritionSummaryDao.insert(newSummary)
        }
    }

    override fun getDailySummariesInRange(
        startDate: LocalDate,
        endDate: LocalDate,
    ): Flow<List<DailyNutritionSummary>> =
        dailyNutritionSummaryDao.getByDateRange(startDate, endDate)
            .map { entities -> entities.map { it.toDomain() } }

    // -- Templates --

    override fun getAllMealTemplates(): Flow<List<MealTemplate>> =
        mealTemplateDao.getAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getMealTemplateWithItems(templateId: String): MealTemplateWithItems? {
        val template = mealTemplateDao.getById(templateId)?.toDomain() ?: return null
        val templateItems = mealTemplateItemDao.getByTemplateIdSync(templateId)
        val itemsWithFood = templateItems.mapNotNull { item ->
            val food = foodItemDao.getById(item.foodId)?.toDomain() ?: return@mapNotNull null
            MealTemplateItemWithFood(item = item.toDomain(), food = food)
        }
        return MealTemplateWithItems(template = template, items = itemsWithFood)
    }

    override suspend fun createMealTemplate(input: MealTemplateInput): String {
        val templateEntity = input.toEntity()
        mealTemplateDao.insert(templateEntity)
        val itemEntities = input.items.map { it.toEntity(templateEntity.id) }
        mealTemplateItemDao.insertAll(itemEntities)
        return templateEntity.id
    }

    override suspend fun deleteMealTemplate(id: String) {
        val existing = mealTemplateDao.getById(id) ?: return
        // Items are cascaded via FK, but we soft-delete the template
        val softDeleted = existing.copy(
            isDeleted = true,
            syncStatus = SyncStatus.PENDING,
            lastModified = Clock.System.now(),
        )
        mealTemplateDao.update(softDeleted)
    }

    override suspend fun logMealFromTemplate(
        templateId: String,
        date: LocalDate,
        mealTime: MealTime,
        amountMultipliers: Map<String, Double>?,
    ) {
        val templateItems = mealTemplateItemDao.getByTemplateIdSync(templateId)
        val entries = templateItems.map { item ->
            val amount = amountMultipliers?.get(item.id) ?: item.defaultAmount
            DailyMealEntryEntity(
                id = UUID.randomUUID().toString(),
                date = date,
                foodId = item.foodId,
                mealTime = mealTime.dbValue,
                amount = amount,
                syncStatus = SyncStatus.PENDING,
                lastModified = Clock.System.now(),
            )
        }
        dailyMealEntryDao.insertAll(entries)
        recalculateDailySummary(date)
    }

    // -- Private Helpers --

    private suspend fun recalculateDailySummary(date: LocalDate) {
        val entries = dailyMealEntryDao.getMealEntriesByDateSync(date)
        var totalCalories = 0.0
        var totalProtein = 0.0
        var totalCarbs = 0.0
        var totalFat = 0.0
        var totalSugar = 0.0

        for (entry in entries) {
            val food = foodItemDao.getById(entry.foodId) ?: continue
            totalCalories += entry.amount * food.caloriesPerUnit
            totalProtein += entry.amount * food.proteinPerUnit
            totalCarbs += entry.amount * food.carbsPerUnit
            totalFat += entry.amount * food.fatPerUnit
            totalSugar += entry.amount * food.sugarPerUnit
        }

        val existing = dailyNutritionSummaryDao.getByDate(date)
        if (existing != null) {
            val updated = existing.copy(
                totalCalories = totalCalories,
                totalProtein = totalProtein,
                totalCarbs = totalCarbs,
                totalFat = totalFat,
                totalSugar = totalSugar,
                syncStatus = SyncStatus.PENDING,
                lastModified = Clock.System.now(),
            )
            dailyNutritionSummaryDao.update(updated)
        } else {
            val newSummary = DailyNutritionSummaryEntity(
                id = UUID.randomUUID().toString(),
                date = date,
                totalCalories = totalCalories,
                totalProtein = totalProtein,
                totalCarbs = totalCarbs,
                totalFat = totalFat,
                totalSugar = totalSugar,
                syncStatus = SyncStatus.PENDING,
                lastModified = Clock.System.now(),
            )
            dailyNutritionSummaryDao.insert(newSummary)
        }
    }
}
