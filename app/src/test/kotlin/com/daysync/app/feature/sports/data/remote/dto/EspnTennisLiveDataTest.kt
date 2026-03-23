package com.daysync.app.feature.sports.data.remote.dto

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests against realistic ESPN Tennis JSON to verify end-to-end
 * deserialization + mapping with actual API data structure.
 */
class EspnTennisLiveDataTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }

    // Real structure: BNP Paribas Open Final - Sinner vs Medvedev
    private val tournamentJson = """
    {
      "events": [{
        "id": "411-2026",
        "date": "2026-03-02T04:00Z",
        "endDate": "2026-03-16T03:59Z",
        "name": "BNP Paribas Open",
        "shortName": "BNP Paribas Open",
        "major": false,
        "status": {"type": {"state": "post", "completed": true}},
        "venue": {"displayName": "Indian Wells, USA"},
        "groupings": [
          {
            "grouping": {"id": "1", "slug": "mens-singles", "displayName": "Men's Singles"},
            "competitions": [
              {
                "id": "172941",
                "date": "2026-03-15T21:25Z",
                "status": {
                  "period": 2,
                  "type": {"state": "post", "completed": true, "detail": "Final"}
                },
                "venue": {"fullName": "Indian Wells, USA", "court": "Stadium 1"},
                "format": {"regulation": {"periods": 3}},
                "round": {"id": 7, "displayName": "Final"},
                "notes": [{"text": "(2) Jannik Sinner (ITA) bt (11) Daniil Medvedev (RUS) 7-6 (8-6) 7-6 (7-4)", "type": "event"}],
                "competitors": [
                  {
                    "id": "3623",
                    "winner": true,
                    "curatedRank": {"current": 2},
                    "linescores": [
                      {"value": 7.0, "tiebreak": 8, "winner": true},
                      {"value": 7.0, "tiebreak": 7, "winner": true}
                    ],
                    "athlete": {
                      "displayName": "Jannik Sinner",
                      "shortName": "J. Sinner",
                      "flag": {"alt": "Italy", "href": "https://flags/ita.png"}
                    }
                  },
                  {
                    "id": "2383",
                    "winner": false,
                    "curatedRank": {"current": 11},
                    "linescores": [
                      {"value": 6.0, "tiebreak": 6, "winner": false},
                      {"value": 6.0, "tiebreak": 4, "winner": false}
                    ],
                    "athlete": {
                      "displayName": "Daniil Medvedev",
                      "shortName": "D. Medvedev",
                      "flag": {"alt": "Russia"}
                    }
                  }
                ]
              },
              {
                "id": "172940",
                "date": "2026-03-14T22:30Z",
                "status": {"period": 2, "type": {"state": "post", "completed": true}},
                "format": {"regulation": {"periods": 3}},
                "round": {"id": 6, "displayName": "Semifinal"},
                "notes": [{"text": "(11) Medvedev bt (1) Alcaraz 6-3 7-6 (7-3)"}],
                "competitors": [
                  {"id": "2383", "winner": true, "curatedRank": {"current": 11},
                   "linescores": [{"value": 6.0}, {"value": 7.0, "tiebreak": 7}],
                   "athlete": {"displayName": "Daniil Medvedev", "shortName": "D. Medvedev", "flag": {"alt": "Russia"}}},
                  {"id": "4602", "winner": false, "curatedRank": {"current": 1},
                   "linescores": [{"value": 3.0}, {"value": 6.0, "tiebreak": 3}],
                   "athlete": {"displayName": "Carlos Alcaraz", "shortName": "C. Alcaraz", "flag": {"alt": "Spain"}}}
                ]
              },
              {
                "id": "172100",
                "date": "2026-03-11T18:00Z",
                "status": {"type": {"state": "post", "completed": true}},
                "format": {"regulation": {"periods": 3}},
                "round": {"id": 2, "displayName": "Round 2"},
                "competitors": [
                  {"id": "1001", "winner": true, "athlete": {"displayName": "Player A"}},
                  {"id": "1002", "winner": false, "athlete": {"displayName": "Player B"}}
                ]
              }
            ]
          },
          {
            "grouping": {"id": "2", "slug": "womens-singles", "displayName": "Women's Singles"},
            "competitions": [
              {
                "id": "172815",
                "date": "2026-03-15T18:05Z",
                "status": {"period": 3, "type": {"state": "post", "completed": true}},
                "format": {"regulation": {"periods": 3}},
                "round": {"id": 7, "displayName": "Final"},
                "notes": [{"text": "(1) Sabalenka bt (3) Rybakina 3-6 6-3 7-6 (8-6)"}],
                "competitors": [
                  {"id": "3126", "winner": false, "curatedRank": {"current": 3},
                   "linescores": [{"value": 6.0}, {"value": 3.0}, {"value": 6.0, "tiebreak": 6}],
                   "athlete": {"displayName": "Elena Rybakina", "shortName": "E. Rybakina", "flag": {"alt": "Kazakhstan"}}},
                  {"id": "4001", "winner": true, "curatedRank": {"current": 1},
                   "linescores": [{"value": 3.0}, {"value": 6.0}, {"value": 7.0, "tiebreak": 8}],
                   "athlete": {"displayName": "Aryna Sabalenka", "shortName": "A. Sabalenka", "flag": {"alt": "Belarus"}}}
                ]
              }
            ]
          },
          {
            "grouping": {"id": "3", "slug": "mens-doubles", "displayName": "Men's Doubles"},
            "competitions": [
              {
                "id": "199999",
                "date": "2026-03-15T16:00Z",
                "round": {"id": 7, "displayName": "Final"},
                "competitors": [
                  {"id": "9001", "athlete": {"displayName": "Team A"}},
                  {"id": "9002", "athlete": {"displayName": "Team B"}}
                ]
              }
            ]
          }
        ],
        "season": {"year": 2026}
      }],
      "leagues": [{"id": "851", "name": "ATP", "calendar": ["2026-03-01T08:00Z", "2026-03-02T08:00Z"]}]
    }
    """.trimIndent()

    @Test
    fun `deserialize tennis tournament JSON`() {
        val response = json.decodeFromString<EspnScoreboardResponse>(tournamentJson)
        assertEquals(1, response.events.size)
        assertEquals("411-2026", response.events[0].id)
        assertEquals(false, response.events[0].major)
        assertEquals("Indian Wells, USA", response.events[0].venue?.displayName)
    }

    @Test
    fun `deserialize string-based calendar without crash`() {
        val response = json.decodeFromString<EspnScoreboardResponse>(tournamentJson)
        val calendar = response.leagues[0].getCalendarEntries()
        assertEquals(2, calendar.size)
        assertEquals("2026-03-01T08:00Z", calendar[0].startDate)
        assertNull(calendar[0].label) // String calendar has no label
    }

    @Test
    fun `deserialize groupings with correct structure`() {
        val response = json.decodeFromString<EspnScoreboardResponse>(tournamentJson)
        val event = response.events[0]
        assertEquals(3, event.groupings.size)
        assertEquals("mens-singles", event.groupings[0].grouping?.slug)
        assertEquals("womens-singles", event.groupings[1].grouping?.slug)
        assertEquals("mens-doubles", event.groupings[2].grouping?.slug)
    }

    @Test
    fun `deserialize match with set scores and tiebreaks`() {
        val response = json.decodeFromString<EspnScoreboardResponse>(tournamentJson)
        val final = response.events[0].groupings[0].competitions[0]
        val sinner = final.competitors[0]
        assertEquals(2, sinner.linescores.size)
        assertEquals(7.0, sinner.linescores[0].value)
        assertEquals(8, sinner.linescores[0].tiebreak)
        assertEquals(true, sinner.linescores[0].winner)
    }

    @Test
    fun `deserialize round info`() {
        val response = json.decodeFromString<EspnScoreboardResponse>(tournamentJson)
        val final = response.events[0].groupings[0].competitions[0]
        assertEquals(7, final.round?.id)
        assertEquals("Final", final.round?.displayName)
    }

    @Test
    fun `deserialize notes with result summary`() {
        val response = json.decodeFromString<EspnScoreboardResponse>(tournamentJson)
        val final = response.events[0].groupings[0].competitions[0]
        assertEquals("(2) Jannik Sinner (ITA) bt (11) Daniil Medvedev (RUS) 7-6 (8-6) 7-6 (7-4)", final.notes[0].text)
    }

    // ── Mapper tests ────────────────────────────────────

    @Test
    fun `maps only singles matches, filters doubles`() {
        val response = json.decodeFromString<EspnScoreboardResponse>(tournamentJson)
        val matches = response.events[0].toTennisMatchEntities("tennis-atp")
        // Doubles match should be excluded
        assertTrue(matches.none { it.event.eventName?.contains("Team") == true })
    }

    @Test
    fun `filters out rounds below Round 3`() {
        val response = json.decodeFromString<EspnScoreboardResponse>(tournamentJson)
        val matches = response.events[0].toTennisMatchEntities("tennis-atp")
        // Round 2 match (id=172100, round.id=2) should be excluded
        assertTrue(matches.none { it.event.id == "espn-match-172100" })
    }

    @Test
    fun `includes men's and women's singles finals`() {
        val response = json.decodeFromString<EspnScoreboardResponse>(tournamentJson)
        val matches = response.events[0].toTennisMatchEntities("tennis-atp")
        val names = matches.map { it.event.eventName }
        assertTrue(names.any { it?.contains("Sinner") == true })
        assertTrue(names.any { it?.contains("Sabalenka") == true || it?.contains("Rybakina") == true })
    }

    @Test
    fun `maps final match correctly`() {
        val response = json.decodeFromString<EspnScoreboardResponse>(tournamentJson)
        val matches = response.events[0].toTennisMatchEntities("tennis-atp")
        val final = matches.first { it.event.id == "espn-match-172941" }

        assertEquals("Jannik Sinner vs Daniil Medvedev", final.event.eventName)
        assertEquals("Final", final.event.round)
        assertEquals("BNP Paribas Open", final.event.season)
        assertEquals("COMPLETED", final.event.status)
        assertEquals("tennis", final.event.sportId)
        assertEquals("espn", final.event.dataSource)
    }

    @Test
    fun `maps player competitor entities`() {
        val response = json.decodeFromString<EspnScoreboardResponse>(tournamentJson)
        val matches = response.events[0].toTennisMatchEntities("tennis-atp")
        val final = matches.first { it.event.id == "espn-match-172941" }

        assertEquals("espn-player-3623", final.player1.id)
        assertEquals("Jannik Sinner", final.player1.name)
        assertEquals("J. Sinner", final.player1.shortName)
        assertEquals("Italy", final.player1.country)
        assertTrue(final.player1.isIndividual)

        assertEquals("espn-player-2383", final.player2.id)
        assertEquals("Daniil Medvedev", final.player2.name)
    }

    @Test
    fun `resultDetail contains set scores and tiebreaks`() {
        val response = json.decodeFromString<EspnScoreboardResponse>(tournamentJson)
        val matches = response.events[0].toTennisMatchEntities("tennis-atp")
        val final = matches.first { it.event.id == "espn-match-172941" }
        val detail = final.event.resultDetail!!

        assertTrue(detail.contains("\"player1_sets\":[7,7]"))
        assertTrue(detail.contains("\"player2_sets\":[6,6]"))
        assertTrue(detail.contains("\"tiebreaks\":[[8,6],[7,4]]"))
    }

    @Test
    fun `resultDetail contains winner and result note`() {
        val response = json.decodeFromString<EspnScoreboardResponse>(tournamentJson)
        val matches = response.events[0].toTennisMatchEntities("tennis-atp")
        val final = matches.first { it.event.id == "espn-match-172941" }
        val detail = final.event.resultDetail!!

        assertTrue(detail.contains("\"winner\":\"Jannik Sinner\""))
        assertTrue(detail.contains("Sinner (ITA) bt"))
    }

    @Test
    fun `resultDetail contains ranks`() {
        val response = json.decodeFromString<EspnScoreboardResponse>(tournamentJson)
        val matches = response.events[0].toTennisMatchEntities("tennis-atp")
        val final = matches.first { it.event.id == "espn-match-172941" }
        val detail = final.event.resultDetail!!

        assertTrue(detail.contains("\"player1_rank\":2"))
        assertTrue(detail.contains("\"player2_rank\":11"))
    }

    @Test
    fun `resultDetail contains tournament metadata`() {
        val response = json.decodeFromString<EspnScoreboardResponse>(tournamentJson)
        val matches = response.events[0].toTennisMatchEntities("tennis-atp")
        val final = matches.first { it.event.id == "espn-match-172941" }
        val detail = final.event.resultDetail!!

        assertTrue(detail.contains("\"tournament\":\"BNP Paribas Open\""))
        assertTrue(detail.contains("\"is_grand_slam\":false"))
        assertTrue(detail.contains("\"round\":\"Final\""))
        assertTrue(detail.contains("\"best_of\":3"))
        assertTrue(detail.contains("\"court\":\"Stadium 1\""))
        assertTrue(detail.contains("\"draw\":\"Men's Singles\""))
    }

    @Test
    fun `women's singles final maps correctly`() {
        val response = json.decodeFromString<EspnScoreboardResponse>(tournamentJson)
        val matches = response.events[0].toTennisMatchEntities("tennis-atp")
        val womensFinal = matches.first { it.event.id == "espn-match-172815" }

        assertEquals("Elena Rybakina vs Aryna Sabalenka", womensFinal.event.eventName)
        val detail = womensFinal.event.resultDetail!!
        assertTrue(detail.contains("\"draw\":\"Women's Singles\""))
        assertTrue(detail.contains("\"player1_sets\":[6,3,6]"))
        assertTrue(detail.contains("\"player2_sets\":[3,6,7]"))
    }

    @Test
    fun `semifinal match included in results`() {
        val response = json.decodeFromString<EspnScoreboardResponse>(tournamentJson)
        val matches = response.events[0].toTennisMatchEntities("tennis-atp")
        val semi = matches.firstOrNull { it.event.id == "espn-match-172940" }
        assertNotNull("Semifinal should be included (round id 6 >= 3)", semi)
        assertEquals("Semifinal", semi!!.event.round)
    }
}
