package com.daysync.app.feature.sports.data.remote

import com.daysync.app.feature.sports.data.remote.dto.JolpicaResponse
import com.daysync.app.feature.sports.di.SportsHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JolpicaApiService @Inject constructor(
    @param:SportsHttpClient private val httpClient: HttpClient,
    private val json: Json,
) {
    private val baseUrl = "https://api.jolpi.ca/ergast/f1"

    suspend fun getCurrentSeasonRaces(): JolpicaResponse {
        val response = httpClient.get("$baseUrl/current.json")
        return json.decodeFromString(response.bodyAsText())
    }

    suspend fun getRaceResults(season: String, round: String): JolpicaResponse {
        val response = httpClient.get("$baseUrl/$season/$round/results.json")
        return json.decodeFromString(response.bodyAsText())
    }

    suspend fun getCurrentSeasonResults(): JolpicaResponse {
        // Default limit is 30 which truncates multi-race results (22 drivers × N races)
        val response = httpClient.get("$baseUrl/current/results.json?limit=1000")
        return json.decodeFromString(response.bodyAsText())
    }

    suspend fun getQualifyingResults(season: String, round: String): JolpicaResponse {
        val response = httpClient.get("$baseUrl/$season/$round/qualifying.json")
        return json.decodeFromString(response.bodyAsText())
    }
}
