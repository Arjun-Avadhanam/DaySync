package com.daysync.app.core.notion

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

data class WeeklySummary(
    val title: String,
    val content: String,
    val pageId: String,
)

/**
 * Reads the latest weekly summary from the "DaySync Weekly Summaries"
 * Notion page. Claude's scheduled routine creates child pages there;
 * this reader fetches the most recent one and returns its text content.
 */
@Singleton
class NotionSummaryReader @Inject constructor(
    private val httpClient: HttpClient,
    private val apiKey: String,
) {
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Returns the most recent weekly summary, or null if none exist.
     */
    suspend fun fetchLatestSummary(): WeeklySummary? {
        if (apiKey.isBlank()) return null

        return try {
            // 1. List child blocks of the parent page to find child pages
            val childrenJson = httpClient.get("$BASE_URL/blocks/$PARENT_PAGE_ID/children?page_size=100") {
                header(HttpHeaders.Authorization, "Bearer $apiKey")
                header("Notion-Version", NOTION_VERSION)
            }.bodyAsText()

            val children = json.parseToJsonElement(childrenJson).jsonObject
            val results = children["results"]?.jsonArray ?: return null

            // Find child_page blocks (most recent = last in the list)
            val childPages = results.filter {
                it.jsonObject["type"]?.jsonPrimitive?.content == "child_page"
            }
            if (childPages.isEmpty()) return null

            val latestBlock = childPages.last().jsonObject
            val pageId = latestBlock["id"]?.jsonPrimitive?.content ?: return null
            val title = latestBlock["child_page"]?.jsonObject
                ?.get("title")?.jsonPrimitive?.content ?: "Weekly Summary"

            // 2. Fetch the content blocks of the child page
            val content = fetchPageContent(pageId)
            if (content.isBlank()) return null

            WeeklySummary(title = title, content = content, pageId = pageId)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch weekly summary from Notion", e)
            null
        }
    }

    private suspend fun fetchPageContent(pageId: String): String {
        val blocksJson = httpClient.get("$BASE_URL/blocks/$pageId/children?page_size=100") {
            header(HttpHeaders.Authorization, "Bearer $apiKey")
            header("Notion-Version", NOTION_VERSION)
        }.bodyAsText()

        val blocks = json.parseToJsonElement(blocksJson).jsonObject
        val results = blocks["results"]?.jsonArray ?: return ""

        return buildString {
            for (block in results) {
                val obj = block.jsonObject
                val type = obj["type"]?.jsonPrimitive?.content ?: continue
                val blockData = obj[type]?.jsonObject ?: continue

                when (type) {
                    "heading_1" -> {
                        appendLine("# ${extractRichText(blockData)}")
                        appendLine()
                    }
                    "heading_2" -> {
                        appendLine("## ${extractRichText(blockData)}")
                        appendLine()
                    }
                    "heading_3" -> {
                        appendLine("### ${extractRichText(blockData)}")
                        appendLine()
                    }
                    "paragraph" -> {
                        val text = extractRichText(blockData)
                        if (text.isNotBlank()) appendLine(text)
                        appendLine()
                    }
                    "bulleted_list_item" -> {
                        appendLine("- ${extractRichText(blockData)}")
                    }
                    "numbered_list_item" -> {
                        appendLine("- ${extractRichText(blockData)}")
                    }
                    "divider" -> {
                        appendLine("---")
                        appendLine()
                    }
                }
            }
        }.trim()
    }

    private fun extractRichText(blockData: JsonObject): String {
        val richText = blockData["rich_text"]?.jsonArray ?: return ""
        return richText.joinToString("") { element ->
            element.jsonObject["plain_text"]?.jsonPrimitive?.content ?: ""
        }
    }

    companion object {
        private const val TAG = "NotionSummaryReader"
        private const val BASE_URL = "https://api.notion.com/v1"
        private const val NOTION_VERSION = "2022-06-28"
        const val PARENT_PAGE_ID = "34672c903eee80dfb18dfd9b788033dd"
    }
}
