package com.daysync.app.feature.media.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.Serializable
import javax.inject.Inject

class GoogleBooksApiClient @Inject constructor(
    private val httpClient: HttpClient,
) {
    private val baseUrl = "https://www.googleapis.com/books/v1"

    suspend fun searchBooks(query: String): List<MediaMetadataResult> = try {
        val response: GoogleBooksResponse = httpClient.get("$baseUrl/volumes") {
            parameter("q", "intitle:$query")
            parameter("maxResults", 10)
            parameter("orderBy", "relevance")
        }.body()
        response.items?.map { it.toResult() } ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }

    private fun GoogleBookItem.toResult(): MediaMetadataResult {
        val thumbnail = volumeInfo.imageLinks?.thumbnail?.replace("http://", "https://")
        return MediaMetadataResult(
            title = volumeInfo.title,
            creators = volumeInfo.authors ?: emptyList(),
            coverImageUrl = thumbnail,
            year = volumeInfo.publishedDate?.take(4),
            description = volumeInfo.description,
            externalId = id,
        )
    }

    @Serializable
    private data class GoogleBooksResponse(
        val items: List<GoogleBookItem>? = null,
    )

    @Serializable
    private data class GoogleBookItem(
        val id: String,
        val volumeInfo: VolumeInfo,
    )

    @Serializable
    private data class VolumeInfo(
        val title: String,
        val authors: List<String>? = null,
        val publishedDate: String? = null,
        val description: String? = null,
        val imageLinks: ImageLinks? = null,
    )

    @Serializable
    private data class ImageLinks(
        val thumbnail: String? = null,
    )
}
