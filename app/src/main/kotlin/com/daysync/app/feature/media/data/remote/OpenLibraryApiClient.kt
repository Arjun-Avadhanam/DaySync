package com.daysync.app.feature.media.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class OpenLibraryApiClient(
    private val httpClient: HttpClient,
) {
    private val baseUrl = "https://openlibrary.org"

    suspend fun searchComics(query: String): List<MediaMetadataResult> = try {
        val response: OpenLibraryResponse = httpClient.get("$baseUrl/search.json") {
            parameter("q", query)
            parameter("limit", 10)
        }.body()
        response.docs.map { it.toResult() }
    } catch (_: Exception) {
        emptyList()
    }

    private fun OpenLibraryDoc.toResult(): MediaMetadataResult {
        val coverUrl = coverId?.let { "https://covers.openlibrary.org/b/id/$it-M.jpg" }
        return MediaMetadataResult(
            title = title,
            creators = authorName ?: emptyList(),
            coverImageUrl = coverUrl,
            year = firstPublishYear?.toString(),
            externalId = key,
        )
    }

    @Serializable
    private data class OpenLibraryResponse(
        val docs: List<OpenLibraryDoc> = emptyList(),
    )

    @Serializable
    private data class OpenLibraryDoc(
        val key: String? = null,
        val title: String,
        @SerialName("author_name") val authorName: List<String>? = null,
        @SerialName("first_publish_year") val firstPublishYear: Int? = null,
        @SerialName("cover_i") val coverId: Int? = null,
    )
}
