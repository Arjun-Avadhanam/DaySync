package com.daysync.app.feature.media.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject

class RawgApiClient @Inject constructor(
    private val httpClient: HttpClient,
    private val apiKey: String,
) {
    private val baseUrl = "https://api.rawg.io/api"

    suspend fun searchGames(query: String): List<MediaMetadataResult> = try {
        val response: RawgSearchResponse = httpClient.get("$baseUrl/games") {
            parameter("search", query)
            parameter("key", apiKey)
            parameter("page_size", 10)
        }.body()
        response.results.map { it.toResult() }
    } catch (_: Exception) {
        emptyList()
    }

    suspend fun getGameDevelopers(rawgId: String): List<String> = try {
        val response: RawgGameDetails = httpClient.get("$baseUrl/games/$rawgId") {
            parameter("key", apiKey)
        }.body()
        response.developers?.map { it.name } ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }

    private fun RawgGame.toResult() = MediaMetadataResult(
        title = name,
        coverImageUrl = backgroundImage,
        year = released?.take(4),
        externalId = id.toString(),
    )

    @Serializable
    private data class RawgSearchResponse(
        val results: List<RawgGame> = emptyList(),
    )

    @Serializable
    private data class RawgGame(
        val id: Int,
        val name: String,
        @SerialName("background_image") val backgroundImage: String? = null,
        val released: String? = null,
    )

    @Serializable
    private data class RawgGameDetails(
        val developers: List<RawgDeveloper>? = null,
    )

    @Serializable
    private data class RawgDeveloper(
        val name: String,
    )
}
