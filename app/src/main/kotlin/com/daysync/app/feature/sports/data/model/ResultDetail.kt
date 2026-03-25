package com.daysync.app.feature.sports.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private val json = Json { ignoreUnknownKeys = true }

sealed interface ResultDetail {

    data class FootballGoal(
        val minute: String,
        val scorer: String,
        val isHome: Boolean,
        val penalty: Boolean,
        val ownGoal: Boolean,
    )

    data class Football(
        val halftimeHome: Int?,
        val halftimeAway: Int?,
        val fulltimeHome: Int,
        val fulltimeAway: Int,
        val extratimeHome: Int?,
        val extratimeAway: Int?,
        val penaltiesHome: Int?,
        val penaltiesAway: Int?,
        val winner: String?,
        val elapsed: String?, // Changed to String for "45'+2'" format
        val goals: List<FootballGoal>,
        val note: String?,
        val venue: String?,
        val homeForm: String?,
        val awayForm: String?,
        val homeRecord: String?,
        val awayRecord: String?,
        val possessionHome: String?,
        val possessionAway: String?,
    ) : ResultDetail

    data class F1(
        val circuit: String?,
        val circuitCity: String?,
        val circuitCountry: String?,
        val totalLaps: String?,
        val winner: String?,
        val winnerTeam: String?,
        val winnerTime: String?,
        val fastestLapDriver: String?,
        val fastestLapTime: String?,
        val fastestLapNumber: String?,
        val poleDriver: String?,
        val poleTeam: String?,
        val poleTime: String?,
        val finishers: Int?,
        val retirements: Int?,
    ) : ResultDetail

    data class Mma(
        val cardName: String?,
        val weightClass: String?,
        val scheduledRounds: Int?,
        val isMainEvent: Boolean,
        val isChampionship: Boolean,
        val fightOrder: Int?,
        val fighter1Record: String?,
        val fighter2Record: String?,
        val winner: String?,
        val method: String?,
        val endedRound: Int?,
        val endedTime: String?,
        val currentRound: Int?,
        val roundTime: String?,
    ) : ResultDetail

    data class Tennis(
        val tournament: String?,
        val isGrandSlam: Boolean,
        val draw: String?,
        val round: String?,
        val bestOf: Int?,
        val court: String?,
        val player1Rank: Int?,
        val player2Rank: Int?,
        val player1Sets: List<Int>,
        val player2Sets: List<Int>,
        val tiebreaks: List<List<Int>?>,
        val winner: String?,
        val resultNote: String?,
        val currentSet: Int?,
    ) : ResultDetail

    data class Basketball(
        val homeQuarters: List<Int>,
        val awayQuarters: List<Int>,
        val homeRecord: String?,
        val awayRecord: String?,
        val isPostseason: Boolean,
        val playoffLabel: String?,
        val seriesSummary: String?,
        val seriesTotalGames: Int?,
        val currentPeriod: Int?,
        val gameClock: String?,
        val venue: String?,
    ) : ResultDetail

    data class Unknown(val raw: String) : ResultDetail

    companion object {
        fun parse(jsonString: String?, sportId: String): ResultDetail? {
            if (jsonString.isNullOrBlank()) return null
            return try {
                val obj = json.decodeFromString<JsonObject>(jsonString)
                when (val type = obj["type"]?.jsonPrimitive?.content ?: sportId) {
                    "football" -> Football(
                        halftimeHome = obj["halftime_home"]?.jsonPrimitive?.intOrNull,
                        halftimeAway = obj["halftime_away"]?.jsonPrimitive?.intOrNull,
                        fulltimeHome = obj["fulltime_home"]?.jsonPrimitive?.intOrNull ?: 0,
                        fulltimeAway = obj["fulltime_away"]?.jsonPrimitive?.intOrNull ?: 0,
                        extratimeHome = obj["extratime_home"]?.jsonPrimitive?.intOrNull,
                        extratimeAway = obj["extratime_away"]?.jsonPrimitive?.intOrNull,
                        penaltiesHome = obj["penalties_home"]?.jsonPrimitive?.intOrNull,
                        penaltiesAway = obj["penalties_away"]?.jsonPrimitive?.intOrNull,
                        winner = obj["winner"]?.jsonPrimitive?.content,
                        elapsed = obj["elapsed"]?.jsonPrimitive?.content,
                        goals = obj["goals"]?.jsonArray?.map { elem ->
                            val g = elem.jsonObject
                            FootballGoal(
                                minute = g["minute"]?.jsonPrimitive?.content ?: "?",
                                scorer = g["scorer"]?.jsonPrimitive?.content ?: "?",
                                isHome = g["home"]?.jsonPrimitive?.booleanOrNull ?: true,
                                penalty = g["penalty"]?.jsonPrimitive?.booleanOrNull ?: false,
                                ownGoal = g["own_goal"]?.jsonPrimitive?.booleanOrNull ?: false,
                            )
                        } ?: emptyList(),
                        note = obj["note"]?.jsonPrimitive?.content,
                        venue = obj["venue"]?.jsonPrimitive?.content,
                        homeForm = obj["home_form"]?.jsonPrimitive?.content,
                        awayForm = obj["away_form"]?.jsonPrimitive?.content,
                        homeRecord = obj["home_record"]?.jsonPrimitive?.content,
                        awayRecord = obj["away_record"]?.jsonPrimitive?.content,
                        possessionHome = obj["possession_home"]?.jsonPrimitive?.content,
                        possessionAway = obj["possession_away"]?.jsonPrimitive?.content,
                    )
                    "f1" -> F1(
                        circuit = obj["circuit"]?.jsonPrimitive?.content,
                        circuitCity = obj["circuit_city"]?.jsonPrimitive?.content,
                        circuitCountry = obj["circuit_country"]?.jsonPrimitive?.content,
                        totalLaps = obj["total_laps"]?.jsonPrimitive?.content,
                        winner = obj["winner"]?.jsonPrimitive?.content,
                        winnerTeam = obj["winner_team"]?.jsonPrimitive?.content,
                        winnerTime = obj["winner_time"]?.jsonPrimitive?.content,
                        fastestLapDriver = obj["fastest_lap_driver"]?.jsonPrimitive?.content,
                        fastestLapTime = obj["fastest_lap_time"]?.jsonPrimitive?.content,
                        fastestLapNumber = obj["fastest_lap_number"]?.jsonPrimitive?.content,
                        poleDriver = obj["pole_driver"]?.jsonPrimitive?.content,
                        poleTeam = obj["pole_team"]?.jsonPrimitive?.content,
                        poleTime = obj["pole_time"]?.jsonPrimitive?.content,
                        finishers = obj["finishers"]?.jsonPrimitive?.intOrNull,
                        retirements = obj["retirements"]?.jsonPrimitive?.intOrNull,
                    )
                    "tennis" -> Tennis(
                        tournament = obj["tournament"]?.jsonPrimitive?.content,
                        isGrandSlam = obj["is_grand_slam"]?.jsonPrimitive?.content == "true",
                        draw = obj["draw"]?.jsonPrimitive?.content,
                        round = obj["round"]?.jsonPrimitive?.content,
                        bestOf = obj["best_of"]?.jsonPrimitive?.intOrNull,
                        court = obj["court"]?.jsonPrimitive?.content,
                        player1Rank = obj["player1_rank"]?.jsonPrimitive?.intOrNull,
                        player2Rank = obj["player2_rank"]?.jsonPrimitive?.intOrNull,
                        player1Sets = obj["player1_sets"]?.jsonArray?.map { it.jsonPrimitive.int } ?: emptyList(),
                        player2Sets = obj["player2_sets"]?.jsonArray?.map { it.jsonPrimitive.int } ?: emptyList(),
                        tiebreaks = obj["tiebreaks"]?.jsonArray?.map { elem ->
                            if (elem is kotlinx.serialization.json.JsonNull) null
                            else elem.jsonArray.map { it.jsonPrimitive.int }
                        } ?: emptyList(),
                        winner = obj["winner"]?.jsonPrimitive?.content,
                        resultNote = obj["result_note"]?.jsonPrimitive?.content,
                        currentSet = obj["current_set"]?.jsonPrimitive?.intOrNull,
                    )
                    "basketball" -> Basketball(
                        homeQuarters = obj["home_quarters"]?.jsonArray?.map { it.jsonPrimitive.int } ?: emptyList(),
                        awayQuarters = obj["away_quarters"]?.jsonArray?.map { it.jsonPrimitive.int } ?: emptyList(),
                        homeRecord = obj["home_record"]?.jsonPrimitive?.content,
                        awayRecord = obj["away_record"]?.jsonPrimitive?.content,
                        isPostseason = obj["is_postseason"]?.jsonPrimitive?.content == "true",
                        playoffLabel = obj["playoff_label"]?.jsonPrimitive?.content,
                        seriesSummary = obj["series_summary"]?.jsonPrimitive?.content,
                        seriesTotalGames = obj["series_total_games"]?.jsonPrimitive?.intOrNull,
                        currentPeriod = obj["current_period"]?.jsonPrimitive?.intOrNull,
                        gameClock = obj["game_clock"]?.jsonPrimitive?.content,
                        venue = obj["venue"]?.jsonPrimitive?.content,
                    )
                    "mma" -> Mma(
                        cardName = obj["card_name"]?.jsonPrimitive?.content,
                        weightClass = obj["weight_class"]?.jsonPrimitive?.content,
                        scheduledRounds = obj["scheduled_rounds"]?.jsonPrimitive?.intOrNull,
                        isMainEvent = obj["is_main_event"]?.jsonPrimitive?.content == "true",
                        isChampionship = obj["is_championship"]?.jsonPrimitive?.content == "true",
                        fightOrder = obj["fight_order"]?.jsonPrimitive?.intOrNull,
                        fighter1Record = obj["fighter1_record"]?.jsonPrimitive?.content,
                        fighter2Record = obj["fighter2_record"]?.jsonPrimitive?.content,
                        winner = obj["winner"]?.jsonPrimitive?.content,
                        method = obj["method"]?.jsonPrimitive?.content,
                        endedRound = obj["ended_round"]?.jsonPrimitive?.intOrNull,
                        endedTime = obj["ended_time"]?.jsonPrimitive?.content,
                        currentRound = obj["current_round"]?.jsonPrimitive?.intOrNull,
                        roundTime = obj["round_time"]?.jsonPrimitive?.content,
                    )
                    else -> Unknown(jsonString)
                }
            } catch (_: Exception) {
                Unknown(jsonString)
            }
        }
    }
}
