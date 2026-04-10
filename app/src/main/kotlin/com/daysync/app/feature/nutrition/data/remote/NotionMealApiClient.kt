package com.daysync.app.feature.nutrition.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val NOTION_VERSION = "2022-06-28"
private const val BASE_URL = "https://api.notion.com/v1"
private const val PAGE_SIZE = 100

// Minimal representation we care about after parsing the verbose Notion shape.
data class NotionMealPage(
    val pageId: String,
    val name: String,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val sugar: Double,
    val category: String?,
    val unit: String?,
    val serving: String?,
)

class NotionMealApiClient(
    private val httpClient: HttpClient,
    private val apiKey: String,
    private val databaseId: String,
) {
    suspend fun fetchAllMeals(): List<NotionMealPage> {
        require(apiKey.isNotBlank()) { "NOTION_API_KEY is not configured" }
        require(databaseId.isNotBlank()) { "NOTION_MEAL_DATABASE_ID is not configured" }

        val results = mutableListOf<NotionMealPage>()
        var cursor: String? = null
        do {
            val response = queryPage(cursor)
            response.results.forEach { page -> results += page.toDomain() }
            cursor = if (response.hasMore) response.nextCursor else null
        } while (cursor != null)
        return results
    }

    private suspend fun queryPage(cursor: String?): NotionQueryResponse {
        val body: JsonObject = buildJsonObject {
            put("page_size", PAGE_SIZE)
            cursor?.let { put("start_cursor", it) }
        }
        return httpClient.post("$BASE_URL/databases/$databaseId/query") {
            header(HttpHeaders.Authorization, "Bearer $apiKey")
            header("Notion-Version", NOTION_VERSION)
            contentType(ContentType.Application.Json)
            setBody(body)
        }.body()
    }

    @Serializable
    internal data class NotionQueryResponse(
        val results: List<NotionPage> = emptyList(),
        @SerialName("has_more") val hasMore: Boolean = false,
        @SerialName("next_cursor") val nextCursor: String? = null,
    )

    @Serializable
    internal data class NotionPage(
        val id: String,
        val properties: NotionMealProperties,
    ) {
        fun toDomain(): NotionMealPage = NotionMealPage(
            pageId = id,
            name = properties.mealName?.title?.firstOrNull()?.plainText.orEmpty(),
            calories = properties.calories?.number ?: 0.0,
            protein = properties.protein?.number ?: 0.0,
            carbs = properties.carbs?.number ?: 0.0,
            fat = properties.fat?.number ?: 0.0,
            sugar = properties.sugar?.number ?: 0.0,
            category = properties.category?.select?.name,
            unit = properties.unit?.select?.name,
            serving = properties.serving?.richText?.firstOrNull()?.plainText,
        )
    }

    // Property names here must match the live Notion database exactly.
    @Serializable
    internal data class NotionMealProperties(
        @SerialName("Meal Name") val mealName: TitleProperty? = null,
        @SerialName("Calories per Unit") val calories: NumberProperty? = null,
        @SerialName("Protein per Unit (g)") val protein: NumberProperty? = null,
        @SerialName("Carbs per Unit (g)") val carbs: NumberProperty? = null,
        @SerialName("Fat per Unit (g)") val fat: NumberProperty? = null,
        @SerialName("Sugar per Unit (g)") val sugar: NumberProperty? = null,
        @SerialName("Food Category") val category: SelectProperty? = null,
        @SerialName("Unit Type") val unit: SelectProperty? = null,
        @SerialName("Serving Description") val serving: RichTextProperty? = null,
    )

    @Serializable
    internal data class TitleProperty(val title: List<RichTextFragment> = emptyList())

    @Serializable
    internal data class RichTextProperty(
        @SerialName("rich_text") val richText: List<RichTextFragment> = emptyList(),
    )

    @Serializable
    internal data class RichTextFragment(
        @SerialName("plain_text") val plainText: String = "",
    )

    @Serializable
    internal data class NumberProperty(val number: Double? = null)

    @Serializable
    internal data class SelectProperty(val select: SelectOption? = null)

    @Serializable
    internal data class SelectOption(val name: String? = null)

    companion object {
        // Expose the Json config the caller should install on the HTTP client.
        val jsonConfig: Json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            explicitNulls = false
        }
    }
}
