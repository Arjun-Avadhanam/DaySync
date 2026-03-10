package com.daysync.app.feature.sports.data.remote

import com.daysync.app.feature.sports.data.remote.dto.ApifFixture
import com.daysync.app.feature.sports.data.remote.dto.ApifResponse
import com.daysync.app.feature.sports.di.SportsHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiFootballService @Inject constructor(
    @param:SportsHttpClient private val httpClient: HttpClient,
    private val json: Json,
    private val apiKey: ApiFootballKey,
) {
    private val baseUrl = "https://v3.football.api-sports.io"
    private val dailyRequestCount = AtomicInteger(0)
    private val lastResetDay = AtomicLong(0L)

    val remainingBudget: Int
        get() {
            checkDailyReset()
            return (DAILY_LIMIT - dailyRequestCount.get()).coerceAtLeast(0)
        }

    val isBudgetExhausted: Boolean
        get() = remainingBudget <= 0

    private fun checkDailyReset() {
        val today = System.currentTimeMillis() / 86_400_000L
        if (lastResetDay.get() != today) {
            dailyRequestCount.set(0)
            lastResetDay.set(today)
        }
    }

    private fun trackRequest() {
        checkDailyReset()
        dailyRequestCount.incrementAndGet()
    }

    suspend fun getFixtures(
        leagueId: Int,
        season: Int,
        from: String? = null,
        to: String? = null,
    ): ApifResponse<ApifFixture> {
        if (isBudgetExhausted) throw BudgetExhaustedException()
        trackRequest()
        val response = httpClient.get("$baseUrl/fixtures") {
            header("x-apisports-key", apiKey.value)
            parameter("league", leagueId)
            parameter("season", season)
            from?.let { parameter("from", it) }
            to?.let { parameter("to", it) }
        }
        return json.decodeFromString(response.bodyAsText())
    }

    suspend fun getLiveFixtures(): ApifResponse<ApifFixture> {
        if (isBudgetExhausted) throw BudgetExhaustedException()
        trackRequest()
        val response = httpClient.get("$baseUrl/fixtures") {
            header("x-apisports-key", apiKey.value)
            parameter("live", "all")
        }
        return json.decodeFromString(response.bodyAsText())
    }

    companion object {
        const val DAILY_LIMIT = 100
    }
}

data class ApiFootballKey(val value: String)

class BudgetExhaustedException : Exception("API-Football daily request budget exhausted")
