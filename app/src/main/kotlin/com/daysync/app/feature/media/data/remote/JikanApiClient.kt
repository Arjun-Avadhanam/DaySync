package com.daysync.app.feature.media.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class JikanApiClient(
    private val httpClient: HttpClient,
) {
    private val baseUrl = "https://api.jikan.moe/v4"

    suspend fun searchManga(query: String): List<MediaMetadataResult> = try {
        val response: JikanResponse = httpClient.get("$baseUrl/manga") {
            parameter("q", query)
            parameter("limit", 10)
        }.body()
        response.data.map { it.toResult() }
    } catch (_: Exception) {
        emptyList()
    }

    suspend fun searchAnime(query: String): List<MediaMetadataResult> = try {
        val response: JikanResponse = httpClient.get("$baseUrl/anime") {
            parameter("q", query)
            parameter("limit", 10)
        }.body()
        response.data.map { it.toResult() }
    } catch (_: Exception) {
        emptyList()
    }

    private fun JikanItem.toResult(): MediaMetadataResult {
        val authorNames = authors.map { it.name }
        val studioNames = studios.map { it.name }
        val creators = authorNames.ifEmpty { studioNames }
        val imageUrl = images?.jpg?.imageUrl
        val yearStr = year?.toString() ?: aired?.from?.take(4)

        return MediaMetadataResult(
            title = titleEnglish ?: title,
            creators = creators,
            coverImageUrl = imageUrl,
            year = yearStr,
            description = synopsis,
            externalId = malId.toString(),
        )
    }

    @Serializable
    private data class JikanResponse(
        val data: List<JikanItem> = emptyList(),
    )

    @Serializable
    private data class JikanItem(
        @SerialName("mal_id") val malId: Int,
        val title: String,
        @SerialName("title_english") val titleEnglish: String? = null,
        val synopsis: String? = null,
        val year: Int? = null,
        val score: Double? = null,
        val images: JikanImages? = null,
        val authors: List<JikanEntity> = emptyList(),
        val studios: List<JikanEntity> = emptyList(),
        val aired: JikanAired? = null,
    )

    @Serializable
    private data class JikanImages(
        val jpg: JikanJpg? = null,
    )

    @Serializable
    private data class JikanJpg(
        @SerialName("image_url") val imageUrl: String? = null,
    )

    @Serializable
    private data class JikanEntity(
        val name: String,
    )

    @Serializable
    private data class JikanAired(
        val from: String? = null,
    )
}
