package com.daysync.app.core.sync.mapper

import com.daysync.app.core.database.entity.CompetitionEntity
import com.daysync.app.core.database.entity.CompetitorEntity
import com.daysync.app.core.database.entity.DailyMealEntryEntity
import com.daysync.app.core.database.entity.DailyNutritionSummaryEntity
import com.daysync.app.core.database.entity.EventParticipantEntity
import com.daysync.app.core.database.entity.ExerciseSessionEntity
import com.daysync.app.core.database.entity.ExpenseEntity
import com.daysync.app.core.database.entity.FollowedCompetitionEntity
import com.daysync.app.core.database.entity.FollowedCompetitorEntity
import com.daysync.app.core.database.entity.FoodItemEntity
import com.daysync.app.core.database.entity.HealthMetricEntity
import com.daysync.app.core.database.entity.JournalEntryEntity
import com.daysync.app.core.database.entity.MealTemplateEntity
import com.daysync.app.core.database.entity.MealTemplateItemEntity
import com.daysync.app.core.database.entity.MediaItemEntity
import com.daysync.app.core.database.entity.SleepSessionEntity
import com.daysync.app.core.database.entity.SportEntity
import com.daysync.app.core.database.entity.SportEventEntity
import com.daysync.app.core.database.entity.SyncLogEntity
import com.daysync.app.core.database.entity.VenueEntity
import com.daysync.app.core.database.entity.WatchlistEntryEntity
import com.daysync.app.core.sync.dto.CompetitionDto
import com.daysync.app.core.sync.dto.CompetitorDto
import com.daysync.app.core.sync.dto.DailyMealEntryDto
import com.daysync.app.core.sync.dto.DailyNutritionSummaryDto
import com.daysync.app.core.sync.dto.EventParticipantDto
import com.daysync.app.core.sync.dto.ExerciseSessionDto
import com.daysync.app.core.sync.dto.ExpenseDto
import com.daysync.app.core.sync.dto.FollowedCompetitionDto
import com.daysync.app.core.sync.dto.FollowedCompetitorDto
import com.daysync.app.core.sync.dto.FoodItemDto
import com.daysync.app.core.sync.dto.HealthMetricDto
import com.daysync.app.core.sync.dto.JournalEntryDto
import com.daysync.app.core.sync.dto.MealTemplateDto
import com.daysync.app.core.sync.dto.MealTemplateItemDto
import com.daysync.app.core.sync.dto.MediaItemDto
import com.daysync.app.core.sync.dto.SleepSessionDto
import com.daysync.app.core.sync.dto.SportDto
import com.daysync.app.core.sync.dto.SportEventDto
import com.daysync.app.core.sync.dto.SyncLogDto
import com.daysync.app.core.sync.dto.VenueDto
import com.daysync.app.core.sync.dto.WatchlistEntryDto
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// Health

fun HealthMetricEntity.toDto() = HealthMetricDto(
    id = id,
    type = type,
    value = value,
    unit = unit,
    timestamp = timestamp.toEpochMilliseconds(),
    source = source,
    lastModified = lastModified.toEpochMilliseconds(),
    isDeleted = isDeleted,
)

fun SleepSessionEntity.toDto() = SleepSessionDto(
    id = id,
    startTime = startTime.toEpochMilliseconds(),
    endTime = endTime.toEpochMilliseconds(),
    totalMinutes = totalMinutes,
    deepMinutes = deepMinutes,
    lightMinutes = lightMinutes,
    remMinutes = remMinutes,
    awakeMinutes = awakeMinutes,
    score = score,
    lastModified = lastModified.toEpochMilliseconds(),
    isDeleted = isDeleted,
)

fun ExerciseSessionEntity.toDto() = ExerciseSessionDto(
    id = id,
    exerciseType = exerciseType,
    startTime = startTime.toEpochMilliseconds(),
    endTime = endTime.toEpochMilliseconds(),
    calories = calories,
    avgHeartRate = avgHeartRate,
    maxHeartRate = maxHeartRate,
    distance = distance,
    elevationGain = elevationGain,
    notes = notes,
    lastModified = lastModified.toEpochMilliseconds(),
    isDeleted = isDeleted,
)

// Nutrition

fun FoodItemEntity.toDto() = FoodItemDto(
    id = id,
    name = name,
    category = category,
    caloriesPerUnit = caloriesPerUnit,
    proteinPerUnit = proteinPerUnit,
    carbsPerUnit = carbsPerUnit,
    fatPerUnit = fatPerUnit,
    sugarPerUnit = sugarPerUnit,
    unitType = unitType,
    servingDescription = servingDescription,
    lastModified = lastModified.toEpochMilliseconds(),
    isDeleted = isDeleted,
)

fun MealTemplateEntity.toDto() = MealTemplateDto(
    id = id,
    name = name,
    description = description,
    lastModified = lastModified.toEpochMilliseconds(),
    isDeleted = isDeleted,
)

fun MealTemplateItemEntity.toDto() = MealTemplateItemDto(
    id = id,
    templateId = templateId,
    foodId = foodId,
    defaultAmount = defaultAmount,
    lastModified = lastModified.toEpochMilliseconds(),
    isDeleted = isDeleted,
)

fun DailyMealEntryEntity.toDto() = DailyMealEntryDto(
    id = id,
    date = date.toString(),
    foodId = foodId,
    mealTime = mealTime,
    amount = amount,
    notes = notes,
    lastModified = lastModified.toEpochMilliseconds(),
    isDeleted = isDeleted,
)

fun DailyNutritionSummaryEntity.toDto() = DailyNutritionSummaryDto(
    id = id,
    date = date.toString(),
    totalCalories = totalCalories,
    totalProtein = totalProtein,
    totalCarbs = totalCarbs,
    totalFat = totalFat,
    totalSugar = totalSugar,
    waterLiters = waterLiters,
    caloriesBurnt = caloriesBurnt,
    mood = mood,
    notes = notes,
    lastModified = lastModified.toEpochMilliseconds(),
    isDeleted = isDeleted,
)

// Expenses

fun ExpenseEntity.toDto() = ExpenseDto(
    id = id,
    title = title,
    item = item,
    date = date.toString(),
    category = category,
    frequency = frequency,
    unitCost = unitCost,
    quantity = quantity,
    deliveryCharge = deliveryCharge,
    totalAmount = totalAmount,
    notes = notes,
    source = source,
    merchantName = merchantName,
    lastModified = lastModified.toEpochMilliseconds(),
    isDeleted = isDeleted,
)

// Sports — syncable entities

fun SportEventEntity.toDto() = SportEventDto(
    id = id,
    sportId = sportId,
    competitionId = competitionId,
    venueId = venueId,
    scheduledAt = scheduledAt.toEpochMilliseconds(),
    status = status,
    homeCompetitorId = homeCompetitorId,
    awayCompetitorId = awayCompetitorId,
    homeScore = homeScore,
    awayScore = awayScore,
    eventName = eventName,
    round = round,
    season = season,
    resultDetail = resultDetail,
    lastUpdated = lastUpdated.toEpochMilliseconds(),
    dataSource = dataSource,
    lastModified = lastModified.toEpochMilliseconds(),
    isDeleted = isDeleted,
)

fun WatchlistEntryEntity.toDto() = WatchlistEntryDto(
    id = id,
    eventId = eventId,
    addedAt = addedAt.toEpochMilliseconds(),
    notify = notify,
    notes = notes,
    lastModified = lastModified.toEpochMilliseconds(),
    isDeleted = isDeleted,
)

fun FollowedCompetitorEntity.toDto() = FollowedCompetitorDto(
    id = id,
    competitorId = competitorId,
    addedAt = addedAt.toEpochMilliseconds(),
    lastModified = lastModified.toEpochMilliseconds(),
    isDeleted = isDeleted,
)

fun FollowedCompetitionEntity.toDto() = FollowedCompetitionDto(
    id = id,
    competitionId = competitionId,
    addedAt = addedAt.toEpochMilliseconds(),
    lastModified = lastModified.toEpochMilliseconds(),
    isDeleted = isDeleted,
)

// Sports — reference data (non-syncable)

fun SportEntity.toDto() = SportDto(
    id = id,
    name = name,
    sportType = sportType,
    icon = icon,
)

fun CompetitionEntity.toDto() = CompetitionDto(
    id = id,
    sportId = sportId,
    name = name,
    shortName = shortName,
    country = country,
    logoUrl = logoUrl,
    apiFootballId = apiFootballId,
    footballDataId = footballDataId,
    espnSlug = espnSlug,
)

fun CompetitorEntity.toDto() = CompetitorDto(
    id = id,
    sportId = sportId,
    name = name,
    shortName = shortName,
    logoUrl = logoUrl,
    country = country,
    isIndividual = isIndividual,
    apiFootballId = apiFootballId,
    footballDataId = footballDataId,
    espnId = espnId,
)

fun VenueEntity.toDto() = VenueDto(
    id = id,
    name = name,
    city = city,
    country = country,
    capacity = capacity,
    imageUrl = imageUrl,
)

fun EventParticipantEntity.toDto() = EventParticipantDto(
    id = id,
    eventId = eventId,
    competitorId = competitorId,
    position = position,
    score = score,
    status = status,
    isWinner = isWinner,
    detail = detail,
)

// Journal

fun JournalEntryEntity.toDto() = JournalEntryDto(
    id = id,
    date = date.toString(),
    title = title,
    content = content,
    mood = mood,
    tags = if (tags.isEmpty()) null else Json.encodeToString(tags),
    isArchived = isArchived,
    lastModified = lastModified.toEpochMilliseconds(),
    isDeleted = isDeleted,
)

// Media

fun MediaItemEntity.toDto() = MediaItemDto(
    id = id,
    title = title,
    mediaType = mediaType,
    status = status,
    score = score,
    creators = if (creators.isEmpty()) null else Json.encodeToString(creators),
    completedDate = completedDate?.toString(),
    notes = notes,
    lastModified = lastModified.toEpochMilliseconds(),
    isDeleted = isDeleted,
)

// Daily Health Overrides

fun com.daysync.app.core.database.entity.DailyHealthOverrideEntity.toDto() =
    com.daysync.app.core.sync.dto.DailyHealthOverrideDto(
        date = date.toString(),
        totalCalories = totalCalories,
        weightMorning = weightMorning,
        weightEvening = weightEvening,
        weightNight = weightNight,
        lastModified = lastModified.toEpochMilliseconds(),
        isDeleted = isDeleted,
    )

// Sync Log

fun SyncLogEntity.toDto() = SyncLogDto(
    id = id,
    tableName = tableName,
    lastSyncAt = lastSyncAt.toEpochMilliseconds(),
    recordCount = recordCount,
    status = status,
)
