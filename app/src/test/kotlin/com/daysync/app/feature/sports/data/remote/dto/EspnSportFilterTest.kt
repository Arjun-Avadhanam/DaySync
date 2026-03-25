package com.daysync.app.feature.sports.data.remote.dto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Tests that each sport's mapper correctly sets sportId,
 * which is critical for the getLiveEventsBySport DAO query.
 * A tennis live event must have sportId="tennis" so it doesn't
 * appear when filtering by "football" etc.
 */
class EspnSportFilterTest {

    // ── Football ────────────────────────────────────────────

    @Test
    fun `football match has sportId football`() {
        val event = EspnEvent(
            id = "1", date = "2026-03-26T19:45:00Z",
            name = "Arsenal vs Chelsea", shortName = "ARS vs CHE",
            status = EspnEventStatus(type = EspnStatusType(state = "in")),
            competitions = listOf(
                EspnCompetition(
                    id = "1",
                    status = EspnEventStatus(type = EspnStatusType(state = "in")),
                    competitors = listOf(
                        EspnCompetitor(homeAway = "home", score = "1", team = EspnTeam(id = "1", displayName = "Arsenal")),
                        EspnCompetitor(homeAway = "away", score = "0", team = EspnTeam(id = "2", displayName = "Chelsea")),
                    ),
                ),
            ),
        )
        val entity = event.toFootballMatchEntity("football-pl")
        assertNotNull(entity)
        assertEquals("football", entity!!.sportId)
        assertEquals("LIVE", entity.status)
    }

    // ── Basketball ──────────────────────────────────────────

    @Test
    fun `NBA game has sportId basketball`() {
        val event = EspnEvent(
            id = "2", date = "2026-03-26T23:30:00Z",
            name = "BOS at OKC", shortName = "BOS @ OKC",
            status = EspnEventStatus(type = EspnStatusType(state = "in")),
            competitions = listOf(
                EspnCompetition(
                    id = "2",
                    type = EspnCompetitionType(abbreviation = "STD"),
                    status = EspnEventStatus(type = EspnStatusType(state = "in")),
                    competitors = listOf(
                        EspnCompetitor(homeAway = "home", score = "55", team = EspnTeam(id = "25")),
                        EspnCompetitor(homeAway = "away", score = "60", team = EspnTeam(id = "2")),
                    ),
                ),
            ),
        )
        val entity = event.toNbaGameEntity("basketball-nba")
        assertNotNull(entity)
        assertEquals("basketball", entity!!.sportId)
        assertEquals("LIVE", entity.status)
    }

    // ── Tennis ───────────────────────────────────────────────

    @Test
    fun `tennis match has sportId tennis`() {
        val event = EspnEvent(
            id = "3", date = "2026-03-26T10:00:00Z",
            name = "Miami Open",
            status = EspnEventStatus(type = EspnStatusType(state = "in")),
            groupings = listOf(
                EspnGrouping(
                    grouping = EspnGroupingType(slug = "womens-singles", displayName = "Women's Singles"),
                    competitions = listOf(
                        EspnCompetition(
                            id = "100",
                            date = "2026-03-26T10:00:00Z",
                            round = EspnRound(id = 5, displayName = "Quarterfinal"),
                            status = EspnEventStatus(type = EspnStatusType(state = "in")),
                            competitors = listOf(
                                EspnCompetitor(id = "1", athlete = EspnAthlete(displayName = "Player A")),
                                EspnCompetitor(id = "2", athlete = EspnAthlete(displayName = "Player B")),
                            ),
                        ),
                    ),
                ),
            ),
        )
        val matches = event.toTennisMatchEntities("tennis-atp")
        assertEquals(1, matches.size)
        assertEquals("tennis", matches[0].event.sportId)
        assertEquals("LIVE", matches[0].event.status)
    }

    // ── MMA ─────────────────────────────────────────────────

    @Test
    fun `MMA fight has sportId mma`() {
        val event = EspnEvent(
            id = "4", date = "2026-03-26T17:00:00Z",
            name = "UFC Fight Night",
            status = EspnEventStatus(type = EspnStatusType(state = "in")),
            competitions = listOf(
                EspnCompetition(
                    id = "200",
                    date = "2026-03-26T17:00:00Z",
                    status = EspnEventStatus(type = EspnStatusType(state = "in")),
                    competitors = listOf(
                        EspnCompetitor(id = "1", athlete = EspnAthlete(displayName = "Fighter A")),
                        EspnCompetitor(id = "2", athlete = EspnAthlete(displayName = "Fighter B")),
                    ),
                ),
            ),
        )
        val fights = event.toMmaFightEntities("mma-ufc")
        assertEquals(1, fights.size)
        assertEquals("mma", fights[0].event.sportId)
        assertEquals("LIVE", fights[0].event.status)
    }

    // ── Cross-sport isolation ───────────────────────────────

    @Test
    fun `different sports produce different sportIds`() {
        // Simulates what would happen if all sports had a live event
        // The DAO query WHERE sportId = :sportId must filter correctly

        val footballEntity = EspnEvent(
            id = "f1", date = "2026-03-26T19:45:00Z",
            status = EspnEventStatus(type = EspnStatusType(state = "in")),
            competitions = listOf(EspnCompetition(id = "f1", status = EspnEventStatus(type = EspnStatusType(state = "in")),
                competitors = listOf(
                    EspnCompetitor(homeAway = "home", team = EspnTeam(id = "1")),
                    EspnCompetitor(homeAway = "away", team = EspnTeam(id = "2")),
                ))),
        ).toFootballMatchEntity("football-pl")!!

        val nbaEntity = EspnEvent(
            id = "b1", date = "2026-03-26T23:00:00Z",
            status = EspnEventStatus(type = EspnStatusType(state = "in")),
            competitions = listOf(EspnCompetition(id = "b1", type = EspnCompetitionType(abbreviation = "STD"),
                status = EspnEventStatus(type = EspnStatusType(state = "in")),
                competitors = listOf(
                    EspnCompetitor(homeAway = "home", team = EspnTeam(id = "3")),
                    EspnCompetitor(homeAway = "away", team = EspnTeam(id = "4")),
                ))),
        ).toNbaGameEntity("basketball-nba")!!

        // Verify they have different sportIds
        assertEquals("football", footballEntity.sportId)
        assertEquals("basketball", nbaEntity.sportId)

        // Both are LIVE
        assertEquals("LIVE", footballEntity.status)
        assertEquals("LIVE", nbaEntity.status)

        // A DAO query with sportId="football" would only return footballEntity
        // A DAO query with sportId="basketball" would only return nbaEntity
    }
}
