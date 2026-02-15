package com.daysync.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.daysync.app.core.sync.SyncStatus
import com.daysync.app.core.sync.SyncableEntity
import kotlin.time.Clock
import kotlin.time.Instant

@Entity(tableName = "food_items")
data class FoodItemEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String? = null,
    val caloriesPerUnit: Double,
    val proteinPerUnit: Double = 0.0,
    val carbsPerUnit: Double = 0.0,
    val fatPerUnit: Double = 0.0,
    val sugarPerUnit: Double = 0.0,
    val unitType: String, // pieces, grams, cups, ml, etc.
    val servingDescription: String? = null,
    override val syncStatus: SyncStatus = SyncStatus.PENDING,
    override val lastModified: Instant = Clock.System.now(),
    override val isDeleted: Boolean = false,
) : SyncableEntity
