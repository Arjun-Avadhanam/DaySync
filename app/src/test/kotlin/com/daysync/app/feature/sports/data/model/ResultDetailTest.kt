package com.daysync.app.feature.sports.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ResultDetailTest {

    // ── Football parsing ────────────────────────────────

    @Test
    fun `parses football result with all scores`() {
        val json = """
            {"type":"football","halftime_home":1,"halftime_away":0,
             "fulltime_home":2,"fulltime_away":1,
             "extratime_home":3,"extratime_away":2,
             "penalties_home":5,"penalties_away":4,
             "winner":"HOME_TEAM","elapsed":90}
        """.trimIndent()

        val result = ResultDetail.parse(json, "football") as ResultDetail.Football
        assertEquals(1, result.halftimeHome)
        assertEquals(0, result.halftimeAway)
        assertEquals(2, result.fulltimeHome)
        assertEquals(1, result.fulltimeAway)
        assertEquals(3, result.extratimeHome)
        assertEquals(2, result.extratimeAway)
        assertEquals(5, result.penaltiesHome)
        assertEquals(4, result.penaltiesAway)
        assertEquals("HOME_TEAM", result.winner)
        assertEquals("90", result.elapsed)
        assertTrue(result.goals.isEmpty()) // No goals in this JSON
    }

    @Test
    fun `parses football result with minimal data`() {
        val json = """{"type":"football","fulltime_home":1,"fulltime_away":0}"""
        val result = ResultDetail.parse(json, "football") as ResultDetail.Football
        assertEquals(1, result.fulltimeHome)
        assertEquals(0, result.fulltimeAway)
        assertNull(result.halftimeHome)
        assertNull(result.extratimeHome)
        assertNull(result.penaltiesHome)
        assertNull(result.winner)
        assertNull(result.elapsed)
    }

    @Test
    fun `football defaults missing fulltime to 0`() {
        val json = """{"type":"football"}"""
        val result = ResultDetail.parse(json, "football") as ResultDetail.Football
        assertEquals(0, result.fulltimeHome)
        assertEquals(0, result.fulltimeAway)
    }

    // ── F1 parsing ──────────────────────────────────────

    @Test
    fun `parses F1 result with all fields`() {
        val json = """
            {"type":"f1","total_laps":"58",
             "winner":"Max Verstappen","winner_team":"Red Bull",
             "winner_time":"1:32:45.123",
             "fastest_lap_driver":"Lando Norris","fastest_lap_time":"1:20.456"}
        """.trimIndent()

        val result = ResultDetail.parse(json, "f1") as ResultDetail.F1
        assertEquals("58", result.totalLaps)
        assertEquals("Max Verstappen", result.winner)
        assertEquals("Red Bull", result.winnerTeam)
        assertEquals("1:32:45.123", result.winnerTime)
        assertEquals("Lando Norris", result.fastestLapDriver)
        assertEquals("1:20.456", result.fastestLapTime)
    }

    @Test
    fun `parses F1 result with minimal data`() {
        val json = """{"type":"f1","winner":"Hamilton"}"""
        val result = ResultDetail.parse(json, "f1") as ResultDetail.F1
        assertEquals("Hamilton", result.winner)
        assertNull(result.totalLaps)
        assertNull(result.winnerTeam)
        assertNull(result.fastestLapDriver)
    }

    // ── Type detection ──────────────────────────────────

    @Test
    fun `uses type field from JSON over sportId`() {
        val json = """{"type":"football","fulltime_home":1,"fulltime_away":0}"""
        val result = ResultDetail.parse(json, "f1") // sportId says f1 but JSON says football
        assertTrue(result is ResultDetail.Football)
    }

    @Test
    fun `falls back to sportId when type field missing`() {
        val json = """{"fulltime_home":1,"fulltime_away":0}"""
        val result = ResultDetail.parse(json, "football")
        assertTrue(result is ResultDetail.Football)
    }

    // ── Unknown type ────────────────────────────────────

    @Test
    fun `unknown type returns Unknown`() {
        val json = """{"type":"cricket","runs":250}"""
        val result = ResultDetail.parse(json, "cricket")
        assertTrue(result is ResultDetail.Unknown)
        assertEquals(json, (result as ResultDetail.Unknown).raw)
    }

    @Test
    fun `unknown sportId with no type returns Unknown`() {
        val json = """{"score":100}"""
        val result = ResultDetail.parse(json, "cricket")
        assertTrue(result is ResultDetail.Unknown)
    }

    // ── Edge cases ──────────────────────────────────────

    @Test
    fun `null input returns null`() {
        assertNull(ResultDetail.parse(null, "football"))
    }

    @Test
    fun `blank input returns null`() {
        assertNull(ResultDetail.parse("", "football"))
    }

    @Test
    fun `whitespace-only input returns null`() {
        assertNull(ResultDetail.parse("   ", "football"))
    }

    @Test
    fun `malformed JSON returns Unknown`() {
        val result = ResultDetail.parse("not json at all", "football")
        assertTrue(result is ResultDetail.Unknown)
    }

    @Test
    fun `partial JSON returns Unknown`() {
        val result = ResultDetail.parse("{broken", "football")
        assertTrue(result is ResultDetail.Unknown)
    }
}
