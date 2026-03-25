package com.daysync.app.feature.sports.data.remote.dto

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests ESPN NBA mapping with real API data structures:
 * regular season, playoffs, live games.
 */
class EspnNbaLiveDataTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }

    // Real ESPN regular season game: DEN 128-112 POR
    private val regularSeasonJson = """
    {
      "events": [{
        "id": "401585600",
        "date": "2026-03-22T21:00:00Z",
        "name": "Portland Trail Blazers at Denver Nuggets",
        "shortName": "POR @ DEN",
        "status": {"type": {"state": "post", "completed": true}},
        "competitions": [{
          "id": "401585600",
          "type": {"id": "1", "abbreviation": "STD"},
          "venue": {"id": "892", "fullName": "Ball Arena", "address": {"city": "Denver", "state": "CO"}},
          "status": {
            "clock": 720.0, "displayClock": "12:00", "period": 4,
            "type": {"state": "post", "completed": true, "detail": "Final"}
          },
          "competitors": [
            {
              "id": "7", "homeAway": "home", "winner": true, "score": "128",
              "team": {"id": "7", "displayName": "Denver Nuggets", "abbreviation": "DEN", "logo": "https://espn.com/den.png"},
              "linescores": [{"value": 42.0}, {"value": 33.0}, {"value": 32.0}, {"value": 21.0}],
              "records": [{"name": "overall", "summary": "44-28"}, {"name": "Home", "summary": "21-13"}]
            },
            {
              "id": "22", "homeAway": "away", "winner": false, "score": "112",
              "team": {"id": "22", "displayName": "Portland Trail Blazers", "abbreviation": "POR", "logo": "https://espn.com/por.png"},
              "linescores": [{"value": 40.0}, {"value": 29.0}, {"value": 24.0}, {"value": 19.0}],
              "records": [{"name": "overall", "summary": "35-37"}]
            }
          ]
        }],
        "season": {"year": 2026}
      }],
      "leagues": [{"id": "46", "name": "NBA", "calendar": ["2025-10-02T07:00Z"]}]
    }
    """.trimIndent()

    // Real ESPN playoff game: MEM @ OKC, 1st Round Game 1
    private val playoffJson = """
    {
      "events": [{
        "id": "401600100",
        "date": "2025-04-20T00:00:00Z",
        "name": "Memphis Grizzlies at Oklahoma City Thunder",
        "shortName": "MEM @ OKC",
        "status": {"type": {"state": "post", "completed": true}},
        "competitions": [{
          "id": "401600100",
          "type": {"id": "14", "abbreviation": "RD16"},
          "venue": {"fullName": "Paycom Center"},
          "status": {
            "period": 4,
            "type": {"state": "post", "completed": true, "detail": "Final"}
          },
          "competitors": [
            {
              "id": "25", "homeAway": "home", "winner": true, "score": "110",
              "team": {"id": "25", "displayName": "Oklahoma City Thunder", "abbreviation": "OKC"},
              "linescores": [{"value": 30.0}, {"value": 28.0}, {"value": 25.0}, {"value": 27.0}],
              "records": [{"name": "overall", "summary": "57-15"}]
            },
            {
              "id": "29", "homeAway": "away", "winner": false, "score": "98",
              "team": {"id": "29", "displayName": "Memphis Grizzlies", "abbreviation": "MEM"},
              "linescores": [{"value": 22.0}, {"value": 24.0}, {"value": 28.0}, {"value": 24.0}],
              "records": [{"name": "overall", "summary": "48-24"}]
            }
          ],
          "series": {
            "type": "playoff",
            "summary": "OKC leads series 1-0",
            "completed": false,
            "totalCompetitions": 4
          },
          "notes": [{"headline": "West 1st Round - Game 1", "type": "event"}]
        }],
        "season": {"year": 2025}
      }]
    }
    """.trimIndent()

    // Simulated live game
    private val liveGameJson = """
    {
      "events": [{
        "id": "401585610",
        "date": "2026-03-25T23:30:00Z",
        "name": "Boston Celtics at Oklahoma City Thunder",
        "shortName": "BOS @ OKC",
        "status": {"type": {"state": "in", "completed": false}},
        "competitions": [{
          "id": "401585610",
          "type": {"abbreviation": "STD"},
          "status": {
            "clock": 342.0, "displayClock": "5:42", "period": 3,
            "type": {"state": "in", "completed": false, "detail": "5:42 - 3rd Quarter"}
          },
          "competitors": [
            {
              "id": "25", "homeAway": "home", "winner": false, "score": "68",
              "team": {"id": "25", "displayName": "Oklahoma City Thunder", "abbreviation": "OKC"},
              "linescores": [{"value": 28.0}, {"value": 22.0}, {"value": 18.0}],
              "records": [{"name": "overall", "summary": "57-15"}]
            },
            {
              "id": "2", "homeAway": "away", "winner": false, "score": "71",
              "team": {"id": "2", "displayName": "Boston Celtics", "abbreviation": "BOS"},
              "linescores": [{"value": 25.0}, {"value": 30.0}, {"value": 16.0}],
              "records": [{"name": "overall", "summary": "47-24"}]
            }
          ]
        }],
        "season": {"year": 2026}
      }]
    }
    """.trimIndent()

    // NBA Finals
    private val finalsJson = """
    {
      "events": [{
        "id": "401700001",
        "date": "2025-06-05T00:00:00Z",
        "name": "Indiana Pacers at Oklahoma City Thunder",
        "shortName": "IND @ OKC",
        "status": {"type": {"state": "post", "completed": true}},
        "competitions": [{
          "id": "401700001",
          "type": {"abbreviation": "FINAL"},
          "status": {"type": {"state": "post", "completed": true}},
          "competitors": [
            {"id": "25", "homeAway": "home", "winner": false, "score": "95",
             "team": {"id": "25", "displayName": "Oklahoma City Thunder", "abbreviation": "OKC"},
             "linescores": [{"value": 20.0}, {"value": 25.0}, {"value": 22.0}, {"value": 28.0}]},
            {"id": "11", "homeAway": "away", "winner": true, "score": "102",
             "team": {"id": "11", "displayName": "Indiana Pacers", "abbreviation": "IND"},
             "linescores": [{"value": 28.0}, {"value": 22.0}, {"value": 25.0}, {"value": 27.0}]}
          ],
          "series": {"type": "playoff", "summary": "IND leads series 1-0", "completed": false, "totalCompetitions": 7},
          "notes": [{"headline": "NBA Finals - Game 1"}]
        }],
        "season": {"year": 2025}
      }]
    }
    """.trimIndent()

    // ── Deserialization tests ───────────────────────────────

    @Test
    fun `deserialize regular season game`() {
        val response = json.decodeFromString<EspnScoreboardResponse>(regularSeasonJson)
        assertEquals(1, response.events.size)
        val comp = response.events[0].competitions[0]
        assertEquals("STD", comp.type?.abbreviation)
        assertNull(comp.series)
    }

    @Test
    fun `deserialize quarter scores`() {
        val response = json.decodeFromString<EspnScoreboardResponse>(regularSeasonJson)
        val home = response.events[0].competitions[0].competitors[0]
        assertEquals(4, home.linescores.size)
        assertEquals(42.0, home.linescores[0].value)
        assertEquals(21.0, home.linescores[3].value)
    }

    @Test
    fun `deserialize team records`() {
        val response = json.decodeFromString<EspnScoreboardResponse>(regularSeasonJson)
        val home = response.events[0].competitions[0].competitors[0]
        assertEquals("44-28", home.records.first { it.name == "overall" }.summary)
    }

    @Test
    fun `deserialize playoff series`() {
        val response = json.decodeFromString<EspnScoreboardResponse>(playoffJson)
        val series = response.events[0].competitions[0].series
        assertNotNull(series)
        assertEquals("OKC leads series 1-0", series!!.summary)
        assertEquals(4, series.totalCompetitions)
        assertEquals(false, series.completed)
    }

    @Test
    fun `deserialize playoff notes`() {
        val response = json.decodeFromString<EspnScoreboardResponse>(playoffJson)
        val notes = response.events[0].competitions[0].notes
        assertEquals("West 1st Round - Game 1", notes[0].headline)
    }

    // ── Regular season mapping ──────────────────────────────

    @Test
    fun `regular season game maps correctly`() {
        val response = json.decodeFromString<EspnScoreboardResponse>(regularSeasonJson)
        val entity = response.events[0].toNbaGameEntity("basketball-nba")
        assertNotNull(entity)
        assertEquals("basketball", entity!!.sportId)
        assertEquals("basketball-nba", entity.competitionId)
        assertEquals("COMPLETED", entity.status)
        assertEquals("POR @ DEN", entity.eventName)
        assertEquals(128, entity.homeScore)
        assertEquals(112, entity.awayScore)
        assertEquals("espn-team-7", entity.homeCompetitorId)
        assertEquals("espn-team-22", entity.awayCompetitorId)
        assertNull(entity.round) // No round for regular season
    }

    @Test
    fun `regular season resultDetail has quarter scores`() {
        val response = json.decodeFromString<EspnScoreboardResponse>(regularSeasonJson)
        val entity = response.events[0].toNbaGameEntity("basketball-nba")!!
        val detail = entity.resultDetail!!
        assertTrue(detail.contains("\"home_quarters\":[42,33,32,21]"))
        assertTrue(detail.contains("\"away_quarters\":[40,29,24,19]"))
    }

    @Test
    fun `regular season resultDetail has records`() {
        val response = json.decodeFromString<EspnScoreboardResponse>(regularSeasonJson)
        val entity = response.events[0].toNbaGameEntity("basketball-nba")!!
        val detail = entity.resultDetail!!
        assertTrue(detail.contains("\"home_record\":\"44-28\""))
        assertTrue(detail.contains("\"away_record\":\"35-37\""))
    }

    @Test
    fun `regular season not postseason`() {
        val response = json.decodeFromString<EspnScoreboardResponse>(regularSeasonJson)
        val entity = response.events[0].toNbaGameEntity("basketball-nba")!!
        assertTrue(entity.resultDetail!!.contains("\"is_postseason\":false"))
    }

    @Test
    fun `regular season has venue`() {
        val response = json.decodeFromString<EspnScoreboardResponse>(regularSeasonJson)
        val entity = response.events[0].toNbaGameEntity("basketball-nba")!!
        assertTrue(entity.resultDetail!!.contains("\"venue\":\"Ball Arena\""))
    }

    // ── Playoff mapping ─────────────────────────────────────

    @Test
    fun `playoff game maps with round label from notes`() {
        val response = json.decodeFromString<EspnScoreboardResponse>(playoffJson)
        val entity = response.events[0].toNbaGameEntity("basketball-nba")!!
        assertEquals("West 1st Round - Game 1", entity.round)
    }

    @Test
    fun `playoff resultDetail has series info`() {
        val response = json.decodeFromString<EspnScoreboardResponse>(playoffJson)
        val entity = response.events[0].toNbaGameEntity("basketball-nba")!!
        val detail = entity.resultDetail!!
        assertTrue(detail.contains("\"is_postseason\":true"))
        assertTrue(detail.contains("\"playoff_label\":\"West 1st Round - Game 1\""))
        assertTrue(detail.contains("\"series_summary\":\"OKC leads series 1-0\""))
        assertTrue(detail.contains("\"series_total_games\":4"))
    }

    @Test
    fun `NBA Finals maps correctly`() {
        val response = json.decodeFromString<EspnScoreboardResponse>(finalsJson)
        val entity = response.events[0].toNbaGameEntity("basketball-nba")!!
        assertEquals("NBA Finals - Game 1", entity.round)
        assertTrue(entity.resultDetail!!.contains("\"series_total_games\":7"))
    }

    // ── Live game mapping ───────────────────────────────────

    @Test
    fun `live game has LIVE status`() {
        val response = json.decodeFromString<EspnScoreboardResponse>(liveGameJson)
        val entity = response.events[0].toNbaGameEntity("basketball-nba")!!
        assertEquals("LIVE", entity.status)
        assertEquals(68, entity.homeScore)
        assertEquals(71, entity.awayScore)
    }

    @Test
    fun `live game resultDetail has current period and clock`() {
        val response = json.decodeFromString<EspnScoreboardResponse>(liveGameJson)
        val entity = response.events[0].toNbaGameEntity("basketball-nba")!!
        val detail = entity.resultDetail!!
        assertTrue(detail.contains("\"current_period\":3"))
        assertTrue(detail.contains("\"game_clock\":\"5:42\""))
    }

    @Test
    fun `live game has partial quarter scores`() {
        val response = json.decodeFromString<EspnScoreboardResponse>(liveGameJson)
        val entity = response.events[0].toNbaGameEntity("basketball-nba")!!
        val detail = entity.resultDetail!!
        assertTrue(detail.contains("\"home_quarters\":[28,22,18]")) // Only 3 quarters so far
    }

    // ── Team competitor mapping ─────────────────────────────

    @Test
    fun `NBA team maps to competitor entity`() {
        val response = json.decodeFromString<EspnScoreboardResponse>(regularSeasonJson)
        val home = response.events[0].competitions[0].competitors[0]
        val entity = home.toNbaTeamEntity()
        assertNotNull(entity)
        assertEquals("espn-team-7", entity!!.id)
        assertEquals("basketball", entity.sportId)
        assertEquals("Denver Nuggets", entity.name)
        assertEquals("DEN", entity.shortName)
        assertEquals("https://espn.com/den.png", entity.logoUrl)
    }

    // ── ResultDetail parsing ────────────────────────────────

    @Test
    fun `ResultDetail parses basketball type`() {
        val response = json.decodeFromString<EspnScoreboardResponse>(regularSeasonJson)
        val entity = response.events[0].toNbaGameEntity("basketball-nba")!!
        val detail = com.daysync.app.feature.sports.data.model.ResultDetail.parse(entity.resultDetail, "basketball")
        assertTrue(detail is com.daysync.app.feature.sports.data.model.ResultDetail.Basketball)
        val bb = detail as com.daysync.app.feature.sports.data.model.ResultDetail.Basketball
        assertEquals(listOf(42, 33, 32, 21), bb.homeQuarters)
        assertEquals(listOf(40, 29, 24, 19), bb.awayQuarters)
        assertEquals("44-28", bb.homeRecord)
        assertFalse(bb.isPostseason)
    }

    @Test
    fun `ResultDetail parses playoff basketball`() {
        val response = json.decodeFromString<EspnScoreboardResponse>(playoffJson)
        val entity = response.events[0].toNbaGameEntity("basketball-nba")!!
        val detail = com.daysync.app.feature.sports.data.model.ResultDetail.parse(entity.resultDetail, "basketball")
        val bb = detail as com.daysync.app.feature.sports.data.model.ResultDetail.Basketball
        assertTrue(bb.isPostseason)
        assertEquals("West 1st Round - Game 1", bb.playoffLabel)
        assertEquals("OKC leads series 1-0", bb.seriesSummary)
        assertEquals(4, bb.seriesTotalGames)
    }
}
