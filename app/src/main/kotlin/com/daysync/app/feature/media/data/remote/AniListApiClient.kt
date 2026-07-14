package com.daysync.app.feature.media.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable

/**
 * AniList GraphQL fallback for anime/manga metadata.
 *
 * Jikan proxies MyAnimeList, which returns 504s whenever MAL is unavailable and
 * rate-limits (429) under per-keystroke search. AniList is keyless, stable, and
 * covers both anime and manga, so it backs Jikan up.
 */
class AniListApiClient(
    private val httpClient: HttpClient,
) {
    private val endpoint = "https://graphql.anilist.co"

    private val searchQuery = """
        query(${'$'}search: String, ${'$'}type: MediaType) {
          Page(perPage: 10) {
            media(search: ${'$'}search, type: ${'$'}type, sort: SEARCH_MATCH) {
              id
              title { romaji english }
              startDate { year }
              coverImage { large }
              description(asHtml: false)
              studios { nodes { name } }
              staff { nodes { name { full } } }
            }
          }
        }
    """.trimIndent()

    suspend fun searchAnime(query: String): List<MediaMetadataResult> = search(query, "ANIME")

    suspend fun searchManga(query: String): List<MediaMetadataResult> = search(query, "MANGA")

    private suspend fun search(query: String, type: String): List<MediaMetadataResult> = try {
        val response: AniListResponse = httpClient.post(endpoint) {
            contentType(ContentType.Application.Json)
            setBody(AniListRequest(searchQuery, AniListVariables(query, type)))
        }.body()
        response.data?.page?.media.orEmpty().mapNotNull { it.toResult(type) }
    } catch (_: Exception) {
        emptyList()
    }

    private fun AniListMedia.toResult(type: String): MediaMetadataResult? {
        val name = title?.english ?: title?.romaji ?: return null
        // Anime is credited to its studio; manga to its author (staff).
        val creators = if (type == "ANIME") {
            studios?.nodes.orEmpty().map { it.name }
        } else {
            staff?.nodes.orEmpty().mapNotNull { it.name?.full }
        }
        return MediaMetadataResult(
            title = name,
            creators = creators.take(3),
            coverImageUrl = coverImage?.large,
            year = startDate?.year?.toString(),
            description = description?.replace(Regex("<[^>]*>"), ""),
            externalId = id?.toString(),
        )
    }

    @Serializable
    private data class AniListRequest(val query: String, val variables: AniListVariables)

    @Serializable
    private data class AniListVariables(val search: String, val type: String)

    @Serializable
    private data class AniListResponse(val data: AniListData? = null)

    @Serializable
    private data class AniListData(@kotlinx.serialization.SerialName("Page") val page: AniListPage? = null)

    @Serializable
    private data class AniListPage(val media: List<AniListMedia> = emptyList())

    @Serializable
    private data class AniListMedia(
        val id: Int? = null,
        val title: AniListTitle? = null,
        val startDate: AniListDate? = null,
        val coverImage: AniListCover? = null,
        val description: String? = null,
        val studios: AniListStudios? = null,
        val staff: AniListStaff? = null,
    )

    @Serializable
    private data class AniListTitle(val romaji: String? = null, val english: String? = null)

    @Serializable
    private data class AniListDate(val year: Int? = null)

    @Serializable
    private data class AniListCover(val large: String? = null)

    @Serializable
    private data class AniListStudios(val nodes: List<AniListNamed> = emptyList())

    @Serializable
    private data class AniListNamed(val name: String)

    @Serializable
    private data class AniListStaff(val nodes: List<AniListStaffNode> = emptyList())

    @Serializable
    private data class AniListStaffNode(val name: AniListStaffName? = null)

    @Serializable
    private data class AniListStaffName(val full: String? = null)
}
