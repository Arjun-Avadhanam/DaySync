package com.daysync.app.feature.sports.data.remote.dto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class BallDontLieMappersTest {

    // ── mapBdlStatus ────────────────────────────────────

    @Test
    fun `Final maps to COMPLETED`() {
        assertEquals("COMPLETED", mapBdlStatus("Final"))
    }

    @Test
    fun `final lowercase maps to COMPLETED`() {
        assertEquals("COMPLETED", mapBdlStatus("final"))
    }

    @Test
    fun `4th Qtr maps to LIVE`() {
        assertEquals("LIVE", mapBdlStatus("4th Qtr"))
    }

    @Test
    fun `1st Qtr maps to LIVE`() {
        assertEquals("LIVE", mapBdlStatus("1st Qtr"))
    }

    @Test
    fun `Half maps to LIVE`() {
        assertEquals("LIVE", mapBdlStatus("Half"))
    }

    @Test
    fun `halftime maps to LIVE`() {
        assertEquals("LIVE", mapBdlStatus("halftime"))
    }

    @Test
    fun `null maps to SCHEDULED`() {
        assertEquals("SCHEDULED", mapBdlStatus(null))
    }

    @Test
    fun `empty string maps to SCHEDULED`() {
        assertEquals("SCHEDULED", mapBdlStatus(""))
    }

    @Test
    fun `unknown status maps to SCHEDULED`() {
        assertEquals("SCHEDULED", mapBdlStatus("7:00 PM ET"))
    }

    // ── BdlGame.toSportEventEntity ──────────────────────

    @Test
    fun `toSportEventEntity maps NBA game`() {
        val game = BdlGame(
            id = 1234,
            date = "2026-03-15T00:00:00Z",
            season = 2025,
            status = "Final",
            homeTeam = BdlTeam(id = 1, fullName = "Los Angeles Lakers", abbreviation = "LAL"),
            visitorTeam = BdlTeam(id = 2, fullName = "Boston Celtics", abbreviation = "BOS"),
            homeTeamScore = 110,
            visitorTeamScore = 105,
        )
        val entity = game.toSportEventEntity()
        assertNotNull(entity)
        assertEquals("bdl-1234", entity!!.id)
        assertEquals("basketball", entity.sportId)
        assertEquals("basketball-nba", entity.competitionId)
        assertEquals("COMPLETED", entity.status)
        assertEquals("bdl-team-1", entity.homeCompetitorId)
        assertEquals("bdl-team-2", entity.awayCompetitorId)
        assertEquals(110, entity.homeScore)
        assertEquals(105, entity.awayScore)
        assertEquals("LAL vs BOS", entity.eventName)
        assertEquals("2025", entity.season)
        assertEquals("balldontlie", entity.dataSource)
    }

    @Test
    fun `toSportEventEntity returns null when date is null`() {
        val game = BdlGame(id = 1, date = null)
        assertNull(game.toSportEventEntity())
    }

    @Test
    fun `toSportEventEntity handles date-only format`() {
        val game = BdlGame(
            id = 2,
            date = "2026-03-15",
            homeTeam = BdlTeam(id = 1, abbreviation = "LAL"),
            visitorTeam = BdlTeam(id = 2, abbreviation = "BOS"),
        )
        val entity = game.toSportEventEntity()
        assertNotNull(entity)
    }

    @Test
    fun `toSportEventEntity handles TBD teams`() {
        val game = BdlGame(
            id = 3,
            date = "2026-03-15T00:00:00Z",
            homeTeam = null,
            visitorTeam = null,
        )
        val entity = game.toSportEventEntity()!!
        assertEquals("TBD vs TBD", entity.eventName)
        assertNull(entity.homeCompetitorId)
        assertNull(entity.awayCompetitorId)
    }

    // ── BdlTeam.toCompetitorEntity ──────────────────────

    @Test
    fun `toCompetitorEntity maps NBA team`() {
        val team = BdlTeam(
            id = 14,
            fullName = "Los Angeles Lakers",
            name = "Lakers",
            abbreviation = "LAL",
        )
        val entity = team.toCompetitorEntity()
        assertNotNull(entity)
        assertEquals("bdl-team-14", entity!!.id)
        assertEquals("basketball", entity.sportId)
        assertEquals("Los Angeles Lakers", entity.name)
        assertEquals("LAL", entity.shortName)
        assertEquals("USA", entity.country)
    }

    @Test
    fun `toCompetitorEntity prefers fullName over name`() {
        val team = BdlTeam(id = 1, fullName = "Full Name", name = "Short")
        assertEquals("Full Name", team.toCompetitorEntity()!!.name)
    }

    @Test
    fun `toCompetitorEntity falls back to name`() {
        val team = BdlTeam(id = 1, fullName = null, name = "Lakers")
        assertEquals("Lakers", team.toCompetitorEntity()!!.name)
    }

    @Test
    fun `toCompetitorEntity falls back to Unknown`() {
        val team = BdlTeam(id = 1, fullName = null, name = null)
        assertEquals("Unknown", team.toCompetitorEntity()!!.name)
    }

    @Test
    fun `toCompetitorEntity returns null when id is null`() {
        val team = BdlTeam(id = null, name = "Test")
        assertNull(team.toCompetitorEntity())
    }
}
