package com.daysync.app.core.sync.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SportDto(
    val id: String,
    val name: String,
    @SerialName("sport_type") val sportType: String,
    val icon: String?,
)

@Serializable
data class CompetitionDto(
    val id: String,
    @SerialName("sport_id") val sportId: String,
    val name: String,
    @SerialName("short_name") val shortName: String?,
    val country: String?,
    @SerialName("logo_url") val logoUrl: String?,
    @SerialName("api_football_id") val apiFootballId: Int?,
    @SerialName("football_data_id") val footballDataId: String?,
    @SerialName("espn_slug") val espnSlug: String?,
)

@Serializable
data class CompetitorDto(
    val id: String,
    @SerialName("sport_id") val sportId: String,
    val name: String,
    @SerialName("short_name") val shortName: String?,
    @SerialName("logo_url") val logoUrl: String?,
    val country: String?,
    @SerialName("is_individual") val isIndividual: Boolean,
    @SerialName("api_football_id") val apiFootballId: Int?,
    @SerialName("football_data_id") val footballDataId: Int?,
    @SerialName("espn_id") val espnId: String?,
)

@Serializable
data class VenueDto(
    val id: String,
    val name: String,
    val city: String?,
    val country: String?,
    val capacity: Int?,
    @SerialName("image_url") val imageUrl: String?,
)

@Serializable
data class SportEventDto(
    val id: String,
    @SerialName("sport_id") val sportId: String,
    @SerialName("competition_id") val competitionId: String,
    @SerialName("venue_id") val venueId: String?,
    @SerialName("scheduled_at") val scheduledAt: Long,
    val status: String,
    @SerialName("home_competitor_id") val homeCompetitorId: String?,
    @SerialName("away_competitor_id") val awayCompetitorId: String?,
    @SerialName("home_score") val homeScore: Int?,
    @SerialName("away_score") val awayScore: Int?,
    @SerialName("event_name") val eventName: String?,
    val round: String?,
    val season: String?,
    @SerialName("result_detail") val resultDetail: String?,
    @SerialName("last_updated") val lastUpdated: Long,
    @SerialName("data_source") val dataSource: String?,
    @SerialName("last_modified") val lastModified: Long,
    @SerialName("is_deleted") val isDeleted: Boolean,
)

@Serializable
data class EventParticipantDto(
    val id: String,
    @SerialName("event_id") val eventId: String,
    @SerialName("competitor_id") val competitorId: String,
    val position: Int?,
    val score: String?,
    val status: String?,
    @SerialName("is_winner") val isWinner: Boolean,
    val detail: String?,
)

@Serializable
data class WatchlistEntryDto(
    val id: String,
    @SerialName("event_id") val eventId: String,
    @SerialName("added_at") val addedAt: Long,
    val notify: Boolean,
    val notes: String? = null,
    val watchnotes: String? = null,
    @SerialName("last_modified") val lastModified: Long,
    @SerialName("is_deleted") val isDeleted: Boolean,
)

@Serializable
data class FollowedCompetitorDto(
    val id: String,
    @SerialName("competitor_id") val competitorId: String,
    @SerialName("added_at") val addedAt: Long,
    @SerialName("last_modified") val lastModified: Long,
    @SerialName("is_deleted") val isDeleted: Boolean,
)

@Serializable
data class FollowedCompetitionDto(
    val id: String,
    @SerialName("competition_id") val competitionId: String,
    @SerialName("added_at") val addedAt: Long,
    @SerialName("last_modified") val lastModified: Long,
    @SerialName("is_deleted") val isDeleted: Boolean,
)
