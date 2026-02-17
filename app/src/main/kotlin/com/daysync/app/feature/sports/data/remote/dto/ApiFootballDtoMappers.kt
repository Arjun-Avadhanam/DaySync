package com.daysync.app.feature.sports.data.remote.dto

import com.daysync.app.core.database.entity.CompetitorEntity
import com.daysync.app.core.database.entity.SportEventEntity
import com.daysync.app.core.database.entity.VenueEntity
import com.daysync.app.core.sync.SyncStatus
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

fun mapApifStatus(short: String?): String = when (short) {
    "1H", "HT", "2H", "ET", "BT", "P", "SUSP", "INT", "LIVE" -> "LIVE"
    "FT", "AET", "PEN" -> "COMPLETED"
    "TBD", "NS" -> "SCHEDULED"
    "PST" -> "POSTPONED"
    "CANC", "ABD", "AWD", "WO" -> "CANCELLED"
    else -> "SCHEDULED"
}

fun ApifFixture.toSportEventEntity(competitionId: String): SportEventEntity? {
    val fixtureId = fixture?.id ?: return null

    val scheduledInstant = fixture.timestamp?.let {
        Instant.fromEpochSeconds(it)
    } ?: fixture.date?.let {
        try { Instant.parse(it) } catch (_: Exception) { null }
    } ?: return null

    val statusStr = mapApifStatus(fixture.status?.short)

    val resultJson = if (goals?.home != null) {
        buildJsonObject {
            put("type", "football")
            score?.halftime?.let { ht ->
                put("halftime_home", ht.home ?: 0)
                put("halftime_away", ht.away ?: 0)
            }
            put("fulltime_home", goals.home)
            put("fulltime_away", goals.away ?: 0)
            score?.extratime?.let { et ->
                if (et.home != null) {
                    put("extratime_home", et.home)
                    put("extratime_away", et.away ?: 0)
                }
            }
            score?.penalty?.let { pen ->
                if (pen.home != null) {
                    put("penalties_home", pen.home)
                    put("penalties_away", pen.away ?: 0)
                }
            }
            if (fixture.status?.short == "LIVE" || fixture.status?.short == "1H" || fixture.status?.short == "2H") {
                fixture.status.elapsed?.let { put("elapsed", it) }
            }
        }.toString()
    } else null

    return SportEventEntity(
        id = "apif-$fixtureId",
        sportId = "football",
        competitionId = competitionId,
        scheduledAt = scheduledInstant,
        status = statusStr,
        homeCompetitorId = teams?.home?.id?.let { "apif-team-$it" },
        awayCompetitorId = teams?.away?.id?.let { "apif-team-$it" },
        homeScore = goals?.home,
        awayScore = goals?.away,
        eventName = "${teams?.home?.name ?: "TBD"} vs ${teams?.away?.name ?: "TBD"}",
        round = league?.round,
        season = league?.season?.toString(),
        resultDetail = resultJson,
        lastUpdated = Clock.System.now(),
        dataSource = "api-football",
        syncStatus = SyncStatus.SYNCED,
    )
}

fun ApifTeamInfo.toCompetitorEntity(): CompetitorEntity? {
    return CompetitorEntity(
        id = "apif-team-${id ?: return null}",
        sportId = "football",
        name = name ?: "Unknown",
        logoUrl = logo,
        apiFootballId = id,
    )
}

fun ApifFixture.toVenueEntity(): VenueEntity? {
    val v = fixture?.venue ?: return null
    val venueId = v.id ?: return null
    return VenueEntity(
        id = "apif-venue-$venueId",
        name = v.name ?: "Unknown",
        city = v.city,
    )
}
