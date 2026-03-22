package com.daysync.app.feature.sports.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

private val json = Json { ignoreUnknownKeys = true }

sealed interface ResultDetail {

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
        val elapsed: Int?,
    ) : ResultDetail

    data class F1(
        val totalLaps: String?,
        val winner: String?,
        val winnerTeam: String?,
        val winnerTime: String?,
        val fastestLapDriver: String?,
        val fastestLapTime: String?,
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
                        elapsed = obj["elapsed"]?.jsonPrimitive?.intOrNull,
                    )
                    "f1" -> F1(
                        totalLaps = obj["total_laps"]?.jsonPrimitive?.content,
                        winner = obj["winner"]?.jsonPrimitive?.content,
                        winnerTeam = obj["winner_team"]?.jsonPrimitive?.content,
                        winnerTime = obj["winner_time"]?.jsonPrimitive?.content,
                        fastestLapDriver = obj["fastest_lap_driver"]?.jsonPrimitive?.content,
                        fastestLapTime = obj["fastest_lap_time"]?.jsonPrimitive?.content,
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
