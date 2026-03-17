package com.daysync.app.feature.sports.data.remote.dto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiFootballMappersTest {

    // ── mapApifStatus ───────────────────────────────────

    @Test
    fun `1H maps to LIVE`() = assertEquals("LIVE", mapApifStatus("1H"))

    @Test
    fun `HT maps to LIVE`() = assertEquals("LIVE", mapApifStatus("HT"))

    @Test
    fun `2H maps to LIVE`() = assertEquals("LIVE", mapApifStatus("2H"))

    @Test
    fun `ET maps to LIVE`() = assertEquals("LIVE", mapApifStatus("ET"))

    @Test
    fun `BT maps to LIVE`() = assertEquals("LIVE", mapApifStatus("BT"))

    @Test
    fun `P maps to LIVE`() = assertEquals("LIVE", mapApifStatus("P"))

    @Test
    fun `SUSP maps to LIVE`() = assertEquals("LIVE", mapApifStatus("SUSP"))

    @Test
    fun `INT maps to LIVE`() = assertEquals("LIVE", mapApifStatus("INT"))

    @Test
    fun `LIVE maps to LIVE`() = assertEquals("LIVE", mapApifStatus("LIVE"))

    @Test
    fun `FT maps to COMPLETED`() = assertEquals("COMPLETED", mapApifStatus("FT"))

    @Test
    fun `AET maps to COMPLETED`() = assertEquals("COMPLETED", mapApifStatus("AET"))

    @Test
    fun `PEN maps to COMPLETED`() = assertEquals("COMPLETED", mapApifStatus("PEN"))

    @Test
    fun `TBD maps to SCHEDULED`() = assertEquals("SCHEDULED", mapApifStatus("TBD"))

    @Test
    fun `NS maps to SCHEDULED`() = assertEquals("SCHEDULED", mapApifStatus("NS"))

    @Test
    fun `PST maps to POSTPONED`() = assertEquals("POSTPONED", mapApifStatus("PST"))

    @Test
    fun `CANC maps to CANCELLED`() = assertEquals("CANCELLED", mapApifStatus("CANC"))

    @Test
    fun `ABD maps to CANCELLED`() = assertEquals("CANCELLED", mapApifStatus("ABD"))

    @Test
    fun `AWD maps to CANCELLED`() = assertEquals("CANCELLED", mapApifStatus("AWD"))

    @Test
    fun `WO maps to CANCELLED`() = assertEquals("CANCELLED", mapApifStatus("WO"))

    @Test
    fun `null maps to SCHEDULED`() = assertEquals("SCHEDULED", mapApifStatus(null))

    @Test
    fun `unknown maps to SCHEDULED`() = assertEquals("SCHEDULED", mapApifStatus("XYZ"))

    // ── ApifFixture.toSportEventEntity ──────────────────

    @Test
    fun `toSportEventEntity maps fixture with timestamp`() {
        val fixture = ApifFixture(
            fixture = ApifFixtureInfo(
                id = 100,
                timestamp = 1773849600L, // 2026-03-16T12:00:00Z
                status = ApifStatus(short = "FT"),
            ),
            league = ApifLeague(season = 2025, round = "Regular Season - 28"),
            teams = ApifTeams(
                home = ApifTeamInfo(id = 10, name = "Arsenal"),
                away = ApifTeamInfo(id = 20, name = "Chelsea"),
            ),
            goals = ApifGoals(home = 3, away = 1),
            score = ApifFullScore(
                halftime = ApifGoals(home = 1, away = 0),
            ),
        )
        val entity = fixture.toSportEventEntity("pl-2025")
        assertNotNull(entity)
        assertEquals("apif-100", entity!!.id)
        assertEquals("football", entity.sportId)
        assertEquals("pl-2025", entity.competitionId)
        assertEquals("COMPLETED", entity.status)
        assertEquals("apif-team-10", entity.homeCompetitorId)
        assertEquals("apif-team-20", entity.awayCompetitorId)
        assertEquals(3, entity.homeScore)
        assertEquals(1, entity.awayScore)
        assertEquals("Arsenal vs Chelsea", entity.eventName)
        assertEquals("Regular Season - 28", entity.round)
        assertEquals("2025", entity.season)
        assertEquals("api-football", entity.dataSource)
    }

    @Test
    fun `toSportEventEntity returns null when fixture id is null`() {
        val fixture = ApifFixture(
            fixture = ApifFixtureInfo(id = null, timestamp = 1000L),
        )
        assertNull(fixture.toSportEventEntity("comp"))
    }

    @Test
    fun `toSportEventEntity returns null when no timestamp or date`() {
        val fixture = ApifFixture(
            fixture = ApifFixtureInfo(id = 1, timestamp = null, date = null),
        )
        assertNull(fixture.toSportEventEntity("comp"))
    }

    @Test
    fun `toSportEventEntity includes elapsed for live match`() {
        val fixture = ApifFixture(
            fixture = ApifFixtureInfo(
                id = 200,
                timestamp = 1773849600L,
                status = ApifStatus(short = "1H", elapsed = 35),
            ),
            goals = ApifGoals(home = 1, away = 0),
        )
        val entity = fixture.toSportEventEntity("comp")!!
        assertTrue(entity.resultDetail!!.contains("\"elapsed\":35"))
    }

    @Test
    fun `toSportEventEntity does not include elapsed for finished match`() {
        val fixture = ApifFixture(
            fixture = ApifFixtureInfo(
                id = 300,
                timestamp = 1773849600L,
                status = ApifStatus(short = "FT", elapsed = 90),
            ),
            goals = ApifGoals(home = 2, away = 0),
        )
        val entity = fixture.toSportEventEntity("comp")!!
        assertTrue(!entity.resultDetail!!.contains("elapsed"))
    }

    @Test
    fun `toSportEventEntity handles TBD team names`() {
        val fixture = ApifFixture(
            fixture = ApifFixtureInfo(id = 400, timestamp = 1773849600L),
            teams = ApifTeams(home = null, away = null),
        )
        val entity = fixture.toSportEventEntity("comp")!!
        assertEquals("TBD vs TBD", entity.eventName)
    }

    @Test
    fun `toSportEventEntity includes penalties in resultDetail`() {
        val fixture = ApifFixture(
            fixture = ApifFixtureInfo(
                id = 500,
                timestamp = 1773849600L,
                status = ApifStatus(short = "PEN"),
            ),
            goals = ApifGoals(home = 2, away = 2),
            score = ApifFullScore(
                halftime = ApifGoals(home = 1, away = 1),
                extratime = ApifGoals(home = 1, away = 1),
                penalty = ApifGoals(home = 5, away = 3),
            ),
        )
        val entity = fixture.toSportEventEntity("comp")!!
        val json = entity.resultDetail!!
        assertTrue(json.contains("\"penalties_home\":5"))
        assertTrue(json.contains("\"penalties_away\":3"))
    }

    // ── ApifTeamInfo.toCompetitorEntity ─────────────────

    @Test
    fun `ApifTeamInfo toCompetitorEntity maps fields`() {
        val team = ApifTeamInfo(id = 42, name = "Arsenal", logo = "https://logo.png")
        val entity = team.toCompetitorEntity()
        assertNotNull(entity)
        assertEquals("apif-team-42", entity!!.id)
        assertEquals("Arsenal", entity.name)
        assertEquals("https://logo.png", entity.logoUrl)
        assertEquals(42, entity.apiFootballId)
    }

    @Test
    fun `ApifTeamInfo returns null when id is null`() {
        val team = ApifTeamInfo(id = null, name = "Test")
        assertNull(team.toCompetitorEntity())
    }

    // ── ApifFixture.toVenueEntity ───────────────────────

    @Test
    fun `toVenueEntity maps venue`() {
        val fixture = ApifFixture(
            fixture = ApifFixtureInfo(
                id = 1,
                venue = ApifVenue(id = 99, name = "Emirates Stadium", city = "London"),
            ),
        )
        val venue = fixture.toVenueEntity()
        assertNotNull(venue)
        assertEquals("apif-venue-99", venue!!.id)
        assertEquals("Emirates Stadium", venue.name)
        assertEquals("London", venue.city)
    }

    @Test
    fun `toVenueEntity returns null when no venue`() {
        val fixture = ApifFixture(fixture = ApifFixtureInfo(id = 1, venue = null))
        assertNull(fixture.toVenueEntity())
    }

    @Test
    fun `toVenueEntity returns null when venue id is null`() {
        val fixture = ApifFixture(
            fixture = ApifFixtureInfo(id = 1, venue = ApifVenue(id = null, name = "Test")),
        )
        assertNull(fixture.toVenueEntity())
    }
}
