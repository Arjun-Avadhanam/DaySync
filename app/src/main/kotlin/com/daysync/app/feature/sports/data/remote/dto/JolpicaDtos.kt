package com.daysync.app.feature.sports.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Jolpica (Ergast replacement) API response DTOs for F1

@Serializable
data class JolpicaResponse(
    @SerialName("MRData") val mrData: JolpicaMRData? = null,
)

@Serializable
data class JolpicaMRData(
    @SerialName("RaceTable") val raceTable: JolpicaRaceTable? = null,
)

@Serializable
data class JolpicaRaceTable(
    val season: String? = null,
    @SerialName("Races") val races: List<JolpicaRace> = emptyList(),
)

@Serializable
data class JolpicaRace(
    val season: String? = null,
    val round: String? = null,
    val url: String? = null,
    val raceName: String? = null,
    @SerialName("Circuit") val circuit: JolpicaCircuit? = null,
    val date: String? = null,
    val time: String? = null,
    @SerialName("Results") val results: List<JolpicaResult> = emptyList(),
    @SerialName("QualifyingResults") val qualifyingResults: List<JolpicaQualifyingResult> = emptyList(),
)

@Serializable
data class JolpicaQualifyingResult(
    val number: String? = null,
    val position: String? = null,
    @SerialName("Driver") val driver: JolpicaDriver? = null,
    @SerialName("Constructor") val constructor: JolpicaConstructor? = null,
    @SerialName("Q1") val q1: String? = null,
    @SerialName("Q2") val q2: String? = null,
    @SerialName("Q3") val q3: String? = null,
)

@Serializable
data class JolpicaCircuit(
    val circuitId: String? = null,
    val circuitName: String? = null,
    @SerialName("Location") val location: JolpicaLocation? = null,
)

@Serializable
data class JolpicaLocation(
    val lat: String? = null,
    val long: String? = null,
    val locality: String? = null,
    val country: String? = null,
)

@Serializable
data class JolpicaResult(
    val number: String? = null,
    val position: String? = null,
    val positionText: String? = null,
    val points: String? = null,
    @SerialName("Driver") val driver: JolpicaDriver? = null,
    @SerialName("Constructor") val constructor: JolpicaConstructor? = null,
    val grid: String? = null,
    val laps: String? = null,
    val status: String? = null,
    @SerialName("Time") val time: JolpicaTime? = null,
    @SerialName("FastestLap") val fastestLap: JolpicaFastestLap? = null,
)

@Serializable
data class JolpicaDriver(
    val driverId: String? = null,
    val permanentNumber: String? = null,
    val code: String? = null,
    val givenName: String? = null,
    val familyName: String? = null,
    val nationality: String? = null,
)

@Serializable
data class JolpicaConstructor(
    val constructorId: String? = null,
    val name: String? = null,
    val nationality: String? = null,
)

@Serializable
data class JolpicaTime(
    val millis: String? = null,
    val time: String? = null,
)

@Serializable
data class JolpicaFastestLap(
    val rank: String? = null,
    val lap: String? = null,
    @SerialName("Time") val time: JolpicaTime? = null,
)
