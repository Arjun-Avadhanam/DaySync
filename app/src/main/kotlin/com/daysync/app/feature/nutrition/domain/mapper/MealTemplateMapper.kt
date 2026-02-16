package com.daysync.app.feature.nutrition.domain.mapper

import com.daysync.app.core.database.entity.MealTemplateEntity
import com.daysync.app.core.database.entity.MealTemplateItemEntity
import com.daysync.app.core.sync.SyncStatus
import com.daysync.app.feature.nutrition.domain.model.MealTemplate
import com.daysync.app.feature.nutrition.domain.model.MealTemplateInput
import com.daysync.app.feature.nutrition.domain.model.MealTemplateItem
import com.daysync.app.feature.nutrition.domain.model.MealTemplateItemInput
import java.util.UUID
import kotlin.time.Clock

fun MealTemplateEntity.toDomain(): MealTemplate = MealTemplate(
    id = id,
    name = name,
    description = description,
)

fun MealTemplateItemEntity.toDomain(): MealTemplateItem = MealTemplateItem(
    id = id,
    templateId = templateId,
    foodId = foodId,
    defaultAmount = defaultAmount,
)

fun MealTemplateInput.toEntity(): MealTemplateEntity = MealTemplateEntity(
    id = UUID.randomUUID().toString(),
    name = name,
    description = description,
    syncStatus = SyncStatus.PENDING,
    lastModified = Clock.System.now(),
)

fun MealTemplateItemInput.toEntity(templateId: String): MealTemplateItemEntity =
    MealTemplateItemEntity(
        id = UUID.randomUUID().toString(),
        templateId = templateId,
        foodId = foodId,
        defaultAmount = defaultAmount,
        syncStatus = SyncStatus.PENDING,
        lastModified = Clock.System.now(),
    )
