package com.daysync.app.feature.sports.data.remote.dto

import com.daysync.app.core.database.entity.CompetitorEntity
import com.daysync.app.core.database.entity.SportEventEntity
import com.daysync.app.core.sync.SyncStatus
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

// Football-Data.org status -> internal status
fun mapFdStatus(fdStatus: String?): String = when (fdStatus) {
    "IN_PLAY", "PAUSED" -> "LIVE"
    "FINISHED", "AWARDED" -> "COMPLETED"
    "TIMED", "SCHEDULED" -> "SCHEDULED"
    "POSTPONED" -> "POSTPONED"
    "CANCELLED", "SUSPENDED" -> "CANCELLED"
    else -> "SCHEDULED"
}

fun FdMatch.toSportEventEntity(competitionId: String): SportEventEntity {
    val scheduledInstant = utcDate?.let {
        try {
            Instant.parse(it)
        } catch (_: Exception) {
            Clock.System.now()
        }
    } ?: Clock.System.now()

    val roundStr = when {
        stage != null && stage != "REGULAR_SEASON" -> stage.replace("_", " ")
        matchday != null -> "Matchday $matchday"
        else -> null
    }

    val resultJson = if (score?.fullTime?.home != null) {
        buildJsonObject {
            put("type", "football")
            score.halfTime?.let { ht ->
                put("halftime_home", ht.home ?: 0)
                put("halftime_away", ht.away ?: 0)
            }
            score.fullTime.let { ft ->
                put("fulltime_home", ft.home ?: 0)
                put("fulltime_away", ft.away ?: 0)
            }
            score.extraTime?.let { et ->
                if (et.home != null) {
                    put("extratime_home", et.home)
                    put("extratime_away", et.away ?: 0)
                }
            }
            score.penalties?.let { pen ->
                if (pen.home != null) {
                    put("penalties_home", pen.home)
                    put("penalties_away", pen.away ?: 0)
                }
            }
            score.winner?.let { put("winner", it) }
        }.toString()
    } else null

    return SportEventEntity(
        id = "fd-$id",
        sportId = "football",
        competitionId = competitionId,
        scheduledAt = scheduledInstant,
        status = mapFdStatus(status),
        homeCompetitorId = homeTeam?.id?.let { "fd-team-$it" },
        awayCompetitorId = awayTeam?.id?.let { "fd-team-$it" },
        homeScore = score?.fullTime?.home,
        awayScore = score?.fullTime?.away,
        eventName = "${homeTeam?.shortName ?: homeTeam?.name ?: "TBD"} vs ${awayTeam?.shortName ?: awayTeam?.name ?: "TBD"}",
        round = roundStr,
        season = season?.startDate?.take(4)?.let { "$it-${it.toInt() + 1}" },
        resultDetail = resultJson,
        lastUpdated = Clock.System.now(),
        dataSource = "football-data.org",
        syncStatus = SyncStatus.SYNCED,
    )
}

fun FdTeam.toCompetitorEntity(): CompetitorEntity {
    return CompetitorEntity(
        id = "fd-team-${id ?: 0}",
        sportId = "football",
        name = name ?: "Unknown",
        shortName = shortName ?: tla,
        logoUrl = crest,
        footballDataId = id,
    )
}

fun FdTeamDetail.toCompetitorEntity(): CompetitorEntity {
    return CompetitorEntity(
        id = "fd-team-${id ?: 0}",
        sportId = "football",
        name = name ?: "Unknown",
        shortName = shortName ?: tla,
        logoUrl = crest,
        country = area?.name,
        footballDataId = id,
    )
}
