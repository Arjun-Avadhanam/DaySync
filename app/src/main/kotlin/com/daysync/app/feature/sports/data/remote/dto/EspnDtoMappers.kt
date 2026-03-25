package com.daysync.app.feature.sports.data.remote.dto

import com.daysync.app.core.database.entity.CompetitorEntity
import com.daysync.app.core.database.entity.SportEventEntity
import com.daysync.app.core.sync.SyncStatus
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
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

/**
 * Extract calendar entries from EspnLeague.calendar which can be either:
 * - MMA: List of objects with {label, startDate}
 * - Tennis: List of date strings
 */
fun EspnLeague.getCalendarEntries(): List<EspnCalendarEntry> {
    return calendar.mapNotNull { element ->
        when (element) {
            is JsonObject -> {
                EspnCalendarEntry(
                    label = element["label"]?.jsonPrimitive?.content,
                    startDate = element["startDate"]?.jsonPrimitive?.content,
                )
            }
            else -> {
                // String date like "2026-01-01T08:00Z"
                val dateStr = element.jsonPrimitive.content
                EspnCalendarEntry(startDate = dateStr)
            }
        }
    }
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

// ── Basketball (team sport with quarter scores + playoff series) ──

fun EspnEvent.toNbaGameEntity(
    competitionId: String,
): SportEventEntity? {
    val scheduledInstant = normalizeEspnDate(date) ?: return null
    val comp = competitions.firstOrNull() ?: return null
    val home = comp.competitors.firstOrNull { it.homeAway == "home" }
    val away = comp.competitors.firstOrNull { it.homeAway == "away" }

    val statusStr = mapEspnStatus(
        comp.status?.type?.state ?: status?.type?.state,
        comp.status?.type?.completed ?: status?.type?.completed,
    )

    val homeQuarters = home?.linescores?.map { it.value?.toInt() ?: 0 } ?: emptyList()
    val awayQuarters = away?.linescores?.map { it.value?.toInt() ?: 0 } ?: emptyList()
    val homeRecord = home?.records?.firstOrNull { it.name == "overall" }?.summary
    val awayRecord = away?.records?.firstOrNull { it.name == "overall" }?.summary

    // Playoff info
    val series = comp.series
    val playoffRound = comp.type?.abbreviation // STD, RD16, SEMI, FINAL
    val noteHeadline = comp.notes.firstOrNull()?.let { it.headline ?: it.text } // "West 1st Round - Game 1"

    val isPostseason = playoffRound != null && playoffRound != "STD"

    val resultJson = buildJsonObject {
        put("type", "basketball")
        if (homeQuarters.isNotEmpty()) {
            put("home_quarters", buildJsonArray { homeQuarters.forEach { add(JsonPrimitive(it)) } })
            put("away_quarters", buildJsonArray { awayQuarters.forEach { add(JsonPrimitive(it)) } })
        }
        homeRecord?.let { put("home_record", it) }
        awayRecord?.let { put("away_record", it) }
        put("is_postseason", isPostseason)
        if (isPostseason) {
            noteHeadline?.let { put("playoff_label", it) }
            series?.summary?.let { put("series_summary", it) }
            series?.totalCompetitions?.let { put("series_total_games", it) }
        }
        if (statusStr == "LIVE") {
            comp.status?.period?.let { put("current_period", it) }
            comp.status?.displayClock?.let { put("game_clock", it) }
        }
        // Venue
        comp.venue?.fullName?.let { put("venue", it) }
    }.toString()

    // Determine round label for display
    val roundLabel = when {
        isPostseason && noteHeadline != null -> noteHeadline
        isPostseason -> when (playoffRound) {
            "RD16" -> "1st Round"
            "QTR" -> "Conference Semifinals"
            "SEMI" -> "Conference Finals"
            "FINAL" -> "NBA Finals"
            else -> "Playoffs"
        }
        else -> null
    }

    return SportEventEntity(
        id = "espn-nba-${comp.id ?: id ?: return null}",
        sportId = "basketball",
        competitionId = competitionId,
        scheduledAt = scheduledInstant,
        status = statusStr,
        homeCompetitorId = home?.team?.id?.let { "espn-team-$it" },
        awayCompetitorId = away?.team?.id?.let { "espn-team-$it" },
        homeScore = home?.score?.toIntOrNull(),
        awayScore = away?.score?.toIntOrNull(),
        eventName = shortName ?: name,
        round = roundLabel,
        season = season?.year?.toString(),
        resultDetail = resultJson,
        lastUpdated = Clock.System.now(),
        dataSource = "espn",
        syncStatus = SyncStatus.SYNCED,
    )
}

fun EspnCompetitor.toNbaTeamEntity(): CompetitorEntity? {
    val t = team ?: return null
    return CompetitorEntity(
        id = "espn-team-${t.id ?: return null}",
        sportId = "basketball",
        name = t.displayName ?: t.name ?: "Unknown",
        shortName = t.abbreviation ?: t.shortDisplayName,
        logoUrl = t.logo,
        espnId = t.id,
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

        val fightTime = normalizeEspnDate(fight.date) ?: normalizeEspnDate(date) ?: return@mapIndexedNotNull null
        val fightStatus = fight.status?.type?.let { mapEspnStatus(it.state, it.completed) }
            ?: mapEspnStatus(status?.type?.state, status?.type?.completed)

        val weightClass = fight.type?.abbreviation
        val scheduledRounds = fight.format?.regulation?.periods ?: 3
        val isMainEvent = index == totalFights - 1
        val isChampionship = scheduledRounds == 5

        val method = fight.details.firstOrNull { det ->
            det.type?.text?.contains("Winner") == true
        }?.type?.text?.removePrefix("Unofficial Winner ")

        val winnerName = when {
            fighter1.winner == true -> fighter1Name
            fighter2.winner == true -> fighter2Name
            else -> null
        }

        val endedRound = fight.status?.period
        val endedTime = fight.status?.displayClock

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
                winnerName?.let { put("winner", it) }
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

        MmaFightData(event = eventEntity, fighter1 = fighter1Entity, fighter2 = fighter2Entity)
    }
}

data class MmaFightData(
    val event: SportEventEntity,
    val fighter1: CompetitorEntity,
    val fighter2: CompetitorEntity,
)

// ── Tennis (individual sport, match-per-event model) ─────────────

private val TENNIS_SINGLES_SLUGS = setOf("mens-singles", "womens-singles")
private const val TENNIS_MIN_ROUND_ID = 3 // Round 3 and above

/**
 * Maps tennis matches from an ESPN event into SportEventEntities.
 * Filters to Men's/Women's Singles, Round 3+.
 */
fun EspnEvent.toTennisMatchEntities(
    competitionId: String,
): List<TennisMatchData> {
    val tournamentName = name ?: "Tennis Tournament"
    val isGrandSlam = major == true
    val tournamentVenue = venue?.displayName

    return groupings
        .filter { it.grouping?.slug in TENNIS_SINGLES_SLUGS }
        .flatMap { grouping ->
            val drawName = grouping.grouping?.displayName ?: "Singles"
            grouping.competitions.mapNotNull { match ->
                mapTennisMatch(match, competitionId, tournamentName, isGrandSlam, tournamentVenue, drawName)
            }
        }
}

private fun EspnEvent.mapTennisMatch(
    match: EspnCompetition,
    competitionId: String,
    tournamentName: String,
    isGrandSlam: Boolean,
    tournamentVenue: String?,
    drawName: String,
): TennisMatchData? {
    val matchId = match.id ?: return null
    val roundId = match.round?.id ?: return null
    if (roundId < TENNIS_MIN_ROUND_ID) return null // Skip early rounds

    val player1 = match.competitors.getOrNull(0) ?: return null
    val player2 = match.competitors.getOrNull(1) ?: return null

    val p1Name = player1.athlete?.displayName ?: "TBD"
    val p2Name = player2.athlete?.displayName ?: "TBD"

    // Skip matches where both players are TBD
    if (p1Name == "TBD" && p2Name == "TBD") return null

    val matchTime = normalizeEspnDate(match.date) ?: normalizeEspnDate(date) ?: return null
    val matchStatus = match.status?.type?.let { mapEspnStatus(it.state, it.completed) }
        ?: mapEspnStatus(status?.type?.state, status?.type?.completed)

    val roundName = match.round?.displayName
    val bestOf = match.format?.regulation?.periods ?: if (isGrandSlam) 5 else 3
    val court = match.venue?.court

    val winnerName = when {
        player1.winner == true -> p1Name
        player2.winner == true -> p2Name
        else -> null
    }

    val noteText = match.notes.firstOrNull()?.text

    // Build set scores arrays
    val p1Sets = player1.linescores.map { it.value?.toInt() ?: 0 }
    val p2Sets = player2.linescores.map { it.value?.toInt() ?: 0 }
    val tiebreaks = player1.linescores.zip(player2.linescores).map { (l1, l2) ->
        if (l1.tiebreak != null || l2.tiebreak != null) {
            listOf(l1.tiebreak ?: 0, l2.tiebreak ?: 0)
        } else null
    }

    val resultJson = buildJsonObject {
        put("type", "tennis")
        put("tournament", tournamentName)
        put("is_grand_slam", isGrandSlam)
        put("draw", drawName)
        roundName?.let { put("round", it) }
        put("best_of", bestOf)
        court?.let { put("court", it) }
        tournamentVenue?.let { put("venue", it) }
        player1.curatedRank?.current?.let { put("player1_rank", it) }
        player2.curatedRank?.current?.let { put("player2_rank", it) }
        player1.seed?.let { put("player1_seed", it) }
        player2.seed?.let { put("player2_seed", it) }
        if (p1Sets.isNotEmpty()) {
            put("player1_sets", buildJsonArray { p1Sets.forEach { add(JsonPrimitive(it)) } })
            put("player2_sets", buildJsonArray { p2Sets.forEach { add(JsonPrimitive(it)) } })
            put("tiebreaks", buildJsonArray {
                tiebreaks.forEach { tb ->
                    if (tb != null) {
                        add(buildJsonArray { tb.forEach { add(JsonPrimitive(it)) } })
                    } else {
                        add(JsonNull)
                    }
                }
            })
        }
        if (matchStatus == "COMPLETED") {
            winnerName?.let { put("winner", it) }
            noteText?.let { put("result_note", it) }
        }
        if (matchStatus == "LIVE") {
            match.status?.period?.let { put("current_set", it) }
        }
    }.toString()

    val p1Entity = CompetitorEntity(
        id = "espn-player-${player1.id}",
        sportId = "tennis",
        name = p1Name,
        shortName = player1.athlete?.shortName,
        country = player1.athlete?.flag?.alt,
        logoUrl = player1.athlete?.flag?.href,
        isIndividual = true,
        espnId = player1.id,
    )

    val p2Entity = CompetitorEntity(
        id = "espn-player-${player2.id}",
        sportId = "tennis",
        name = p2Name,
        shortName = player2.athlete?.shortName,
        country = player2.athlete?.flag?.alt,
        logoUrl = player2.athlete?.flag?.href,
        isIndividual = true,
        espnId = player2.id,
    )

    val eventEntity = SportEventEntity(
        id = "espn-match-$matchId",
        sportId = "tennis",
        competitionId = competitionId,
        scheduledAt = matchTime,
        status = matchStatus,
        homeCompetitorId = p1Entity.id,
        awayCompetitorId = p2Entity.id,
        eventName = "$p1Name vs $p2Name",
        round = roundName,
        season = tournamentName,
        resultDetail = resultJson,
        lastUpdated = Clock.System.now(),
        dataSource = "espn",
        syncStatus = SyncStatus.SYNCED,
    )

    return TennisMatchData(event = eventEntity, player1 = p1Entity, player2 = p2Entity)
}

data class TennisMatchData(
    val event: SportEventEntity,
    val player1: CompetitorEntity,
    val player2: CompetitorEntity,
)
