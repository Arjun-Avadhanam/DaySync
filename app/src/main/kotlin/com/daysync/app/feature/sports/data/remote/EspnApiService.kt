package com.daysync.app.feature.sports.data.remote

import com.daysync.app.feature.sports.data.remote.dto.EspnScoreboardResponse
import com.daysync.app.feature.sports.di.SportsHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EspnApiService @Inject constructor(
    @param:SportsHttpClient private val httpClient: HttpClient,
    private val json: Json,
) {
    private val baseUrl = "https://site.api.espn.com/apis/site/v2/sports"

    suspend fun getScoreboard(
        sport: String,
        league: String,
        dates: String? = null,
    ): EspnScoreboardResponse {
        val response = httpClient.get("$baseUrl/$sport/$league/scoreboard") {
            dates?.let { parameter("dates", it) }
        }
        return json.decodeFromString(response.bodyAsText())
    }
}
