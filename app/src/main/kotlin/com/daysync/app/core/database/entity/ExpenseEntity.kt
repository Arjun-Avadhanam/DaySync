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
    tableName = "expenses",
    indices = [Index("date"), Index("category")]
)
data class ExpenseEntity(
    @PrimaryKey val id: String,
    val title: String? = null,
    val item: String? = null,
    val date: LocalDate,
    val category: String? = null,
    val frequency: String? = null,
    val unitCost: Double,
    val quantity: Double = 1.0,
    val deliveryCharge: Double = 0.0,
    val totalAmount: Double,
    val notes: String? = null,
    val source: String = "MANUAL", // NOTIFICATION, MANUAL, CSV, EMAIL
    val merchantName: String? = null,
    override val syncStatus: SyncStatus = SyncStatus.PENDING,
    override val lastModified: Instant = Clock.System.now(),
    override val isDeleted: Boolean = false,
) : SyncableEntity
