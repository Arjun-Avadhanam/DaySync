package com.daysync.app.feature.sports.data.remote.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

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
    val endDate: String? = null,
    val name: String? = null,
    val shortName: String? = null,
    val major: Boolean? = null, // Grand Slam indicator for tennis
    val status: EspnEventStatus? = null,
    val competitions: List<EspnCompetition> = emptyList(),
    val groupings: List<EspnGrouping> = emptyList(), // Tennis: matches grouped by draw
    val season: EspnSeason? = null,
    val venue: EspnSimpleVenue? = null, // Tennis tournament venue
)

@Serializable
data class EspnSimpleVenue(
    val displayName: String? = null,
)

@Serializable
data class EspnGrouping(
    val grouping: EspnGroupingType? = null,
    val competitions: List<EspnCompetition> = emptyList(),
)

@Serializable
data class EspnGroupingType(
    val id: String? = null,
    val slug: String? = null, // "mens-singles", "womens-singles"
    val displayName: String? = null,
)

@Serializable
data class EspnEventStatus(
    val clock: Double? = null,
    val displayClock: String? = null,
    val period: Int? = null,
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
    val date: String? = null,
    val venue: EspnVenue? = null,
    val competitors: List<EspnCompetitor> = emptyList(),
    val status: EspnEventStatus? = null,
    val type: EspnCompetitionType? = null,
    val format: EspnFormat? = null,
    val details: List<EspnDetail> = emptyList(),
    val notes: List<EspnNote> = emptyList(),
    val round: EspnRound? = null, // Tennis: round info
    val series: EspnSeries? = null, // Basketball: playoff series info
)

@Serializable
data class EspnSeries(
    val type: String? = null, // "playoff"
    val summary: String? = null, // "OKC leads series 1-0"
    val completed: Boolean? = null,
    val totalCompetitions: Int? = null, // Best of 4, 5, or 7
)

@Serializable
data class EspnRound(
    val id: Int? = null,
    val displayName: String? = null, // "Final", "Semifinal", "Round 3", etc.
)

@Serializable
data class EspnNote(
    val text: String? = null,
    val headline: String? = null, // NBA playoffs use headline, MMA uses text
    val type: String? = null,
)

@Serializable
data class EspnCompetitionType(
    val abbreviation: String? = null,
)

@Serializable
data class EspnFormat(
    val regulation: EspnRegulation? = null,
)

@Serializable
data class EspnRegulation(
    val periods: Int? = null,
)

@Serializable
data class EspnDetail(
    val type: EspnDetailType? = null,
    val clock: EspnClock? = null,
    val team: EspnTeamRef? = null,
    val scoringPlay: Boolean? = null,
    val athletesInvolved: List<EspnAthleteRef> = emptyList(),
    val penaltyKick: Boolean? = null,
    val ownGoal: Boolean? = null,
)

@Serializable
data class EspnClock(
    val value: Double? = null,
    val displayValue: String? = null,
)

@Serializable
data class EspnTeamRef(
    val id: String? = null,
)

@Serializable
data class EspnAthleteRef(
    val displayName: String? = null,
)

@Serializable
data class EspnDetailType(
    val id: String? = null,
    val text: String? = null,
)

@Serializable
data class EspnCompetitor(
    val id: String? = null,
    val homeAway: String? = null,
    val winner: Boolean? = null,
    val team: EspnTeam? = null,
    val athlete: EspnAthlete? = null,
    val score: String? = null,
    val records: List<EspnRecord> = emptyList(),
    val curatedRank: EspnRank? = null,
    val seed: String? = null,
    val linescores: List<EspnLinescore> = emptyList(),
    val form: String? = null, // Football: "WLDWW"
    val statistics: List<EspnStatistic> = emptyList(), // Football: possession, fouls, etc.
)

@Serializable
data class EspnStatistic(
    val name: String? = null,
    val displayValue: String? = null,
)

@Serializable
data class EspnRank(
    val current: Int? = null,
)

@Serializable
data class EspnLinescore(
    val value: Double? = null,
    val tiebreak: Int? = null,
    val winner: Boolean? = null,
)

@Serializable
data class EspnAthlete(
    val fullName: String? = null,
    val displayName: String? = null,
    val shortName: String? = null,
    val flag: EspnFlag? = null,
)

@Serializable
data class EspnFlag(
    val href: String? = null,
    val alt: String? = null,
)

@Serializable
data class EspnRecord(
    val name: String? = null,
    val summary: String? = null,
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
    val court: String? = null, // Tennis court name
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
    // Calendar can be List<EspnCalendarEntry> (MMA) or List<String> (Tennis)
    // Use JsonElement to handle both polymorphic formats
    val calendar: List<JsonElement> = emptyList(),
)

@Serializable
data class EspnCalendarEntry(
    val label: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
)
