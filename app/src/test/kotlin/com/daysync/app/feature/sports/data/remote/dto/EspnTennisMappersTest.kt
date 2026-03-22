package com.daysync.app.feature.sports.data.remote.dto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Tests ESPN Tennis mapping with real API data structure.
 * Tennis tournaments return 0 competitions when not live.
 */
class EspnTennisMappersTest {

    @Test
    fun `completed tournament with empty competitions creates entity`() {
        // Real data: Miami Open - completed, 0 competitions
        val event = EspnEvent(
            id = "713-2026",
            date = "2026-03-16T04:00Z",
            name = "Miami Open presented by Itau",
            shortName = "Miami Open",
            status = EspnEventStatus(
                type = EspnStatusType(state = "post", completed = true),
            ),
            competitions = emptyList(), // ESPN returns empty for completed tournaments
            season = EspnSeason(year = 2026),
        )
        val entity = event.toSportEventEntity("tennis", "tennis-atp")
        assertNotNull("Should create entity even with empty competitions", entity)
        assertEquals("espn-713-2026", entity!!.id)
        assertEquals("tennis", entity.sportId)
        assertEquals("tennis-atp", entity.competitionId)
        assertEquals("COMPLETED", entity.status)
        assertEquals("Miami Open", entity.eventName)
        assertEquals("espn", entity.dataSource)
    }

    @Test
    fun `scheduled tournament with empty competitions creates entity`() {
        // Real data: BMW Open - scheduled, 0 competitions
        val event = EspnEvent(
            id = "12-2026",
            date = "2026-04-13T04:00Z",
            name = "BMW Open by Bitpanda",
            shortName = "BMW Open",
            status = EspnEventStatus(
                type = EspnStatusType(state = "pre", completed = false),
            ),
            competitions = emptyList(),
            season = EspnSeason(year = 2026),
        )
        val entity = event.toSportEventEntity("tennis", "tennis-atp")
        assertNotNull(entity)
        assertEquals("SCHEDULED", entity!!.status)
        assertEquals("BMW Open", entity.eventName)
    }

    @Test
    fun `tournament with null competitor IDs since no competitions`() {
        val event = EspnEvent(
            id = "1-2026",
            date = "2026-03-16T04:00Z",
            name = "Test Open",
            competitions = emptyList(),
        )
        val entity = event.toSportEventEntity("tennis", "tennis-atp")!!
        assertNull(entity.homeCompetitorId)
        assertNull(entity.awayCompetitorId)
        assertNull(entity.homeScore)
        assertNull(entity.awayScore)
    }

    @Test
    fun `multiple tournaments on same date both create entities`() {
        // Real data: Apr 13 has BMW Open and Barcelona Open
        val event1 = EspnEvent(
            id = "12-2026", date = "2026-04-13T04:00Z",
            name = "BMW Open", competitions = emptyList(),
        )
        val event2 = EspnEvent(
            id = "338-2026", date = "2026-04-13T04:00Z",
            name = "Barcelona Open", competitions = emptyList(),
        )
        val entity1 = event1.toSportEventEntity("tennis", "tennis-atp")
        val entity2 = event2.toSportEventEntity("tennis", "tennis-atp")
        assertNotNull(entity1)
        assertNotNull(entity2)
        assertEquals("espn-12-2026", entity1!!.id)
        assertEquals("espn-338-2026", entity2!!.id)
    }

    @Test
    fun `ESPN date without seconds parses correctly for tennis`() {
        val event = EspnEvent(
            id = "1-2026",
            date = "2026-04-13T04:00Z", // no seconds
            name = "Test",
            competitions = emptyList(),
        )
        val entity = event.toSportEventEntity("tennis", "tennis-atp")
        assertNotNull("Date without seconds should parse", entity)
    }
}
