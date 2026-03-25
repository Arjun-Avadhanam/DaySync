package com.daysync.app.feature.sports.data

import android.util.Log
import com.daysync.app.core.database.dao.SportEventDao
import com.daysync.app.core.database.entity.CompetitionEntity
import com.daysync.app.core.database.entity.CompetitorEntity
import com.daysync.app.core.database.entity.EventParticipantEntity
import com.daysync.app.core.database.entity.FollowedCompetitionEntity
import com.daysync.app.core.database.entity.FollowedCompetitorEntity
import com.daysync.app.core.database.entity.SportEntity
import com.daysync.app.core.database.entity.SportEventEntity
import com.daysync.app.core.database.entity.WatchlistEntryEntity
import com.daysync.app.feature.sports.data.model.SportEventWithDetails
import com.daysync.app.feature.sports.data.remote.ApiFootballService
import com.daysync.app.feature.sports.data.remote.BallDontLieApiService
import com.daysync.app.feature.sports.data.remote.EspnApiService
import com.daysync.app.feature.sports.data.remote.FootballDataApiService
import com.daysync.app.feature.sports.data.remote.JolpicaApiService
import com.daysync.app.feature.sports.data.remote.dto.EspnEvent
import com.daysync.app.feature.sports.data.remote.dto.getCalendarEntries
import com.daysync.app.feature.sports.data.remote.dto.toCompetitorEntity
import com.daysync.app.feature.sports.data.remote.dto.toMmaFightEntities
import com.daysync.app.feature.sports.data.remote.dto.toFootballMatchEntity
import com.daysync.app.feature.sports.data.remote.dto.toFootballTeamEntity
import com.daysync.app.feature.sports.data.remote.dto.toNbaGameEntity
import com.daysync.app.feature.sports.data.remote.dto.toNbaTeamEntity
import com.daysync.app.feature.sports.data.remote.dto.toParticipantEntity
import com.daysync.app.feature.sports.data.remote.dto.toSportEventEntity
import com.daysync.app.feature.sports.data.remote.dto.toTennisMatchEntities
import com.daysync.app.feature.sports.data.remote.dto.toVenueEntity
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SportsRepositoryImpl @Inject constructor(
    private val dao: SportEventDao,
    private val footballDataApi: FootballDataApiService,
    private val espnApi: EspnApiService,
    private val ballDontLieApi: BallDontLieApiService,
    private val jolpicaApi: JolpicaApiService,
    private val apiFootballService: ApiFootballService,
) : SportsRepository {

    companion object {
        private const val TAG = "SportsRepository"
    }

    override suspend fun ensureSeedData() {
        SeedData.ensureSeedData(dao)
    }

    // Events
    override fun getUpcomingEvents(sportId: String?): Flow<List<SportEventEntity>> {
        return if (sportId != null) dao.getUpcomingEventsBySport(sportId) else dao.getUpcomingEvents()
    }

    override fun getLiveEvents(sportId: String?): Flow<List<SportEventEntity>> {
        return if (sportId != null) dao.getLiveEventsBySport(sportId) else dao.getLiveEvents()
    }

    override fun getRecentResults(sportId: String?): Flow<List<SportEventEntity>> {
        return if (sportId != null) dao.getRecentResultsBySport(sportId) else dao.getRecentResults()
    }

    override fun getWatchlistedEvents(): Flow<List<SportEventEntity>> = dao.getWatchlistedEvents()

    override fun getEventsByCompetition(competitionId: String): Flow<List<SportEventEntity>> {
        return dao.getEventsByCompetition(competitionId)
    }

    override suspend fun getEventById(eventId: String): SportEventEntity? = dao.getEventById(eventId)

    override suspend fun getParticipantsByEvent(eventId: String): List<EventParticipantEntity> {
        return dao.getParticipantsByEventList(eventId)
    }

    override suspend fun getCompetitorName(competitorId: String): String? {
        return dao.getCompetitorById(competitorId)?.name
    }

    // Enrichment
    override suspend fun enrichEvent(
        event: SportEventEntity,
        watchlistedIds: Set<String>,
    ): SportEventWithDetails {
        val competition = dao.getCompetitionById(event.competitionId)
        val sport = dao.getSportById(event.sportId)
        val home = event.homeCompetitorId?.let { dao.getCompetitorById(it) }
        val away = event.awayCompetitorId?.let { dao.getCompetitorById(it) }

        return SportEventWithDetails(
            id = event.id,
            sportId = event.sportId,
            sportName = sport?.name ?: event.sportId,
            competitionId = event.competitionId,
            competitionName = competition?.name ?: "",
            competitionShortName = competition?.shortName,
            scheduledAt = event.scheduledAt,
            status = event.status,
            homeCompetitorId = event.homeCompetitorId,
            awayCompetitorId = event.awayCompetitorId,
            homeCompetitorName = home?.name,
            awayCompetitorName = away?.name,
            homeCompetitorLogo = home?.logoUrl,
            awayCompetitorLogo = away?.logoUrl,
            homeScore = event.homeScore,
            awayScore = event.awayScore,
            eventName = event.eventName,
            round = event.round,
            season = event.season,
            resultDetail = event.resultDetail,
            dataSource = event.dataSource,
            isWatchlisted = event.id in watchlistedIds,
        )
    }

    // Reference data
    override fun getAllSports(): Flow<List<SportEntity>> = dao.getAllSports()
    override fun getAllCompetitions(): Flow<List<CompetitionEntity>> = dao.getAllCompetitions()
    override fun getCompetitionsBySport(sportId: String): Flow<List<CompetitionEntity>> =
        dao.getCompetitionsBySport(sportId)
    override suspend fun getCompetitionById(competitionId: String): CompetitionEntity? =
        dao.getCompetitionById(competitionId)
    override suspend fun getCompetitorById(competitorId: String): CompetitorEntity? =
        dao.getCompetitorById(competitorId)

    // Watchlist
    override fun getWatchlistedEventIds(): Flow<List<String>> = dao.getWatchlistedEventIds()

    override suspend fun toggleWatchlist(eventId: String) {
        if (dao.isEventWatchlisted(eventId)) {
            dao.deleteWatchlistByEventId(eventId)
        } else {
            dao.insertWatchlistEntry(
                WatchlistEntryEntity(
                    id = UUID.randomUUID().toString(),
                    eventId = eventId,
                    addedAt = Clock.System.now(),
                )
            )
        }
    }

    // Following
    override fun getFollowedCompetitions(): Flow<List<FollowedCompetitionEntity>> =
        dao.getFollowedCompetitions()
    override fun getFollowedCompetitors(): Flow<List<FollowedCompetitorEntity>> =
        dao.getFollowedCompetitors()
    override suspend fun getFollowedCompetitionIds(): List<String> =
        dao.getFollowedCompetitionIds()
    override suspend fun getFollowedCompetitorIds(): List<String> =
        dao.getFollowedCompetitorIds()

    override suspend fun toggleFollowCompetition(competitionId: String) {
        val followed = dao.getFollowedCompetitionIds()
        if (competitionId in followed) {
            dao.deleteFollowedCompetitionById(competitionId)
        } else {
            dao.insertFollowedCompetition(
                FollowedCompetitionEntity(
                    id = UUID.randomUUID().toString(),
                    competitionId = competitionId,
                    addedAt = Clock.System.now(),
                )
            )
        }
    }

    override suspend fun toggleFollowCompetitor(competitorId: String) {
        val followed = dao.getFollowedCompetitorIds()
        if (competitorId in followed) {
            dao.deleteFollowedCompetitorById(competitorId)
        } else {
            dao.insertFollowedCompetitor(
                FollowedCompetitorEntity(
                    id = UUID.randomUUID().toString(),
                    competitorId = competitorId,
                    addedAt = Clock.System.now(),
                )
            )
        }
    }

    // Refresh
    override suspend fun refreshFootballFixtures(competitionCode: String, competitionId: String) {
        val response = footballDataApi.getMatches(competitionCode)
        // Insert competitors BEFORE events (events have FK to competitors)
        val competitors = response.matches.flatMap { match ->
            listOfNotNull(
                match.homeTeam?.toCompetitorEntity(),
                match.awayTeam?.toCompetitorEntity(),
            )
        }.distinctBy { it.id }
        if (competitors.isNotEmpty()) {
            dao.insertCompetitors(competitors)
        }
        val events = response.matches.map { it.toSportEventEntity(competitionId) }
        if (events.isNotEmpty()) {
            dao.insertEvents(events)
        }
    }

    override suspend fun refreshFootballTeams(competitionCode: String) {
        try {
            val response = footballDataApi.getTeams(competitionCode)
            val competitors = response.teams.map { it.toCompetitorEntity() }
            if (competitors.isNotEmpty()) {
                dao.insertCompetitors(competitors)
            }
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun refreshAllSports() {
        val errors = mutableListOf<String>()

        // Football via ESPN (all 16 competitions with goal scorers)
        try { refreshFootballEspn() } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh Football: ${e.message}", e)
            errors += "Football: ${e.message}"
        }

        // NBA via ESPN (quarter scores, records, playoff series)
        try { refreshNbaGamesEspn() } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh NBA: ${e.message}", e)
            errors += "NBA: ${e.message}"
        }

        // F1 via Jolpica
        try { refreshF1Schedule() } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh F1: ${e.message}", e)
            errors += "F1: ${e.message}"
        }

        // UFC via ESPN (MMA-specific: each fight = separate event)
        try { refreshMmaEvents() } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh MMA: ${e.message}", e)
            errors += "MMA: ${e.message}"
        }

        // Tennis via ESPN (match-per-event model, Men's + Women's Singles, R3+)
        try { refreshTennisEvents() } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh Tennis: ${e.message}", e)
            errors += "Tennis: ${e.message}"
        }

        if (errors.isNotEmpty()) {
            throw Exception("Some sports failed to refresh:\n${errors.joinToString("\n")}")
        }
    }

    private suspend fun refreshApiFootballCompetitions() {
        if (apiFootballService.isBudgetExhausted) return
        val apifCompetitions = mapOf(
            3 to "football-el",     // Europa League
            48 to "football-efl",   // EFL Cup
            5 to "football-unl",    // Nations League
        )
        val currentYear = Clock.System.now()
            .toLocalDateTime(TimeZone.UTC).year
        for ((leagueId, compId) in apifCompetitions) {
            if (apiFootballService.isBudgetExhausted) break
            try {
                val response = apiFootballService.getFixtures(leagueId, currentYear)
                // Insert competitors BEFORE events (FK dependency)
                val competitors = response.response.flatMap { fixture ->
                    listOfNotNull(
                        fixture.teams?.home?.toCompetitorEntity(),
                        fixture.teams?.away?.toCompetitorEntity(),
                    )
                }.distinctBy { it.id }
                if (competitors.isNotEmpty()) dao.insertCompetitors(competitors)
                val events = response.response.mapNotNull {
                    it.toSportEventEntity(compId)
                }
                if (events.isNotEmpty()) dao.insertEvents(events)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to refresh API-Football league $leagueId: ${e.message}", e)
            }
        }
    }

    private suspend fun refreshFootballEspn() {
        // All 16 competitions with ESPN slugs
        val footballComps = mapOf(
            "eng.1" to "football-pl",
            "esp.1" to "football-laliga",
            "ita.1" to "football-sa",
            "ger.1" to "football-bl1",
            "fra.1" to "football-fl1",
            "uefa.champions" to "football-cl",
            "uefa.europa" to "football-el",
            "eng.fa" to "football-fa",
            "eng.league_cup" to "football-efl",
            "ger.dfb_pokal" to "football-dfb",
            "esp.copa_del_rey" to "football-cdr",
            "ita.coppa_italia" to "football-ci",
            "fifa.world" to "football-wc",
            "uefa.euro" to "football-euro",
            "uefa.nations" to "football-unl",
            "conmebol.america" to "football-copa",
        )

        val allTeams = mutableListOf<CompetitorEntity>()
        val allEvents = mutableListOf<SportEventEntity>()

        for ((slug, compId) in footballComps) {
            try {
                val response = espnApi.getScoreboard("soccer", slug)
                for (event in response.events) {
                    val comp = event.competitions.firstOrNull() ?: continue
                    comp.competitors.forEach { competitor ->
                        competitor.toFootballTeamEntity()?.let { allTeams += it }
                    }
                    event.toFootballMatchEntity(compId)?.let { allEvents += it }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to fetch football $slug: ${e.message}")
            }
        }

        val distinctTeams = allTeams.distinctBy { it.id }
        if (distinctTeams.isNotEmpty()) dao.insertCompetitors(distinctTeams)
        if (allEvents.isNotEmpty()) dao.insertEvents(allEvents)
    }

    private suspend fun refreshNbaGamesEspn() {
        val today = Clock.System.now().toLocalDateTime(TimeZone.UTC).date

        // Fetch past 3 days + next 7 days
        val datesToFetch = (-3..7).map { offset ->
            today.plus(offset.toLong(), DateTimeUnit.DAY).toString().replace("-", "")
        }

        val allTeams = mutableListOf<CompetitorEntity>()
        val allEvents = mutableListOf<SportEventEntity>()

        for (dateStr in datesToFetch) {
            try {
                val response = espnApi.getScoreboard("basketball", "nba", dates = dateStr)
                for (event in response.events) {
                    val comp = event.competitions.firstOrNull() ?: continue
                    // Insert teams
                    comp.competitors.forEach { competitor ->
                        competitor.toNbaTeamEntity()?.let { allTeams += it }
                    }
                    // Create game entity
                    event.toNbaGameEntity("basketball-nba")?.let { allEvents += it }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to fetch NBA for date $dateStr: ${e.message}")
            }
        }

        val distinctTeams = allTeams.distinctBy { it.id }
        if (distinctTeams.isNotEmpty()) dao.insertCompetitors(distinctTeams)
        if (allEvents.isNotEmpty()) dao.insertEvents(allEvents)
    }

    private suspend fun refreshF1Schedule() {
        // 1. Get season schedule
        val scheduleResponse = jolpicaApi.getCurrentSeasonRaces()
        val scheduleRaces = scheduleResponse.mrData?.raceTable?.races ?: return
        val season = scheduleRaces.firstOrNull()?.season ?: return

        // Insert venues from schedule
        val venues = scheduleRaces.mapNotNull { it.toVenueEntity() }
        if (venues.isNotEmpty()) dao.insertVenues(venues)

        // 2. Determine which races are completed (past date)
        val now = Clock.System.now()
        val allDrivers = mutableListOf<CompetitorEntity>()
        val allParticipants = mutableListOf<EventParticipantEntity>()
        val allEvents = mutableListOf<SportEventEntity>()

        for (scheduleRace in scheduleRaces) {
            val round = scheduleRace.round ?: continue
            val raceDate = try {
                val dateStr = scheduleRace.date ?: continue
                val timeStr = scheduleRace.time ?: "12:00:00Z"
                kotlin.time.Instant.parse("${dateStr}T$timeStr")
            } catch (_: Exception) { continue }

            if (raceDate < now) {
                // Completed race: fetch results + qualifying per round
                try {
                    val resultsResponse = jolpicaApi.getRaceResults(season, round)
                    val resultRace = resultsResponse.mrData?.raceTable?.races?.firstOrNull()

                    if (resultRace != null) {
                        // Also fetch qualifying
                        var mergedRace = resultRace
                        try {
                            val qualResponse = jolpicaApi.getQualifyingResults(season, round)
                            val qualRace = qualResponse.mrData?.raceTable?.races?.firstOrNull()
                            if (qualRace != null) {
                                mergedRace = resultRace.copy(qualifyingResults = qualRace.qualifyingResults)
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to fetch qualifying for R$round: ${e.message}")
                        }

                        // Use schedule race circuit info if result race doesn't have it
                        if (mergedRace.circuit == null && scheduleRace.circuit != null) {
                            mergedRace = mergedRace.copy(circuit = scheduleRace.circuit)
                        }

                        val eventId = "f1-$season-$round"
                        allDrivers += mergedRace.results.mapNotNull { it.driver?.toCompetitorEntity() }
                        allDrivers += mergedRace.qualifyingResults.mapNotNull { it.driver?.toCompetitorEntity() }
                        allParticipants += mergedRace.results.mapNotNull { it.toParticipantEntity(eventId) }
                        allParticipants += mergedRace.qualifyingResults.mapNotNull { it.toParticipantEntity(eventId) }
                        mergedRace.toSportEventEntity()?.let { allEvents += it }
                    } else {
                        // No results yet (race just happened, API not updated)
                        scheduleRace.toSportEventEntity()?.let { allEvents += it }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to fetch results for R$round: ${e.message}")
                    scheduleRace.toSportEventEntity()?.let { allEvents += it }
                }
            } else {
                // Upcoming race: use schedule data
                scheduleRace.toSportEventEntity()?.let { allEvents += it }
            }
        }

        // Insert all: drivers before participants (FK), then events
        val distinctDrivers = allDrivers.distinctBy { it.id }
        if (distinctDrivers.isNotEmpty()) dao.insertCompetitors(distinctDrivers)
        if (allEvents.isNotEmpty()) dao.insertEvents(allEvents)
        if (allParticipants.isNotEmpty()) dao.insertParticipants(allParticipants)
    }

    private suspend fun refreshMmaEvents() {
        // Get calendar for past 3 + next 5 events
        val currentResponse = espnApi.getScoreboard("mma", "ufc")
        val calendar = currentResponse.leagues.flatMap { it.getCalendarEntries() }

        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val nowIso = now.toString()
        val pastEntries = calendar.filter { (it.startDate ?: "") < nowIso }.takeLast(3)
        val futureEntries = calendar.filter { (it.startDate ?: "") >= nowIso }.take(5)
        val entriesToFetch = pastEntries + futureEntries

        // Fetch each event by date (startDate - 1 day for ESPN's local-time query)
        for (entry in entriesToFetch) {
            val startDate = entry.startDate ?: continue
            try {
                // Parse UTC date and subtract 1 day for ESPN query date
                val datePart = startDate.take(10) // "2026-03-08"
                val localDate = kotlinx.datetime.LocalDate.parse(datePart)
                val queryDate = localDate.minus(1, DateTimeUnit.DAY)
                val queryStr = queryDate.toString().replace("-", "")

                val response = espnApi.getScoreboard("mma", "ufc", dates = queryStr)
                for (event in response.events) {
                    val fights = event.toMmaFightEntities("mma-ufc")
                    if (fights.isEmpty()) continue

                    // Insert fighters BEFORE fight events (FK dependency)
                    val fighters = fights.flatMap { listOf(it.fighter1, it.fighter2) }.distinctBy { it.id }
                    dao.insertCompetitors(fighters)
                    dao.insertEvents(fights.map { it.event })
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to fetch MMA event for ${entry.label}: ${e.message}")
            }
        }

        // Also process current response (may contain live/just-completed event)
        for (event in currentResponse.events) {
            val fights = event.toMmaFightEntities("mma-ufc")
            if (fights.isEmpty()) continue
            val fighters = fights.flatMap { listOf(it.fighter1, it.fighter2) }.distinctBy { it.id }
            dao.insertCompetitors(fighters)
            dao.insertEvents(fights.map { it.event })
        }
    }

    private suspend fun refreshTennisEvents() {
        // Get current scoreboard to find active/recent tournaments
        val currentResponse = espnApi.getScoreboard("tennis", "atp")

        // Process current events
        for (event in currentResponse.events) {
            insertTennisMatches(event, "tennis-atp")
        }

        // Tennis calendar is day-based strings — find tournament dates by checking
        // a range of upcoming dates for events with match data
        val today = Clock.System.now().toLocalDateTime(TimeZone.UTC).date

        // Check past 14 days for recent results and next 30 days for upcoming
        val datesToCheck = (-14..30 step 7).map { offset ->
            today.plus(offset.toLong(), DateTimeUnit.DAY).toString().replace("-", "")
        }

        for (dateStr in datesToCheck) {
            try {
                val response = espnApi.getScoreboard("tennis", "atp", dates = dateStr)
                for (event in response.events) {
                    insertTennisMatches(event, "tennis-atp")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to fetch tennis for date $dateStr: ${e.message}")
            }
        }
    }

    private suspend fun insertTennisMatches(event: EspnEvent, competitionId: String) {
        val matches = event.toTennisMatchEntities(competitionId)
        if (matches.isEmpty()) return
        val players = matches.flatMap { listOf(it.player1, it.player2) }.distinctBy { it.id }
        dao.insertCompetitors(players)
        dao.insertEvents(matches.map { it.event })
    }

    private suspend fun refreshEspnScoreboard(sport: String, league: String, competitionId: String) {
        val response = espnApi.getScoreboard(sport, league)
        val sportId = when (sport) {
            "mma" -> "mma"
            "basketball" -> "basketball"
            "tennis" -> "tennis"
            "racing" -> "f1"
            else -> sport
        }
        // Insert competitors BEFORE events (FK dependency)
        val competitors = response.events.flatMap { event ->
            event.competitions.flatMap { comp ->
                comp.competitors.mapNotNull { it.toCompetitorEntity(sportId) }
            }
        }.distinctBy { it.id }
        if (competitors.isNotEmpty()) dao.insertCompetitors(competitors)
        val events = response.events.mapNotNull { it.toSportEventEntity(sportId, competitionId) }
        if (events.isNotEmpty()) dao.insertEvents(events)
    }

    override suspend fun getStandings(competitionCode: String): List<StandingRow> {
        return try {
            val response = footballDataApi.getStandings(competitionCode)
            val table = response.standings.firstOrNull { it.type == "TOTAL" }?.table ?: emptyList()
            table.map { row ->
                StandingRow(
                    position = row.position ?: 0,
                    teamName = row.team?.name ?: "Unknown",
                    teamLogoUrl = row.team?.crest,
                    played = row.playedGames ?: 0,
                    won = row.won ?: 0,
                    draw = row.draw ?: 0,
                    lost = row.lost ?: 0,
                    goalsFor = row.goalsFor ?: 0,
                    goalsAgainst = row.goalsAgainst ?: 0,
                    goalDifference = row.goalDifference ?: 0,
                    points = row.points ?: 0,
                    form = row.form,
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
