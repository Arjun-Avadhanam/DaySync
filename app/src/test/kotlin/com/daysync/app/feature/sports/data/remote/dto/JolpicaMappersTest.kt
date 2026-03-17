package com.daysync.app.feature.sports.data.remote.dto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JolpicaMappersTest {

    // ── JolpicaRace.toSportEventEntity ──────────────────

    @Test
    fun `toSportEventEntity maps scheduled race`() {
        val race = JolpicaRace(
            season = "2026",
            round = "5",
            raceName = "Monaco Grand Prix",
            date = "2026-05-24",
            time = "13:00:00Z",
            circuit = JolpicaCircuit(
                circuitId = "monaco",
                circuitName = "Circuit de Monaco",
                location = JolpicaLocation(locality = "Monte-Carlo", country = "Monaco"),
            ),
        )
        val entity = race.toSportEventEntity()
        assertNotNull(entity)
        assertEquals("f1-2026-5", entity!!.id)
        assertEquals("f1", entity.sportId)
        assertEquals("f1-championship", entity.competitionId)
        assertEquals("SCHEDULED", entity.status)
        assertEquals("Monaco Grand Prix", entity.eventName)
        assertEquals("Round 5", entity.round)
        assertEquals("2026", entity.season)
        assertNull(entity.resultDetail)
        assertEquals("jolpica", entity.dataSource)
    }

    @Test
    fun `toSportEventEntity maps completed race with results`() {
        val race = JolpicaRace(
            season = "2026",
            round = "3",
            raceName = "Australian Grand Prix",
            date = "2026-03-16",
            time = "05:00:00Z",
            results = listOf(
                JolpicaResult(
                    position = "1",
                    driver = JolpicaDriver(driverId = "verstappen", givenName = "Max", familyName = "Verstappen", code = "VER"),
                    constructor = JolpicaConstructor(name = "Red Bull"),
                    laps = "58",
                    time = JolpicaTime(time = "1:32:45.123"),
                    status = "Finished",
                    fastestLap = JolpicaFastestLap(rank = "1", time = JolpicaTime(time = "1:20.456")),
                ),
                JolpicaResult(
                    position = "2",
                    driver = JolpicaDriver(driverId = "norris", givenName = "Lando", familyName = "Norris", code = "NOR"),
                    constructor = JolpicaConstructor(name = "McLaren"),
                    status = "Finished",
                ),
            ),
        )
        val entity = race.toSportEventEntity()!!
        assertEquals("COMPLETED", entity.status)
        assertNotNull(entity.resultDetail)

        val json = entity.resultDetail!!
        assertTrue(json.contains("\"type\":\"f1\""))
        assertTrue(json.contains("\"winner\":\"Max Verstappen\""))
        assertTrue(json.contains("\"winner_team\":\"Red Bull\""))
        assertTrue(json.contains("\"winner_time\":\"1:32:45.123\""))
        assertTrue(json.contains("\"fastest_lap_driver\":\"Max Verstappen\""))
        assertTrue(json.contains("\"fastest_lap_time\":\"1:20.456\""))
        assertTrue(json.contains("\"total_laps\":\"58\""))
    }

    @Test
    fun `toSportEventEntity returns null when season is null`() {
        val race = JolpicaRace(season = null, round = "1", date = "2026-03-01")
        assertNull(race.toSportEventEntity())
    }

    @Test
    fun `toSportEventEntity returns null when round is null`() {
        val race = JolpicaRace(season = "2026", round = null, date = "2026-03-01")
        assertNull(race.toSportEventEntity())
    }

    @Test
    fun `toSportEventEntity returns null when date is null`() {
        val race = JolpicaRace(season = "2026", round = "1", date = null)
        assertNull(race.toSportEventEntity())
    }

    @Test
    fun `toSportEventEntity defaults time to 12 00 00Z`() {
        val race = JolpicaRace(
            season = "2026", round = "1",
            raceName = "Test GP",
            date = "2026-03-01",
            time = null,
        )
        val entity = race.toSportEventEntity()
        assertNotNull(entity)
    }

    @Test
    fun `toSportEventEntity uses Race N as fallback name`() {
        val race = JolpicaRace(
            season = "2026", round = "7",
            raceName = null,
            date = "2026-05-01",
        )
        assertEquals("Race 7", race.toSportEventEntity()!!.eventName)
    }

    // ── JolpicaRace.toVenueEntity ───────────────────────

    @Test
    fun `toVenueEntity maps circuit`() {
        val race = JolpicaRace(
            season = "2026", round = "1",
            circuit = JolpicaCircuit(
                circuitId = "silverstone",
                circuitName = "Silverstone Circuit",
                location = JolpicaLocation(locality = "Silverstone", country = "UK"),
            ),
        )
        val venue = race.toVenueEntity()
        assertNotNull(venue)
        assertEquals("f1-circuit-silverstone", venue!!.id)
        assertEquals("Silverstone Circuit", venue.name)
        assertEquals("Silverstone", venue.city)
        assertEquals("UK", venue.country)
    }

    @Test
    fun `toVenueEntity returns null when circuit is null`() {
        val race = JolpicaRace(season = "2026", round = "1", circuit = null)
        assertNull(race.toVenueEntity())
    }

    @Test
    fun `toVenueEntity returns null when circuitId is null`() {
        val race = JolpicaRace(
            season = "2026", round = "1",
            circuit = JolpicaCircuit(circuitId = null, circuitName = "Test"),
        )
        assertNull(race.toVenueEntity())
    }

    // ── JolpicaResult.toParticipantEntity ───────────────

    @Test
    fun `toParticipantEntity maps winner`() {
        val result = JolpicaResult(
            position = "1",
            driver = JolpicaDriver(driverId = "verstappen", givenName = "Max", familyName = "Verstappen"),
            time = JolpicaTime(time = "1:32:45.123"),
            status = "Finished",
        )
        val entity = result.toParticipantEntity("event-1")
        assertNotNull(entity)
        assertEquals("event-1-verstappen", entity!!.id)
        assertEquals("event-1", entity.eventId)
        assertEquals("f1-driver-verstappen", entity.competitorId)
        assertEquals(1, entity.position)
        assertEquals("1:32:45.123", entity.score)
        assertEquals("Finished", entity.status)
        assertTrue(entity.isWinner)
    }

    @Test
    fun `toParticipantEntity maps non-winner`() {
        val result = JolpicaResult(
            position = "5",
            driver = JolpicaDriver(driverId = "norris"),
            status = "Finished",
        )
        val entity = result.toParticipantEntity("event-1")!!
        assertEquals(5, entity.position)
        assertEquals(false, entity.isWinner)
    }

    @Test
    fun `toParticipantEntity uses status as score when no time`() {
        val result = JolpicaResult(
            position = "DNF",
            driver = JolpicaDriver(driverId = "test"),
            time = null,
            status = "Collision",
        )
        val entity = result.toParticipantEntity("event-1")!!
        assertEquals("Collision", entity.score)
    }

    @Test
    fun `toParticipantEntity returns null when driver is null`() {
        val result = JolpicaResult(position = "1", driver = null)
        assertNull(result.toParticipantEntity("event-1"))
    }

    @Test
    fun `toParticipantEntity returns null when driverId is null`() {
        val result = JolpicaResult(
            position = "1",
            driver = JolpicaDriver(driverId = null),
        )
        assertNull(result.toParticipantEntity("event-1"))
    }

    // ── JolpicaDriver.toCompetitorEntity ────────────────

    @Test
    fun `toCompetitorEntity maps driver`() {
        val driver = JolpicaDriver(
            driverId = "verstappen",
            givenName = "Max",
            familyName = "Verstappen",
            code = "VER",
            nationality = "Dutch",
        )
        val entity = driver.toCompetitorEntity()
        assertEquals("f1-driver-verstappen", entity.id)
        assertEquals("f1", entity.sportId)
        assertEquals("Max Verstappen", entity.name)
        assertEquals("VER", entity.shortName)
        assertEquals("Dutch", entity.country)
        assertTrue(entity.isIndividual)
    }

    @Test
    fun `toCompetitorEntity handles null names`() {
        val driver = JolpicaDriver(driverId = "test", givenName = null, familyName = null)
        assertEquals("", driver.toCompetitorEntity().name)
    }

    @Test
    fun `toCompetitorEntity trims name`() {
        val driver = JolpicaDriver(driverId = "test", givenName = "Max", familyName = null)
        assertEquals("Max", driver.toCompetitorEntity().name)
    }

    @Test
    fun `toCompetitorEntity handles null driverId`() {
        val driver = JolpicaDriver(driverId = null)
        assertEquals("f1-driver-unknown", driver.toCompetitorEntity().id)
    }
}
