package com.daysync.app.feature.nutrition.domain.model

import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class NutritionModelsTest {

    // ── MealTime.fromDbValue ────────────────────────────

    @Test
    fun `fromDbValue BREAKFAST`() {
        assertEquals(MealTime.BREAKFAST, MealTime.fromDbValue("BREAKFAST"))
    }

    @Test
    fun `fromDbValue LUNCH`() {
        assertEquals(MealTime.LUNCH, MealTime.fromDbValue("LUNCH"))
    }

    @Test
    fun `fromDbValue DINNER`() {
        assertEquals(MealTime.DINNER, MealTime.fromDbValue("DINNER"))
    }

    @Test
    fun `fromDbValue SNACKS`() {
        assertEquals(MealTime.SNACKS, MealTime.fromDbValue("SNACKS"))
    }

    @Test
    fun `fromDbValue unknown defaults to SNACKS`() {
        assertEquals(MealTime.SNACKS, MealTime.fromDbValue("BRUNCH"))
    }

    @Test
    fun `fromDbValue empty defaults to SNACKS`() {
        assertEquals(MealTime.SNACKS, MealTime.fromDbValue(""))
    }

    @Test
    fun `MealTime displayName`() {
        assertEquals("Breakfast", MealTime.BREAKFAST.displayName)
        assertEquals("Lunch", MealTime.LUNCH.displayName)
        assertEquals("Dinner", MealTime.DINNER.displayName)
        assertEquals("Snacks", MealTime.SNACKS.displayName)
    }

    // ── UnitType.fromDbValue ────────────────────────────

    @Test
    fun `fromDbValue PIECES`() {
        assertEquals(UnitType.PIECES, UnitType.fromDbValue("PIECES"))
    }

    @Test
    fun `fromDbValue GRAMS`() {
        assertEquals(UnitType.GRAMS, UnitType.fromDbValue("GRAMS"))
    }

    @Test
    fun `fromDbValue ML`() {
        assertEquals(UnitType.ML, UnitType.fromDbValue("ML"))
    }

    @Test
    fun `fromDbValue all types`() {
        UnitType.entries.forEach { unitType ->
            assertEquals(unitType, UnitType.fromDbValue(unitType.name))
        }
    }

    @Test
    fun `fromDbValue unknown defaults to SERVING`() {
        assertEquals(UnitType.SERVING, UnitType.fromDbValue("UNKNOWN"))
    }

    @Test
    fun `UnitType symbols`() {
        assertEquals("pcs", UnitType.PIECES.symbol)
        assertEquals("g", UnitType.GRAMS.symbol)
        assertEquals("ml", UnitType.ML.symbol)
        assertEquals("cup", UnitType.CUPS.symbol)
        assertEquals("tbsp", UnitType.TABLESPOON.symbol)
        assertEquals("tsp", UnitType.TEASPOON.symbol)
        assertEquals("serving", UnitType.SERVING.symbol)
    }

    // ── MealEntryWithFood.from ──────────────────────────

    private val sampleFood = FoodItem(
        id = "food-1",
        name = "Rice",
        category = "Grains",
        caloriesPerUnit = 130.0,
        proteinPerUnit = 2.7,
        carbsPerUnit = 28.0,
        fatPerUnit = 0.3,
        sugarPerUnit = 0.1,
        unitType = UnitType.CUPS,
        servingDescription = "1 cup cooked",
    )

    private val sampleEntry = MealEntry(
        id = "entry-1",
        date = LocalDate(2026, 3, 15),
        foodId = "food-1",
        mealTime = MealTime.LUNCH,
        amount = 2.0,
        notes = null,
    )

    @Test
    fun `from multiplies nutrients by amount`() {
        val result = MealEntryWithFood.from(sampleEntry, sampleFood)
        assertEquals(260.0, result.calories, 0.01)  // 130 * 2
        assertEquals(5.4, result.protein, 0.01)      // 2.7 * 2
        assertEquals(56.0, result.carbs, 0.01)       // 28 * 2
        assertEquals(0.6, result.fat, 0.01)           // 0.3 * 2
        assertEquals(0.2, result.sugar, 0.01)         // 0.1 * 2
    }

    @Test
    fun `from with amount 1 returns per-unit values`() {
        val entry = sampleEntry.copy(amount = 1.0)
        val result = MealEntryWithFood.from(entry, sampleFood)
        assertEquals(130.0, result.calories, 0.01)
        assertEquals(2.7, result.protein, 0.01)
    }

    @Test
    fun `from with fractional amount`() {
        val entry = sampleEntry.copy(amount = 0.5)
        val result = MealEntryWithFood.from(entry, sampleFood)
        assertEquals(65.0, result.calories, 0.01)   // 130 * 0.5
        assertEquals(1.35, result.protein, 0.01)     // 2.7 * 0.5
    }

    @Test
    fun `from with zero amount`() {
        val entry = sampleEntry.copy(amount = 0.0)
        val result = MealEntryWithFood.from(entry, sampleFood)
        assertEquals(0.0, result.calories, 0.01)
        assertEquals(0.0, result.protein, 0.01)
    }

    @Test
    fun `from preserves entry and food references`() {
        val result = MealEntryWithFood.from(sampleEntry, sampleFood)
        assertEquals(sampleEntry, result.entry)
        assertEquals(sampleFood, result.food)
    }

    @Test
    fun `from with large amount`() {
        val entry = sampleEntry.copy(amount = 10.0)
        val result = MealEntryWithFood.from(entry, sampleFood)
        assertEquals(1300.0, result.calories, 0.01)
    }
}
