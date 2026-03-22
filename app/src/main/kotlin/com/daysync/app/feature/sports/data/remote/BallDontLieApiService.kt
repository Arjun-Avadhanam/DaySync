package com.daysync.app.feature.sports.data.remote

import com.daysync.app.feature.sports.data.remote.dto.BdlGamesResponse
import com.daysync.app.feature.sports.di.SportsHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BallDontLieApiService @Inject constructor(
    @param:SportsHttpClient private val httpClient: HttpClient,
    private val json: Json,
    private val apiKey: BallDontLieApiKey,
) {
    private val baseUrl = "https://api.balldontlie.io/v1"

    suspend fun getGames(
        dates: List<String>? = null,
        seasons: List<Int>? = null,
        perPage: Int = 25,
    ): BdlGamesResponse {
        val response = httpClient.get("$baseUrl/games") {
            header("Authorization", apiKey.value)
            dates?.forEach { parameter("dates[]", it) }
            seasons?.forEach { parameter("seasons[]", it) }
            parameter("per_page", perPage)
        }
        return json.decodeFromString(response.bodyAsText())
    }
}

data class BallDontLieApiKey(val value: String)
