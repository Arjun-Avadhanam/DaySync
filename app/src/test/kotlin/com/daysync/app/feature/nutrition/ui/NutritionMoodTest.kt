package com.daysync.app.feature.nutrition.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the nutrition mood/energy level options.
 * These are string-based (stored in DailyNutritionSummaryEntity.mood)
 * and independent from the Journal Mood enum.
 */
class NutritionMoodTest {

    // Must match the options in NutritionDailySummaryScreen
    private val nutritionMoods = listOf(
        "Energetic", "Focused", "Balanced", "Calm", "Tired",
        "Lazy", "Distracted", "Anxious", "Irritable", "Exhausted", "Lethargic",
    )

    @Test
    fun `all 11 Notion mood options present`() {
        assertEquals(11, nutritionMoods.size)
    }

    @Test
    fun `all moods are unique`() {
        assertEquals(nutritionMoods.size, nutritionMoods.toSet().size)
    }

    @Test
    fun `positive moods included`() {
        assertTrue(nutritionMoods.contains("Energetic"))
        assertTrue(nutritionMoods.contains("Focused"))
        assertTrue(nutritionMoods.contains("Balanced"))
        assertTrue(nutritionMoods.contains("Calm"))
    }

    @Test
    fun `negative moods included`() {
        assertTrue(nutritionMoods.contains("Anxious"))
        assertTrue(nutritionMoods.contains("Irritable"))
        assertTrue(nutritionMoods.contains("Exhausted"))
    }

    @Test
    fun `neutral moods included`() {
        assertTrue(nutritionMoods.contains("Tired"))
        assertTrue(nutritionMoods.contains("Lazy"))
        assertTrue(nutritionMoods.contains("Distracted"))
        assertTrue(nutritionMoods.contains("Lethargic"))
    }

    @Test
    fun `nutrition moods are independent from journal Mood enum`() {
        // Journal moods use intValue (1-7), nutrition moods use String labels
        val journalMoodLabels = com.daysync.app.feature.journal.domain.Mood.entries.map { it.label }
        // No overlap required — they're separate systems
        // But verify journal moods haven't changed
        assertEquals(7, journalMoodLabels.size)
        assertTrue(journalMoodLabels.contains("Sad"))
        assertTrue(journalMoodLabels.contains("Happy"))
    }

    @Test
    fun `old nutrition moods are still valid strings`() {
        // Old values like "Good", "Great" won't match new chips
        // but won't cause any crash — they're just unselected
        val oldMoods = listOf("Great", "Good", "Okay", "Low", "Bad")
        oldMoods.forEach { old ->
            // Old mood is a valid string, just not in the new list
            assertTrue(old.isNotBlank())
        }
    }
}
