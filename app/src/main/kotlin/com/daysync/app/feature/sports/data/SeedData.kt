package com.daysync.app.feature.sports.data

import com.daysync.app.core.database.dao.SportEventDao
import com.daysync.app.core.database.entity.CompetitionEntity
import com.daysync.app.core.database.entity.FollowedCompetitionEntity
import com.daysync.app.core.database.entity.SportEntity
import java.util.UUID

object SeedData {

    private val sports = listOf(
        SportEntity(id = "football", name = "Football", sportType = "TEAM", icon = "sports_soccer"),
        SportEntity(id = "basketball", name = "Basketball", sportType = "TEAM", icon = "sports_basketball"),
        SportEntity(id = "tennis", name = "Tennis", sportType = "INDIVIDUAL", icon = "sports_tennis"),
        SportEntity(id = "f1", name = "Formula 1", sportType = "RACING", icon = "sports_motorsports"),
        SportEntity(id = "mma", name = "MMA", sportType = "INDIVIDUAL", icon = "sports_mma"),
    )

    private val competitions = listOf(
        // Football
        CompetitionEntity(
            id = "football-pl", sportId = "football", name = "Premier League",
            shortName = "PL", country = "England",
            footballDataId = "PL", apiFootballId = 39, espnSlug = "eng.1",
        ),
        CompetitionEntity(
            id = "football-cl", sportId = "football", name = "Champions League",
            shortName = "CL", country = "Europe",
            footballDataId = "CL", apiFootballId = 2, espnSlug = "uefa.champions",
        ),
        CompetitionEntity(
            id = "football-sa", sportId = "football", name = "Serie A",
            shortName = "SA", country = "Italy",
            footballDataId = "SA", apiFootballId = 135, espnSlug = "ita.1",
        ),
        CompetitionEntity(
            id = "football-laliga", sportId = "football", name = "La Liga",
            shortName = "LL", country = "Spain",
            footballDataId = "PD", apiFootballId = 140, espnSlug = "esp.1",
        ),
        CompetitionEntity(
            id = "football-wc", sportId = "football", name = "World Cup",
            shortName = "WC", country = "International",
            footballDataId = "WC", apiFootballId = 1, espnSlug = "fifa.world",
        ),
        CompetitionEntity(
            id = "football-el", sportId = "football", name = "Europa League",
            shortName = "EL", country = "Europe",
            apiFootballId = 3, espnSlug = "uefa.europa",
        ),
        CompetitionEntity(
            id = "football-efl", sportId = "football", name = "EFL Cup",
            shortName = "EFL", country = "England",
            apiFootballId = 48, espnSlug = "eng.league_cup",
        ),
        CompetitionEntity(
            id = "football-bl1", sportId = "football", name = "Bundesliga",
            shortName = "BL", country = "Germany",
            footballDataId = "BL1", espnSlug = "ger.1",
        ),
        CompetitionEntity(
            id = "football-fl1", sportId = "football", name = "Ligue 1",
            shortName = "L1", country = "France",
            footballDataId = "FL1", espnSlug = "fra.1",
        ),
        CompetitionEntity(
            id = "football-fa", sportId = "football", name = "FA Cup",
            shortName = "FA", country = "England",
            espnSlug = "eng.fa",
        ),
        CompetitionEntity(
            id = "football-efl", sportId = "football", name = "Carabao Cup",
            shortName = "EFL", country = "England",
            espnSlug = "eng.league_cup",
        ),
        CompetitionEntity(
            id = "football-dfb", sportId = "football", name = "DFB Pokal",
            shortName = "DFB", country = "Germany",
            espnSlug = "ger.dfb_pokal",
        ),
        CompetitionEntity(
            id = "football-cdr", sportId = "football", name = "Copa Del Rey",
            shortName = "CDR", country = "Spain",
            espnSlug = "esp.copa_del_rey",
        ),
        CompetitionEntity(
            id = "football-ci", sportId = "football", name = "Coppa Italia",
            shortName = "CI", country = "Italy",
            espnSlug = "ita.coppa_italia",
        ),
        CompetitionEntity(
            id = "football-wc", sportId = "football", name = "World Cup",
            shortName = "WC", country = "International",
            footballDataId = "WC", espnSlug = "fifa.world",
        ),
        CompetitionEntity(
            id = "football-euro", sportId = "football", name = "UEFA Euros",
            shortName = "EUR", country = "Europe",
            footballDataId = "EC", espnSlug = "uefa.euro",
        ),
        CompetitionEntity(
            id = "football-unl", sportId = "football", name = "Nations League",
            shortName = "UNL", country = "Europe",
            espnSlug = "uefa.nations",
        ),
        CompetitionEntity(
            id = "football-copa", sportId = "football", name = "Copa America",
            shortName = "CA", country = "South America",
            espnSlug = "conmebol.america",
        ),
        // Basketball
        CompetitionEntity(
            id = "basketball-nba", sportId = "basketball", name = "NBA",
            shortName = "NBA", country = "USA",
            espnSlug = "nba",
        ),
        // Tennis
        CompetitionEntity(
            id = "tennis-ao", sportId = "tennis", name = "Australian Open",
            shortName = "AO", country = "Australia",
            espnSlug = "australian-open",
        ),
        CompetitionEntity(
            id = "tennis-rg", sportId = "tennis", name = "Roland Garros",
            shortName = "RG", country = "France",
            espnSlug = "roland-garros",
        ),
        CompetitionEntity(
            id = "tennis-wim", sportId = "tennis", name = "Wimbledon",
            shortName = "WIM", country = "UK",
            espnSlug = "wimbledon",
        ),
        CompetitionEntity(
            id = "tennis-uso", sportId = "tennis", name = "US Open",
            shortName = "USO", country = "USA",
            espnSlug = "us-open",
        ),
        // F1
        CompetitionEntity(
            id = "f1-championship", sportId = "f1", name = "F1 World Championship",
            shortName = "F1", country = "International",
            espnSlug = "f1",
        ),
        // MMA
        CompetitionEntity(
            id = "mma-ufc", sportId = "mma", name = "UFC",
            shortName = "UFC", country = "International",
            espnSlug = "ufc",
        ),
        // Tennis - ATP Tour (generic for ESPN scoreboard)
        CompetitionEntity(
            id = "tennis-atp", sportId = "tennis", name = "ATP Tour",
            shortName = "ATP", country = "International",
            espnSlug = "atp",
        ),
    )

    suspend fun ensureSeedData(dao: SportEventDao) {
        dao.insertSports(sports)
        dao.insertCompetitions(competitions)

        // Default every competition to followed. Only inserts rows that aren't
        // already present, so users who explicitly unfollowed something keep
        // that state across upgrades — and any new competitions added in
        // future SeedData updates auto-follow.
        val alreadyFollowed = dao.getFollowedCompetitionIds().toSet()
        val newFollowed = competitions
            .filter { it.id !in alreadyFollowed }
            .map { competition ->
                FollowedCompetitionEntity(
                    id = UUID.randomUUID().toString(),
                    competitionId = competition.id,
                )
            }
        newFollowed.forEach { dao.insertFollowedCompetition(it) }
    }
}
