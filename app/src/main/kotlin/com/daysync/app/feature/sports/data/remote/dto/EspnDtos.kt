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
    val clock: Double? = null, // Seconds remaining in current round
    val displayClock: String? = null, // e.g. "1:17", "5:00"
    val period: Int? = null, // Current/ended round number
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
    val date: String? = null, // Individual fight time
    val venue: EspnVenue? = null,
    val competitors: List<EspnCompetitor> = emptyList(),
    val status: EspnEventStatus? = null,
    val type: EspnCompetitionType? = null,
    val format: EspnFormat? = null,
    val details: List<EspnDetail> = emptyList(),
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
    val periods: Int? = null, // 5 = championship/main event, 3 = regular
)

@Serializable
data class EspnDetail(
    val type: EspnDetailType? = null,
)

@Serializable
data class EspnDetailType(
    val id: String? = null,
    val text: String? = null, // "Unofficial Winner Decision", "Unofficial Winner Kotko", etc.
)

@Serializable
data class EspnCompetitor(
    val id: String? = null,
    val homeAway: String? = null,
    val winner: Boolean? = null,
    val team: EspnTeam? = null,
    val athlete: EspnAthlete? = null, // For individual sports (MMA, Tennis)
    val score: String? = null,
    val records: List<EspnRecord> = emptyList(),
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
    val href: String? = null, // Country flag image URL
    val alt: String? = null, // Country name
)

@Serializable
data class EspnRecord(
    val name: String? = null,
    val summary: String? = null, // e.g. "20-0-0"
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
    val calendar: List<EspnCalendarEntry> = emptyList(),
)

@Serializable
data class EspnCalendarEntry(
    val label: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
)
