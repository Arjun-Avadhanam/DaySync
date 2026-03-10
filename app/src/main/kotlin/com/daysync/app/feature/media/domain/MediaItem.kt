package com.daysync.app.feature.media.domain

import kotlinx.datetime.LocalDate

data class MediaItem(
    val id: String,
    val title: String,
    val mediaType: MediaType,
    val status: MediaStatus = MediaStatus.NOT_STARTED,
    val score: Double? = null,
    val creators: List<String> = emptyList(),
    val completedDate: LocalDate? = null,
    val notes: String? = null,
    val coverImageUrl: String? = null,
)
