package com.daysync.app.feature.sports.data.remote.dto

import com.daysync.app.core.database.entity.CompetitorEntity
import com.daysync.app.core.database.entity.SportEventEntity
import com.daysync.app.core.sync.SyncStatus
import kotlin.time.Clock
import kotlin.time.Instant

fun mapEspnStatus(state: String?, completed: Boolean?): String = when {
    completed == true -> "COMPLETED"
    state == "in" -> "LIVE"
    state == "post" -> "COMPLETED"
    state == "pre" -> "SCHEDULED"
    else -> "SCHEDULED"
}

fun EspnEvent.toSportEventEntity(
    sportId: String,
    competitionId: String,
): SportEventEntity? {
    val comp = competitions.firstOrNull() ?: return null
    val home = comp.competitors.firstOrNull { it.homeAway == "home" }
    val away = comp.competitors.firstOrNull { it.homeAway == "away" }

    val scheduledInstant = date?.let {
        try { Instant.parse(it) } catch (_: Exception) { null }
    } ?: return null

    val statusStr = mapEspnStatus(
        status?.type?.state,
        status?.type?.completed,
    )

    return SportEventEntity(
        id = "espn-${id ?: return null}",
        sportId = sportId,
        competitionId = competitionId,
        scheduledAt = scheduledInstant,
        status = statusStr,
        homeCompetitorId = home?.team?.id?.let { "espn-team-$it" },
        awayCompetitorId = away?.team?.id?.let { "espn-team-$it" },
        homeScore = home?.score?.toIntOrNull(),
        awayScore = away?.score?.toIntOrNull(),
        eventName = shortName ?: name,
        season = season?.year?.toString(),
        lastUpdated = Clock.System.now(),
        dataSource = "espn",
        syncStatus = SyncStatus.SYNCED,
    )
}

fun EspnCompetitor.toCompetitorEntity(sportId: String): CompetitorEntity? {
    val t = team ?: return null
    return CompetitorEntity(
        id = "espn-team-${t.id ?: return null}",
        sportId = sportId,
        name = t.displayName ?: t.name ?: "Unknown",
        shortName = t.abbreviation ?: t.shortDisplayName,
        logoUrl = t.logo,
        espnId = t.id,
    )
}
