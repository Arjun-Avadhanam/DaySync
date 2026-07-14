package com.daysync.app.core.notion

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

data class WeeklySummary(
    val title: String,
    val blocks: List<NotionBlock>,
    val pageId: String,
)

/**
 * Reads the latest weekly summary from the "DaySync Weekly Summaries"
 * Notion page. Claude's scheduled routine creates child pages there;
 * this reader fetches the most recent one and returns its content as
 * structured blocks (headings, paragraphs, lists, dividers, tables).
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
    suspend fun fetchLatestSummary(parentPageId: String = PARENT_PAGE_ID): WeeklySummary? {
        if (apiKey.isBlank()) return null
        val effectivePageId = parentPageId.ifBlank { PARENT_PAGE_ID }
        if (effectivePageId.isBlank()) return null

        return try {
            // 1. List child blocks of the parent page to find child pages
            val results = fetchChildren(effectivePageId) ?: return null

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
            val blocks = fetchPageBlocks(pageId)
            if (blocks.isEmpty()) return null

            WeeklySummary(title = title, blocks = blocks, pageId = pageId)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch weekly summary from Notion", e)
            null
        }
    }

    private suspend fun fetchPageBlocks(pageId: String): List<NotionBlock> {
        val results = fetchChildren(pageId) ?: return emptyList()

        // Notion keeps table rows as children of the table block, so each table
        // needs its own call. Fire them together — 5 tables cost one round-trip
        // of latency, not five.
        val tableIds = NotionBlockParser.tableBlockIds(results)
        val tableRows: Map<String, JsonArray> = if (tableIds.isEmpty()) {
            emptyMap()
        } else {
            coroutineScope {
                tableIds.map { id ->
                    async { id to runCatching { fetchChildren(id) }.getOrNull() }
                }.awaitAll()
            }.mapNotNull { (id, rows) -> rows?.let { id to it } }.toMap()
        }

        return NotionBlockParser.parse(results, tableRows)
    }

    private suspend fun fetchChildren(blockId: String): JsonArray? {
        val body = httpClient.get("$BASE_URL/blocks/$blockId/children?page_size=100") {
            header(HttpHeaders.Authorization, "Bearer $apiKey")
            header("Notion-Version", NOTION_VERSION)
        }.bodyAsText()
        return json.parseToJsonElement(body).jsonObject["results"]?.jsonArray
    }

    companion object {
        private const val TAG = "NotionSummaryReader"
        private const val BASE_URL = "https://api.notion.com/v1"
        private const val NOTION_VERSION = "2022-06-28"
        const val PARENT_PAGE_ID = "34672c903eee80dfb18dfd9b788033dd"
    }
}
