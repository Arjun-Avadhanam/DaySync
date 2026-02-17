package com.daysync.app.feature.sports.data.remote.dto

import kotlinx.serialization.Serializable

// ESPN unofficial API response DTOs

@Serializable
data class EspnScoreboardResponse(
    val events: List<EspnEvent> = emptyList(),
    val leagues: List<EspnLeague> = emptyList(),
)

@Serializable
data class EspnEvent(
    val id: String? = null,
    val date: String? = null,
    val name: String? = null,
    val shortName: String? = null,
    val status: EspnEventStatus? = null,
    val competitions: List<EspnCompetition> = emptyList(),
    val season: EspnSeason? = null,
)

@Serializable
data class EspnEventStatus(
    val type: EspnStatusType? = null,
)

@Serializable
data class EspnStatusType(
    val id: String? = null,
    val name: String? = null,
    val state: String? = null, // "pre", "in", "post"
    val completed: Boolean? = null,
    val description: String? = null,
    val detail: String? = null,
    val shortDetail: String? = null,
)

@Serializable
data class EspnCompetition(
    val id: String? = null,
    val venue: EspnVenue? = null,
    val competitors: List<EspnCompetitor> = emptyList(),
    val status: EspnEventStatus? = null,
    val type: EspnCompetitionType? = null,
)

@Serializable
data class EspnCompetitionType(
    val abbreviation: String? = null,
)

@Serializable
data class EspnCompetitor(
    val id: String? = null,
    val homeAway: String? = null,
    val winner: Boolean? = null,
    val team: EspnTeam? = null,
    val score: String? = null,
)

@Serializable
data class EspnTeam(
    val id: String? = null,
    val name: String? = null,
    val abbreviation: String? = null,
    val displayName: String? = null,
    val shortDisplayName: String? = null,
    val logo: String? = null,
    val location: String? = null,
)

@Serializable
data class EspnVenue(
    val id: String? = null,
    val fullName: String? = null,
    val address: EspnAddress? = null,
    val capacity: Int? = null,
)

@Serializable
data class EspnAddress(
    val city: String? = null,
    val state: String? = null,
    val country: String? = null,
)

@Serializable
data class EspnSeason(
    val year: Int? = null,
    val type: Int? = null,
    val displayName: String? = null,
)

@Serializable
data class EspnLeague(
    val id: String? = null,
    val name: String? = null,
    val abbreviation: String? = null,
)
