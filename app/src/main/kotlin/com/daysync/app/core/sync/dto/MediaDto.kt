package com.daysync.app.core.sync.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MediaItemDto(
    val id: String,
    val title: String,
    @SerialName("media_type") val mediaType: String,
    val status: String,
    val score: Double?,
    val creators: String?,
    @SerialName("completed_date") val completedDate: String?,
    val notes: String?,
    @SerialName("last_modified") val lastModified: Long,
    @SerialName("is_deleted") val isDeleted: Boolean,
)
