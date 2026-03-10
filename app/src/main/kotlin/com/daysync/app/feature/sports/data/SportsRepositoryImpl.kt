package com.daysync.app.feature.sports.data

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
import com.daysync.app.feature.sports.data.remote.dto.toCompetitorEntity
import com.daysync.app.feature.sports.data.remote.dto.toParticipantEntity
import com.daysync.app.feature.sports.data.remote.dto.toSportEventEntity
import com.daysync.app.feature.sports.data.remote.dto.toVenueEntity
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
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

    override suspend fun ensureSeedData() {
        SeedData.ensureSeedData(dao)
    }

    // Events
    override fun getUpcomingEvents(sportId: String?): Flow<List<SportEventEntity>> {
        return if (sportId != null) dao.getUpcomingEventsBySport(sportId) else dao.getUpcomingEvents()
    }

    override fun getLiveEvents(): Flow<List<SportEventEntity>> = dao.getLiveEvents()

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
        try {
            val response = footballDataApi.getMatches(competitionCode)
            val events = response.matches.map { it.toSportEventEntity(competitionId) }
            if (events.isNotEmpty()) {
                dao.insertEvents(events)
            }
            // Also insert teams as competitors
            val competitors = response.matches.flatMap { match ->
                listOfNotNull(
                    match.homeTeam?.toCompetitorEntity(),
                    match.awayTeam?.toCompetitorEntity(),
                )
            }.distinctBy { it.id }
            if (competitors.isNotEmpty()) {
                dao.insertCompetitors(competitors)
            }
        } catch (e: Exception) {
            // Log but don't crash -- caller handles
            throw e
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
        // Football-Data.org competitions (free tier)
        val fdCompetitions = mapOf(
            "PL" to "football-pl",
            "CL" to "football-cl",
            "SA" to "football-sa",
            "PD" to "football-laliga",
        )
        for ((code, id) in fdCompetitions) {
            try {
                refreshFootballFixtures(code, id)
            } catch (_: Exception) {
                // Continue with other competitions
            }
        }

        // API-Football for gap competitions (Europa, EFL, etc.)
        refreshApiFootballCompetitions()

        // NBA via BallDontLie
        try { refreshNbaGames() } catch (_: Exception) {}

        // F1 via Jolpica
        try { refreshF1Schedule() } catch (_: Exception) {}

        // ESPN for UFC/Tennis
        try { refreshEspnScoreboard("mma", "ufc", "mma-ufc") } catch (_: Exception) {}
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
                val events = response.response.mapNotNull {
                    it.toSportEventEntity(compId)
                }
                if (events.isNotEmpty()) dao.insertEvents(events)
                val competitors = response.response.flatMap { fixture ->
                    listOfNotNull(
                        fixture.teams?.home?.toCompetitorEntity(),
                        fixture.teams?.away?.toCompetitorEntity(),
                    )
                }.distinctBy { it.id }
                if (competitors.isNotEmpty()) dao.insertCompetitors(competitors)
            } catch (_: Exception) {}
        }
    }

    private suspend fun refreshNbaGames() {
        val today = Clock.System.now()
            .toLocalDateTime(TimeZone.UTC).date
        val dates = (-3..7).map { today.plus(it, DateTimeUnit.DAY).toString() }
        val response = ballDontLieApi.getGames(dates = dates, perPage = 100)
        val events = response.data.mapNotNull { it.toSportEventEntity() }
        if (events.isNotEmpty()) dao.insertEvents(events)
        val competitors = response.data.flatMap { game ->
            listOfNotNull(game.homeTeam?.toCompetitorEntity(), game.visitorTeam?.toCompetitorEntity())
        }.distinctBy { it.id }
        if (competitors.isNotEmpty()) dao.insertCompetitors(competitors)
    }

    private suspend fun refreshF1Schedule() {
        val response = jolpicaApi.getCurrentSeasonRaces()
        val races = response.mrData?.raceTable?.races ?: return
        val events = races.mapNotNull { it.toSportEventEntity() }
        if (events.isNotEmpty()) dao.insertEvents(events)
        val venues = races.mapNotNull {
            it.toVenueEntity()
        }
        if (venues.isNotEmpty()) dao.insertVenues(venues)
        // Insert drivers from results if available
        val drivers = races.flatMap { race ->
            race.results.map { it.driver }.filterNotNull().map { it.toCompetitorEntity() }
        }.distinctBy { it.id }
        if (drivers.isNotEmpty()) dao.insertCompetitors(drivers)
        // Insert participants from results
        val participants = races.flatMap { race ->
            val eventId = "f1-${race.season}-${race.round}"
            race.results.mapNotNull { it.toParticipantEntity(eventId) }
        }
        if (participants.isNotEmpty()) dao.insertParticipants(participants)
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
        val events = response.events.mapNotNull { it.toSportEventEntity(sportId, competitionId) }
        if (events.isNotEmpty()) dao.insertEvents(events)
        val competitors = response.events.flatMap { event ->
            event.competitions.flatMap { comp ->
                comp.competitors.mapNotNull { it.toCompetitorEntity(sportId) }
            }
        }.distinctBy { it.id }
        if (competitors.isNotEmpty()) dao.insertCompetitors(competitors)
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
