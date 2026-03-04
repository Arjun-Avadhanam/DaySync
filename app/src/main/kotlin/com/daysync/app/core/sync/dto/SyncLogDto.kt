package com.daysync.app.core.sync.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SyncLogDto(
    val id: String,
    @SerialName("table_name") val tableName: String,
    @SerialName("last_sync_at") val lastSyncAt: Long,
    @SerialName("record_count") val recordCount: Int,
    val status: String,
)
