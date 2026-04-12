package com.daysync.app.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.daysync.app.core.database.entity.CompetitionEntity
import com.daysync.app.core.database.entity.CompetitorEntity
import com.daysync.app.core.database.entity.EventParticipantEntity
import com.daysync.app.core.database.entity.FollowedCompetitionEntity
import com.daysync.app.core.database.entity.FollowedCompetitorEntity
import com.daysync.app.core.database.entity.SportEntity
import com.daysync.app.core.database.entity.SportEventEntity
import com.daysync.app.core.database.entity.VenueEntity
import com.daysync.app.core.database.entity.WatchlistEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SportEventDao {
    // Sports
    @Upsert
    suspend fun insertSport(entity: SportEntity)

    @Upsert
    suspend fun insertSports(entities: List<SportEntity>)

    @Query("SELECT * FROM sports")
    fun getAllSports(): Flow<List<SportEntity>>

    @Query("SELECT * FROM sports WHERE id = :id")
    suspend fun getSportById(id: String): SportEntity?

    // Competitions
    @Upsert
    suspend fun insertCompetition(entity: CompetitionEntity)

    @Upsert
    suspend fun insertCompetitions(entities: List<CompetitionEntity>)

    @Query("SELECT * FROM competitions WHERE sportId = :sportId")
    fun getCompetitionsBySport(sportId: String): Flow<List<CompetitionEntity>>

    @Query("SELECT * FROM competitions WHERE id = :id")
    suspend fun getCompetitionById(id: String): CompetitionEntity?

    @Query("SELECT * FROM competitions")
    fun getAllCompetitions(): Flow<List<CompetitionEntity>>

    @Query("SELECT * FROM competitions")
    suspend fun getAllCompetitionsList(): List<CompetitionEntity>

    // Competitors
    @Upsert
    suspend fun insertCompetitor(entity: CompetitorEntity)

    @Upsert
    suspend fun insertCompetitors(entities: List<CompetitorEntity>)

    @Query("SELECT * FROM competitors WHERE sportId = :sportId")
    fun getCompetitorsBySport(sportId: String): Flow<List<CompetitorEntity>>

    @Query(
        "SELECT * FROM competitors " +
            "WHERE name LIKE '%' || :query || '%' " +
            "AND (:sportId IS NULL OR sportId = :sportId) " +
            "ORDER BY name COLLATE NOCASE ASC LIMIT 30"
    )
    fun searchCompetitors(query: String, sportId: String?): Flow<List<CompetitorEntity>>

    @Query(
        "SELECT * FROM sport_events " +
            "WHERE (homeCompetitorId = :competitorId OR awayCompetitorId = :competitorId) " +
            "AND isDeleted = 0 ORDER BY scheduledAt DESC"
    )
    fun getEventsByCompetitor(competitorId: String): Flow<List<SportEventEntity>>

    @Query("SELECT * FROM competitors WHERE id = :id")
    suspend fun getCompetitorById(id: String): CompetitorEntity?

    @Query("SELECT * FROM competitors WHERE id IN (:ids)")
    suspend fun getCompetitorsByIds(ids: List<String>): List<CompetitorEntity>

    // Venues
    @Upsert
    suspend fun insertVenue(entity: VenueEntity)

    @Upsert
    suspend fun insertVenues(entities: List<VenueEntity>)

    // Events — use @Upsert (INSERT OR IGNORE + UPDATE) instead of
    // @Insert(REPLACE) to avoid triggering CASCADE deletes on the
    // watchlist_entries foreign key.
    @Upsert
    suspend fun insertEvent(entity: SportEventEntity)

    @Upsert
    suspend fun insertEvents(entities: List<SportEventEntity>)

    @Update
    suspend fun updateEvent(entity: SportEventEntity)

    @Delete
    suspend fun deleteEvent(entity: SportEventEntity)

    @Query("SELECT * FROM sport_events WHERE id = :id")
    suspend fun getEventById(id: String): SportEventEntity?

    @Query("SELECT * FROM sport_events WHERE isDeleted = 0 ORDER BY scheduledAt ASC")
    fun getAllEvents(): Flow<List<SportEventEntity>>

    @Query(
        """SELECT * FROM sport_events
           WHERE status IN ('SCHEDULED', 'LIVE') AND isDeleted = 0
           ORDER BY scheduledAt ASC"""
    )
    fun getUpcomingEvents(): Flow<List<SportEventEntity>>

    @Query(
        """SELECT * FROM sport_events
           WHERE status IN ('SCHEDULED', 'LIVE') AND isDeleted = 0 AND sportId = :sportId
           ORDER BY scheduledAt ASC"""
    )
    fun getUpcomingEventsBySport(sportId: String): Flow<List<SportEventEntity>>

    @Query(
        """SELECT * FROM sport_events
           WHERE status = 'COMPLETED' AND isDeleted = 0
           ORDER BY scheduledAt DESC"""
    )
    fun getRecentResults(): Flow<List<SportEventEntity>>

    @Query(
        """SELECT * FROM sport_events
           WHERE status = 'COMPLETED' AND isDeleted = 0 AND sportId = :sportId
           ORDER BY scheduledAt DESC"""
    )
    fun getRecentResultsBySport(sportId: String): Flow<List<SportEventEntity>>

    @Query("SELECT * FROM sport_events WHERE status = 'LIVE' AND isDeleted = 0")
    fun getLiveEvents(): Flow<List<SportEventEntity>>

    @Query("SELECT * FROM sport_events WHERE status = 'LIVE' AND isDeleted = 0 AND sportId = :sportId")
    fun getLiveEventsBySport(sportId: String): Flow<List<SportEventEntity>>

    @Query(
        """SELECT * FROM sport_events
           WHERE competitionId = :competitionId AND isDeleted = 0
           ORDER BY scheduledAt ASC"""
    )
    fun getEventsByCompetition(competitionId: String): Flow<List<SportEventEntity>>

    @Query(
        """SELECT * FROM sport_events
           WHERE scheduledAt BETWEEN :startMillis AND :endMillis AND isDeleted = 0
           ORDER BY scheduledAt ASC"""
    )
    fun getEventsByDateRange(startMillis: Long, endMillis: Long): Flow<List<SportEventEntity>>

    @Query("SELECT * FROM sport_events WHERE status = :status AND isDeleted = 0 ORDER BY scheduledAt ASC")
    fun getEventsByStatus(status: String): Flow<List<SportEventEntity>>

    @Query("SELECT * FROM sport_events WHERE syncStatus = 'PENDING'")
    suspend fun getPendingSyncEvents(): List<SportEventEntity>

    @Query("UPDATE sport_events SET syncStatus = 'SYNCED' WHERE id IN (:ids)")
    suspend fun markEventsSynced(ids: List<String>)

    // Event Participants
    @Upsert
    suspend fun insertParticipant(entity: EventParticipantEntity)

    @Upsert
    suspend fun insertParticipants(entities: List<EventParticipantEntity>)

    @Query("SELECT * FROM event_participants WHERE eventId = :eventId")
    fun getParticipantsByEvent(eventId: String): Flow<List<EventParticipantEntity>>

    @Query("SELECT * FROM event_participants WHERE eventId = :eventId")
    suspend fun getParticipantsByEventList(eventId: String): List<EventParticipantEntity>

    // Watchlist
    @Upsert
    suspend fun insertWatchlistEntry(entity: WatchlistEntryEntity)

    @Delete
    suspend fun deleteWatchlistEntry(entity: WatchlistEntryEntity)

    @Query("DELETE FROM watchlist_entries WHERE eventId = :eventId")
    suspend fun deleteWatchlistByEventId(eventId: String)

    @Query("SELECT * FROM watchlist_entries WHERE isDeleted = 0")
    fun getWatchlist(): Flow<List<WatchlistEntryEntity>>

    @Query("SELECT eventId FROM watchlist_entries WHERE isDeleted = 0")
    fun getWatchlistedEventIds(): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist_entries WHERE eventId = :eventId AND isDeleted = 0)")
    suspend fun isEventWatchlisted(eventId: String): Boolean

    @Query(
        """SELECT e.* FROM sport_events e
           INNER JOIN watchlist_entries w ON e.id = w.eventId
           WHERE w.isDeleted = 0 AND e.isDeleted = 0
           ORDER BY e.scheduledAt ASC"""
    )
    fun getWatchlistedEvents(): Flow<List<SportEventEntity>>

    @Query("SELECT * FROM watchlist_entries WHERE syncStatus = 'PENDING'")
    suspend fun getPendingSyncWatchlist(): List<WatchlistEntryEntity>

    @Query("UPDATE watchlist_entries SET syncStatus = 'SYNCED' WHERE id IN (:ids)")
    suspend fun markWatchlistSynced(ids: List<String>)

    // Followed Competitors
    @Upsert
    suspend fun insertFollowedCompetitor(entity: FollowedCompetitorEntity)

    @Delete
    suspend fun deleteFollowedCompetitor(entity: FollowedCompetitorEntity)

    @Query("SELECT * FROM followed_competitors WHERE isDeleted = 0")
    fun getFollowedCompetitors(): Flow<List<FollowedCompetitorEntity>>

    @Query("SELECT competitorId FROM followed_competitors WHERE isDeleted = 0")
    suspend fun getFollowedCompetitorIds(): List<String>

    @Query("DELETE FROM followed_competitors WHERE competitorId = :competitorId")
    suspend fun deleteFollowedCompetitorById(competitorId: String)

    @Query("SELECT * FROM followed_competitors WHERE syncStatus = 'PENDING'")
    suspend fun getPendingSyncFollowedCompetitors(): List<FollowedCompetitorEntity>

    @Query("UPDATE followed_competitors SET syncStatus = 'SYNCED' WHERE id IN (:ids)")
    suspend fun markFollowedCompetitorsSynced(ids: List<String>)

    // Followed Competitions
    @Upsert
    suspend fun insertFollowedCompetition(entity: FollowedCompetitionEntity)

    @Delete
    suspend fun deleteFollowedCompetition(entity: FollowedCompetitionEntity)

    @Query("SELECT * FROM followed_competitions WHERE isDeleted = 0")
    fun getFollowedCompetitions(): Flow<List<FollowedCompetitionEntity>>

    @Query("SELECT competitionId FROM followed_competitions WHERE isDeleted = 0")
    suspend fun getFollowedCompetitionIds(): List<String>

    @Query("DELETE FROM followed_competitions WHERE competitionId = :competitionId")
    suspend fun deleteFollowedCompetitionById(competitionId: String)

    @Query("SELECT * FROM sport_events WHERE scheduledAt >= :startMillis AND scheduledAt <= :endMillis AND isDeleted = 0 ORDER BY scheduledAt ASC")
    suspend fun getEventsByDateRangeList(startMillis: Long, endMillis: Long): List<SportEventEntity>

    @Query("SELECT se.* FROM sport_events se INNER JOIN watchlist_entries we ON se.id = we.eventId WHERE se.isDeleted = 0 ORDER BY se.scheduledAt DESC")
    suspend fun getWatchlistedEventsList(): List<SportEventEntity>

    @Query("SELECT * FROM followed_competitions WHERE syncStatus = 'PENDING'")
    suspend fun getPendingSyncFollowedCompetitions(): List<FollowedCompetitionEntity>

    @Query("UPDATE followed_competitions SET syncStatus = 'SYNCED' WHERE id IN (:ids)")
    suspend fun markFollowedCompetitionsSynced(ids: List<String>)

    // Reference data getters (suspend, returns List — for backup sync)
    @Query("SELECT * FROM sports")
    suspend fun getAllSportsList(): List<SportEntity>

    @Query("SELECT * FROM competitors")
    suspend fun getAllCompetitorsList(): List<CompetitorEntity>

    @Query("SELECT * FROM venues")
    suspend fun getAllVenuesList(): List<VenueEntity>

    @Query("SELECT * FROM event_participants")
    suspend fun getAllParticipantsList(): List<EventParticipantEntity>
}
