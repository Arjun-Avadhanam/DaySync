package com.daysync.app.feature.sports.data.remote.dto

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests against realistic Jolpica F1 JSON to verify end-to-end
 * deserialization + mapping with actual API data structure.
 */
class JolpicaF1LiveDataTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }

    // Real structure: Australian GP R1 results from Jolpica
    private val raceResultsJson = """
    {
      "MRData": {
        "RaceTable": {
          "season": "2026",
          "Races": [{
            "season": "2026",
            "round": "1",
            "raceName": "Australian Grand Prix",
            "Circuit": {
              "circuitId": "albert_park",
              "circuitName": "Albert Park Grand Prix Circuit",
              "Location": {"locality": "Melbourne", "country": "Australia"}
            },
            "date": "2026-03-08",
            "time": "04:00:00Z",
            "Results": [
              {
                "number": "63", "position": "1", "positionText": "1", "points": "25",
                "Driver": {"driverId": "russell", "code": "RUS", "givenName": "George", "familyName": "Russell", "nationality": "British"},
                "Constructor": {"constructorId": "mercedes", "name": "Mercedes"},
                "grid": "1", "laps": "58", "status": "Finished",
                "Time": {"millis": "4986801", "time": "1:23:06.801"},
                "FastestLap": {"rank": "6", "lap": "21", "Time": {"time": "1:22.670"}}
              },
              {
                "number": "12", "position": "2", "positionText": "2", "points": "18",
                "Driver": {"driverId": "antonelli", "code": "ANT", "givenName": "Andrea Kimi", "familyName": "Antonelli", "nationality": "Italian"},
                "Constructor": {"constructorId": "mercedes", "name": "Mercedes"},
                "grid": "2", "laps": "58", "status": "Finished",
                "Time": {"time": "+2.974"},
                "FastestLap": {"rank": "3", "lap": "57", "Time": {"time": "1:22.417"}}
              },
              {
                "number": "1", "position": "6", "positionText": "6", "points": "8",
                "Driver": {"driverId": "max_verstappen", "code": "VER", "givenName": "Max", "familyName": "Verstappen", "nationality": "Dutch"},
                "Constructor": {"constructorId": "red_bull", "name": "Red Bull"},
                "grid": "20", "laps": "58", "status": "Finished",
                "Time": {"time": "+54.617"},
                "FastestLap": {"rank": "1", "lap": "43", "Time": {"time": "1:22.091"}}
              },
              {
                "number": "18", "position": "18", "positionText": "R", "points": "0",
                "Driver": {"driverId": "alonso", "code": "ALO", "givenName": "Fernando", "familyName": "Alonso", "nationality": "Spanish"},
                "Constructor": {"constructorId": "aston_martin", "name": "Aston Martin"},
                "grid": "17", "laps": "21", "status": "Retired"
              }
            ]
          }]
        }
      }
    }
    """.trimIndent()

    private val qualifyingJson = """
    {
      "MRData": {
        "RaceTable": {
          "season": "2026",
          "Races": [{
            "season": "2026",
            "round": "1",
            "raceName": "Australian Grand Prix",
            "QualifyingResults": [
              {
                "number": "63", "position": "1",
                "Driver": {"driverId": "russell", "code": "RUS", "givenName": "George", "familyName": "Russell"},
                "Constructor": {"constructorId": "mercedes", "name": "Mercedes"},
                "Q1": "1:19.507", "Q2": "1:18.934", "Q3": "1:18.518"
              },
              {
                "number": "12", "position": "2",
                "Driver": {"driverId": "antonelli", "code": "ANT", "givenName": "Andrea Kimi", "familyName": "Antonelli"},
                "Constructor": {"constructorId": "mercedes", "name": "Mercedes"},
                "Q1": "1:20.120", "Q2": "1:19.435", "Q3": "1:18.811"
              },
              {
                "number": "1", "position": "20",
                "Driver": {"driverId": "max_verstappen", "code": "VER", "givenName": "Max", "familyName": "Verstappen"},
                "Constructor": {"constructorId": "red_bull", "name": "Red Bull"},
                "Q1": "1:20.800"
              }
            ]
          }]
        }
      }
    }
    """.trimIndent()

    private val scheduleJson = """
    {
      "MRData": {
        "RaceTable": {
          "season": "2026",
          "Races": [
            {
              "season": "2026", "round": "1",
              "raceName": "Australian Grand Prix",
              "Circuit": {"circuitId": "albert_park", "circuitName": "Albert Park Grand Prix Circuit", "Location": {"locality": "Melbourne", "country": "Australia"}},
              "date": "2026-03-08", "time": "04:00:00Z"
            },
            {
              "season": "2026", "round": "3",
              "raceName": "Japanese Grand Prix",
              "Circuit": {"circuitId": "suzuka", "circuitName": "Suzuka International Racing Course", "Location": {"locality": "Suzuka", "country": "Japan"}},
              "date": "2026-03-29", "time": "05:00:00Z"
            }
          ]
        }
      }
    }
    """.trimIndent()

    // ── Deserialization tests ───────────────────────────────

    @Test
    fun `deserialize race results`() {
        val response = json.decodeFromString<JolpicaResponse>(raceResultsJson)
        val races = response.mrData?.raceTable?.races!!
        assertEquals(1, races.size)
        assertEquals(4, races[0].results.size)
        assertEquals("Australian Grand Prix", races[0].raceName)
    }

    @Test
    fun `deserialize qualifying results`() {
        val response = json.decodeFromString<JolpicaResponse>(qualifyingJson)
        val races = response.mrData?.raceTable?.races!!
        assertEquals(3, races[0].qualifyingResults.size)
        assertEquals("1:18.518", races[0].qualifyingResults[0].q3)
    }

    @Test
    fun `deserialize driver with constructor`() {
        val response = json.decodeFromString<JolpicaResponse>(raceResultsJson)
        val result = response.mrData?.raceTable?.races!![0].results[0]
        assertEquals("George", result.driver?.givenName)
        assertEquals("Russell", result.driver?.familyName)
        assertEquals("Mercedes", result.constructor?.name)
    }

    @Test
    fun `deserialize fastest lap data`() {
        val response = json.decodeFromString<JolpicaResponse>(raceResultsJson)
        val verstappen = response.mrData?.raceTable?.races!![0].results[2]
        assertEquals("1", verstappen.fastestLap?.rank)
        assertEquals("43", verstappen.fastestLap?.lap)
        assertEquals("1:22.091", verstappen.fastestLap?.time?.time)
    }

    // ── Race event mapping tests ────────────────────────────

    @Test
    fun `completed race maps to COMPLETED status`() {
        val response = json.decodeFromString<JolpicaResponse>(raceResultsJson)
        val event = response.mrData?.raceTable?.races!![0].toSportEventEntity()!!
        assertEquals("COMPLETED", event.status)
    }

    @Test
    fun `scheduled race maps to SCHEDULED status`() {
        val response = json.decodeFromString<JolpicaResponse>(scheduleJson)
        val event = response.mrData?.raceTable?.races!![1].toSportEventEntity()!!
        assertEquals("SCHEDULED", event.status)
        assertEquals("Japanese Grand Prix", event.eventName)
    }

    @Test
    fun `race event has correct ID format`() {
        val response = json.decodeFromString<JolpicaResponse>(raceResultsJson)
        val event = response.mrData?.raceTable?.races!![0].toSportEventEntity()!!
        assertEquals("f1-2026-1", event.id)
    }

    @Test
    fun `resultDetail contains winner info`() {
        val response = json.decodeFromString<JolpicaResponse>(raceResultsJson)
        val event = response.mrData?.raceTable?.races!![0].toSportEventEntity()!!
        val detail = event.resultDetail!!
        assertTrue(detail.contains("\"winner\":\"George Russell\""))
        assertTrue(detail.contains("\"winner_team\":\"Mercedes\""))
        assertTrue(detail.contains("\"winner_time\":\"1:23:06.801\""))
    }

    @Test
    fun `resultDetail contains fastest lap info`() {
        val response = json.decodeFromString<JolpicaResponse>(raceResultsJson)
        val event = response.mrData?.raceTable?.races!![0].toSportEventEntity()!!
        val detail = event.resultDetail!!
        assertTrue(detail.contains("\"fastest_lap_driver\":\"Max Verstappen\""))
        assertTrue(detail.contains("\"fastest_lap_time\":\"1:22.091\""))
        assertTrue(detail.contains("\"fastest_lap_number\":\"43\""))
    }

    @Test
    fun `resultDetail contains circuit info`() {
        val response = json.decodeFromString<JolpicaResponse>(raceResultsJson)
        val event = response.mrData?.raceTable?.races!![0].toSportEventEntity()!!
        val detail = event.resultDetail!!
        assertTrue(detail.contains("\"circuit\":\"Albert Park Grand Prix Circuit\""))
        assertTrue(detail.contains("\"circuit_city\":\"Melbourne\""))
        assertTrue(detail.contains("\"circuit_country\":\"Australia\""))
    }

    @Test
    fun `resultDetail contains finisher and retirement counts`() {
        val response = json.decodeFromString<JolpicaResponse>(raceResultsJson)
        val event = response.mrData?.raceTable?.races!![0].toSportEventEntity()!!
        val detail = event.resultDetail!!
        assertTrue(detail.contains("\"finishers\":3"))
        assertTrue(detail.contains("\"retirements\":1"))
    }

    @Test
    fun `resultDetail contains total laps`() {
        val response = json.decodeFromString<JolpicaResponse>(raceResultsJson)
        val event = response.mrData?.raceTable?.races!![0].toSportEventEntity()!!
        assertTrue(event.resultDetail!!.contains("\"total_laps\":\"58\""))
    }

    @Test
    fun `scheduled race resultDetail has circuit but no winner`() {
        val response = json.decodeFromString<JolpicaResponse>(scheduleJson)
        val event = response.mrData?.raceTable?.races!![1].toSportEventEntity()!!
        val detail = event.resultDetail!!
        assertTrue(detail.contains("\"circuit\":\"Suzuka International Racing Course\""))
        assertTrue(!detail.contains("winner"))
    }

    // ── Qualifying mapping tests ────────────────────────────

    @Test
    fun `qualifying merged into race resultDetail has pole info`() {
        val raceResponse = json.decodeFromString<JolpicaResponse>(raceResultsJson)
        val qualResponse = json.decodeFromString<JolpicaResponse>(qualifyingJson)
        val race = raceResponse.mrData?.raceTable?.races!![0]
        val qualRace = qualResponse.mrData?.raceTable?.races!![0]
        assertEquals(3, qualRace.qualifyingResults.size)
        val merged = race.copy(qualifyingResults = qualRace.qualifyingResults)
        assertEquals(3, merged.qualifyingResults.size)
        val event = merged.toSportEventEntity()!!
        val detail = event.resultDetail!!
        assertTrue("Expected pole_driver in: $detail", detail.contains("\"pole_driver\":\"George Russell\""))
        assertTrue("Expected pole_team in: $detail", detail.contains("\"pole_team\":\"Mercedes\""))
        assertTrue("Expected pole_time in: $detail", detail.contains("\"pole_time\":\"1:18.518\""))
    }

    // ── Participant mapping tests ───────────────────────────

    @Test
    fun `race result maps to participant with position and time`() {
        val response = json.decodeFromString<JolpicaResponse>(raceResultsJson)
        val result = response.mrData?.raceTable?.races!![0].results[0]
        val participant = result.toParticipantEntity("f1-2026-1")!!
        assertEquals("f1-2026-1-russell", participant.id)
        assertEquals("f1-driver-russell", participant.competitorId)
        assertEquals(1, participant.position)
        assertEquals("1:23:06.801", participant.score)
        assertEquals("Finished", participant.status)
        assertTrue(participant.isWinner)
    }

    @Test
    fun `participant detail contains grid, laps, points, constructor`() {
        val response = json.decodeFromString<JolpicaResponse>(raceResultsJson)
        val verstappen = response.mrData?.raceTable?.races!![0].results[2]
        val participant = verstappen.toParticipantEntity("f1-2026-1")!!
        val detail = participant.detail!!
        assertTrue(detail.contains("\"grid\":\"20\""))
        assertTrue(detail.contains("\"laps\":\"58\""))
        assertTrue(detail.contains("\"points\":\"8\""))
        assertTrue(detail.contains("\"constructor\":\"Red Bull\""))
    }

    @Test
    fun `participant detail contains fastest lap info`() {
        val response = json.decodeFromString<JolpicaResponse>(raceResultsJson)
        val verstappen = response.mrData?.raceTable?.races!![0].results[2]
        val participant = verstappen.toParticipantEntity("f1-2026-1")!!
        val detail = participant.detail!!
        assertTrue(detail.contains("\"fastest_lap_time\":\"1:22.091\""))
        assertTrue(detail.contains("\"fastest_lap_rank\":\"1\""))
        assertTrue(detail.contains("\"fastest_lap_number\":\"43\""))
    }

    @Test
    fun `retired driver has correct status`() {
        val response = json.decodeFromString<JolpicaResponse>(raceResultsJson)
        val alonso = response.mrData?.raceTable?.races!![0].results[3]
        val participant = alonso.toParticipantEntity("f1-2026-1")!!
        assertEquals(18, participant.position)
        assertEquals("Retired", participant.score) // No time, falls back to status
        assertEquals("Retired", participant.status)
        assertEquals(false, participant.isWinner)
    }

    @Test
    fun `qualifying result maps to participant with session marker`() {
        val response = json.decodeFromString<JolpicaResponse>(qualifyingJson)
        val pole = response.mrData?.raceTable?.races!![0].qualifyingResults[0]
        val participant = pole.toParticipantEntity("f1-2026-1")!!
        assertEquals("f1-2026-1-qual-russell", participant.id)
        assertEquals("f1-driver-russell", participant.competitorId)
        assertEquals(1, participant.position)
        assertEquals("1:18.518", participant.score) // Q3 time
        assertEquals("qualifying", participant.status)
        assertTrue(participant.isWinner) // Pole position
    }

    @Test
    fun `qualifying participant detail contains Q1 Q2 Q3 times`() {
        val response = json.decodeFromString<JolpicaResponse>(qualifyingJson)
        val pole = response.mrData?.raceTable?.races!![0].qualifyingResults[0]
        val participant = pole.toParticipantEntity("f1-2026-1")!!
        val detail = participant.detail!!
        assertTrue(detail.contains("\"session\":\"qualifying\""))
        assertTrue(detail.contains("\"q1\":\"1:19.507\""))
        assertTrue(detail.contains("\"q2\":\"1:18.934\""))
        assertTrue(detail.contains("\"q3\":\"1:18.518\""))
        assertTrue(detail.contains("\"constructor\":\"Mercedes\""))
    }

    @Test
    fun `qualifying without Q3 uses Q1 as score`() {
        val response = json.decodeFromString<JolpicaResponse>(qualifyingJson)
        val verstappen = response.mrData?.raceTable?.races!![0].qualifyingResults[2]
        val participant = verstappen.toParticipantEntity("f1-2026-1")!!
        assertEquals("1:20.800", participant.score) // Only Q1 available
        assertEquals(20, participant.position)
    }

    // ── Driver competitor mapping ───────────────────────────

    @Test
    fun `driver maps to competitor entity`() {
        val response = json.decodeFromString<JolpicaResponse>(raceResultsJson)
        val driver = response.mrData?.raceTable?.races!![0].results[0].driver!!
        val entity = driver.toCompetitorEntity()
        assertEquals("f1-driver-russell", entity.id)
        assertEquals("George Russell", entity.name)
        assertEquals("RUS", entity.shortName)
        assertEquals("British", entity.country)
        assertTrue(entity.isIndividual)
    }

    // ── Venue mapping ───────────────────────────────────────

    @Test
    fun `circuit maps to venue entity`() {
        val response = json.decodeFromString<JolpicaResponse>(raceResultsJson)
        val venue = response.mrData?.raceTable?.races!![0].toVenueEntity()!!
        assertEquals("f1-circuit-albert_park", venue.id)
        assertEquals("Albert Park Grand Prix Circuit", venue.name)
        assertEquals("Melbourne", venue.city)
        assertEquals("Australia", venue.country)
    }
}
