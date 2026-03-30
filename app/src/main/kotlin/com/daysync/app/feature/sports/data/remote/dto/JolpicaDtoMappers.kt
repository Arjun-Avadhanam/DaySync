package com.daysync.app.feature.sports.data.remote.dto

import com.daysync.app.core.database.entity.CompetitorEntity
import com.daysync.app.core.database.entity.EventParticipantEntity
import com.daysync.app.core.database.entity.SportEventEntity
import com.daysync.app.core.database.entity.VenueEntity
import com.daysync.app.core.sync.SyncStatus
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

fun JolpicaRace.toSportEventEntity(): SportEventEntity? {
    val s = season ?: return null
    val r = round ?: return null

    val scheduledInstant = try {
        val dateStr = date ?: return null
        val timeStr = time ?: "12:00:00Z"
        Instant.parse("${dateStr}T$timeStr")
    } catch (_: Exception) {
        return null
    }

    val hasResults = results.isNotEmpty()
    val hasQualifying = qualifyingResults.isNotEmpty()
    val status = if (hasResults) "COMPLETED" else "SCHEDULED"

    val resultJson = buildJsonObject {
        put("type", "f1")
        circuit?.let {
            it.circuitName?.let { name -> put("circuit", name) }
            it.location?.let { loc ->
                loc.locality?.let { city -> put("circuit_city", city) }
                loc.country?.let { country -> put("circuit_country", country) }
            }
        }
        if (hasResults) {
            put("total_laps", results.firstOrNull()?.laps ?: "0")
            val winner = results.firstOrNull()
            winner?.let {
                put("winner", "${it.driver?.givenName} ${it.driver?.familyName}")
                put("winner_team", it.constructor?.name ?: "")
                it.time?.time?.let { t -> put("winner_time", t) }
            }
            results.firstOrNull { it.fastestLap?.rank == "1" }?.let { fl ->
                put("fastest_lap_driver", "${fl.driver?.givenName} ${fl.driver?.familyName}")
                fl.fastestLap?.time?.time?.let { t -> put("fastest_lap_time", t) }
                fl.fastestLap?.lap?.let { l -> put("fastest_lap_number", l) }
            }
            // "Finished" + "Lapped" = completed the race; "Retired" = DNF; "Did not start" = DNS
            put("finishers", results.count { it.status == "Finished" || it.status?.contains("Lap") == true })
            put("retirements", results.count { it.status == "Retired" })
            put("dns", results.count { it.status == "Did not start" })
        }
        if (hasQualifying) {
            val pole = qualifyingResults.firstOrNull()
            pole?.let {
                put("pole_driver", "${it.driver?.givenName} ${it.driver?.familyName}")
                put("pole_team", it.constructor?.name ?: "")
                val poleTime = it.q3 ?: it.q2 ?: it.q1
                poleTime?.let { t -> put("pole_time", t) }
            }
        }
    }.toString()

    return SportEventEntity(
        id = "f1-$s-$r",
        sportId = "f1",
        competitionId = "f1-championship",
        scheduledAt = scheduledInstant,
        status = status,
        eventName = raceName ?: "Race $r",
        round = "Round $r",
        season = s,
        resultDetail = resultJson,
        lastUpdated = Clock.System.now(),
        dataSource = "jolpica",
        syncStatus = SyncStatus.SYNCED,
    )
}

fun JolpicaRace.toVenueEntity(): VenueEntity? {
    val c = circuit ?: return null
    return VenueEntity(
        id = "f1-circuit-${c.circuitId ?: return null}",
        name = c.circuitName ?: "Unknown Circuit",
        city = c.location?.locality,
        country = c.location?.country,
    )
}

fun JolpicaResult.toParticipantEntity(eventId: String): EventParticipantEntity? {
    val d = driver ?: return null
    val detailJson = buildJsonObject {
        grid?.let { put("grid", it) }
        laps?.let { put("laps", it) }
        points?.let { put("points", it) }
        constructor?.name?.let { put("constructor", it) }
        fastestLap?.let { fl ->
            fl.time?.time?.let { put("fastest_lap_time", it) }
            fl.rank?.let { put("fastest_lap_rank", it) }
            fl.lap?.let { put("fastest_lap_number", it) }
        }
    }.toString()

    return EventParticipantEntity(
        id = "$eventId-${d.driverId ?: return null}",
        eventId = eventId,
        competitorId = "f1-driver-${d.driverId}",
        position = position?.toIntOrNull(),
        score = time?.time ?: status,
        status = status,
        isWinner = position == "1",
        detail = detailJson,
    )
}

fun JolpicaQualifyingResult.toParticipantEntity(eventId: String): EventParticipantEntity? {
    val d = driver ?: return null
    val detailJson = buildJsonObject {
        put("session", "qualifying")
        constructor?.name?.let { put("constructor", it) }
        q1?.let { put("q1", it) }
        q2?.let { put("q2", it) }
        q3?.let { put("q3", it) }
    }.toString()

    return EventParticipantEntity(
        id = "$eventId-qual-${d.driverId ?: return null}",
        eventId = eventId,
        competitorId = "f1-driver-${d.driverId}",
        position = position?.toIntOrNull(),
        score = q3 ?: q2 ?: q1,
        status = "qualifying",
        isWinner = position == "1",
        detail = detailJson,
    )
}

fun JolpicaDriver.toCompetitorEntity(): CompetitorEntity {
    return CompetitorEntity(
        id = "f1-driver-${driverId ?: "unknown"}",
        sportId = "f1",
        name = "${givenName ?: ""} ${familyName ?: ""}".trim(),
        shortName = code,
        country = nationality,
        isIndividual = true,
    )
}
