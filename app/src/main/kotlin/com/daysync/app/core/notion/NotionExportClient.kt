package com.daysync.app.core.notion

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

private const val NOTION_VERSION = "2022-06-28"
private const val BASE_URL = "https://api.notion.com/v1"

/**
 * Creates pages in Notion databases via POST /v1/pages.
 * Shared by food library, journal, and media export features.
 */
class NotionExportClient(
    private val httpClient: HttpClient,
    private val apiKey: String,
) {
    /**
     * Create a page in the given database with the specified properties
     * and optional body content (rich text blocks).
     */
    suspend fun createPage(
        databaseId: String,
        properties: JsonObject,
        bodyText: String? = null,
    ): Result<String> = runCatching {
        val body = buildJsonObject {
            putJsonObject("parent") {
                put("database_id", databaseId)
            }
            put("properties", properties)
            if (bodyText != null) {
                putJsonArray("children") {
                    add(buildJsonObject {
                        put("object", "block")
                        put("type", "paragraph")
                        putJsonObject("paragraph") {
                            putJsonArray("rich_text") {
                                add(buildJsonObject {
                                    put("type", "text")
                                    putJsonObject("text") {
                                        put("content", bodyText.take(2000))
                                    }
                                })
                            }
                        }
                    })
                }
            }
        }.toString()

        val response = httpClient.post("$BASE_URL/pages") {
            header(HttpHeaders.Authorization, "Bearer $apiKey")
            header("Notion-Version", NOTION_VERSION)
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        val responseText = response.bodyAsText()
        if (response.status.value !in 200..299) {
            throw IllegalStateException("Notion API error ${response.status}: $responseText")
        }
        responseText
    }

    companion object {
        // ── Property builders ────────────────────────────────────────

        fun titleProperty(value: String) = buildJsonObject {
            putJsonArray("title") {
                add(buildJsonObject {
                    put("type", "text")
                    putJsonObject("text") { put("content", value) }
                })
            }
        }

        fun numberProperty(value: Double) = buildJsonObject {
            put("number", value)
        }

        fun selectProperty(value: String) = buildJsonObject {
            putJsonObject("select") { put("name", value) }
        }

        fun multiSelectProperty(values: List<String>) = buildJsonObject {
            putJsonArray("multi_select") {
                values.forEach { v ->
                    add(buildJsonObject { put("name", v) })
                }
            }
        }

        fun dateProperty(isoDate: String) = buildJsonObject {
            putJsonObject("date") { put("start", isoDate) }
        }

        fun richTextProperty(value: String) = buildJsonObject {
            putJsonArray("rich_text") {
                add(buildJsonObject {
                    put("type", "text")
                    putJsonObject("text") { put("content", value) }
                })
            }
        }

        fun checkboxProperty(value: Boolean) = buildJsonObject {
            put("checkbox", value)
        }

        fun statusProperty(value: String) = buildJsonObject {
            putJsonObject("status") { put("name", value) }
        }
    }
}
