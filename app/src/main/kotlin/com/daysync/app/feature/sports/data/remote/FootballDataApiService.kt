package com.daysync.app.feature.sports.data.remote

import com.daysync.app.feature.sports.data.remote.dto.FdMatchesResponse
import com.daysync.app.feature.sports.data.remote.dto.FdStandingsResponse
import com.daysync.app.feature.sports.data.remote.dto.FdTeamsResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import com.daysync.app.feature.sports.di.SportsHttpClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FootballDataApiService @Inject constructor(
    @param:SportsHttpClient private val httpClient: HttpClient,
    private val json: Json,
    private val apiKey: FootballDataApiKey,
) {
    private val baseUrl = "https://api.football-data.org/v4"

    suspend fun getMatches(
        competitionCode: String,
        dateFrom: String? = null,
        dateTo: String? = null,
        status: String? = null,
    ): FdMatchesResponse {
        val response = httpClient.get("$baseUrl/competitions/$competitionCode/matches") {
            header("X-Auth-Token", apiKey.value)
            dateFrom?.let { parameter("dateFrom", it) }
            dateTo?.let { parameter("dateTo", it) }
            status?.let { parameter("status", it) }
        }
        return json.decodeFromString(response.bodyAsText())
    }

    suspend fun getTeams(competitionCode: String): FdTeamsResponse {
        val response = httpClient.get("$baseUrl/competitions/$competitionCode/teams") {
            header("X-Auth-Token", apiKey.value)
        }
        return json.decodeFromString(response.bodyAsText())
    }

    suspend fun getStandings(competitionCode: String): FdStandingsResponse {
        val response = httpClient.get("$baseUrl/competitions/$competitionCode/standings") {
            header("X-Auth-Token", apiKey.value)
        }
        return json.decodeFromString(response.bodyAsText())
    }
}

data class FootballDataApiKey(val value: String)
