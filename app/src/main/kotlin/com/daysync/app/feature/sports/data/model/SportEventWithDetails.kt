package com.daysync.app.feature.sports.data.model

import kotlin.time.Instant

data class SportEventWithDetails(
    val id: String,
    val sportId: String,
    val sportName: String,
    val competitionId: String,
    val competitionName: String,
    val competitionShortName: String?,
    val scheduledAt: Instant,
    val status: String,
    val homeCompetitorId: String?,
    val awayCompetitorId: String?,
    val homeCompetitorName: String?,
    val awayCompetitorName: String?,
    val homeCompetitorLogo: String?,
    val awayCompetitorLogo: String?,
    val homeScore: Int?,
    val awayScore: Int?,
    val eventName: String?,
    val round: String?,
    val season: String?,
    val resultDetail: String?,
    val dataSource: String?,
    val isWatchlisted: Boolean,
    val venueName: String? = null,
)
