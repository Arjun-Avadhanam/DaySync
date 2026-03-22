package com.daysync.app.feature.sports.data.remote.dto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class EspnMappersTest {

    // ── mapEspnStatus ───────────────────────────────────

    @Test
    fun `completed true overrides state`() {
        assertEquals("COMPLETED", mapEspnStatus("in", true))
    }

    @Test
    fun `state in maps to LIVE`() {
        assertEquals("LIVE", mapEspnStatus("in", false))
    }

    @Test
    fun `state post maps to COMPLETED`() {
        assertEquals("COMPLETED", mapEspnStatus("post", null))
    }

    @Test
    fun `state pre maps to SCHEDULED`() {
        assertEquals("SCHEDULED", mapEspnStatus("pre", null))
    }

    @Test
    fun `null state defaults to SCHEDULED`() {
        assertEquals("SCHEDULED", mapEspnStatus(null, null))
    }

    @Test
    fun `unknown state defaults to SCHEDULED`() {
        assertEquals("SCHEDULED", mapEspnStatus("unknown", null))
    }

    @Test
    fun `state in with null completed maps to LIVE`() {
        assertEquals("LIVE", mapEspnStatus("in", null))
    }

    // ── EspnEvent.toSportEventEntity ────────────────────

    private fun makeEvent(
        id: String? = "401234",
        date: String? = "2026-03-15T20:00:00Z",
        shortName: String? = "ARS vs CHE",
        name: String? = "Arsenal vs Chelsea",
        state: String? = "post",
        completed: Boolean? = true,
        homeTeam: EspnTeam? = EspnTeam(id = "1", displayName = "Arsenal", abbreviation = "ARS"),
        awayTeam: EspnTeam? = EspnTeam(id = "2", displayName = "Chelsea", abbreviation = "CHE"),
        homeScore: String? = "2",
        awayScore: String? = "1",
        seasonYear: Int? = 2026,
    ): EspnEvent {
        val competitors = listOfNotNull(
            homeTeam?.let { EspnCompetitor(team = it, homeAway = "home", score = homeScore) },
            awayTeam?.let { EspnCompetitor(team = it, homeAway = "away", score = awayScore) },
        )
        return EspnEvent(
            id = id,
            date = date,
            shortName = shortName,
            name = name,
            status = EspnEventStatus(type = EspnStatusType(state = state, completed = completed)),
            competitions = listOf(EspnCompetition(competitors = competitors)),
            season = seasonYear?.let { EspnSeason(year = it) },
        )
    }

    @Test
    fun `toSportEventEntity maps completed event`() {
        val entity = makeEvent().toSportEventEntity("football", "epl-comp")
        assertNotNull(entity)
        assertEquals("espn-401234", entity!!.id)
        assertEquals("football", entity.sportId)
        assertEquals("epl-comp", entity.competitionId)
        assertEquals("COMPLETED", entity.status)
        assertEquals("espn-team-1", entity.homeCompetitorId)
        assertEquals("espn-team-2", entity.awayCompetitorId)
        assertEquals(2, entity.homeScore)
        assertEquals(1, entity.awayScore)
        assertEquals("ARS vs CHE", entity.eventName)
        assertEquals("2026", entity.season)
        assertEquals("espn", entity.dataSource)
    }

    @Test
    fun `toSportEventEntity creates entity with empty competitions`() {
        // Individual sports (tennis, MMA) return empty competitions when not live
        val event = EspnEvent(
            id = "1", date = "2026-03-15T20:00:00Z",
            competitions = emptyList(),
        )
        val entity = event.toSportEventEntity("football", "comp")
        assertNotNull(entity)
        assertNull(entity!!.homeCompetitorId)
        assertNull(entity.awayCompetitorId)
    }

    @Test
    fun `toSportEventEntity returns null when id is null`() {
        val entity = makeEvent(id = null).toSportEventEntity("football", "comp")
        assertNull(entity)
    }

    @Test
    fun `toSportEventEntity returns null when date is null`() {
        val entity = makeEvent(date = null).toSportEventEntity("football", "comp")
        assertNull(entity)
    }

    @Test
    fun `toSportEventEntity returns null with invalid date`() {
        val entity = makeEvent(date = "bad-date").toSportEventEntity("football", "comp")
        assertNull(entity)
    }

    @Test
    fun `toSportEventEntity falls back to name when shortName is null`() {
        val entity = makeEvent(shortName = null).toSportEventEntity("football", "comp")
        assertEquals("Arsenal vs Chelsea", entity!!.eventName)
    }

    @Test
    fun `toSportEventEntity parses score strings to int`() {
        val entity = makeEvent(homeScore = "103", awayScore = "97").toSportEventEntity("basketball", "nba")
        assertEquals(103, entity!!.homeScore)
        assertEquals(97, entity.awayScore)
    }

    @Test
    fun `toSportEventEntity handles non-numeric score`() {
        val entity = makeEvent(homeScore = "TBD", awayScore = null).toSportEventEntity("football", "comp")
        assertNull(entity!!.homeScore)
        assertNull(entity.awayScore)
    }

    // ── EspnCompetitor.toCompetitorEntity ───────────────

    @Test
    fun `toCompetitorEntity maps team`() {
        val comp = EspnCompetitor(
            team = EspnTeam(id = "42", displayName = "Arsenal", abbreviation = "ARS", logo = "https://logo.png"),
        )
        val entity = comp.toCompetitorEntity("football")
        assertNotNull(entity)
        assertEquals("espn-team-42", entity!!.id)
        assertEquals("football", entity.sportId)
        assertEquals("Arsenal", entity.name)
        assertEquals("ARS", entity.shortName)
        assertEquals("https://logo.png", entity.logoUrl)
        assertEquals("42", entity.espnId)
    }

    @Test
    fun `toCompetitorEntity returns null when team is null`() {
        val comp = EspnCompetitor(team = null)
        assertNull(comp.toCompetitorEntity("football"))
    }

    @Test
    fun `toCompetitorEntity returns null when team id is null`() {
        val comp = EspnCompetitor(team = EspnTeam(id = null, name = "Test"))
        assertNull(comp.toCompetitorEntity("football"))
    }

    @Test
    fun `toCompetitorEntity prefers displayName over name`() {
        val comp = EspnCompetitor(
            team = EspnTeam(id = "1", displayName = "Display Name", name = "Short Name"),
        )
        assertEquals("Display Name", comp.toCompetitorEntity("football")!!.name)
    }

    @Test
    fun `toCompetitorEntity falls back to name when displayName null`() {
        val comp = EspnCompetitor(
            team = EspnTeam(id = "1", displayName = null, name = "Fallback Name"),
        )
        assertEquals("Fallback Name", comp.toCompetitorEntity("football")!!.name)
    }
}
