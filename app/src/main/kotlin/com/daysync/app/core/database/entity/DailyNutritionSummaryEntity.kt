package com.daysync.app.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.daysync.app.core.sync.SyncStatus
import com.daysync.app.core.sync.SyncableEntity
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

@Entity(
    tableName = "daily_nutrition_summaries",
    indices = [Index(value = ["date"], unique = true)]
)
data class DailyNutritionSummaryEntity(
    @PrimaryKey val id: String,
    val date: LocalDate,
    val totalCalories: Double = 0.0,
    val totalProtein: Double = 0.0,
    val totalCarbs: Double = 0.0,
    val totalFat: Double = 0.0,
    val totalSugar: Double = 0.0,
    val waterLiters: Double = 0.0,
    val caloriesBurnt: Double = 0.0,
    val mood: String? = null,
    val notes: String? = null,
    override val syncStatus: SyncStatus = SyncStatus.PENDING,
    override val lastModified: Instant = Clock.System.now(),
    override val isDeleted: Boolean = false,
) : SyncableEntity
