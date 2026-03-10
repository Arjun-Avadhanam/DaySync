package com.daysync.app.feature.nutrition.domain.mapper

import com.daysync.app.core.database.entity.FoodItemEntity
import com.daysync.app.core.sync.SyncStatus
import com.daysync.app.feature.nutrition.domain.model.FoodItem
import com.daysync.app.feature.nutrition.domain.model.FoodItemInput
import com.daysync.app.feature.nutrition.domain.model.UnitType
import java.util.UUID
import kotlin.time.Clock

fun FoodItemEntity.toDomain(): FoodItem = FoodItem(
    id = id,
    name = name,
    category = category,
    caloriesPerUnit = caloriesPerUnit,
    proteinPerUnit = proteinPerUnit,
    carbsPerUnit = carbsPerUnit,
    fatPerUnit = fatPerUnit,
    sugarPerUnit = sugarPerUnit,
    unitType = UnitType.fromDbValue(unitType),
    servingDescription = servingDescription,
)

fun FoodItem.toEntity(): FoodItemEntity = FoodItemEntity(
    id = id,
    name = name,
    category = category,
    caloriesPerUnit = caloriesPerUnit,
    proteinPerUnit = proteinPerUnit,
    carbsPerUnit = carbsPerUnit,
    fatPerUnit = fatPerUnit,
    sugarPerUnit = sugarPerUnit,
    unitType = unitType.name,
    servingDescription = servingDescription,
    syncStatus = SyncStatus.PENDING,
    lastModified = Clock.System.now(),
)

fun FoodItemInput.toEntity(): FoodItemEntity = FoodItemEntity(
    id = UUID.randomUUID().toString(),
    name = name,
    category = category,
    caloriesPerUnit = caloriesPerUnit,
    proteinPerUnit = proteinPerUnit,
    carbsPerUnit = carbsPerUnit,
    fatPerUnit = fatPerUnit,
    sugarPerUnit = sugarPerUnit,
    unitType = unitType.name,
    servingDescription = servingDescription,
    syncStatus = SyncStatus.PENDING,
    lastModified = Clock.System.now(),
)
