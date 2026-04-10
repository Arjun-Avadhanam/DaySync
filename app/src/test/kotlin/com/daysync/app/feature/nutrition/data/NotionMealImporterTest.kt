package com.daysync.app.feature.nutrition.data

import com.daysync.app.core.database.dao.FoodItemDao
import com.daysync.app.core.database.entity.FoodItemEntity
import com.daysync.app.core.sync.SyncStatus
import com.daysync.app.feature.nutrition.data.remote.NotionMealApiClient
import com.daysync.app.feature.nutrition.data.remote.NotionMealPage
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlin.time.Clock
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.decodeFromString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for the Notion live importer.
 *
 * Parsing tests use a real trimmed fixture captured from the live Notion API so
 * any schema drift (renamed properties, shape changes) is caught here before it
 * reaches the device.
 */
class NotionMealImporterTest {

    private lateinit var foodItemDao: FoodItemDao
    private lateinit var apiClient: NotionMealApiClient
    private lateinit var importer: NotionMealImporter

    @Before
    fun setUp() {
        foodItemDao = mockk(relaxed = true)
        apiClient = mockk()
        importer = NotionMealImporter(apiClient, foodItemDao)
    }

    // ── DTO parsing against real captured Notion response ─────────────────

    @Test
    fun `real Notion response parses into domain pages`() {
        val response = NotionMealApiClient.jsonConfig
            .decodeFromString<NotionMealApiClient.NotionQueryResponse>(FIXTURE_QUERY_RESPONSE)

        assertEquals(2, response.results.size)
        assertTrue("response has_more is true", response.hasMore)
        assertEquals("24b72c90-3eee-808d-9022-eafb2667caa3", response.nextCursor)

        val firstPage = response.results[0].toDomain()
        assertEquals("24a72c90-3eee-802e-adbd-c2cc5d39dddd", firstPage.pageId)
        assertEquals("Big Banana", firstPage.name)
        assertEquals(58.0, firstPage.calories, 0.0)
        assertEquals(0.7, firstPage.protein, 0.0)
        assertEquals(15.0, firstPage.carbs, 0.0)
        assertEquals(0.2, firstPage.fat, 0.0)
        assertEquals(8.0, firstPage.sugar, 0.0)
        assertEquals("Fruits", firstPage.category)
        assertEquals("pieces", firstPage.unit)
        assertNull(firstPage.serving) // rich_text is empty in fixture

        val secondPage = response.results[1].toDomain()
        assertEquals("Protein oats + 200 ml milk", secondPage.name)
        assertEquals(316.0, secondPage.calories, 0.0)
        assertEquals("Proteins", secondPage.category)
        assertEquals("items", secondPage.unit)
    }

    @Test
    fun `unknown fields in response are ignored`() {
        // "Daily Meal Entries" relation, "Notes" rich_text, "Created Date",
        // "Last Updated" all appear in the fixture but aren't part of our DTO.
        // Parsing must not fail.
        val response = NotionMealApiClient.jsonConfig
            .decodeFromString<NotionMealApiClient.NotionQueryResponse>(FIXTURE_QUERY_RESPONSE)
        assertEquals(2, response.results.size)
    }

    // ── Importer behaviour ────────────────────────────────────────────────

    @Test
    fun `forceImport upserts all fetched pages with notion-pageId ids`() = runTest {
        coEvery { apiClient.fetchAllMeals() } returns listOf(
            samplePage("page-1", "Big Banana", "Fruits", "pieces"),
            samplePage("page-2", "Roti", "Grains & Cereals & Legumes", "pieces"),
        )
        coEvery { foodItemDao.getActiveNotionIds() } returns emptyList()

        val captured = slot<List<FoodItemEntity>>()
        coEvery { foodItemDao.insertAll(capture(captured)) } just Runs

        val result = importer.forceImport()

        assertEquals(2, result.upserted)
        assertEquals(0, result.softDeleted)
        val inserted = captured.captured
        assertEquals(listOf("notion-page-1", "notion-page-2"), inserted.map { it.id })
        assertEquals("Big Banana", inserted[0].name)
        assertEquals("Fruits", inserted[0].category)
        assertEquals("PIECES", inserted[0].unitType)
        assertEquals(SyncStatus.PENDING, inserted[0].syncStatus)
    }

    @Test
    fun `forceImport soft-deletes stale notion rows`() = runTest {
        coEvery { apiClient.fetchAllMeals() } returns listOf(
            samplePage("page-1", "Big Banana", "Fruits", "pieces"),
        )
        // Locally we already have two items: the one we'll re-fetch and a stale one.
        coEvery { foodItemDao.getActiveNotionIds() } returns listOf(
            "notion-page-1",
            "notion-page-stale",
        )
        val staleEntity = entity("notion-page-stale", "Old Item")
        coEvery { foodItemDao.getById("notion-page-stale") } returns staleEntity

        val updatedSlot = slot<FoodItemEntity>()
        coEvery { foodItemDao.update(capture(updatedSlot)) } just Runs

        val result = importer.forceImport()

        assertEquals(1, result.upserted)
        assertEquals(1, result.softDeleted)
        coVerify(exactly = 1) { foodItemDao.update(any()) }
        val updated = updatedSlot.captured
        assertEquals("notion-page-stale", updated.id)
        assertTrue("stale row is soft-deleted", updated.isDeleted)
        assertEquals(SyncStatus.PENDING, updated.syncStatus)
    }

    @Test
    fun `forceImport treats legacy name-hash rows as stale`() = runTest {
        // Simulates first live import for a user whose local DB still has the old
        // notion-<nameHash> rows from the asset-based importer. These don't match
        // any new notion-<pageId> and should be soft-deleted.
        coEvery { apiClient.fetchAllMeals() } returns listOf(
            samplePage("pg-big-banana", "Big Banana", "Fruits", "pieces"),
        )
        val legacyId = "notion-d8f2e1a3-4c5b-6789-abcd-ef0123456789"
        coEvery { foodItemDao.getActiveNotionIds() } returns listOf(legacyId)
        coEvery { foodItemDao.getById(legacyId) } returns entity(legacyId, "Big Banana")

        val updatedSlot = slot<FoodItemEntity>()
        coEvery { foodItemDao.update(capture(updatedSlot)) } just Runs

        val result = importer.forceImport()

        assertEquals(1, result.upserted)
        assertEquals(1, result.softDeleted)
        assertTrue(updatedSlot.captured.isDeleted)
    }

    @Test
    fun `forceImport re-import is idempotent`() = runTest {
        val page = samplePage("page-1", "Big Banana", "Fruits", "pieces")
        coEvery { apiClient.fetchAllMeals() } returns listOf(page)
        // On the second run the row already exists with the new scheme.
        coEvery { foodItemDao.getActiveNotionIds() } returns listOf("notion-page-1")

        val result = importer.forceImport()

        assertEquals(1, result.upserted)
        assertEquals(0, result.softDeleted)
        coVerify(exactly = 0) { foodItemDao.update(any()) }
    }

    @Test
    fun `unit types map correctly`() = runTest {
        coEvery { foodItemDao.getActiveNotionIds() } returns emptyList()
        val mappings = listOf(
            "pieces" to "PIECES",
            "items" to "PIECES",
            "slices" to "SLICE",
            "grams" to "GRAMS",
            "cups" to "CUPS",
            "tablespoons" to "TABLESPOON",
            "ml" to "ML",
            "unknown-garbage" to "SERVING",
        )
        val pages = mappings.mapIndexed { i, (notionUnit, _) ->
            samplePage("page-$i", "Food $i", "Fruits", notionUnit)
        }
        coEvery { apiClient.fetchAllMeals() } returns pages

        val captured = slot<List<FoodItemEntity>>()
        coEvery { foodItemDao.insertAll(capture(captured)) } just Runs

        importer.forceImport()

        mappings.forEachIndexed { i, (_, expected) ->
            assertEquals(expected, captured.captured[i].unitType)
        }
    }

    @Test
    fun `blank category becomes null`() = runTest {
        coEvery { apiClient.fetchAllMeals() } returns listOf(
            samplePage("page-1", "Nameless", category = "", unit = "pieces"),
        )
        coEvery { foodItemDao.getActiveNotionIds() } returns emptyList()
        val captured = slot<List<FoodItemEntity>>()
        coEvery { foodItemDao.insertAll(capture(captured)) } just Runs

        importer.forceImport()

        assertNull(captured.captured[0].category)
    }

    @Test
    fun `importer propagates api failures`() = runTest {
        coEvery { apiClient.fetchAllMeals() } throws IllegalStateException("offline")

        val failure = runCatching { importer.forceImport() }.exceptionOrNull()
        assertNotNull(failure)
        assertFalse(failure is Error)
        assertEquals("offline", failure?.message)
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private fun samplePage(
        pageId: String,
        name: String,
        category: String? = "Fruits",
        unit: String? = "pieces",
    ): NotionMealPage = NotionMealPage(
        pageId = pageId,
        name = name,
        calories = 100.0,
        protein = 1.0,
        carbs = 10.0,
        fat = 0.5,
        sugar = 5.0,
        category = category,
        unit = unit,
        serving = null,
    )

    private fun entity(id: String, name: String): FoodItemEntity = FoodItemEntity(
        id = id,
        name = name,
        caloriesPerUnit = 100.0,
        unitType = "PIECES",
        syncStatus = SyncStatus.SYNCED,
        lastModified = Clock.System.now(),
    )

    companion object {
        // Trimmed capture of a real POST /v1/databases/{id}/query response. Comments
        // removed, irrelevant metadata (user/time fields, annotations) stripped so the
        // string stays readable while still exercising every DTO path we use.
        private val FIXTURE_QUERY_RESPONSE = """
{
  "object": "list",
  "results": [
    {
      "id": "24a72c90-3eee-802e-adbd-c2cc5d39dddd",
      "properties": {
        "Carbs per Unit (g)": { "type": "number", "number": 15 },
        "Sugar per Unit (g)": { "type": "number", "number": 8 },
        "Protein per Unit (g)": { "type": "number", "number": 0.7 },
        "Last Updated": { "type": "last_edited_time", "last_edited_time": "2025-08-09T09:54:00.000Z" },
        "Unit Type": {
          "type": "select",
          "select": { "id": "806a908c", "name": "pieces" }
        },
        "Daily Meal Entries": { "type": "relation", "relation": [], "has_more": true },
        "Serving Description": { "type": "rich_text", "rich_text": [] },
        "Notes": {
          "type": "rich_text",
          "rich_text": [{ "type": "text", "plain_text": "Weighed -96 g" }]
        },
        "Created Date": { "type": "created_time", "created_time": "2025-08-09T09:47:00.000Z" },
        "Fat per Unit (g)": { "type": "number", "number": 0.2 },
        "Calories per Unit": { "type": "number", "number": 58 },
        "Food Category": {
          "type": "select",
          "select": { "id": "4baf8bf5", "name": "Fruits" }
        },
        "Meal Name": {
          "type": "title",
          "title": [{ "type": "text", "plain_text": "Big Banana" }]
        }
      }
    },
    {
      "id": "24b72c90-3eee-8048-a957-cc83852dd636",
      "properties": {
        "Carbs per Unit (g)": { "type": "number", "number": 37.3 },
        "Sugar per Unit (g)": { "type": "number", "number": 15.2 },
        "Protein per Unit (g)": { "type": "number", "number": 18.7 },
        "Unit Type": {
          "type": "select",
          "select": { "id": "509eb726", "name": "items" }
        },
        "Serving Description": { "type": "rich_text", "rich_text": [] },
        "Fat per Unit (g)": { "type": "number", "number": 10.2 },
        "Calories per Unit": { "type": "number", "number": 316 },
        "Food Category": {
          "type": "select",
          "select": { "id": "d345790d", "name": "Proteins" }
        },
        "Meal Name": {
          "type": "title",
          "title": [{ "type": "text", "plain_text": "Protein oats + 200 ml milk" }]
        }
      }
    }
  ],
  "next_cursor": "24b72c90-3eee-808d-9022-eafb2667caa3",
  "has_more": true
}
        """.trimIndent()
    }
}
