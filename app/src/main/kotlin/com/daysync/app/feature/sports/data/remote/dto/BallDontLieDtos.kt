package com.daysync.app.feature.sports.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// BallDontLie API response DTOs (NBA)

@Serializable
data class BdlGamesResponse(
    val data: List<BdlGame> = emptyList(),
    val meta: BdlMeta? = null,
)

@Serializable
data class BdlMeta(
    @SerialName("next_cursor") val nextCursor: Int? = null,
    @SerialName("per_page") val perPage: Int? = null,
)

@Serializable
data class BdlGame(
    val id: Int,
    val date: String? = null,
    val season: Int? = null,
    val status: String? = null,
    val period: Int? = null,
    val time: String? = null,
    val postseason: Boolean? = null,
    @SerialName("home_team") val homeTeam: BdlTeam? = null,
    @SerialName("visitor_team") val visitorTeam: BdlTeam? = null,
    @SerialName("home_team_score") val homeTeamScore: Int? = null,
    @SerialName("visitor_team_score") val visitorTeamScore: Int? = null,
)

@Serializable
data class BdlTeam(
    val id: Int? = null,
    val conference: String? = null,
    val division: String? = null,
    val city: String? = null,
    val name: String? = null,
    @SerialName("full_name") val fullName: String? = null,
    val abbreviation: String? = null,
)
