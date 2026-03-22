package com.daysync.app.feature.sports.data.remote.dto

import kotlin.time.Instant
import org.junit.Assert.assertNotNull
import org.junit.Assert.fail
import org.junit.Test

class EspnDateParseTest {

    @Test
    fun `raw Instant parse fails without seconds - confirms the root cause`() {
        // This proves kotlin.time.Instant.parse does NOT support HH:mm without seconds
        try {
            Instant.parse("2026-03-21T17:00Z")
            fail("Should have thrown - if this passes, the root cause is different")
        } catch (_: Exception) {
            // Expected: "The input string is too short"
        }
    }

    @Test
    fun `Instant parse succeeds with seconds`() {
        val instant = Instant.parse("2026-03-21T17:00:00Z")
        assertNotNull(instant)
    }

    @Test
    fun `mapper handles ESPN date without seconds via normalization`() {
        val event = EspnEvent(
            id = "600057365",
            date = "2026-03-21T17:00Z", // ESPN format without seconds
            name = "UFC Fight Night",
            competitions = listOf(
                EspnCompetition(
                    competitors = listOf(EspnCompetitor(id = "1", team = null)),
                ),
            ),
        )
        val entity = event.toSportEventEntity("mma", "mma-ufc")
        assertNotNull("Mapper should normalize date and create entity", entity)
    }

    @Test
    fun `mapper handles standard ISO date with seconds`() {
        val event = EspnEvent(
            id = "12345",
            date = "2026-03-21T17:00:00Z",
            name = "Test Event",
            competitions = listOf(
                EspnCompetition(
                    competitors = listOf(EspnCompetitor(id = "1", team = null)),
                ),
            ),
        )
        val entity = event.toSportEventEntity("mma", "mma-ufc")
        assertNotNull(entity)
    }
}
