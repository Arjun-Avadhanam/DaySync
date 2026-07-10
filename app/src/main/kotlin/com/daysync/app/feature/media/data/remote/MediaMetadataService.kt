package com.daysync.app.feature.media.data.remote

import com.daysync.app.feature.media.domain.MediaType
import javax.inject.Inject

class MediaMetadataService @Inject constructor(
    private val omdbClient: OmdbApiClient,
    private val googleBooksClient: GoogleBooksApiClient,
    private val jikanClient: JikanApiClient,
    private val steamClient: SteamApiClient,
    private val musicBrainzClient: MusicBrainzApiClient,
    private val openLibraryClient: OpenLibraryApiClient,
) {
    suspend fun search(query: String, mediaType: MediaType): List<MediaMetadataResult> {
        if (query.isBlank()) return emptyList()
        return when (mediaType) {
            MediaType.MOVIE -> omdbClient.searchMovies(query)
            MediaType.TV_SERIES -> omdbClient.searchSeries(query)
            MediaType.MANGA -> jikanClient.searchManga(query)
            MediaType.ANIME -> jikanClient.searchAnime(query)
            // Google Books rejects keyless requests with 429 (anonymous daily
            // quota is 0 since late 2025), so OpenLibrary is the effective
            // primary; keep Google first for its richer metadata if it recovers.
            MediaType.BOOK, MediaType.ARTICLE ->
                googleBooksClient.searchBooks(query).ifEmpty { openLibraryClient.search(query) }
            MediaType.COMIC -> openLibraryClient.search(query)
            MediaType.GAME -> steamClient.searchGames(query)
            MediaType.MUSIC -> musicBrainzClient.searchMusic(query)
            MediaType.PODCAST -> emptyList() // No podcast source currently
        }
    }

    suspend fun fetchCreators(externalId: String, mediaType: MediaType): List<String> {
        if (externalId.isBlank()) return emptyList()
        return when (mediaType) {
            MediaType.MOVIE, MediaType.TV_SERIES -> omdbClient.getDetail(externalId)
            MediaType.GAME -> steamClient.getGameDevelopers(externalId)
            else -> emptyList()
        }
    }
}
