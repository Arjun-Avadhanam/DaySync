package com.daysync.app.core.sync.mapper

import com.daysync.app.core.database.entity.ExpenseEntity
import com.daysync.app.core.database.entity.HealthMetricEntity
import com.daysync.app.core.database.entity.JournalEntryEntity
import com.daysync.app.core.database.entity.MediaItemEntity
import com.daysync.app.core.database.entity.SleepSessionEntity
import com.daysync.app.core.database.entity.SportEventEntity
import com.daysync.app.core.sync.SyncStatus
import kotlinx.datetime.LocalDate
import kotlin.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncMappersTest {

    private val testInstant = Instant.fromEpochMilliseconds(1710504000000L) // 2024-03-15T12:00:00Z

    // ── Health ──────────────────────────────────────────

    @Test
    fun `HealthMetricEntity toDto maps all fields`() {
        val entity = HealthMetricEntity(
            id = "hm-1",
            type = "STEPS",
            value = 8500.0,
            unit = "count",
            timestamp = testInstant,
            source = "health_connect",
            lastModified = testInstant,
            isDeleted = false,
        )
        val dto = entity.toDto()
        assertEquals("hm-1", dto.id)
        assertEquals("STEPS", dto.type)
        assertEquals(8500.0, dto.value, 0.01)
        assertEquals("count", dto.unit)
        assertEquals(testInstant.toEpochMilliseconds(), dto.timestamp)
        assertEquals("health_connect", dto.source)
        assertEquals(testInstant.toEpochMilliseconds(), dto.lastModified)
        assertEquals(false, dto.isDeleted)
    }

    @Test
    fun `SleepSessionEntity toDto maps all fields`() {
        val entity = SleepSessionEntity(
            id = "sleep-1",
            startTime = testInstant,
            endTime = Instant.fromEpochMilliseconds(testInstant.toEpochMilliseconds() + 28800000L), // +8h
            totalMinutes = 480,
            deepMinutes = 120,
            lightMinutes = 200,
            remMinutes = 100,
            awakeMinutes = 60,
            score = 85,
            lastModified = testInstant,
        )
        val dto = entity.toDto()
        assertEquals("sleep-1", dto.id)
        assertEquals(480, dto.totalMinutes)
        assertEquals(120, dto.deepMinutes)
        assertEquals(200, dto.lightMinutes)
        assertEquals(100, dto.remMinutes)
        assertEquals(60, dto.awakeMinutes)
        assertEquals(85, dto.score)
    }

    // ── Expenses ────────────────────────────────────────

    @Test
    fun `ExpenseEntity toDto maps all fields`() {
        val entity = ExpenseEntity(
            id = "exp-1",
            title = "Lunch",
            item = "Biryani",
            date = LocalDate(2026, 3, 15),
            category = "Food>Delivery",
            frequency = "ONE_TIME",
            unitCost = 250.0,
            quantity = 1.0,
            deliveryCharge = 30.0,
            totalAmount = 280.0,
            notes = "Swiggy order",
            source = "NOTIFICATION",
            merchantName = "Swiggy",
            lastModified = testInstant,
        )
        val dto = entity.toDto()
        assertEquals("exp-1", dto.id)
        assertEquals("Lunch", dto.title)
        assertEquals("Biryani", dto.item)
        assertEquals("2026-03-15", dto.date)
        assertEquals("Food>Delivery", dto.category)
        assertEquals(250.0, dto.unitCost, 0.01)
        assertEquals(280.0, dto.totalAmount, 0.01)
        assertEquals("NOTIFICATION", dto.source)
        assertEquals("Swiggy", dto.merchantName)
    }

    @Test
    fun `ExpenseEntity toDto handles nulls`() {
        val entity = ExpenseEntity(
            id = "exp-2",
            date = LocalDate(2026, 3, 15),
            unitCost = 100.0,
            totalAmount = 100.0,
            lastModified = testInstant,
        )
        val dto = entity.toDto()
        assertNull(dto.title)
        assertNull(dto.item)
        assertNull(dto.category)
        assertNull(dto.notes)
        assertNull(dto.merchantName)
    }

    // ── Sports ──────────────────────────────────────────

    @Test
    fun `SportEventEntity toDto maps all fields`() {
        val entity = SportEventEntity(
            id = "fd-123",
            sportId = "football",
            competitionId = "pl-2025",
            scheduledAt = testInstant,
            status = "COMPLETED",
            homeCompetitorId = "fd-team-1",
            awayCompetitorId = "fd-team-2",
            homeScore = 3,
            awayScore = 1,
            eventName = "Arsenal vs Chelsea",
            round = "Matchday 28",
            season = "2025-2026",
            resultDetail = """{"type":"football"}""",
            lastUpdated = testInstant,
            dataSource = "football-data.org",
            lastModified = testInstant,
        )
        val dto = entity.toDto()
        assertEquals("fd-123", dto.id)
        assertEquals("football", dto.sportId)
        assertEquals("pl-2025", dto.competitionId)
        assertEquals("COMPLETED", dto.status)
        assertEquals(3, dto.homeScore)
        assertEquals(1, dto.awayScore)
        assertEquals("Arsenal vs Chelsea", dto.eventName)
        assertEquals("Matchday 28", dto.round)
        assertEquals("2025-2026", dto.season)
        assertEquals("football-data.org", dto.dataSource)
    }

    // ── Journal ─────────────────────────────────────────

    @Test
    fun `JournalEntryEntity toDto maps fields and serializes tags`() {
        val entity = JournalEntryEntity(
            id = "j-1",
            date = LocalDate(2026, 3, 15),
            title = "Good day",
            content = "Had a productive day",
            mood = 7,
            tags = listOf("work", "productive"),
            isArchived = false,
            lastModified = testInstant,
        )
        val dto = entity.toDto()
        assertEquals("j-1", dto.id)
        assertEquals("2026-03-15", dto.date)
        assertEquals("Good day", dto.title)
        assertEquals("Had a productive day", dto.content)
        assertEquals(7, dto.mood)
        assertTrue(dto.tags!!.contains("work"))
        assertTrue(dto.tags!!.contains("productive"))
        assertEquals(false, dto.isArchived)
    }

    @Test
    fun `JournalEntryEntity toDto null tags when empty`() {
        val entity = JournalEntryEntity(
            id = "j-2",
            date = LocalDate(2026, 3, 15),
            tags = emptyList(),
            lastModified = testInstant,
        )
        val dto = entity.toDto()
        assertNull(dto.tags)
    }

    // ── Media ───────────────────────────────────────────

    @Test
    fun `MediaItemEntity toDto maps fields and serializes creators`() {
        val entity = MediaItemEntity(
            id = "m-1",
            title = "Inception",
            mediaType = "MOVIE",
            status = "DONE",
            score = 9.5,
            creators = listOf("Christopher Nolan"),
            completedDate = LocalDate(2026, 3, 10),
            notes = "Great film",
            lastModified = testInstant,
        )
        val dto = entity.toDto()
        assertEquals("m-1", dto.id)
        assertEquals("Inception", dto.title)
        assertEquals("MOVIE", dto.mediaType)
        assertEquals("DONE", dto.status)
        assertEquals(9.5, dto.score!!, 0.01)
        assertTrue(dto.creators!!.contains("Christopher Nolan"))
        assertEquals("2026-03-10", dto.completedDate)
        assertEquals("Great film", dto.notes)
    }

    @Test
    fun `MediaItemEntity toDto null creators when empty`() {
        val entity = MediaItemEntity(
            id = "m-2",
            title = "Test",
            mediaType = "BOOK",
            creators = emptyList(),
            lastModified = testInstant,
        )
        val dto = entity.toDto()
        assertNull(dto.creators)
    }

    @Test
    fun `MediaItemEntity toDto null completedDate when not set`() {
        val entity = MediaItemEntity(
            id = "m-3",
            title = "Test",
            mediaType = "BOOK",
            completedDate = null,
            lastModified = testInstant,
        )
        val dto = entity.toDto()
        assertNull(dto.completedDate)
    }

    // ── Timestamp conversion ────────────────────────────

    @Test
    fun `toDto converts Instant to epoch millis correctly`() {
        val entity = HealthMetricEntity(
            id = "test",
            type = "STEPS",
            value = 0.0,
            unit = "count",
            timestamp = Instant.fromEpochMilliseconds(1710504000000L),
            lastModified = Instant.fromEpochMilliseconds(1710504000000L),
        )
        val dto = entity.toDto()
        assertEquals(1710504000000L, dto.timestamp)
        assertEquals(1710504000000L, dto.lastModified)
    }
}
