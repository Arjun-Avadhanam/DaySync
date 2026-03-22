package com.daysync.app.feature.sports.data.remote.dto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests ESPN MMA mapping with real API data structure.
 * Each fight on a UFC card becomes a separate SportEventEntity.
 */
class EspnMmaMappersTest {

    // ── Helper: replicate real ESPN MMA API data ────────────

    private fun makeCompletedCard(): EspnEvent {
        return EspnEvent(
            id = "600057365",
            date = "2026-03-21T17:00Z",
            name = "UFC Fight Night: Evloev vs. Murphy",
            shortName = "UFC Fight Night",
            status = EspnEventStatus(
                type = EspnStatusType(state = "post", completed = true),
            ),
            competitions = listOf(
                // Fight 0: undercard (3 rounds)
                EspnCompetition(
                    id = "401859204",
                    date = "2026-03-21T17:00Z",
                    type = EspnCompetitionType(abbreviation = "W Strawweight"),
                    format = EspnFormat(regulation = EspnRegulation(periods = 3)),
                    status = EspnEventStatus(
                        type = EspnStatusType(state = "post", completed = true),
                    ),
                    competitors = listOf(
                        EspnCompetitor(
                            id = "5157674", winner = true,
                            athlete = EspnAthlete(
                                displayName = "Shanelle Dyer",
                                shortName = "S. Dyer",
                                flag = EspnFlag(alt = "England", href = "https://flags/eng.png"),
                            ),
                            records = listOf(EspnRecord(name = "overall", summary = "7-1-0")),
                        ),
                        EspnCompetitor(
                            id = "5159959", winner = false,
                            athlete = EspnAthlete(
                                displayName = "Ravena Oliveira",
                                shortName = "R. Oliveira",
                                flag = EspnFlag(alt = "Brazil", href = "https://flags/bra.png"),
                            ),
                            records = listOf(EspnRecord(name = "overall", summary = "7-4-1")),
                        ),
                    ),
                    details = listOf(
                        EspnDetail(type = EspnDetailType(text = "Unofficial Winner Kotko")),
                    ),
                ),
                // Fight 1: main event (5 rounds = championship)
                EspnCompetition(
                    id = "401838357",
                    date = "2026-03-21T20:00Z",
                    type = EspnCompetitionType(abbreviation = "Featherweight"),
                    format = EspnFormat(regulation = EspnRegulation(periods = 5)),
                    status = EspnEventStatus(
                        type = EspnStatusType(state = "post", completed = true),
                    ),
                    competitors = listOf(
                        EspnCompetitor(
                            id = "4029275", winner = true,
                            athlete = EspnAthlete(
                                displayName = "Movsar Evloev",
                                shortName = "M. Evloev",
                                flag = EspnFlag(alt = "Russia"),
                            ),
                            records = listOf(EspnRecord(name = "overall", summary = "20-0-0")),
                        ),
                        EspnCompetitor(
                            id = "4576101", winner = false,
                            athlete = EspnAthlete(
                                displayName = "Lerone Murphy",
                                shortName = "L. Murphy",
                                flag = EspnFlag(alt = "England"),
                            ),
                            records = listOf(EspnRecord(name = "overall", summary = "17-1-1")),
                        ),
                    ),
                    details = listOf(
                        EspnDetail(type = EspnDetailType(text = "Unofficial Winner Decision")),
                    ),
                ),
            ),
            season = EspnSeason(year = 2026),
        )
    }

    private fun makeScheduledCard(): EspnEvent {
        return EspnEvent(
            id = "600057366",
            date = "2026-03-28T21:00Z",
            name = "UFC Fight Night: Adesanya vs. Pyfer",
            shortName = "UFC Fight Night",
            status = EspnEventStatus(
                type = EspnStatusType(state = "pre", completed = false),
            ),
            competitions = listOf(
                EspnCompetition(
                    id = "401860001",
                    date = "2026-03-28T21:00Z",
                    type = EspnCompetitionType(abbreviation = "Featherweight"),
                    format = EspnFormat(regulation = EspnRegulation(periods = 3)),
                    status = EspnEventStatus(
                        type = EspnStatusType(state = "pre", completed = false),
                    ),
                    competitors = listOf(
                        EspnCompetitor(
                            id = "1001",
                            athlete = EspnAthlete(displayName = "Marcio Barbosa", shortName = "M. Barbosa"),
                            records = listOf(EspnRecord(summary = "10-2-0")),
                        ),
                        EspnCompetitor(
                            id = "1002",
                            athlete = EspnAthlete(displayName = "Zhu Kangjie", shortName = "Z. Kangjie"),
                            records = listOf(EspnRecord(summary = "8-1-0")),
                        ),
                    ),
                ),
                EspnCompetition(
                    id = "401860014",
                    date = "2026-03-29T01:00Z",
                    type = EspnCompetitionType(abbreviation = "Middleweight"),
                    format = EspnFormat(regulation = EspnRegulation(periods = 5)),
                    status = EspnEventStatus(
                        type = EspnStatusType(state = "pre", completed = false),
                    ),
                    competitors = listOf(
                        EspnCompetitor(
                            id = "2001",
                            athlete = EspnAthlete(displayName = "Israel Adesanya", shortName = "I. Adesanya"),
                            records = listOf(EspnRecord(summary = "24-4-0")),
                        ),
                        EspnCompetitor(
                            id = "2002",
                            athlete = EspnAthlete(displayName = "Joe Pyfer", shortName = "J. Pyfer"),
                            records = listOf(EspnRecord(summary = "14-3-0")),
                        ),
                    ),
                ),
            ),
            season = EspnSeason(year = 2026),
        )
    }

    // ── Tests: fight count ──────────────────────────────────

    @Test
    fun `completed card creates one event per fight`() {
        val fights = makeCompletedCard().toMmaFightEntities("mma-ufc")
        assertEquals(2, fights.size)
    }

    @Test
    fun `scheduled card creates one event per fight`() {
        val fights = makeScheduledCard().toMmaFightEntities("mma-ufc")
        assertEquals(2, fights.size)
    }

    // ── Tests: fight event entity fields ────────────────────

    @Test
    fun `fight event has correct ID format`() {
        val fights = makeCompletedCard().toMmaFightEntities("mma-ufc")
        assertEquals("espn-fight-401859204", fights[0].event.id)
        assertEquals("espn-fight-401838357", fights[1].event.id)
    }

    @Test
    fun `fight event has fighter names as eventName`() {
        val fights = makeCompletedCard().toMmaFightEntities("mma-ufc")
        assertEquals("Shanelle Dyer vs Ravena Oliveira", fights[0].event.eventName)
        assertEquals("Movsar Evloev vs Lerone Murphy", fights[1].event.eventName)
    }

    @Test
    fun `fight event has weight class as round`() {
        val fights = makeCompletedCard().toMmaFightEntities("mma-ufc")
        assertEquals("W Strawweight", fights[0].event.round)
        assertEquals("Featherweight", fights[1].event.round)
    }

    @Test
    fun `fight event has card name as season`() {
        val fights = makeCompletedCard().toMmaFightEntities("mma-ufc")
        assertEquals("UFC Fight Night: Evloev vs. Murphy", fights[0].event.season)
    }

    @Test
    fun `fight event has correct sportId and competitionId`() {
        val fight = makeCompletedCard().toMmaFightEntities("mma-ufc")[0]
        assertEquals("mma", fight.event.sportId)
        assertEquals("mma-ufc", fight.event.competitionId)
    }

    @Test
    fun `completed fight has COMPLETED status`() {
        val fights = makeCompletedCard().toMmaFightEntities("mma-ufc")
        assertEquals("COMPLETED", fights[0].event.status)
        assertEquals("COMPLETED", fights[1].event.status)
    }

    @Test
    fun `scheduled fight has SCHEDULED status`() {
        val fights = makeScheduledCard().toMmaFightEntities("mma-ufc")
        assertEquals("SCHEDULED", fights[0].event.status)
        assertEquals("SCHEDULED", fights[1].event.status)
    }

    @Test
    fun `fight uses its own date not card date`() {
        val fights = makeCompletedCard().toMmaFightEntities("mma-ufc")
        // Fight 0 starts at 17:00, fight 1 at 20:00
        assertTrue(fights[1].event.scheduledAt > fights[0].event.scheduledAt)
    }

    // ── Tests: fighter competitor entities ───────────────────

    @Test
    fun `fighter1 has correct competitor entity`() {
        val fight = makeCompletedCard().toMmaFightEntities("mma-ufc")[1] // main event
        assertEquals("espn-fighter-4029275", fight.fighter1.id)
        assertEquals("Movsar Evloev", fight.fighter1.name)
        assertEquals("M. Evloev", fight.fighter1.shortName)
        assertEquals("Russia", fight.fighter1.country)
        assertEquals("mma", fight.fighter1.sportId)
        assertTrue(fight.fighter1.isIndividual)
        assertEquals("4029275", fight.fighter1.espnId)
    }

    @Test
    fun `fighter2 has correct competitor entity`() {
        val fight = makeCompletedCard().toMmaFightEntities("mma-ufc")[1]
        assertEquals("espn-fighter-4576101", fight.fighter2.id)
        assertEquals("Lerone Murphy", fight.fighter2.name)
        assertEquals("England", fight.fighter2.country)
    }

    @Test
    fun `fight event references fighter competitor IDs`() {
        val fight = makeCompletedCard().toMmaFightEntities("mma-ufc")[1]
        assertEquals("espn-fighter-4029275", fight.event.homeCompetitorId)
        assertEquals("espn-fighter-4576101", fight.event.awayCompetitorId)
    }

    @Test
    fun `fighter flag URL stored as logoUrl`() {
        val fight = makeCompletedCard().toMmaFightEntities("mma-ufc")[0]
        assertEquals("https://flags/eng.png", fight.fighter1.logoUrl)
        assertEquals("https://flags/bra.png", fight.fighter2.logoUrl)
    }

    // ── Tests: resultDetail JSON ────────────────────────────

    @Test
    fun `resultDetail contains mma type`() {
        val fight = makeCompletedCard().toMmaFightEntities("mma-ufc")[0]
        assertTrue(fight.event.resultDetail!!.contains("\"type\":\"mma\""))
    }

    @Test
    fun `resultDetail contains card name`() {
        val fight = makeCompletedCard().toMmaFightEntities("mma-ufc")[0]
        assertTrue(fight.event.resultDetail!!.contains("UFC Fight Night: Evloev vs. Murphy"))
    }

    @Test
    fun `resultDetail contains weight class`() {
        val fight = makeCompletedCard().toMmaFightEntities("mma-ufc")[0]
        assertTrue(fight.event.resultDetail!!.contains("\"weight_class\":\"W Strawweight\""))
    }

    @Test
    fun `resultDetail contains scheduled rounds`() {
        val fights = makeCompletedCard().toMmaFightEntities("mma-ufc")
        assertTrue(fights[0].event.resultDetail!!.contains("\"scheduled_rounds\":3"))
        assertTrue(fights[1].event.resultDetail!!.contains("\"scheduled_rounds\":5"))
    }

    @Test
    fun `resultDetail identifies championship fight`() {
        val fights = makeCompletedCard().toMmaFightEntities("mma-ufc")
        assertTrue(fights[0].event.resultDetail!!.contains("\"is_championship\":false"))
        assertTrue(fights[1].event.resultDetail!!.contains("\"is_championship\":true"))
    }

    @Test
    fun `resultDetail identifies main event`() {
        val fights = makeCompletedCard().toMmaFightEntities("mma-ufc")
        assertTrue(fights[0].event.resultDetail!!.contains("\"is_main_event\":false"))
        assertTrue(fights[1].event.resultDetail!!.contains("\"is_main_event\":true"))
    }

    @Test
    fun `resultDetail contains fighter records`() {
        val fight = makeCompletedCard().toMmaFightEntities("mma-ufc")[1]
        assertTrue(fight.event.resultDetail!!.contains("\"fighter1_record\":\"20-0-0\""))
        assertTrue(fight.event.resultDetail!!.contains("\"fighter2_record\":\"17-1-1\""))
    }

    @Test
    fun `completed fight resultDetail contains method`() {
        val fights = makeCompletedCard().toMmaFightEntities("mma-ufc")
        assertTrue(fights[0].event.resultDetail!!.contains("\"method\":\"Kotko\""))
        assertTrue(fights[1].event.resultDetail!!.contains("\"method\":\"Decision\""))
    }

    @Test
    fun `scheduled fight resultDetail has no method`() {
        val fights = makeScheduledCard().toMmaFightEntities("mma-ufc")
        assertFalse(fights[0].event.resultDetail!!.contains("method"))
    }

    @Test
    fun `resultDetail contains fight order`() {
        val fights = makeCompletedCard().toMmaFightEntities("mma-ufc")
        assertTrue(fights[0].event.resultDetail!!.contains("\"fight_order\":0"))
        assertTrue(fights[1].event.resultDetail!!.contains("\"fight_order\":1"))
    }

    // ── Tests: edge cases ───────────────────────────────────

    @Test
    fun `fight with missing competition id is skipped`() {
        val event = EspnEvent(
            id = "1", date = "2026-03-21T17:00Z", name = "Test",
            competitions = listOf(
                EspnCompetition(
                    id = null,
                    competitors = listOf(
                        EspnCompetitor(id = "1", athlete = EspnAthlete(displayName = "A")),
                        EspnCompetitor(id = "2", athlete = EspnAthlete(displayName = "B")),
                    ),
                ),
            ),
        )
        assertEquals(0, event.toMmaFightEntities("mma-ufc").size)
    }

    @Test
    fun `fight with only one competitor is skipped`() {
        val event = EspnEvent(
            id = "1", date = "2026-03-21T17:00Z", name = "Test",
            competitions = listOf(
                EspnCompetition(
                    id = "123",
                    competitors = listOf(
                        EspnCompetitor(id = "1", athlete = EspnAthlete(displayName = "A")),
                    ),
                ),
            ),
        )
        assertEquals(0, event.toMmaFightEntities("mma-ufc").size)
    }

    @Test
    fun `event with empty competitions returns empty list`() {
        val event = EspnEvent(
            id = "1", date = "2026-03-21T17:00Z", name = "Test",
            competitions = emptyList(),
        )
        assertEquals(0, event.toMmaFightEntities("mma-ufc").size)
    }

    @Test
    fun `fighter with missing athlete name defaults to TBD`() {
        val event = EspnEvent(
            id = "1", date = "2026-03-21T17:00Z", name = "Test",
            competitions = listOf(
                EspnCompetition(
                    id = "123",
                    competitors = listOf(
                        EspnCompetitor(id = "1", athlete = null),
                        EspnCompetitor(id = "2", athlete = EspnAthlete(displayName = "Known")),
                    ),
                ),
            ),
        )
        val fights = event.toMmaFightEntities("mma-ufc")
        assertEquals(1, fights.size)
        assertEquals("TBD vs Known", fights[0].event.eventName)
    }

    // ── Tests: ESPN date normalization ───────────────────────

    @Test
    fun `fight date without seconds parses correctly`() {
        val event = EspnEvent(
            id = "1", date = "2026-03-21T17:00Z", name = "Test",
            competitions = listOf(
                EspnCompetition(
                    id = "123", date = "2026-03-21T17:00Z",
                    competitors = listOf(
                        EspnCompetitor(id = "1", athlete = EspnAthlete(displayName = "A")),
                        EspnCompetitor(id = "2", athlete = EspnAthlete(displayName = "B")),
                    ),
                ),
            ),
        )
        val fights = event.toMmaFightEntities("mma-ufc")
        assertEquals(1, fights.size)
        assertNotNull(fights[0].event.scheduledAt)
    }

    // ── Tests: team sport mapper still works ─────────────────

    @Test
    fun `toSportEventEntity still works for team sports with competitions`() {
        val event = EspnEvent(
            id = "401234", date = "2026-03-15T20:00:00Z",
            name = "Arsenal vs Chelsea", shortName = "ARS vs CHE",
            status = EspnEventStatus(type = EspnStatusType(state = "post", completed = true)),
            competitions = listOf(
                EspnCompetition(
                    competitors = listOf(
                        EspnCompetitor(
                            homeAway = "home", score = "2",
                            team = EspnTeam(id = "1", displayName = "Arsenal"),
                        ),
                        EspnCompetitor(
                            homeAway = "away", score = "1",
                            team = EspnTeam(id = "2", displayName = "Chelsea"),
                        ),
                    ),
                ),
            ),
            season = EspnSeason(year = 2026),
        )
        val entity = event.toSportEventEntity("football", "epl")
        assertNotNull(entity)
        assertEquals("espn-team-1", entity!!.homeCompetitorId)
        assertEquals(2, entity.homeScore)
    }

    @Test
    fun `toSportEventEntity creates entity with empty competitions for tennis`() {
        val event = EspnEvent(
            id = "713-2026", date = "2026-03-16T04:00Z",
            name = "Miami Open", competitions = emptyList(),
        )
        val entity = event.toSportEventEntity("tennis", "tennis-atp")
        assertNotNull(entity)
        assertNull(entity!!.homeCompetitorId)
    }
}
