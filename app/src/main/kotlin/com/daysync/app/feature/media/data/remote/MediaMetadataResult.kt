package com.daysync.app.feature.media.data.remote

data class MediaMetadataResult(
    val title: String,
    val creators: List<String> = emptyList(),
    val coverImageUrl: String? = null,
    val year: String? = null,
    val description: String? = null,
    val externalId: String? = null,
)
