package com.daysync.app.core.notion

import com.daysync.app.core.database.entity.JournalEntryEntity
import com.daysync.app.core.notion.NotionExportClient.Companion.checkboxProperty
import com.daysync.app.core.notion.NotionExportClient.Companion.dateProperty
import com.daysync.app.core.notion.NotionExportClient.Companion.multiSelectProperty
import com.daysync.app.core.notion.NotionExportClient.Companion.titleProperty
import kotlinx.serialization.json.buildJsonObject

class NotionJournalExporter(
    private val client: NotionExportClient,
    private val databaseId: String,
) {
    suspend fun export(entry: JournalEntryEntity): Result<String> {
        val props = buildJsonObject {
            put("Title", titleProperty(entry.title ?: entry.date.toString()))
            put("Date", dateProperty(entry.date.toString()))
            entry.mood?.let { mood ->
                val moodNames = mapMood(mood)
                if (moodNames.isNotEmpty()) {
                    put("Mood", multiSelectProperty(moodNames))
                }
            }
            val tags = parseTags(entry.tags)
            if (tags.isNotEmpty()) {
                put("Reflection Tags", multiSelectProperty(tags))
            }
            put("Archived", checkboxProperty(entry.isArchived))
        }
        return client.createPage(databaseId, props, bodyText = entry.content)
    }

    private fun mapMood(mood: Int): List<String> = when (mood) {
        1 -> listOf("Demotivated")
        2 -> listOf("Sad")
        3 -> listOf("Neutral")
        4 -> listOf("Happy")
        5 -> listOf("Motivated")
        else -> emptyList()
    }

    private fun parseTags(tags: List<String>): List<String> {
        val validTags = setOf("Work", "Personal Growth", "Health", "Relationships", "Others", "Academics")
        return tags.filter { it in validTags }
    }
}
