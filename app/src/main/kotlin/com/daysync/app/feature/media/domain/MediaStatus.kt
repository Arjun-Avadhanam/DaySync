package com.daysync.app.feature.media.domain

enum class MediaStatus(val displayName: String) {
    NOT_STARTED("Not Started"),
    IN_PROGRESS("In Progress"),
    DONE("Done"),
    DROPPED("Dropped");

    companion object {
        fun fromString(value: String): MediaStatus =
            entries.firstOrNull { it.name == value } ?: NOT_STARTED
    }
}
