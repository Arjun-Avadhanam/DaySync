package com.daysync.app.feature.sports.data

import com.daysync.app.core.database.dao.SportEventDao
import com.daysync.app.feature.sports.data.remote.ApiFootballService
import com.daysync.app.feature.sports.data.remote.EspnApiService
import com.daysync.app.feature.sports.data.remote.dto.mapApifStatus
import com.daysync.app.feature.sports.data.remote.dto.toCompetitorEntity
import com.daysync.app.feature.sports.data.remote.dto.toSportEventEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SportsRefreshManager @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
    private val dao: SportEventDao,
    private val apiFootballService: ApiFootballService,
    private val espnApi: EspnApiService,
) {
    private var pollingJob: Job? = null
    private val _isPolling = MutableStateFlow(false)
    val isPolling: StateFlow<Boolean> = _isPolling

    fun startPolling(scope: CoroutineScope) {
        if (pollingJob?.isActive == true) return
        pollingJob = scope.launch {
            _isPolling.value = true
            while (isActive) {
                try {
                    pollLiveScores()
                } catch (_: CancellationException) {
                    break
                } catch (_: Exception) {
                    // Log and continue
                }
                delay(POLL_INTERVAL_MS)
            }
            _isPolling.value = false
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
        _isPolling.value = false
    }

    private suspend fun pollLiveScores() {
        // Snapshot watchlisted event statuses before refresh to detect completions
        val watchlistedIds = dao.getWatchlistedEventIds().first().toSet()
        val preRefreshStatuses = mutableMapOf<String, String>()
        for (id in watchlistedIds) {
            dao.getEventById(id)?.let { preRefreshStatuses[id] = it.status }
        }

        val followed = dao.getFollowedCompetitionIds().toSet()

        // Poll API-Football for live football (1 request = all live matches)
        if (!apiFootballService.isBudgetExhausted) {
            try {
                val response = apiFootballService.getLiveFixtures()
                val allCompetitions = dao.getAllCompetitionsList()
                val apifIdToCompId = allCompetitions.associate { (it.apiFootballId ?: -1) to it.id }

                response.response.forEach { fixture ->
                    val leagueId = fixture.league?.id ?: return@forEach
                    val competitionId = apifIdToCompId[leagueId] ?: return@forEach
                    if (competitionId !in followed) return@forEach
                    val event = fixture.toSportEventEntity(competitionId) ?: return@forEach
                    dao.insertEvent(event)
                    listOfNotNull(
                        fixture.teams?.home?.toCompetitorEntity(),
                        fixture.teams?.away?.toCompetitorEntity(),
                    ).forEach { dao.insertCompetitor(it) }
                }
            } catch (_: Exception) {}
        }

        // Poll ESPN for other sports
        val espnSports = listOf(
            Triple("basketball", "nba", "basketball-nba"),
            Triple("mma", "ufc", "mma-ufc"),
        )
        for ((sport, league, compId) in espnSports) {
            if (compId !in followed) continue
            try {
                val response = espnApi.getScoreboard(sport, league)
                val sportId = when (sport) {
                    "basketball" -> "basketball"
                    "mma" -> "mma"
                    else -> sport
                }
                response.events.forEach { espnEvent ->
                    val event = espnEvent.toSportEventEntity(sportId, compId) ?: return@forEach
                    dao.insertEvent(event)
                    espnEvent.competitions.flatMap { comp ->
                        comp.competitors.mapNotNull { it.toCompetitorEntity(sportId) }
                    }.forEach { dao.insertCompetitor(it) }
                }
            } catch (_: Exception) {}
        }

        // Check for watchlisted events that just transitioned to COMPLETED
        for (id in watchlistedIds) {
            val oldStatus = preRefreshStatuses[id] ?: continue
            val newEvent = dao.getEventById(id) ?: continue
            if (oldStatus != "COMPLETED" && newEvent.status == "COMPLETED") {
                // Schedule post-match notification 30 min from now
                val triggerAt = System.currentTimeMillis() + POST_MATCH_DELAY_MS
                val name = newEvent.eventName ?: "Match"
                com.daysync.app.feature.sports.service.MatchNotificationReceiver.schedule(
                    context, id, name,
                    com.daysync.app.feature.sports.service.MatchNotificationReceiver.TYPE_POST_MATCH,
                    triggerAt,
                )
            }
        }
    }

    companion object {
        const val POLL_INTERVAL_MS = 60_000L // 60 seconds
        private const val POST_MATCH_DELAY_MS = 30 * 60 * 1000L
    }
}
