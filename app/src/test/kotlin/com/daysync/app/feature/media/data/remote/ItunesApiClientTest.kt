package com.daysync.app.feature.media.data.remote

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests iTunes API response parsing.
 * iTunes returns text/javascript content type, not application/json,
 * so the client uses bodyAsText() + manual Json.decodeFromString().
 */
class ItunesApiClientTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    // Real iTunes music album response structure
    private val musicResponseJson = """
    {
      "resultCount": 3,
      "results": [
        {
          "wrapperType": "collection",
          "collectionType": "Album",
          "artistName": "Pink Floyd",
          "collectionName": "The Wall",
          "collectionId": 1065975633,
          "artworkUrl100": "https://is1-ssl.mzstatic.com/image/thumb/Music125/v4/test/100x100bb.jpg",
          "releaseDate": "1979-11-30T08:00:00Z"
        },
        {
          "wrapperType": "collection",
          "collectionType": "Album",
          "artistName": "Pink Floyd",
          "collectionName": "Wish You Were Here",
          "collectionId": 1065976000,
          "artworkUrl100": "https://is1-ssl.mzstatic.com/image/thumb/Music125/v4/test2/100x100bb.jpg",
          "releaseDate": "1975-09-12T07:00:00Z"
        },
        {
          "wrapperType": "collection",
          "collectionType": "Album",
          "artistName": "Pink Floyd",
          "collectionName": "The Dark Side of the Moon",
          "collectionId": 1065976001,
          "artworkUrl100": null,
          "releaseDate": "1973-03-01T08:00:00Z"
        }
      ]
    }
    """.trimIndent()

    // Real iTunes podcast response structure
    private val podcastResponseJson = """
    {
      "resultCount": 2,
      "results": [
        {
          "wrapperType": "track",
          "collectionName": "The Joe Rogan Experience",
          "artistName": "Joe Rogan",
          "collectionId": 360084272,
          "artworkUrl100": "https://is1-ssl.mzstatic.com/image/thumb/Podcasts/100x100bb.jpg",
          "releaseDate": "2026-03-20T00:00:00Z"
        },
        {
          "wrapperType": "track",
          "collectionName": "The Joe Rogan AI Experience",
          "artistName": "Joe Rogan AI",
          "collectionId": 999999999,
          "artworkUrl100": "https://is1-ssl.mzstatic.com/image/thumb/Podcasts2/100x100bb.jpg"
        }
      ]
    }
    """.trimIndent()

    // Simulated empty response
    private val emptyResponseJson = """
    {
      "resultCount": 0,
      "results": []
    }
    """.trimIndent()

    // ── Music deserialization ────────────────────────────────

    @Test
    fun `deserialize music album response`() {
        val response = json.decodeFromString<ItunesTestResponse>(musicResponseJson)
        assertEquals(3, response.resultCount)
        assertEquals(3, response.results.size)
    }

    @Test
    fun `music result has correct album name`() {
        val response = json.decodeFromString<ItunesTestResponse>(musicResponseJson)
        assertEquals("The Wall", response.results[0].collectionName)
        assertEquals("Wish You Were Here", response.results[1].collectionName)
    }

    @Test
    fun `music result has artist name`() {
        val response = json.decodeFromString<ItunesTestResponse>(musicResponseJson)
        assertEquals("Pink Floyd", response.results[0].artistName)
    }

    @Test
    fun `music result has release year`() {
        val response = json.decodeFromString<ItunesTestResponse>(musicResponseJson)
        val year = response.results[0].releaseDate?.take(4)
        assertEquals("1979", year)
    }

    @Test
    fun `music result has artwork URL`() {
        val response = json.decodeFromString<ItunesTestResponse>(musicResponseJson)
        assertNotNull(response.results[0].artworkUrl100)
        assertTrue(response.results[0].artworkUrl100!!.contains("100x100"))
    }

    @Test
    fun `music artwork URL upscaled to 600x600`() {
        val response = json.decodeFromString<ItunesTestResponse>(musicResponseJson)
        val upscaled = response.results[0].artworkUrl100?.replace("100x100", "600x600")
        assertTrue(upscaled!!.contains("600x600"))
    }

    @Test
    fun `music result handles null artwork`() {
        val response = json.decodeFromString<ItunesTestResponse>(musicResponseJson)
        // Third result has null artworkUrl100
        assertEquals(null, response.results[2].artworkUrl100)
    }

    @Test
    fun `music result has collection ID for external reference`() {
        val response = json.decodeFromString<ItunesTestResponse>(musicResponseJson)
        assertEquals(1065975633L, response.results[0].collectionId)
    }

    // ── Podcast deserialization ──────────────────────────────

    @Test
    fun `deserialize podcast response`() {
        val response = json.decodeFromString<ItunesTestResponse>(podcastResponseJson)
        assertEquals(2, response.resultCount)
    }

    @Test
    fun `podcast result has correct show name`() {
        val response = json.decodeFromString<ItunesTestResponse>(podcastResponseJson)
        assertEquals("The Joe Rogan Experience", response.results[0].collectionName)
    }

    @Test
    fun `podcast result has host name`() {
        val response = json.decodeFromString<ItunesTestResponse>(podcastResponseJson)
        assertEquals("Joe Rogan", response.results[0].artistName)
    }

    @Test
    fun `podcast result has artwork`() {
        val response = json.decodeFromString<ItunesTestResponse>(podcastResponseJson)
        assertNotNull(response.results[0].artworkUrl100)
    }

    // ── Empty response ──────────────────────────────────────

    @Test
    fun `empty response returns empty results`() {
        val response = json.decodeFromString<ItunesTestResponse>(emptyResponseJson)
        assertEquals(0, response.resultCount)
        assertTrue(response.results.isEmpty())
    }

    // ── Content type handling ────────────────────────────────

    @Test
    fun `manual JSON deserialization works for text_javascript content`() {
        // This simulates what the client does: bodyAsText() then manual decode
        // iTunes returns text/javascript, not application/json
        val rawText = musicResponseJson
        val response = json.decodeFromString<ItunesTestResponse>(rawText)
        assertEquals(3, response.results.size)
        assertEquals("Pink Floyd", response.results[0].artistName)
    }
}

// Test-only DTO that mirrors the private ItunesResponse in the client
@kotlinx.serialization.Serializable
data class ItunesTestResponse(
    val resultCount: Int = 0,
    val results: List<ItunesTestItem> = emptyList(),
)

@kotlinx.serialization.Serializable
data class ItunesTestItem(
    val trackName: String? = null,
    val collectionName: String? = null,
    val artistName: String? = null,
    val collectionId: Long? = null,
    val artworkUrl100: String? = null,
    val releaseDate: String? = null,
)
