package com.daysync.app.core.notion

import com.daysync.app.core.database.entity.MediaItemEntity
import com.daysync.app.core.notion.NotionExportClient.Companion.dateProperty
import com.daysync.app.core.notion.NotionExportClient.Companion.multiSelectProperty
import com.daysync.app.core.notion.NotionExportClient.Companion.selectProperty
import com.daysync.app.core.notion.NotionExportClient.Companion.statusProperty
import com.daysync.app.core.notion.NotionExportClient.Companion.titleProperty
import kotlinx.serialization.json.buildJsonObject

class NotionMediaExporter(
    private val client: NotionExportClient,
    private val databaseId: String,
) {
    suspend fun export(item: MediaItemEntity): Result<String> {
        val props = buildJsonObject {
            put("Media", titleProperty(item.title))
            put("Type", selectProperty(mapType(item.mediaType)))
            put("", statusProperty(mapStatus(item.status)))
            item.score?.let { score ->
                val scoreStr = formatScore(score)
                if (scoreStr != null) put("Score", selectProperty(scoreStr))
            }
            if (item.creators.isNotEmpty()) {
                put("Creator", multiSelectProperty(item.creators))
            }
            item.completedDate?.let {
                put("Completed", dateProperty(it.toString()))
            }
        }
        return client.createPage(databaseId, props, bodyText = item.notes)
    }

    private fun mapType(mediaType: String): String = when (mediaType) {
        "BOOK" -> "Book"
        "ARTICLE" -> "Article"
        "TV_SERIES" -> "TV Series"
        "FILM" -> "Film"
        "PODCAST" -> "Podcast"
        "MANGA" -> "Manga"
        "ANIME" -> "Anime"
        "COMIC" -> "Comic"
        "MUSIC" -> "Music"
        "MOVIE" -> "Movie"
        "GAME" -> "Game"
        else -> "Book"
    }

    private fun mapStatus(status: String): String = when (status) {
        "NOT_STARTED" -> "Not started"
        "IN_PROGRESS" -> "In progress"
        "DONE" -> "Done"
        "DROPPED" -> "Dropped"
        else -> "Not started"
    }

    private fun formatScore(score: Double): String? {
        val valid = listOf(1.0, 1.5, 2.0, 2.5, 3.0, 3.5, 4.0, 4.5, 5.0, 5.5, 6.0, 6.5, 7.0, 7.5, 8.0, 8.5, 9.0, 9.5, 10.0)
        val closest = valid.minByOrNull { kotlin.math.abs(it - score) } ?: return null
        return if (closest == closest.toLong().toDouble()) {
            closest.toLong().toString()
        } else {
            closest.toString()
        }
    }
}
