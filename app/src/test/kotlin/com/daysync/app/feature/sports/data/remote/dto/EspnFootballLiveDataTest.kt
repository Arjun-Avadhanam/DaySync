package com.daysync.app.feature.sports.data.remote.dto

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests ESPN Football mapping with real API data structures:
 * league matches, knockout rounds, goal scorers, half-time scores.
 */
class EspnFootballLiveDataTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }

    // Real ESPN PL match: Sunderland 2-1 Newcastle with goal scorers
    private val leagueMatchJson = """
    {
      "events": [{
        "id": "694461",
        "date": "2026-03-22T12:00:00Z",
        "name": "Sunderland at Newcastle United",
        "shortName": "SUN @ NEW",
        "status": {"type": {"state": "post", "completed": true}},
        "competitions": [{
          "id": "694461",
          "status": {
            "clock": 5400.0, "displayClock": "90'+10'", "period": 2,
            "type": {"state": "post", "completed": true, "detail": "FT"}
          },
          "venue": {"fullName": "St. James' Park", "address": {"city": "Newcastle-upon-Tyne", "country": "England"}},
          "competitors": [
            {
              "homeAway": "home", "winner": false, "score": "1",
              "team": {"id": "361", "displayName": "Newcastle United", "abbreviation": "NEW", "logo": "https://espn.com/new.png"},
              "form": "LLWDL",
              "records": [{"name": "All Splits", "summary": "12-6-13"}],
              "statistics": [{"name": "possessionPct", "displayValue": "59.9"}]
            },
            {
              "homeAway": "away", "winner": true, "score": "2",
              "team": {"id": "366", "displayName": "Sunderland AFC", "abbreviation": "SUN", "logo": "https://espn.com/sun.png"},
              "form": "WLLWD",
              "records": [{"name": "All Splits", "summary": "11-10-10"}],
              "statistics": [{"name": "possessionPct", "displayValue": "40.1"}]
            }
          ],
          "details": [
            {
              "type": {"id": "70", "text": "Goal"},
              "clock": {"value": 559.0, "displayValue": "10'"},
              "team": {"id": "361"},
              "scoringPlay": true,
              "athletesInvolved": [{"displayName": "Anthony Gordon"}]
            },
            {
              "type": {"id": "94", "text": "Yellow Card"},
              "clock": {"value": 2620.0, "displayValue": "44'"},
              "team": {"id": "366"},
              "scoringPlay": false
            },
            {
              "type": {"id": "70", "text": "Goal"},
              "clock": {"value": 3383.0, "displayValue": "57'"},
              "team": {"id": "366"},
              "scoringPlay": true,
              "athletesInvolved": [{"displayName": "Chemsdine Talbi"}]
            },
            {
              "type": {"id": "70", "text": "Goal"},
              "clock": {"value": 5384.0, "displayValue": "90'"},
              "team": {"id": "366"},
              "scoringPlay": true,
              "athletesInvolved": [{"displayName": "Brian Brobbey"}]
            }
          ]
        }],
        "season": {"year": 2026}
      }],
      "leagues": [{"id": "23", "name": "English Premier League"}]
    }
    """.trimIndent()

    // CL knockout: 2nd leg with aggregate note
    private val knockoutMatchJson = """
    {
      "events": [{
        "id": "700100",
        "date": "2026-03-11T20:00:00Z",
        "name": "Galatasaray at Liverpool",
        "shortName": "GAL @ LIV",
        "status": {"type": {"state": "post", "completed": true}},
        "competitions": [{
          "id": "700100",
          "status": {"type": {"state": "post", "completed": true, "detail": "FT"}},
          "venue": {"fullName": "Anfield", "address": {"city": "Liverpool"}},
          "competitors": [
            {
              "homeAway": "home", "winner": true, "score": "3",
              "team": {"id": "364", "displayName": "Liverpool", "abbreviation": "LIV"}
            },
            {
              "homeAway": "away", "winner": false, "score": "0",
              "team": {"id": "450", "displayName": "Galatasaray", "abbreviation": "GAL"}
            }
          ],
          "notes": [{"headline": "2nd Leg - Liverpool advance 4-1 on aggregate"}],
          "details": [
            {
              "type": {"text": "Goal"},
              "clock": {"value": 1200.0, "displayValue": "20'"},
              "team": {"id": "364"},
              "scoringPlay": true,
              "athletesInvolved": [{"displayName": "Mohamed Salah"}],
              "penaltyKick": true
            },
            {
              "type": {"text": "Goal"},
              "clock": {"value": 2700.0, "displayValue": "45'+2'"},
              "team": {"id": "364"},
              "scoringPlay": true,
              "athletesInvolved": [{"displayName": "Virgil van Dijk"}]
            },
            {
              "type": {"text": "Goal"},
              "clock": {"value": 4500.0, "displayValue": "75'"},
              "team": {"id": "364"},
              "scoringPlay": true,
              "athletesInvolved": [{"displayName": "Diogo Jota"}]
            }
          ]
        }],
        "season": {"year": 2026}
      }]
    }
    """.trimIndent()

    // Live match
    private val liveMatchJson = """
    {
      "events": [{
        "id": "700200",
        "date": "2026-03-26T19:45:00Z",
        "name": "Arsenal vs Chelsea",
        "shortName": "ARS vs CHE",
        "status": {"type": {"state": "in", "completed": false}},
        "competitions": [{
          "id": "700200",
          "status": {
            "clock": 2100.0, "displayClock": "35'", "period": 1,
            "type": {"state": "in", "completed": false, "detail": "35'"}
          },
          "competitors": [
            {
              "homeAway": "home", "score": "1",
              "team": {"id": "359", "displayName": "Arsenal", "abbreviation": "ARS"},
              "statistics": [{"name": "possessionPct", "displayValue": "62.3"}]
            },
            {
              "homeAway": "away", "score": "0",
              "team": {"id": "363", "displayName": "Chelsea", "abbreviation": "CHE"},
              "statistics": [{"name": "possessionPct", "displayValue": "37.7"}]
            }
          ],
          "details": [
            {
              "type": {"text": "Goal"},
              "clock": {"value": 1500.0, "displayValue": "25'"},
              "team": {"id": "359"},
              "scoringPlay": true,
              "athletesInvolved": [{"displayName": "Bukayo Saka"}]
            }
          ]
        }],
        "season": {"year": 2026}
      }]
    }
    """.trimIndent()

    // ── Deserialization ─────────────────────────────────────

    @Test
    fun `deserialize goal details with scorers`() {
        val response = json.decodeFromString<EspnScoreboardResponse>(leagueMatchJson)
        val comp = response.events[0].competitions[0]
        val goals = comp.details.filter { it.scoringPlay == true }
        assertEquals(3, goals.size)
        assertEquals("Anthony Gordon", goals[0].athletesInvolved[0].displayName)
        assertEquals("10'", goals[0].clock?.displayValue)
        assertEquals(559.0, goals[0].clock?.value)
    }

    @Test
    fun `deserialize team form and records`() {
        val response = json.decodeFromString<EspnScoreboardResponse>(leagueMatchJson)
        val home = response.events[0].competitions[0].competitors[0]
        assertEquals("LLWDL", home.form)
        assertEquals("12-6-13", home.records.first().summary)
    }

    @Test
    fun `deserialize possession statistics`() {
        val response = json.decodeFromString<EspnScoreboardResponse>(leagueMatchJson)
        val home = response.events[0].competitions[0].competitors[0]
        val poss = home.statistics.first { it.name == "possessionPct" }
        assertEquals("59.9", poss.displayValue)
    }

    @Test
    fun `deserialize knockout aggregate note`() {
        val response = json.decodeFromString<EspnScoreboardResponse>(knockoutMatchJson)
        val note = response.events[0].competitions[0].notes[0].headline
        assertEquals("2nd Leg - Liverpool advance 4-1 on aggregate", note)
    }

    @Test
    fun `deserialize penalty goal flag`() {
        val response = json.decodeFromString<EspnScoreboardResponse>(knockoutMatchJson)
        val goals = response.events[0].competitions[0].details.filter { it.scoringPlay == true }
        assertEquals(true, goals[0].penaltyKick)
        assertNull(goals[1].penaltyKick)
    }

    // ── Match mapping ───────────────────────────────────────

    @Test
    fun `league match maps correctly`() {
        val response = json.decodeFromString<EspnScoreboardResponse>(leagueMatchJson)
        val entity = response.events[0].toFootballMatchEntity("football-pl")
        assertNotNull(entity)
        assertEquals("football", entity!!.sportId)
        assertEquals("football-pl", entity.competitionId)
        assertEquals("COMPLETED", entity.status)
        assertEquals("SUN @ NEW", entity.eventName)
        assertEquals(1, entity.homeScore)
        assertEquals(2, entity.awayScore)
        assertEquals("espn-team-361", entity.homeCompetitorId)
        assertEquals("espn-team-366", entity.awayCompetitorId)
    }

    @Test
    fun `resultDetail contains goal scorers`() {
        val response = json.decodeFromString<EspnScoreboardResponse>(leagueMatchJson)
        val entity = response.events[0].toFootballMatchEntity("football-pl")!!
        val detail = entity.resultDetail!!
        assertTrue(detail.contains("Anthony Gordon"))
        assertTrue(detail.contains("Chemsdine Talbi"))
        assertTrue(detail.contains("Brian Brobbey"))
        assertTrue(detail.contains("\"minute\":\"10'\""))
        assertTrue(detail.contains("\"minute\":\"57'\""))
        assertTrue(detail.contains("\"minute\":\"90'\""))
    }

    @Test
    fun `resultDetail derives halftime score from goals`() {
        val response = json.decodeFromString<EspnScoreboardResponse>(leagueMatchJson)
        val entity = response.events[0].toFootballMatchEntity("football-pl")!!
        val detail = entity.resultDetail!!
        // Gordon scored at 10' (value=559, first half) for home team
        // So HT should be 1-0
        assertTrue(detail.contains("\"halftime_home\":1"))
        assertTrue(detail.contains("\"halftime_away\":0"))
    }

    @Test
    fun `resultDetail contains form`() {
        val response = json.decodeFromString<EspnScoreboardResponse>(leagueMatchJson)
        val entity = response.events[0].toFootballMatchEntity("football-pl")!!
        val detail = entity.resultDetail!!
        assertTrue(detail.contains("\"home_form\":\"LLWDL\""))
        assertTrue(detail.contains("\"away_form\":\"WLLWD\""))
    }

    @Test
    fun `resultDetail contains possession`() {
        val response = json.decodeFromString<EspnScoreboardResponse>(leagueMatchJson)
        val entity = response.events[0].toFootballMatchEntity("football-pl")!!
        assertTrue(entity.resultDetail!!.contains("\"possession_home\":\"59.9\""))
    }

    @Test
    fun `resultDetail contains venue`() {
        val response = json.decodeFromString<EspnScoreboardResponse>(leagueMatchJson)
        val entity = response.events[0].toFootballMatchEntity("football-pl")!!
        assertTrue(entity.resultDetail!!.contains("St. James' Park"))
    }

    @Test
    fun `resultDetail contains records`() {
        val response = json.decodeFromString<EspnScoreboardResponse>(leagueMatchJson)
        val entity = response.events[0].toFootballMatchEntity("football-pl")!!
        assertTrue(entity.resultDetail!!.contains("\"home_record\":\"12-6-13\""))
    }

    // ── Knockout round mapping ──────────────────────────────

    @Test
    fun `knockout match has aggregate note as round`() {
        val response = json.decodeFromString<EspnScoreboardResponse>(knockoutMatchJson)
        val entity = response.events[0].toFootballMatchEntity("football-cl")!!
        assertEquals("2nd Leg - Liverpool advance 4-1 on aggregate", entity.round)
    }

    @Test
    fun `knockout resultDetail contains note`() {
        val response = json.decodeFromString<EspnScoreboardResponse>(knockoutMatchJson)
        val entity = response.events[0].toFootballMatchEntity("football-cl")!!
        assertTrue(entity.resultDetail!!.contains("Liverpool advance 4-1 on aggregate"))
    }

    @Test
    fun `penalty goal flagged in resultDetail`() {
        val response = json.decodeFromString<EspnScoreboardResponse>(knockoutMatchJson)
        val entity = response.events[0].toFootballMatchEntity("football-cl")!!
        assertTrue(entity.resultDetail!!.contains("\"penalty\":true"))
    }

    @Test
    fun `HT score includes added time goals`() {
        val response = json.decodeFromString<EspnScoreboardResponse>(knockoutMatchJson)
        val entity = response.events[0].toFootballMatchEntity("football-cl")!!
        // Goals: 20' (pen, value=1200) and 45'+2' (value=2700) are first half
        // 75' (value=4500) is second half
        // HT should be 2-0
        assertTrue(entity.resultDetail!!.contains("\"halftime_home\":2"))
        assertTrue(entity.resultDetail!!.contains("\"halftime_away\":0"))
    }

    // ── Live match ──────────────────────────────────────────

    @Test
    fun `live match has LIVE status`() {
        val response = json.decodeFromString<EspnScoreboardResponse>(liveMatchJson)
        val entity = response.events[0].toFootballMatchEntity("football-pl")!!
        assertEquals("LIVE", entity.status)
        assertEquals(1, entity.homeScore)
        assertEquals(0, entity.awayScore)
    }

    @Test
    fun `live match has elapsed time in resultDetail`() {
        val response = json.decodeFromString<EspnScoreboardResponse>(liveMatchJson)
        val entity = response.events[0].toFootballMatchEntity("football-pl")!!
        assertTrue(entity.resultDetail!!.contains("\"elapsed\":\"35'\""))
    }

    @Test
    fun `live match has live goal scorer`() {
        val response = json.decodeFromString<EspnScoreboardResponse>(liveMatchJson)
        val entity = response.events[0].toFootballMatchEntity("football-pl")!!
        assertTrue(entity.resultDetail!!.contains("Bukayo Saka"))
    }

    // ── Team mapping ────────────────────────────────────────

    @Test
    fun `football team maps to competitor entity`() {
        val response = json.decodeFromString<EspnScoreboardResponse>(leagueMatchJson)
        val home = response.events[0].competitions[0].competitors[0]
        val entity = home.toFootballTeamEntity()
        assertNotNull(entity)
        assertEquals("espn-team-361", entity!!.id)
        assertEquals("football", entity.sportId)
        assertEquals("Newcastle United", entity.name)
        assertEquals("NEW", entity.shortName)
    }

    // ── ResultDetail parsing ────────────────────────────────

    @Test
    fun `ResultDetail parses football with goals`() {
        val response = json.decodeFromString<EspnScoreboardResponse>(leagueMatchJson)
        val entity = response.events[0].toFootballMatchEntity("football-pl")!!
        val detail = com.daysync.app.feature.sports.data.model.ResultDetail.parse(entity.resultDetail, "football")
        assertTrue(detail is com.daysync.app.feature.sports.data.model.ResultDetail.Football)
        val fb = detail as com.daysync.app.feature.sports.data.model.ResultDetail.Football
        assertEquals(3, fb.goals.size)
        assertEquals("Anthony Gordon", fb.goals[0].scorer)
        assertEquals("10'", fb.goals[0].minute)
        assertTrue(fb.goals[0].isHome)
        assertFalse(fb.goals[1].isHome)
        assertEquals("LLWDL", fb.homeForm)
        assertEquals("59.9", fb.possessionHome)
    }
}
