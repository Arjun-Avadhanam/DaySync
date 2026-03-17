package com.daysync.app.feature.sports.data.remote.dto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FootballDataMappersTest {

    // ── mapFdStatus ─────────────────────────────────────

    @Test
    fun `IN_PLAY maps to LIVE`() {
        assertEquals("LIVE", mapFdStatus("IN_PLAY"))
    }

    @Test
    fun `PAUSED maps to LIVE`() {
        assertEquals("LIVE", mapFdStatus("PAUSED"))
    }

    @Test
    fun `FINISHED maps to COMPLETED`() {
        assertEquals("COMPLETED", mapFdStatus("FINISHED"))
    }

    @Test
    fun `AWARDED maps to COMPLETED`() {
        assertEquals("COMPLETED", mapFdStatus("AWARDED"))
    }

    @Test
    fun `TIMED maps to SCHEDULED`() {
        assertEquals("SCHEDULED", mapFdStatus("TIMED"))
    }

    @Test
    fun `SCHEDULED maps to SCHEDULED`() {
        assertEquals("SCHEDULED", mapFdStatus("SCHEDULED"))
    }

    @Test
    fun `POSTPONED maps to POSTPONED`() {
        assertEquals("POSTPONED", mapFdStatus("POSTPONED"))
    }

    @Test
    fun `CANCELLED maps to CANCELLED`() {
        assertEquals("CANCELLED", mapFdStatus("CANCELLED"))
    }

    @Test
    fun `SUSPENDED maps to CANCELLED`() {
        assertEquals("CANCELLED", mapFdStatus("SUSPENDED"))
    }

    @Test
    fun `null status defaults to SCHEDULED`() {
        assertEquals("SCHEDULED", mapFdStatus(null))
    }

    @Test
    fun `unknown status defaults to SCHEDULED`() {
        assertEquals("SCHEDULED", mapFdStatus("SOMETHING_ELSE"))
    }

    // ── FdMatch.toSportEventEntity ──────────────────────

    @Test
    fun `toSportEventEntity maps basic match`() {
        val match = FdMatch(
            id = 12345,
            utcDate = "2026-03-15T20:00:00Z",
            status = "FINISHED",
            matchday = 28,
            homeTeam = FdTeam(id = 1, name = "Arsenal", shortName = "Arsenal", tla = "ARS"),
            awayTeam = FdTeam(id = 2, name = "Chelsea", shortName = "Chelsea", tla = "CHE"),
            score = FdScore(
                fullTime = FdScoreDetail(home = 2, away = 1),
                halfTime = FdScoreDetail(home = 1, away = 0),
                winner = "HOME_TEAM",
            ),
            season = FdSeason(startDate = "2025-08-16"),
        )

        val entity = match.toSportEventEntity("pl-2025")
        assertEquals("fd-12345", entity.id)
        assertEquals("football", entity.sportId)
        assertEquals("pl-2025", entity.competitionId)
        assertEquals("COMPLETED", entity.status)
        assertEquals("fd-team-1", entity.homeCompetitorId)
        assertEquals("fd-team-2", entity.awayCompetitorId)
        assertEquals(2, entity.homeScore)
        assertEquals(1, entity.awayScore)
        assertEquals("Arsenal vs Chelsea", entity.eventName)
        assertEquals("Matchday 28", entity.round)
        assertEquals("2025-2026", entity.season)
        assertEquals("football-data.org", entity.dataSource)
        assertNotNull(entity.resultDetail)
    }

    @Test
    fun `toSportEventEntity uses stage when not REGULAR_SEASON`() {
        val match = FdMatch(
            id = 100,
            utcDate = "2026-03-15T20:00:00Z",
            status = "SCHEDULED",
            stage = "QUARTER_FINALS",
            homeTeam = FdTeam(id = 1, name = "Team A"),
            awayTeam = FdTeam(id = 2, name = "Team B"),
        )
        val entity = match.toSportEventEntity("cl-2025")
        assertEquals("QUARTER FINALS", entity.round)
    }

    @Test
    fun `toSportEventEntity uses matchday for REGULAR_SEASON stage`() {
        val match = FdMatch(
            id = 101,
            utcDate = "2026-03-15T20:00:00Z",
            status = "SCHEDULED",
            stage = "REGULAR_SEASON",
            matchday = 10,
        )
        val entity = match.toSportEventEntity("pl")
        assertEquals("Matchday 10", entity.round)
    }

    @Test
    fun `toSportEventEntity handles null teams with TBD`() {
        val match = FdMatch(
            id = 200,
            utcDate = "2026-04-01T15:00:00Z",
            status = "SCHEDULED",
        )
        val entity = match.toSportEventEntity("comp")
        assertEquals("TBD vs TBD", entity.eventName)
        assertNull(entity.homeCompetitorId)
        assertNull(entity.awayCompetitorId)
    }

    @Test
    fun `toSportEventEntity prefers shortName over name for eventName`() {
        val match = FdMatch(
            id = 300,
            utcDate = "2026-03-15T20:00:00Z",
            homeTeam = FdTeam(id = 1, name = "Arsenal FC", shortName = "Arsenal"),
            awayTeam = FdTeam(id = 2, name = "Chelsea FC", shortName = "Chelsea"),
        )
        val entity = match.toSportEventEntity("comp")
        assertEquals("Arsenal vs Chelsea", entity.eventName)
    }

    @Test
    fun `toSportEventEntity handles invalid utcDate with fallback`() {
        val match = FdMatch(
            id = 400,
            utcDate = "not-a-date",
        )
        val entity = match.toSportEventEntity("comp")
        // Should not crash, uses Clock.System.now() as fallback
        assertNotNull(entity.scheduledAt)
    }

    @Test
    fun `toSportEventEntity builds resultDetail JSON with all scores`() {
        val match = FdMatch(
            id = 500,
            utcDate = "2026-03-15T20:00:00Z",
            status = "FINISHED",
            score = FdScore(
                fullTime = FdScoreDetail(home = 2, away = 2),
                halfTime = FdScoreDetail(home = 1, away = 1),
                extraTime = FdScoreDetail(home = 3, away = 3),
                penalties = FdScoreDetail(home = 5, away = 4),
                winner = "HOME_TEAM",
            ),
        )
        val entity = match.toSportEventEntity("comp")
        val json = entity.resultDetail!!
        assertTrue(json.contains("\"type\":\"football\""))
        assertTrue(json.contains("\"halftime_home\":1"))
        assertTrue(json.contains("\"fulltime_home\":2"))
        assertTrue(json.contains("\"extratime_home\":3"))
        assertTrue(json.contains("\"penalties_home\":5"))
        assertTrue(json.contains("\"winner\":\"HOME_TEAM\""))
    }

    @Test
    fun `toSportEventEntity null resultDetail when no fulltime score`() {
        val match = FdMatch(
            id = 600,
            utcDate = "2026-03-15T20:00:00Z",
            score = FdScore(fullTime = FdScoreDetail(home = null, away = null)),
        )
        val entity = match.toSportEventEntity("comp")
        assertNull(entity.resultDetail)
    }

    // ── FdTeam.toCompetitorEntity ───────────────────────

    @Test
    fun `FdTeam toCompetitorEntity maps fields`() {
        val team = FdTeam(id = 57, name = "Arsenal FC", shortName = "Arsenal", tla = "ARS", crest = "https://crests.football-data.org/57.png")
        val entity = team.toCompetitorEntity()
        assertEquals("fd-team-57", entity.id)
        assertEquals("football", entity.sportId)
        assertEquals("Arsenal FC", entity.name)
        assertEquals("Arsenal", entity.shortName)
        assertEquals("https://crests.football-data.org/57.png", entity.logoUrl)
        assertEquals(57, entity.footballDataId)
    }

    @Test
    fun `FdTeam toCompetitorEntity fallback shortName to tla`() {
        val team = FdTeam(id = 1, name = "Team", shortName = null, tla = "TLA")
        val entity = team.toCompetitorEntity()
        assertEquals("TLA", entity.shortName)
    }

    @Test
    fun `FdTeam toCompetitorEntity handles null name`() {
        val team = FdTeam(id = 1, name = null)
        val entity = team.toCompetitorEntity()
        assertEquals("Unknown", entity.name)
    }

    @Test
    fun `FdTeam toCompetitorEntity handles null id`() {
        val team = FdTeam(id = null, name = "Test")
        val entity = team.toCompetitorEntity()
        assertEquals("fd-team-0", entity.id)
    }

    // ── FdTeamDetail.toCompetitorEntity ─────────────────

    @Test
    fun `FdTeamDetail toCompetitorEntity includes country`() {
        val team = FdTeamDetail(
            id = 57, name = "Arsenal FC", shortName = "Arsenal",
            area = FdArea(name = "England"),
        )
        val entity = team.toCompetitorEntity()
        assertEquals("England", entity.country)
    }
}
