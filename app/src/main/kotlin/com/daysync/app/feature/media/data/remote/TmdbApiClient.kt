package com.daysync.app.feature.media.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject

class TmdbApiClient @Inject constructor(
    private val httpClient: HttpClient,
    private val apiKey: String,
) {
    private val baseUrl = "https://api.themoviedb.org/3"
    private val imageBaseUrl = "https://image.tmdb.org/t/p/w500"

    suspend fun searchMovies(query: String): List<MediaMetadataResult> = try {
        val response: TmdbSearchResponse = httpClient.get("$baseUrl/search/movie") {
            header("Authorization", "Bearer $apiKey")
            parameter("query", query)
            parameter("page", 1)
        }.body()
        response.results.take(10).map { it.toResult() }
    } catch (_: Exception) {
        emptyList()
    }

    suspend fun searchTv(query: String): List<MediaMetadataResult> = try {
        val response: TmdbTvSearchResponse = httpClient.get("$baseUrl/search/tv") {
            header("Authorization", "Bearer $apiKey")
            parameter("query", query)
            parameter("page", 1)
        }.body()
        response.results.take(10).map { it.toResult() }
    } catch (_: Exception) {
        emptyList()
    }

    suspend fun getMovieCredits(tmdbId: String): List<String> = try {
        val response: TmdbCreditsResponse = httpClient.get("$baseUrl/movie/$tmdbId/credits") {
            header("Authorization", "Bearer $apiKey")
        }.body()
        response.crew.filter { it.job == "Director" }.map { it.name }
    } catch (_: Exception) {
        emptyList()
    }

    suspend fun getTvCredits(tmdbId: String): List<String> = try {
        val response: TmdbTvDetailsResponse = httpClient.get("$baseUrl/tv/$tmdbId") {
            header("Authorization", "Bearer $apiKey")
        }.body()
        response.createdBy.map { it.name }
    } catch (_: Exception) {
        emptyList()
    }

    private fun TmdbMovie.toResult() = MediaMetadataResult(
        title = title,
        coverImageUrl = posterPath?.let { "$imageBaseUrl$it" },
        year = releaseDate?.take(4),
        description = overview,
        externalId = id.toString(),
    )

    private fun TmdbTvShow.toResult() = MediaMetadataResult(
        title = name,
        coverImageUrl = posterPath?.let { "$imageBaseUrl$it" },
        year = firstAirDate?.take(4),
        description = overview,
        externalId = id.toString(),
    )

    @Serializable
    private data class TmdbSearchResponse(
        val results: List<TmdbMovie> = emptyList(),
    )

    @Serializable
    private data class TmdbMovie(
        val id: Int,
        val title: String,
        @SerialName("poster_path") val posterPath: String? = null,
        @SerialName("release_date") val releaseDate: String? = null,
        val overview: String? = null,
    )

    @Serializable
    private data class TmdbTvSearchResponse(
        val results: List<TmdbTvShow> = emptyList(),
    )

    @Serializable
    private data class TmdbTvShow(
        val id: Int,
        val name: String,
        @SerialName("poster_path") val posterPath: String? = null,
        @SerialName("first_air_date") val firstAirDate: String? = null,
        val overview: String? = null,
    )

    @Serializable
    private data class TmdbCreditsResponse(
        val crew: List<TmdbCrewMember> = emptyList(),
    )

    @Serializable
    private data class TmdbCrewMember(
        val name: String,
        val job: String,
    )

    @Serializable
    private data class TmdbTvDetailsResponse(
        @SerialName("created_by") val createdBy: List<TmdbCreator> = emptyList(),
    )

    @Serializable
    private data class TmdbCreator(
        val name: String,
    )
}
