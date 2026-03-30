package com.daysync.app.feature.media.domain

enum class MediaType(val displayName: String, val isVisualHeavy: Boolean) {
    BOOK("Book", true),
    ARTICLE("Article", false),
    TV_SERIES("TV Series", true),
    MOVIE("Movie", true),
    PODCAST("Podcast", false),
    MANGA("Manga", true),
    ANIME("Anime", true),
    COMIC("Comic", true),
    MUSIC("Music", false),
    GAME("Game", true);

    companion object {
        val FILM_AND_TV = setOf(MOVIE, TV_SERIES)
        val READING = setOf(BOOK, ARTICLE, COMIC, MANGA)

        fun fromString(value: String): MediaType = when (value) {
            "FILM" -> MOVIE // Backward compat: old FILM entries map to MOVIE
            else -> entries.firstOrNull { it.name == value } ?: BOOK
        }
    }
}
