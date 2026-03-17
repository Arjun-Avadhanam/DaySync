package com.daysync.app.feature.journal.domain

import com.daysync.app.core.database.entity.JournalEntryEntity
import com.daysync.app.core.sync.SyncStatus
import kotlinx.datetime.LocalDate
import kotlin.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JournalModelsTest {

    // ── Mood.fromInt ────────────────────────────────────

    @Test
    fun `fromInt 1 returns SAD`() {
        assertEquals(Mood.SAD, Mood.fromInt(1))
    }

    @Test
    fun `fromInt 2 returns GUILTY`() {
        assertEquals(Mood.GUILTY, Mood.fromInt(2))
    }

    @Test
    fun `fromInt 3 returns STRESSED`() {
        assertEquals(Mood.STRESSED, Mood.fromInt(3))
    }

    @Test
    fun `fromInt 4 returns UNMOTIVATED`() {
        assertEquals(Mood.UNMOTIVATED, Mood.fromInt(4))
    }

    @Test
    fun `fromInt 5 returns NEUTRAL`() {
        assertEquals(Mood.NEUTRAL, Mood.fromInt(5))
    }

    @Test
    fun `fromInt 6 returns MOTIVATED`() {
        assertEquals(Mood.MOTIVATED, Mood.fromInt(6))
    }

    @Test
    fun `fromInt 7 returns HAPPY`() {
        assertEquals(Mood.HAPPY, Mood.fromInt(7))
    }

    @Test
    fun `fromInt null returns null`() {
        assertNull(Mood.fromInt(null))
    }

    @Test
    fun `fromInt 0 returns null`() {
        assertNull(Mood.fromInt(0))
    }

    @Test
    fun `fromInt 8 returns null`() {
        assertNull(Mood.fromInt(8))
    }

    @Test
    fun `fromInt negative returns null`() {
        assertNull(Mood.fromInt(-1))
    }

    @Test
    fun `all moods have unique intValues`() {
        val values = Mood.entries.map { it.intValue }
        assertEquals(values.size, values.toSet().size)
    }

    @Test
    fun `mood labels`() {
        assertEquals("Sad", Mood.SAD.label)
        assertEquals("Happy", Mood.HAPPY.label)
        assertEquals("Neutral", Mood.NEUTRAL.label)
    }

    // ── JournalEntryEntity.toDomain ─────────────────────

    private val testInstant = Instant.fromEpochMilliseconds(1710504000000L)

    @Test
    fun `toDomain maps all fields`() {
        val entity = JournalEntryEntity(
            id = "j-1",
            date = LocalDate(2026, 3, 15),
            title = "Good day",
            content = "Had fun",
            mood = 7,
            tags = listOf("work", "fun"),
            isArchived = false,
            lastModified = testInstant,
        )
        val domain = entity.toDomain()
        assertEquals("j-1", domain.id)
        assertEquals(LocalDate(2026, 3, 15), domain.date)
        assertEquals("Good day", domain.title)
        assertEquals("Had fun", domain.content)
        assertEquals(Mood.HAPPY, domain.mood)
        assertEquals(listOf("work", "fun"), domain.tags)
        assertEquals(false, domain.isArchived)
    }

    @Test
    fun `toDomain handles null mood`() {
        val entity = JournalEntryEntity(
            id = "j-2",
            date = LocalDate(2026, 3, 15),
            mood = null,
            lastModified = testInstant,
        )
        assertNull(entity.toDomain().mood)
    }

    @Test
    fun `toDomain handles null title and content`() {
        val entity = JournalEntryEntity(
            id = "j-3",
            date = LocalDate(2026, 3, 15),
            title = null,
            content = null,
            lastModified = testInstant,
        )
        val domain = entity.toDomain()
        assertNull(domain.title)
        assertNull(domain.content)
    }

    @Test
    fun `toDomain handles empty tags`() {
        val entity = JournalEntryEntity(
            id = "j-4",
            date = LocalDate(2026, 3, 15),
            tags = emptyList(),
            lastModified = testInstant,
        )
        assertTrue(entity.toDomain().tags.isEmpty())
    }

    @Test
    fun `toDomain handles archived entry`() {
        val entity = JournalEntryEntity(
            id = "j-5",
            date = LocalDate(2026, 3, 15),
            isArchived = true,
            lastModified = testInstant,
        )
        assertTrue(entity.toDomain().isArchived)
    }

    @Test
    fun `toDomain maps invalid mood int to null`() {
        val entity = JournalEntryEntity(
            id = "j-6",
            date = LocalDate(2026, 3, 15),
            mood = 99,
            lastModified = testInstant,
        )
        assertNull(entity.toDomain().mood)
    }

    // ── JournalEntry.toEntity ───────────────────────────

    @Test
    fun `toEntity maps domain to entity`() {
        val domain = JournalEntry(
            id = "j-1",
            date = LocalDate(2026, 3, 15),
            title = "Test",
            content = "Content",
            mood = Mood.HAPPY,
            tags = listOf("tag1"),
            isArchived = false,
        )
        val entity = domain.toEntity()
        assertEquals("j-1", entity.id)
        assertEquals(LocalDate(2026, 3, 15), entity.date)
        assertEquals("Test", entity.title)
        assertEquals("Content", entity.content)
        assertEquals(7, entity.mood) // HAPPY.intValue
        assertEquals(listOf("tag1"), entity.tags)
        assertEquals(false, entity.isArchived)
        assertEquals(SyncStatus.PENDING, entity.syncStatus)
    }

    @Test
    fun `toEntity handles null mood`() {
        val domain = JournalEntry(
            id = "j-2",
            date = LocalDate(2026, 3, 15),
            title = null,
            content = null,
            mood = null,
            tags = emptyList(),
            isArchived = false,
        )
        assertNull(domain.toEntity().mood)
    }

    // ── newJournalEntryId ───────────────────────────────

    @Test
    fun `newJournalEntryId generates non-empty string`() {
        val id = newJournalEntryId()
        assertTrue(id.isNotEmpty())
    }

    @Test
    fun `newJournalEntryId generates unique IDs`() {
        val ids = (1..100).map { newJournalEntryId() }.toSet()
        assertEquals(100, ids.size)
    }
}
