package com.daysync.app.feature.media.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class SteamApiClient(
    private val httpClient: HttpClient,
) {
    private val baseUrl = "https://store.steampowered.com/api"

    suspend fun searchGames(query: String): List<MediaMetadataResult> = try {
        val response: SteamSearchResponse = httpClient.get("$baseUrl/storesearch/") {
            parameter("term", query)
            parameter("l", "english")
            parameter("cc", "US")
        }.body()
        response.items.map { it.toResult() }
    } catch (_: Exception) {
        emptyList()
    }

    suspend fun getGameDevelopers(appId: String): List<String> = try {
        val response: Map<String, SteamAppDetailWrapper> =
            httpClient.get("$baseUrl/appdetails") {
                parameter("appids", appId)
            }.body()
        response[appId]?.data?.developers ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }

    private fun SteamSearchItem.toResult(): MediaMetadataResult {
        return MediaMetadataResult(
            title = name,
            coverImageUrl = tinyImage,
            externalId = id.toString(),
        )
    }

    @Serializable
    private data class SteamSearchResponse(
        val total: Int = 0,
        val items: List<SteamSearchItem> = emptyList(),
    )

    @Serializable
    private data class SteamSearchItem(
        val id: Int,
        val name: String,
        @SerialName("tiny_image") val tinyImage: String? = null,
    )

    @Serializable
    private data class SteamAppDetailWrapper(
        val success: Boolean = false,
        val data: SteamAppDetail? = null,
    )

    @Serializable
    private data class SteamAppDetail(
        val name: String? = null,
        val developers: List<String> = emptyList(),
    )
}
