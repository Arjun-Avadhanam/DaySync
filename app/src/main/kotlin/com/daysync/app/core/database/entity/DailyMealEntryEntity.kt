package com.daysync.app.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.daysync.app.core.sync.SyncStatus
import com.daysync.app.core.sync.SyncableEntity
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

@Entity(
    tableName = "daily_meal_entries",
    foreignKeys = [
        ForeignKey(
            entity = FoodItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["foodId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("foodId"), Index("date")]
)
data class DailyMealEntryEntity(
    @PrimaryKey val id: String,
    val date: LocalDate,
    val foodId: String,
    val mealTime: String, // BREAKFAST, LUNCH, DINNER, SNACKS
    val amount: Double,
    val notes: String? = null,
    override val syncStatus: SyncStatus = SyncStatus.PENDING,
    override val lastModified: Instant = Clock.System.now(),
    override val isDeleted: Boolean = false,
) : SyncableEntity
