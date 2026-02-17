package com.daysync.app.feature.sports.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Football-Data.org v4 API response DTOs

@Serializable
data class FdMatchesResponse(
    val matches: List<FdMatch> = emptyList(),
    val resultSet: FdResultSet? = null,
)

@Serializable
data class FdResultSet(
    val count: Int? = null,
    val played: Int? = null,
)

@Serializable
data class FdMatch(
    val id: Int,
    val competition: FdCompetition? = null,
    val season: FdSeason? = null,
    val utcDate: String? = null,
    val status: String? = null,
    val matchday: Int? = null,
    val stage: String? = null,
    val group: String? = null,
    val homeTeam: FdTeam? = null,
    val awayTeam: FdTeam? = null,
    val score: FdScore? = null,
    val venue: String? = null,
    val referees: List<FdReferee> = emptyList(),
)

@Serializable
data class FdCompetition(
    val id: Int? = null,
    val name: String? = null,
    val code: String? = null,
    val emblem: String? = null,
)

@Serializable
data class FdSeason(
    val id: Int? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val currentMatchday: Int? = null,
)

@Serializable
data class FdTeam(
    val id: Int? = null,
    val name: String? = null,
    val shortName: String? = null,
    val tla: String? = null,
    val crest: String? = null,
)

@Serializable
data class FdScore(
    val winner: String? = null,
    val duration: String? = null,
    val fullTime: FdScoreDetail? = null,
    val halfTime: FdScoreDetail? = null,
    val extraTime: FdScoreDetail? = null,
    val penalties: FdScoreDetail? = null,
)

@Serializable
data class FdScoreDetail(
    val home: Int? = null,
    val away: Int? = null,
)

@Serializable
data class FdReferee(
    val id: Int? = null,
    val name: String? = null,
    val nationality: String? = null,
)

// Teams endpoint
@Serializable
data class FdTeamsResponse(
    val teams: List<FdTeamDetail> = emptyList(),
)

@Serializable
data class FdTeamDetail(
    val id: Int? = null,
    val name: String? = null,
    val shortName: String? = null,
    val tla: String? = null,
    val crest: String? = null,
    val address: String? = null,
    val website: String? = null,
    val founded: Int? = null,
    val clubColors: String? = null,
    val venue: String? = null,
    @SerialName("area") val area: FdArea? = null,
)

@Serializable
data class FdArea(
    val id: Int? = null,
    val name: String? = null,
    val code: String? = null,
)

// Standings
@Serializable
data class FdStandingsResponse(
    val competition: FdCompetition? = null,
    val season: FdSeason? = null,
    val standings: List<FdStandingGroup> = emptyList(),
)

@Serializable
data class FdStandingGroup(
    val stage: String? = null,
    val type: String? = null,
    val group: String? = null,
    val table: List<FdStandingRow> = emptyList(),
)

@Serializable
data class FdStandingRow(
    val position: Int? = null,
    val team: FdTeam? = null,
    val playedGames: Int? = null,
    val won: Int? = null,
    val draw: Int? = null,
    val lost: Int? = null,
    val points: Int? = null,
    val goalsFor: Int? = null,
    val goalsAgainst: Int? = null,
    val goalDifference: Int? = null,
    val form: String? = null,
)
