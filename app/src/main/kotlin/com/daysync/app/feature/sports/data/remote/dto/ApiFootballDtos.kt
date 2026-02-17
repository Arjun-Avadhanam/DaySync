package com.daysync.app.feature.sports.data.remote.dto

import kotlinx.serialization.Serializable

// API-Football response DTOs

@Serializable
data class ApifResponse<T>(
    val get: String? = null,
    val parameters: Map<String, String> = emptyMap(),
    val errors: Map<String, String> = emptyMap(),
    val results: Int? = null,
    val response: List<T> = emptyList(),
)

@Serializable
data class ApifFixture(
    val fixture: ApifFixtureInfo? = null,
    val league: ApifLeague? = null,
    val teams: ApifTeams? = null,
    val goals: ApifGoals? = null,
    val score: ApifFullScore? = null,
)

@Serializable
data class ApifFixtureInfo(
    val id: Int? = null,
    val referee: String? = null,
    val timezone: String? = null,
    val date: String? = null,
    val timestamp: Long? = null,
    val venue: ApifVenue? = null,
    val status: ApifStatus? = null,
)

@Serializable
data class ApifStatus(
    val long: String? = null,
    val short: String? = null,
    val elapsed: Int? = null,
)

@Serializable
data class ApifLeague(
    val id: Int? = null,
    val name: String? = null,
    val country: String? = null,
    val logo: String? = null,
    val flag: String? = null,
    val season: Int? = null,
    val round: String? = null,
)

@Serializable
data class ApifTeams(
    val home: ApifTeamInfo? = null,
    val away: ApifTeamInfo? = null,
)

@Serializable
data class ApifTeamInfo(
    val id: Int? = null,
    val name: String? = null,
    val logo: String? = null,
    val winner: Boolean? = null,
)

@Serializable
data class ApifGoals(
    val home: Int? = null,
    val away: Int? = null,
)

@Serializable
data class ApifFullScore(
    val halftime: ApifGoals? = null,
    val fulltime: ApifGoals? = null,
    val extratime: ApifGoals? = null,
    val penalty: ApifGoals? = null,
)

@Serializable
data class ApifVenue(
    val id: Int? = null,
    val name: String? = null,
    val city: String? = null,
)
