package com.daysync.app.feature.media.data.remote

import com.daysync.app.feature.media.domain.MediaType
import javax.inject.Inject

class MediaMetadataService @Inject constructor(
    private val tmdbClient: TmdbApiClient,
    private val googleBooksClient: GoogleBooksApiClient,
    private val rawgClient: RawgApiClient,
) {
    suspend fun search(query: String, mediaType: MediaType): List<MediaMetadataResult> {
        if (query.isBlank()) return emptyList()
        return when (mediaType) {
            MediaType.FILM, MediaType.MOVIE -> tmdbClient.searchMovies(query)
            MediaType.TV_SERIES, MediaType.ANIME -> tmdbClient.searchTv(query)
            MediaType.BOOK, MediaType.ARTICLE, MediaType.COMIC, MediaType.MANGA ->
                googleBooksClient.searchBooks(query)
            MediaType.GAME -> rawgClient.searchGames(query)
            MediaType.PODCAST, MediaType.MUSIC -> emptyList()
        }
    }

    suspend fun fetchCreators(externalId: String, mediaType: MediaType): List<String> {
        if (externalId.isBlank()) return emptyList()
        return when (mediaType) {
            MediaType.FILM, MediaType.MOVIE -> tmdbClient.getMovieCredits(externalId)
            MediaType.TV_SERIES, MediaType.ANIME -> tmdbClient.getTvCredits(externalId)
            MediaType.GAME -> rawgClient.getGameDevelopers(externalId)
            else -> emptyList()
        }
    }
}
