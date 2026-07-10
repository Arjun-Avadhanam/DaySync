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
    tableName = "budgets",
    indices = [Index("type"), Index("yearMonth")],
)
data class BudgetEntity(
    @PrimaryKey val id: String,
    val type: String,                 // "MONTHLY" | "WEEKLY" | "CUSTOM"
    val category: String? = null,     // null = overall (reserved for future per-category)
    val amount: Double,
    val recurring: Boolean,           // true = template applied every period; false = specific instance
    val yearMonth: String? = null,    // "YYYY-MM" for month-specific rows and all CUSTOM; null for recurring templates
    val weekBlock: Int? = null,       // 1..5 for WEEKLY rows; null otherwise
    val startDate: LocalDate? = null, // set for CUSTOM only
    val endDate: LocalDate? = null,   // set for CUSTOM only
    val label: String? = null,        // optional label for CUSTOM
    override val syncStatus: SyncStatus = SyncStatus.PENDING,
    override val lastModified: Instant = Clock.System.now(),
    override val isDeleted: Boolean = false,
) : SyncableEntity
