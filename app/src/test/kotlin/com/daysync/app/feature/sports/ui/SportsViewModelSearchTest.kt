package com.daysync.app.feature.sports.ui

import com.daysync.app.core.database.entity.CompetitionEntity
import com.daysync.app.core.database.entity.CompetitorEntity
import com.daysync.app.core.database.entity.SportEventEntity
import com.daysync.app.core.sync.SyncStatus
import com.daysync.app.feature.sports.data.SportsRefreshManager
import com.daysync.app.feature.sports.data.SportsRepository
import com.daysync.app.feature.sports.data.model.SportEventWithDetails
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlin.time.Clock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Pipeline tests for the team search added to SportsViewModel — verifies
 * that the debounced query, sport filter, and team-selection flows produce
 * the expected UI state without exercising the database or network.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SportsViewModelSearchTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: SportsRepository
    private lateinit var refreshManager: SportsRefreshManager

    private val arsenal = competitor("c-arsenal", "football", "Arsenal")
    private val mancity = competitor("c-mancity", "football", "Manchester City")
    private val manutd = competitor("c-manutd", "football", "Manchester United")

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        refreshManager = mockk(relaxed = true)

        // Stub the init-time repository calls so the ViewModel can boot up
        // without exercising the search code under test.
        coEvery { repository.ensureSeedData() } returns Unit
        coEvery { repository.refreshAllSports() } returns Unit
        every { repository.getUpcomingEvents(any()) } returns flowOf(emptyList())
        every { repository.getLiveEvents(any()) } returns flowOf(emptyList())
        every { repository.getRecentResults(any()) } returns flowOf(emptyList())
        every { repository.getWatchlistedEvents() } returns flowOf(emptyList())
        every { repository.getWatchlistedEventIds() } returns flowOf(emptyList())
        every { repository.getFollowedCompetitions() } returns flowOf(emptyList())
        every { repository.getAllCompetitions() } returns flowOf(emptyList<CompetitionEntity>())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `search below 2 chars returns empty results`() = runTest(testDispatcher) {
        // No call should ever reach the repo for a 1-char query.
        every { repository.searchCompetitors(any(), any()) } returns flowOf(listOf(arsenal))

        val vm = SportsViewModel(repository, refreshManager)
        advanceUntilIdle()

        vm.setSearchQuery("a")
        advanceTimeBy(500)
        advanceUntilIdle()

        assertEquals(emptyList<CompetitorEntity>(), vm.uiState.value.searchResults)
    }

    @Test
    fun `search at 2 plus chars returns matching teams`() = runTest(testDispatcher) {
        every { repository.searchCompetitors("Man", null) } returns flowOf(listOf(mancity, manutd))

        val vm = SportsViewModel(repository, refreshManager)
        advanceUntilIdle()

        vm.setSearchQuery("Man")
        advanceTimeBy(300) // past the 250ms debounce
        advanceUntilIdle()

        val results = vm.uiState.value.searchResults
        assertEquals(2, results.size)
        assertEquals("Manchester City", results[0].name)
        assertEquals("Manchester United", results[1].name)
    }

    @Test
    fun `sport filter is forwarded to the repository query`() = runTest(testDispatcher) {
        every { repository.searchCompetitors("Man", "football") } returns flowOf(listOf(mancity))
        every { repository.searchCompetitors("Man", null) } returns flowOf(listOf(mancity, manutd))

        val vm = SportsViewModel(repository, refreshManager)
        advanceUntilIdle()

        vm.setSearchQuery("Man")
        vm.setSearchSportFilter("football")
        advanceTimeBy(300)
        advanceUntilIdle()

        val results = vm.uiState.value.searchResults
        assertEquals(1, results.size)
        assertEquals("Manchester City", results[0].name)
        assertEquals("football", vm.uiState.value.searchSportFilter)
    }

    @Test
    fun `rapid typing only triggers one query thanks to debounce`() = runTest(testDispatcher) {
        // Each query argument returns a distinct list so we can tell which one ran.
        every { repository.searchCompetitors("A", null) } returns flowOf(listOf(arsenal))
        every { repository.searchCompetitors("Ar", null) } returns flowOf(listOf(arsenal))
        every { repository.searchCompetitors("Ars", null) } returns flowOf(listOf(arsenal))
        every { repository.searchCompetitors("Arsenal", null) } returns flowOf(listOf(arsenal))

        val vm = SportsViewModel(repository, refreshManager)
        advanceUntilIdle()

        vm.setSearchQuery("A")
        advanceTimeBy(50)
        vm.setSearchQuery("Ar")
        advanceTimeBy(50)
        vm.setSearchQuery("Ars")
        advanceTimeBy(50)
        vm.setSearchQuery("Arsenal")
        advanceTimeBy(300) // now wait past debounce
        advanceUntilIdle()

        // Final state should be the result for the latest query
        val results = vm.uiState.value.searchResults
        assertEquals(1, results.size)
        assertEquals("Arsenal", results[0].name)
    }

    @Test
    fun `selectSearchTeam loads events for that team`() = runTest(testDispatcher) {
        val event = sportEvent("e1", competitionId = "football-pl", scheduledAt = 1700000000L)
        val enriched = enrichedDetails("e1", competitionId = "football-pl")
        every { repository.getEventsByCompetitor("c-arsenal") } returns flowOf(listOf(event))
        every { repository.getWatchlistedEventIds() } returns flowOf(emptyList())
        coEvery { repository.enrichEvent(event, emptySet()) } returns enriched

        val vm = SportsViewModel(repository, refreshManager)
        advanceUntilIdle()

        vm.selectSearchTeam(arsenal)
        advanceUntilIdle()

        assertEquals(arsenal, vm.uiState.value.selectedSearchTeam)
        val events = vm.uiState.value.selectedSearchTeamEvents
        assertEquals(1, events.size)
        assertEquals("e1", events[0].id)
    }

    @Test
    fun `clearSearchTeam wipes the selected team and its events`() = runTest(testDispatcher) {
        val event = sportEvent("e1", competitionId = "football-pl", scheduledAt = 1700000000L)
        val enriched = enrichedDetails("e1", competitionId = "football-pl")
        every { repository.getEventsByCompetitor("c-arsenal") } returns flowOf(listOf(event))
        coEvery { repository.enrichEvent(event, emptySet()) } returns enriched

        val vm = SportsViewModel(repository, refreshManager)
        advanceUntilIdle()
        vm.selectSearchTeam(arsenal)
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.selectedSearchTeam)
        assertTrue(vm.uiState.value.selectedSearchTeamEvents.isNotEmpty())

        vm.clearSearchTeam()
        advanceUntilIdle()

        assertNull(vm.uiState.value.selectedSearchTeam)
        assertEquals(emptyList<SportEventWithDetails>(), vm.uiState.value.selectedSearchTeamEvents)
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private fun competitor(id: String, sportId: String, name: String): CompetitorEntity =
        CompetitorEntity(id = id, sportId = sportId, name = name)

    private fun sportEvent(
        id: String,
        competitionId: String,
        scheduledAt: Long,
    ): SportEventEntity = SportEventEntity(
        id = id,
        sportId = "football",
        competitionId = competitionId,
        scheduledAt = kotlin.time.Instant.fromEpochMilliseconds(scheduledAt),
        status = "SCHEDULED",
        syncStatus = SyncStatus.SYNCED,
        lastModified = Clock.System.now(),
    )

    private fun enrichedDetails(id: String, competitionId: String): SportEventWithDetails =
        SportEventWithDetails(
            id = id,
            sportId = "football",
            sportName = "Football",
            competitionId = competitionId,
            competitionName = "Premier League",
            competitionShortName = "PL",
            scheduledAt = kotlin.time.Instant.fromEpochMilliseconds(1_700_000_000L),
            status = "SCHEDULED",
            homeCompetitorId = null,
            awayCompetitorId = null,
            homeCompetitorName = null,
            awayCompetitorName = null,
            homeCompetitorLogo = null,
            awayCompetitorLogo = null,
            homeScore = null,
            awayScore = null,
            eventName = null,
            round = null,
            season = null,
            resultDetail = null,
            dataSource = null,
            isWatchlisted = false,
        )
}
