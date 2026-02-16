package com.daysync.app.feature.nutrition.domain.mapper

import com.daysync.app.core.database.entity.DailyNutritionSummaryEntity
import com.daysync.app.feature.nutrition.domain.model.DailyNutritionSummary

fun DailyNutritionSummaryEntity.toDomain(): DailyNutritionSummary = DailyNutritionSummary(
    id = id,
    date = date,
    totalCalories = totalCalories,
    totalProtein = totalProtein,
    totalCarbs = totalCarbs,
    totalFat = totalFat,
    totalSugar = totalSugar,
    waterLiters = waterLiters,
    caloriesBurnt = caloriesBurnt,
    mood = mood,
    notes = notes,
)
