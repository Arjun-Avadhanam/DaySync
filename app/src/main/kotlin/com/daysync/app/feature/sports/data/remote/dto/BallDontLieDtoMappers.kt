package com.daysync.app.feature.sports.data.remote.dto

import com.daysync.app.core.database.entity.CompetitorEntity
import com.daysync.app.core.database.entity.SportEventEntity
import com.daysync.app.core.sync.SyncStatus
import kotlin.time.Clock
import kotlin.time.Instant

fun mapBdlStatus(status: String?): String = when {
    status == null -> "SCHEDULED"
    status.contains("Final", ignoreCase = true) -> "COMPLETED"
    status.contains("Qtr", ignoreCase = true) || status.contains("Half", ignoreCase = true) -> "LIVE"
    else -> "SCHEDULED"
}

fun BdlGame.toSportEventEntity(): SportEventEntity? {
    val scheduledInstant = date?.let {
        try {
            // BDL dates are "YYYY-MM-DD" format
            Instant.parse("${it.take(10)}T00:00:00Z")
        } catch (_: Exception) {
            null
        }
    } ?: return null

    return SportEventEntity(
        id = "bdl-$id",
        sportId = "basketball",
        competitionId = "basketball-nba",
        scheduledAt = scheduledInstant,
        status = mapBdlStatus(status),
        homeCompetitorId = homeTeam?.id?.let { "bdl-team-$it" },
        awayCompetitorId = visitorTeam?.id?.let { "bdl-team-$it" },
        homeScore = homeTeamScore,
        awayScore = visitorTeamScore,
        eventName = "${homeTeam?.abbreviation ?: "TBD"} vs ${visitorTeam?.abbreviation ?: "TBD"}",
        season = season?.toString(),
        lastUpdated = Clock.System.now(),
        dataSource = "balldontlie",
        syncStatus = SyncStatus.SYNCED,
    )
}

fun BdlTeam.toCompetitorEntity(): CompetitorEntity? {
    return CompetitorEntity(
        id = "bdl-team-${id ?: return null}",
        sportId = "basketball",
        name = fullName ?: name ?: "Unknown",
        shortName = abbreviation,
        country = "USA",
    )
}
