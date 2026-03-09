package com.daysync.app.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
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
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSport(entity: SportEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSports(entities: List<SportEntity>)

    @Query("SELECT * FROM sports")
    fun getAllSports(): Flow<List<SportEntity>>

    // Competitions
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompetition(entity: CompetitionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompetitions(entities: List<CompetitionEntity>)

    @Query("SELECT * FROM competitions WHERE sportId = :sportId")
    fun getCompetitionsBySport(sportId: String): Flow<List<CompetitionEntity>>

    // Competitors
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompetitor(entity: CompetitorEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompetitors(entities: List<CompetitorEntity>)

    @Query("SELECT * FROM competitors WHERE sportId = :sportId")
    fun getCompetitorsBySport(sportId: String): Flow<List<CompetitorEntity>>

    // Venues
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVenue(entity: VenueEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVenues(entities: List<VenueEntity>)

    // Events
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(entity: SportEventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(entities: List<SportEventEntity>)

    @Update
    suspend fun updateEvent(entity: SportEventEntity)

    @Delete
    suspend fun deleteEvent(entity: SportEventEntity)

    @Query("SELECT * FROM sport_events WHERE id = :id")
    suspend fun getEventById(id: String): SportEventEntity?

    @Query("SELECT * FROM sport_events WHERE isDeleted = 0 ORDER BY scheduledAt ASC")
    fun getAllEvents(): Flow<List<SportEventEntity>>

    @Query("SELECT * FROM sport_events WHERE status = 'LIVE' AND isDeleted = 0")
    fun getLiveEvents(): Flow<List<SportEventEntity>>

    @Query("SELECT * FROM sport_events WHERE syncStatus = 'PENDING'")
    suspend fun getPendingSyncEvents(): List<SportEventEntity>

    // Event Participants
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParticipant(entity: EventParticipantEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParticipants(entities: List<EventParticipantEntity>)

    @Query("SELECT * FROM event_participants WHERE eventId = :eventId")
    fun getParticipantsByEvent(eventId: String): Flow<List<EventParticipantEntity>>

    // Watchlist
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWatchlistEntry(entity: WatchlistEntryEntity)

    @Delete
    suspend fun deleteWatchlistEntry(entity: WatchlistEntryEntity)

    @Query("SELECT * FROM watchlist_entries WHERE isDeleted = 0")
    fun getWatchlist(): Flow<List<WatchlistEntryEntity>>

    @Query("SELECT * FROM watchlist_entries WHERE syncStatus = 'PENDING'")
    suspend fun getPendingSyncWatchlist(): List<WatchlistEntryEntity>

    // Followed Competitors
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFollowedCompetitor(entity: FollowedCompetitorEntity)

    @Delete
    suspend fun deleteFollowedCompetitor(entity: FollowedCompetitorEntity)

    @Query("SELECT * FROM followed_competitors WHERE isDeleted = 0")
    fun getFollowedCompetitors(): Flow<List<FollowedCompetitorEntity>>

    // Followed Competitions
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFollowedCompetition(entity: FollowedCompetitionEntity)

    @Delete
    suspend fun deleteFollowedCompetition(entity: FollowedCompetitionEntity)

    @Query("SELECT * FROM followed_competitions WHERE isDeleted = 0")
    fun getFollowedCompetitions(): Flow<List<FollowedCompetitionEntity>>

    @Query("SELECT * FROM sport_events WHERE scheduledAt >= :startMillis AND scheduledAt <= :endMillis AND isDeleted = 0 ORDER BY scheduledAt ASC")
    suspend fun getEventsByDateRange(startMillis: Long, endMillis: Long): List<SportEventEntity>

    @Query("SELECT se.* FROM sport_events se INNER JOIN watchlist_entries we ON se.id = we.eventId WHERE se.isDeleted = 0 ORDER BY se.scheduledAt DESC")
    suspend fun getWatchlistedEventsList(): List<SportEventEntity>
}
