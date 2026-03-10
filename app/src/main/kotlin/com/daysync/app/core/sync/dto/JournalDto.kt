package com.daysync.app.core.sync.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JournalEntryDto(
    val id: String,
    val date: String,
    val title: String?,
    val content: String?,
    val mood: Int?,
    val tags: String?,
    @SerialName("is_archived") val isArchived: Boolean,
    @SerialName("last_modified") val lastModified: Long,
    @SerialName("is_deleted") val isDeleted: Boolean,
)
