package com.daysync.app.feature.media.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class ItunesApiClient(
    private val httpClient: HttpClient,
) {
    private val baseUrl = "https://itunes.apple.com/search"
    // iTunes returns text/javascript, not application/json, so we parse manually
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun searchMusic(query: String): List<MediaMetadataResult> = try {
        val text = httpClient.get(baseUrl) {
            parameter("term", query)
            parameter("media", "music")
            parameter("entity", "album")
            parameter("limit", 10)
        }.bodyAsText()
        json.decodeFromString<ItunesResponse>(text).results.map { it.toMusicResult() }
    } catch (_: Exception) {
        emptyList()
    }

    suspend fun searchPodcasts(query: String): List<MediaMetadataResult> = try {
        val text = httpClient.get(baseUrl) {
            parameter("term", query)
            parameter("media", "podcast")
            parameter("limit", 10)
        }.bodyAsText()
        json.decodeFromString<ItunesResponse>(text).results.map { it.toPodcastResult() }
    } catch (_: Exception) {
        emptyList()
    }

    private fun ItunesItem.toMusicResult(): MediaMetadataResult {
        return MediaMetadataResult(
            title = collectionName ?: trackName ?: "Unknown",
            creators = listOfNotNull(artistName),
            coverImageUrl = artworkUrl100?.replace("100x100", "600x600"),
            year = releaseDate?.take(4),
            externalId = collectionId?.toString(),
        )
    }

    private fun ItunesItem.toPodcastResult(): MediaMetadataResult {
        return MediaMetadataResult(
            title = collectionName ?: trackName ?: "Unknown",
            creators = listOfNotNull(artistName),
            coverImageUrl = artworkUrl100?.replace("100x100", "600x600"),
            externalId = collectionId?.toString(),
        )
    }

    @Serializable
    private data class ItunesResponse(
        val resultCount: Int = 0,
        val results: List<ItunesItem> = emptyList(),
    )

    @Serializable
    private data class ItunesItem(
        val trackName: String? = null,
        val collectionName: String? = null,
        val artistName: String? = null,
        val collectionId: Long? = null,
        val artworkUrl100: String? = null,
        val releaseDate: String? = null,
    )
}
