package com.daysync.app.feature.nutrition.domain.mapper

import com.daysync.app.core.database.entity.DailyMealEntryEntity
import com.daysync.app.core.sync.SyncStatus
import com.daysync.app.feature.nutrition.domain.model.MealEntry
import com.daysync.app.feature.nutrition.domain.model.MealEntryInput
import com.daysync.app.feature.nutrition.domain.model.MealTime
import java.util.UUID
import kotlin.time.Clock

fun DailyMealEntryEntity.toDomain(): MealEntry = MealEntry(
    id = id,
    date = date,
    foodId = foodId,
    mealTime = MealTime.fromDbValue(mealTime),
    amount = amount,
    notes = notes,
)

fun MealEntry.toEntity(): DailyMealEntryEntity = DailyMealEntryEntity(
    id = id,
    date = date,
    foodId = foodId,
    mealTime = mealTime.dbValue,
    amount = amount,
    notes = notes,
    syncStatus = SyncStatus.PENDING,
    lastModified = Clock.System.now(),
)

fun MealEntryInput.toEntity(): DailyMealEntryEntity = DailyMealEntryEntity(
    id = UUID.randomUUID().toString(),
    date = date,
    foodId = foodId,
    mealTime = mealTime.dbValue,
    amount = amount,
    notes = notes,
    syncStatus = SyncStatus.PENDING,
    lastModified = Clock.System.now(),
)
