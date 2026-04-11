package com.daysync.app.feature.sports.data

import com.daysync.app.core.database.entity.CompetitionEntity
import com.daysync.app.core.database.entity.CompetitorEntity
import com.daysync.app.core.database.entity.FollowedCompetitionEntity
import com.daysync.app.core.database.entity.FollowedCompetitorEntity
import com.daysync.app.core.database.entity.SportEntity
import com.daysync.app.core.database.entity.SportEventEntity
import com.daysync.app.core.database.entity.EventParticipantEntity
import com.daysync.app.core.database.entity.WatchlistEntryEntity
import com.daysync.app.feature.sports.data.model.SportEventWithDetails
import kotlinx.coroutines.flow.Flow

interface SportsRepository {

    // Seed data
    suspend fun ensureSeedData()

    // Events
    fun getUpcomingEvents(sportId: String? = null): Flow<List<SportEventEntity>>
    fun getLiveEvents(sportId: String? = null): Flow<List<SportEventEntity>>
    fun getRecentResults(sportId: String? = null): Flow<List<SportEventEntity>>
    fun getWatchlistedEvents(): Flow<List<SportEventEntity>>
    fun getEventsByCompetition(competitionId: String): Flow<List<SportEventEntity>>
    suspend fun getEventById(eventId: String): SportEventEntity?
    suspend fun getParticipantsByEvent(eventId: String): List<EventParticipantEntity>
    suspend fun getCompetitorName(competitorId: String): String?

    // Enrichment
    suspend fun enrichEvent(event: SportEventEntity, watchlistedIds: Set<String>): SportEventWithDetails

    // Reference data
    fun getAllSports(): Flow<List<SportEntity>>
    fun getAllCompetitions(): Flow<List<CompetitionEntity>>
    fun getCompetitionsBySport(sportId: String): Flow<List<CompetitionEntity>>
    suspend fun getCompetitionById(competitionId: String): CompetitionEntity?
    suspend fun getCompetitorById(competitorId: String): CompetitorEntity?

    // Search
    fun searchCompetitors(query: String, sportId: String?): Flow<List<CompetitorEntity>>
    fun getEventsByCompetitor(competitorId: String): Flow<List<SportEventEntity>>

    // Watchlist
    fun getWatchlistedEventIds(): Flow<List<String>>
    suspend fun toggleWatchlist(eventId: String)

    // Following
    fun getFollowedCompetitions(): Flow<List<FollowedCompetitionEntity>>
    fun getFollowedCompetitors(): Flow<List<FollowedCompetitorEntity>>
    suspend fun getFollowedCompetitionIds(): List<String>
    suspend fun getFollowedCompetitorIds(): List<String>
    suspend fun toggleFollowCompetition(competitionId: String)
    suspend fun toggleFollowCompetitor(competitorId: String)

    // Refresh
    suspend fun refreshAllSports()

    // Standings
    suspend fun getStandings(competitionCode: String): List<StandingRow>
}

data class StandingRow(
    val position: Int,
    val teamName: String,
    val teamLogoUrl: String?,
    val played: Int,
    val won: Int,
    val draw: Int,
    val lost: Int,
    val goalsFor: Int,
    val goalsAgainst: Int,
    val goalDifference: Int,
    val points: Int,
    val form: String?,
)
