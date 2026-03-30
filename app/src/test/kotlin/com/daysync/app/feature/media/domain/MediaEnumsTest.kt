package com.daysync.app.feature.media.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaEnumsTest {

    // ── MediaType.fromString ────────────────────────────

    @Test
    fun `fromString BOOK`() {
        assertEquals(MediaType.BOOK, MediaType.fromString("BOOK"))
    }

    @Test
    fun `fromString MOVIE`() {
        assertEquals(MediaType.MOVIE, MediaType.fromString("MOVIE"))
    }

    @Test
    fun `fromString FILM maps to MOVIE for backward compat`() {
        assertEquals(MediaType.MOVIE, MediaType.fromString("FILM"))
    }

    @Test
    fun `fromString TV_SERIES`() {
        assertEquals(MediaType.TV_SERIES, MediaType.fromString("TV_SERIES"))
    }

    @Test
    fun `fromString GAME`() {
        assertEquals(MediaType.GAME, MediaType.fromString("GAME"))
    }

    @Test
    fun `fromString ANIME`() {
        assertEquals(MediaType.ANIME, MediaType.fromString("ANIME"))
    }

    @Test
    fun `fromString all types`() {
        MediaType.entries.forEach { type ->
            assertEquals(type, MediaType.fromString(type.name))
        }
    }

    @Test
    fun `fromString unknown defaults to BOOK`() {
        assertEquals(MediaType.BOOK, MediaType.fromString("UNKNOWN"))
    }

    @Test
    fun `fromString empty defaults to BOOK`() {
        assertEquals(MediaType.BOOK, MediaType.fromString(""))
    }

    // ── MediaType categories ────────────────────────────

    @Test
    fun `FILM_AND_TV contains correct types`() {
        assertTrue(MediaType.FILM_AND_TV.contains(MediaType.MOVIE))
        assertTrue(MediaType.FILM_AND_TV.contains(MediaType.TV_SERIES))
        assertFalse(MediaType.FILM_AND_TV.contains(MediaType.BOOK))
    }

    @Test
    fun `READING contains correct types`() {
        assertTrue(MediaType.READING.contains(MediaType.BOOK))
        assertTrue(MediaType.READING.contains(MediaType.ARTICLE))
        assertTrue(MediaType.READING.contains(MediaType.COMIC))
        assertTrue(MediaType.READING.contains(MediaType.MANGA))
        assertFalse(MediaType.READING.contains(MediaType.MOVIE))
    }

    // ── MediaType.isVisualHeavy ─────────────────────────

    @Test
    fun `visual heavy types`() {
        assertTrue(MediaType.BOOK.isVisualHeavy)
        assertTrue(MediaType.MOVIE.isVisualHeavy)
        assertTrue(MediaType.TV_SERIES.isVisualHeavy)
        assertTrue(MediaType.ANIME.isVisualHeavy)
        assertTrue(MediaType.GAME.isVisualHeavy)
    }

    @Test
    fun `non-visual types`() {
        assertFalse(MediaType.ARTICLE.isVisualHeavy)
        assertFalse(MediaType.PODCAST.isVisualHeavy)
        assertFalse(MediaType.MUSIC.isVisualHeavy)
    }

    // ── MediaType.displayName ───────────────────────────

    @Test
    fun `displayName formatting`() {
        assertEquals("Book", MediaType.BOOK.displayName)
        assertEquals("TV Series", MediaType.TV_SERIES.displayName)
        assertEquals("Movie", MediaType.MOVIE.displayName)
        assertEquals("Podcast", MediaType.PODCAST.displayName)
    }

    // ── MediaStatus.fromString ──────────────────────────

    @Test
    fun `fromString NOT_STARTED`() {
        assertEquals(MediaStatus.NOT_STARTED, MediaStatus.fromString("NOT_STARTED"))
    }

    @Test
    fun `fromString IN_PROGRESS`() {
        assertEquals(MediaStatus.IN_PROGRESS, MediaStatus.fromString("IN_PROGRESS"))
    }

    @Test
    fun `fromString DONE`() {
        assertEquals(MediaStatus.DONE, MediaStatus.fromString("DONE"))
    }

    @Test
    fun `fromString DROPPED`() {
        assertEquals(MediaStatus.DROPPED, MediaStatus.fromString("DROPPED"))
    }

    @Test
    fun `fromString all statuses`() {
        MediaStatus.entries.forEach { status ->
            assertEquals(status, MediaStatus.fromString(status.name))
        }
    }

    @Test
    fun `fromString unknown defaults to NOT_STARTED`() {
        assertEquals(MediaStatus.NOT_STARTED, MediaStatus.fromString("UNKNOWN"))
    }

    // ── MediaStatus.displayName ─────────────────────────

    @Test
    fun `status displayName formatting`() {
        assertEquals("Not Started", MediaStatus.NOT_STARTED.displayName)
        assertEquals("In Progress", MediaStatus.IN_PROGRESS.displayName)
        assertEquals("Done", MediaStatus.DONE.displayName)
        assertEquals("Dropped", MediaStatus.DROPPED.displayName)
    }
}
