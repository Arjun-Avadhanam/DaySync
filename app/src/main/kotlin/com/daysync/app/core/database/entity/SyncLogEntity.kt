package com.daysync.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.time.Instant

@Entity(tableName = "sync_logs")
data class SyncLogEntity(
    @PrimaryKey val id: String,
    val tableName: String,
    val lastSyncAt: Instant,
    val recordCount: Int = 0,
    val status: String, // SUCCESS, FAILED, PARTIAL
)
