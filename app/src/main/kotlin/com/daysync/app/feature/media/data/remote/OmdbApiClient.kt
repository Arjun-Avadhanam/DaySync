package com.daysync.app.feature.media.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class OmdbApiClient(
    private val httpClient: HttpClient,
    private val apiKey: String,
) {
    private val baseUrl = "https://www.omdbapi.com"

    suspend fun searchMovies(query: String): List<MediaMetadataResult> =
        search(query, type = "movie")

    suspend fun searchSeries(query: String): List<MediaMetadataResult> =
        search(query, type = "series")

    private suspend fun search(query: String, type: String): List<MediaMetadataResult> = try {
        if (apiKey.isBlank()) return emptyList()
        val response: OmdbSearchResponse = httpClient.get(baseUrl) {
            parameter("apikey", apiKey)
            parameter("s", query)
            parameter("type", type)
        }.body()
        response.search?.map { it.toResult() } ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }

    suspend fun getDetail(imdbId: String): List<String> = try {
        if (apiKey.isBlank()) return emptyList()
        val response: OmdbDetailResponse = httpClient.get(baseUrl) {
            parameter("apikey", apiKey)
            parameter("i", imdbId)
        }.body()
        // Return director for movies, creator for series
        val creators = (response.director ?: response.writer ?: "")
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() && it != "N/A" }
        creators
    } catch (_: Exception) {
        emptyList()
    }

    private fun OmdbSearchItem.toResult(): MediaMetadataResult {
        val posterUrl = if (poster != null && poster != "N/A") poster else null
        return MediaMetadataResult(
            title = title,
            coverImageUrl = posterUrl,
            year = year,
            externalId = imdbId,
        )
    }

    @Serializable
    private data class OmdbSearchResponse(
        @SerialName("Search") val search: List<OmdbSearchItem>? = null,
        @SerialName("Response") val response: String? = null,
    )

    @Serializable
    private data class OmdbSearchItem(
        @SerialName("Title") val title: String,
        @SerialName("Year") val year: String? = null,
        @SerialName("imdbID") val imdbId: String,
        @SerialName("Type") val type: String? = null,
        @SerialName("Poster") val poster: String? = null,
    )

    @Serializable
    private data class OmdbDetailResponse(
        @SerialName("Title") val title: String? = null,
        @SerialName("Director") val director: String? = null,
        @SerialName("Writer") val writer: String? = null,
        @SerialName("Response") val response: String? = null,
    )
}
