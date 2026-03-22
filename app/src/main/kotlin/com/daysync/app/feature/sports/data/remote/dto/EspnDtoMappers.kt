package com.daysync.app.feature.sports.data.remote.dto

import com.daysync.app.core.database.entity.CompetitorEntity
import com.daysync.app.core.database.entity.SportEventEntity
import com.daysync.app.core.sync.SyncStatus
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

fun mapEspnStatus(state: String?, completed: Boolean?): String = when {
    completed == true -> "COMPLETED"
    state == "in" -> "LIVE"
    state == "post" -> "COMPLETED"
    state == "pre" -> "SCHEDULED"
    else -> "SCHEDULED"
}

/**
 * Normalize ESPN date strings that may omit seconds (e.g. "2026-03-21T17:00Z").
 * kotlin.time.Instant.parse requires seconds.
 */
fun normalizeEspnDate(date: String?): Instant? {
    val normalized = date?.let {
        if (it.matches(Regex("""\d{4}-\d{2}-\d{2}T\d{2}:\d{2}Z"""))) {
            it.replace("Z", ":00Z")
        } else {
            it
        }
    } ?: return null
    return try { Instant.parse(normalized) } catch (_: Exception) { null }
}

// ── Team sports (football, basketball) ──────────────────────────

fun EspnEvent.toSportEventEntity(
    sportId: String,
    competitionId: String,
): SportEventEntity? {
    val scheduledInstant = normalizeEspnDate(date) ?: return null

    val statusStr = mapEspnStatus(
        status?.type?.state,
        status?.type?.completed,
    )

    // For team sports: extract home/away from first competition
    // For individual sports (MMA/Tennis): competitions may be empty or use athlete instead of team
    val comp = competitions.firstOrNull()
    val home = comp?.competitors?.firstOrNull { it.homeAway == "home" }
    val away = comp?.competitors?.firstOrNull { it.homeAway == "away" }

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

// ── MMA (individual sport, fight-per-event model) ───────────────

/**
 * Maps each fight on a UFC card to a separate SportEventEntity.
 * Returns list of (event, fighter1, fighter2) triples.
 */
fun EspnEvent.toMmaFightEntities(
    competitionId: String,
): List<MmaFightData> {
    val cardName = name ?: "UFC Event"
    val totalFights = competitions.size

    return competitions.mapIndexedNotNull { index, fight ->
        val fightId = fight.id ?: return@mapIndexedNotNull null
        val fighter1 = fight.competitors.getOrNull(0) ?: return@mapIndexedNotNull null
        val fighter2 = fight.competitors.getOrNull(1) ?: return@mapIndexedNotNull null

        val fighter1Name = fighter1.athlete?.displayName ?: "TBD"
        val fighter2Name = fighter2.athlete?.displayName ?: "TBD"

        // Use fight-level date/status if available, fall back to event-level
        val fightTime = normalizeEspnDate(fight.date) ?: normalizeEspnDate(date) ?: return@mapIndexedNotNull null
        val fightStatus = fight.status?.type?.let { mapEspnStatus(it.state, it.completed) }
            ?: mapEspnStatus(status?.type?.state, status?.type?.completed)

        val weightClass = fight.type?.abbreviation
        val scheduledRounds = fight.format?.regulation?.periods ?: 3
        val isMainEvent = index == totalFights - 1 // Last fight is the main event
        val isChampionship = scheduledRounds == 5

        // Extract result method from details
        val method = fight.details.firstOrNull { det ->
            det.type?.text?.contains("Winner") == true
        }?.type?.text?.removePrefix("Unofficial Winner ")

        val endedRound = fight.status?.period
        val endedTime = fight.status?.displayClock

        // Build resultDetail JSON
        val resultJson = buildJsonObject {
            put("type", "mma")
            put("card_name", cardName)
            weightClass?.let { put("weight_class", it) }
            put("scheduled_rounds", scheduledRounds)
            put("is_main_event", isMainEvent)
            put("is_championship", isChampionship)
            put("fight_order", index)
            fighter1.records.firstOrNull()?.summary?.let { put("fighter1_record", it) }
            fighter2.records.firstOrNull()?.summary?.let { put("fighter2_record", it) }
            if (fightStatus == "COMPLETED") {
                method?.let { put("method", it) }
                endedRound?.let { put("ended_round", it) }
                endedTime?.let { put("ended_time", it) }
            }
            if (fightStatus == "LIVE") {
                endedRound?.let { put("current_round", it) }
                endedTime?.let { put("round_time", it) }
            }
        }.toString()

        val fighter1Entity = CompetitorEntity(
            id = "espn-fighter-${fighter1.id}",
            sportId = "mma",
            name = fighter1Name,
            shortName = fighter1.athlete?.shortName,
            country = fighter1.athlete?.flag?.alt,
            logoUrl = fighter1.athlete?.flag?.href,
            isIndividual = true,
            espnId = fighter1.id,
        )

        val fighter2Entity = CompetitorEntity(
            id = "espn-fighter-${fighter2.id}",
            sportId = "mma",
            name = fighter2Name,
            shortName = fighter2.athlete?.shortName,
            country = fighter2.athlete?.flag?.alt,
            logoUrl = fighter2.athlete?.flag?.href,
            isIndividual = true,
            espnId = fighter2.id,
        )

        val eventEntity = SportEventEntity(
            id = "espn-fight-$fightId",
            sportId = "mma",
            competitionId = competitionId,
            scheduledAt = fightTime,
            status = fightStatus,
            homeCompetitorId = fighter1Entity.id,
            awayCompetitorId = fighter2Entity.id,
            eventName = "$fighter1Name vs $fighter2Name",
            round = weightClass,
            season = cardName,
            resultDetail = resultJson,
            lastUpdated = Clock.System.now(),
            dataSource = "espn",
            syncStatus = SyncStatus.SYNCED,
        )

        MmaFightData(
            event = eventEntity,
            fighter1 = fighter1Entity,
            fighter2 = fighter2Entity,
        )
    }
}

data class MmaFightData(
    val event: SportEventEntity,
    val fighter1: CompetitorEntity,
    val fighter2: CompetitorEntity,
)
