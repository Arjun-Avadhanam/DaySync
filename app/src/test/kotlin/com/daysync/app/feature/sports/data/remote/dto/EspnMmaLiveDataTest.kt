package com.daysync.app.feature.sports.data.remote.dto

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests against realistic ESPN MMA JSON to verify end-to-end
 * deserialization + mapping works with actual API data structure.
 */
class EspnMmaLiveDataTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }

    // Minimal but structurally accurate ESPN MMA JSON
    private val completedCardJson = """
    {
      "events": [{
        "id": "600057365",
        "date": "2026-03-21T17:00Z",
        "name": "UFC Fight Night: Evloev vs. Murphy",
        "shortName": "UFC Fight Night",
        "status": {
          "type": {"state": "post", "completed": true}
        },
        "competitions": [
          {
            "id": "401859204",
            "date": "2026-03-21T17:00Z",
            "type": {"abbreviation": "W Strawweight"},
            "format": {"regulation": {"periods": 3}},
            "status": {
              "clock": 77.0,
              "displayClock": "1:17",
              "period": 2,
              "type": {"state": "post", "completed": true}
            },
            "competitors": [
              {
                "id": "5157674",
                "winner": true,
                "athlete": {
                  "displayName": "Shanelle Dyer",
                  "shortName": "S. Dyer",
                  "flag": {"alt": "England"}
                },
                "records": [{"name": "overall", "summary": "7-1-0"}]
              },
              {
                "id": "5159959",
                "winner": false,
                "athlete": {
                  "displayName": "Ravena Oliveira",
                  "shortName": "R. Oliveira",
                  "flag": {"alt": "Brazil"}
                },
                "records": [{"name": "overall", "summary": "7-4-1"}]
              }
            ],
            "details": [
              {"type": {"id": "21", "text": "Unofficial Winner Kotko"}},
              {"type": {"id": "23", "text": "Results"}}
            ],
            "venue": {"id": "2590", "fullName": "O2 Arena (ENG)"}
          },
          {
            "id": "401838357",
            "date": "2026-03-21T20:00Z",
            "type": {"abbreviation": "Featherweight"},
            "format": {"regulation": {"periods": 5}},
            "status": {
              "clock": 300.0,
              "displayClock": "5:00",
              "period": 5,
              "type": {"state": "post", "completed": true}
            },
            "competitors": [
              {
                "id": "4029275",
                "winner": true,
                "athlete": {
                  "displayName": "Movsar Evloev",
                  "shortName": "M. Evloev",
                  "flag": {"alt": "Russia"}
                },
                "records": [{"name": "overall", "summary": "20-0-0"}]
              },
              {
                "id": "4576101",
                "winner": false,
                "athlete": {
                  "displayName": "Lerone Murphy",
                  "shortName": "L. Murphy",
                  "flag": {"alt": "England"}
                },
                "records": [{"name": "overall", "summary": "17-1-1"}]
              }
            ],
            "details": [
              {"type": {"id": "22", "text": "Unofficial Winner Decision"}}
            ]
          }
        ],
        "season": {"year": 2026}
      }],
      "leagues": [{
        "id": "3321",
        "name": "UFC",
        "calendar": [
          {"label": "UFC 326: Holloway vs. Oliveira 2", "startDate": "2026-03-08T01:30Z"},
          {"label": "UFC Fight Night: Evloev vs. Murphy", "startDate": "2026-03-21T20:00Z"},
          {"label": "UFC Fight Night: Adesanya vs. Pyfer", "startDate": "2026-03-29T00:00Z"},
          {"label": "UFC 327: Procházka vs. Ulberg", "startDate": "2026-04-12T00:00Z"}
        ]
      }]
    }
    """.trimIndent()

    private val scheduledCardJson = """
    {
      "events": [{
        "id": "600057366",
        "date": "2026-03-28T21:00Z",
        "name": "UFC Fight Night: Adesanya vs. Pyfer",
        "shortName": "UFC Fight Night",
        "status": {
          "type": {"state": "pre", "completed": false}
        },
        "competitions": [
          {
            "id": "401860014",
            "date": "2026-03-29T01:00Z",
            "type": {"abbreviation": "Middleweight"},
            "format": {"regulation": {"periods": 5}},
            "status": {"type": {"state": "pre", "completed": false}},
            "competitors": [
              {
                "id": "2001",
                "athlete": {"displayName": "Israel Adesanya", "shortName": "I. Adesanya"},
                "records": [{"summary": "24-4-0"}]
              },
              {
                "id": "2002",
                "athlete": {"displayName": "Joe Pyfer", "shortName": "J. Pyfer"},
                "records": [{"summary": "14-3-0"}]
              }
            ]
          }
        ]
      }],
      "leagues": []
    }
    """.trimIndent()

    @Test
    fun `deserialize completed card JSON`() {
        val response = json.decodeFromString<EspnScoreboardResponse>(completedCardJson)
        assertEquals(1, response.events.size)
        assertEquals(2, response.events[0].competitions.size)
        assertEquals("600057365", response.events[0].id)
    }

    @Test
    fun `deserialize calendar entries`() {
        val response = json.decodeFromString<EspnScoreboardResponse>(completedCardJson)
        val calendar = response.leagues[0].getCalendarEntries()
        assertEquals(4, calendar.size)
        assertEquals("UFC 326: Holloway vs. Oliveira 2", calendar[0].label)
        assertEquals("2026-03-08T01:30Z", calendar[0].startDate)
    }

    @Test
    fun `deserialize fighter athlete data`() {
        val response = json.decodeFromString<EspnScoreboardResponse>(completedCardJson)
        val fighter = response.events[0].competitions[1].competitors[0]
        assertEquals("4029275", fighter.id)
        assertEquals(true, fighter.winner)
        assertEquals("Movsar Evloev", fighter.athlete?.displayName)
        assertEquals("Russia", fighter.athlete?.flag?.alt)
        assertEquals("20-0-0", fighter.records[0].summary)
    }

    @Test
    fun `deserialize fight status with clock and period`() {
        val response = json.decodeFromString<EspnScoreboardResponse>(completedCardJson)
        val status = response.events[0].competitions[0].status
        assertEquals(77.0, status?.clock)
        assertEquals("1:17", status?.displayClock)
        assertEquals(2, status?.period)
    }

    @Test
    fun `map completed card to fight entities`() {
        val response = json.decodeFromString<EspnScoreboardResponse>(completedCardJson)
        val fights = response.events[0].toMmaFightEntities("mma-ufc")
        assertEquals(2, fights.size)
    }

    @Test
    fun `mapped fight has correct fighter names`() {
        val response = json.decodeFromString<EspnScoreboardResponse>(completedCardJson)
        val fights = response.events[0].toMmaFightEntities("mma-ufc")
        assertEquals("Shanelle Dyer vs Ravena Oliveira", fights[0].event.eventName)
        assertEquals("Movsar Evloev vs Lerone Murphy", fights[1].event.eventName)
    }

    @Test
    fun `mapped fight has correct result details`() {
        val response = json.decodeFromString<EspnScoreboardResponse>(completedCardJson)
        val fights = response.events[0].toMmaFightEntities("mma-ufc")
        val mainEvent = fights[1].event.resultDetail!!
        assertTrue(mainEvent.contains("\"method\":\"Decision\""))
        assertTrue(mainEvent.contains("\"ended_round\":5"))
        assertTrue(mainEvent.contains("\"ended_time\":\"5:00\""))
        assertTrue(mainEvent.contains("\"is_championship\":true"))
        assertTrue(mainEvent.contains("\"fighter1_record\":\"20-0-0\""))
    }

    @Test
    fun `mapped undercard fight has KO method and correct round`() {
        val response = json.decodeFromString<EspnScoreboardResponse>(completedCardJson)
        val fights = response.events[0].toMmaFightEntities("mma-ufc")
        val undercard = fights[0].event.resultDetail!!
        assertTrue(undercard.contains("\"method\":\"Kotko\""))
        assertTrue(undercard.contains("\"ended_round\":2"))
        assertTrue(undercard.contains("\"ended_time\":\"1:17\""))
        assertTrue(undercard.contains("\"is_championship\":false"))
    }

    @Test
    fun `map scheduled card to fight entities`() {
        val response = json.decodeFromString<EspnScoreboardResponse>(scheduledCardJson)
        val fights = response.events[0].toMmaFightEntities("mma-ufc")
        assertEquals(1, fights.size)
        assertEquals("SCHEDULED", fights[0].event.status)
        assertEquals("Israel Adesanya vs Joe Pyfer", fights[0].event.eventName)
        assertEquals("Middleweight", fights[0].event.round)
    }

    @Test
    fun `scheduled fight has no method in resultDetail`() {
        val response = json.decodeFromString<EspnScoreboardResponse>(scheduledCardJson)
        val fights = response.events[0].toMmaFightEntities("mma-ufc")
        val detail = fights[0].event.resultDetail!!
        assertTrue(!detail.contains("method"))
        assertTrue(!detail.contains("ended_round"))
        assertTrue(detail.contains("\"fighter1_record\":\"24-4-0\""))
    }

    @Test
    fun `fighter competitor entities have correct fields`() {
        val response = json.decodeFromString<EspnScoreboardResponse>(completedCardJson)
        val fights = response.events[0].toMmaFightEntities("mma-ufc")
        val evloev = fights[1].fighter1
        assertEquals("espn-fighter-4029275", evloev.id)
        assertEquals("mma", evloev.sportId)
        assertEquals("Movsar Evloev", evloev.name)
        assertEquals("M. Evloev", evloev.shortName)
        assertEquals("Russia", evloev.country)
        assertTrue(evloev.isIndividual)
    }

    @Test
    fun `fight event references correct fighter IDs`() {
        val response = json.decodeFromString<EspnScoreboardResponse>(completedCardJson)
        val fights = response.events[0].toMmaFightEntities("mma-ufc")
        val mainEvent = fights[1].event
        assertEquals("espn-fighter-4029275", mainEvent.homeCompetitorId)
        assertEquals("espn-fighter-4576101", mainEvent.awayCompetitorId)
    }

    @Test
    fun `fights are in correct order and main event is last`() {
        val response = json.decodeFromString<EspnScoreboardResponse>(completedCardJson)
        val fights = response.events[0].toMmaFightEntities("mma-ufc")
        assertTrue(fights[0].event.resultDetail!!.contains("\"is_main_event\":false"))
        assertTrue(fights[1].event.resultDetail!!.contains("\"is_main_event\":true"))
    }
}
