package com.daysync.app.feature.media.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Searches MusicBrainz for albums (release-groups) and returns metadata
 * with cover art URLs from the Cover Art Archive.
 *
 * No API key required. Rate limit: 1 req/sec (enforced by user-agent policy).
 * Cover Art Archive provides album art via release-group MBID.
 */
class MusicBrainzApiClient(
    private val httpClient: HttpClient,
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun searchMusic(query: String): List<MediaMetadataResult> = try {
        val text = httpClient.get(BASE_URL) {
            parameter("query", query)
            parameter("type", "album")
            parameter("limit", 10)
            parameter("fmt", "json")
            header("User-Agent", USER_AGENT)
        }.bodyAsText()

        val response = json.decodeFromString<MbSearchResponse>(text)
        response.releaseGroups.map { it.toResult() }
    } catch (_: Exception) {
        emptyList()
    }

    private fun MbReleaseGroup.toResult(): MediaMetadataResult {
        val artistName = artistCredit?.firstOrNull()?.name
        val coverUrl = id?.let { "$COVER_ART_BASE/release-group/$it/front-250" }
        return MediaMetadataResult(
            title = title ?: "Unknown",
            creators = listOfNotNull(artistName),
            coverImageUrl = coverUrl,
            year = firstReleaseDate?.take(4),
            externalId = id,
        )
    }

    companion object {
        private const val BASE_URL = "https://musicbrainz.org/ws/2/release-group"
        private const val COVER_ART_BASE = "https://coverartarchive.org"
        private const val USER_AGENT = "DaySync/2.4.1 (arjun@daysync.app)"
    }

    @Serializable
    private data class MbSearchResponse(
        @SerialName("release-groups")
        val releaseGroups: List<MbReleaseGroup> = emptyList(),
    )

    @Serializable
    private data class MbReleaseGroup(
        val id: String? = null,
        val title: String? = null,
        @SerialName("first-release-date")
        val firstReleaseDate: String? = null,
        @SerialName("artist-credit")
        val artistCredit: List<MbArtistCredit>? = null,
    )

    @Serializable
    private data class MbArtistCredit(
        val name: String? = null,
    )
}
