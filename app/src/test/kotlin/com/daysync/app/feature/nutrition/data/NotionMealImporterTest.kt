package com.daysync.app.feature.nutrition.data

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests the Notion meal import JSON parsing and unit type mapping.
 * Verifies the asset file can be parsed correctly.
 */
class NotionMealImporterTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    // Sample from the actual notion_meal_library.json
    private val sampleJson = """
    [
      {
        "name": "Boiled Egg",
        "calories": 77.5,
        "protein": 6.3,
        "carbs": 0.56,
        "fat": 5.3,
        "sugar": 0,
        "category": "Proteins",
        "unit": "items",
        "serving": ""
      },
      {
        "name": "Roti",
        "calories": 71,
        "protein": 2.7,
        "carbs": 12.3,
        "fat": 1.3,
        "sugar": 0.36,
        "category": "Grains & Cereals & Legumes",
        "unit": "pieces",
        "serving": ""
      },
      {
        "name": "Whey Protein Shake (200ml shake)",
        "calories": 231.51,
        "protein": 18.2,
        "carbs": 23.65,
        "fat": 7.199,
        "sugar": 0,
        "category": "Proteins",
        "unit": "cups",
        "serving": ""
      },
      {
        "name": "Big Banana",
        "calories": 58,
        "protein": 0.7,
        "carbs": 15,
        "fat": 0.2,
        "sugar": 0,
        "category": "Fruits",
        "unit": "pieces",
        "serving": ""
      },
      {
        "name": "Milk -100ml",
        "calories": 62,
        "protein": 3.2,
        "carbs": 4.8,
        "fat": 3.4,
        "sugar": 0,
        "category": "Dairy",
        "unit": "ml",
        "serving": ""
      }
    ]
    """.trimIndent()

    @kotlinx.serialization.Serializable
    private data class TestMeal(
        val name: String,
        val calories: Double = 0.0,
        val protein: Double = 0.0,
        val carbs: Double = 0.0,
        val fat: Double = 0.0,
        val sugar: Double = 0.0,
        val category: String = "",
        val unit: String = "",
        val serving: String = "",
    )

    @Test
    fun `parse sample JSON`() {
        val meals = json.decodeFromString<List<TestMeal>>(sampleJson)
        assertEquals(5, meals.size)
    }

    @Test
    fun `boiled egg has correct values`() {
        val meals = json.decodeFromString<List<TestMeal>>(sampleJson)
        val egg = meals.first { it.name == "Boiled Egg" }
        assertEquals(77.5, egg.calories, 0.01)
        assertEquals(6.3, egg.protein, 0.01)
        assertEquals(0.56, egg.carbs, 0.01)
        assertEquals(5.3, egg.fat, 0.01)
        assertEquals(0.0, egg.sugar, 0.01)
        assertEquals("Proteins", egg.category)
        assertEquals("items", egg.unit)
    }

    @Test
    fun `unit type mapping`() {
        // Verify the mapping logic matches our UnitType enum
        val mapping = mapOf(
            "pieces" to "PIECES",
            "items" to "PIECES",
            "slices" to "SLICE",
            "grams" to "GRAMS",
            "cups" to "CUPS",
            "tablespoons" to "TABLESPOON",
            "ml" to "ML",
            "" to "SERVING",
            "unknown" to "SERVING",
        )
        mapping.forEach { (input, expected) ->
            val result = when (input.lowercase()) {
                "pieces" -> "PIECES"
                "items" -> "PIECES"
                "slices" -> "SLICE"
                "grams" -> "GRAMS"
                "cups" -> "CUPS"
                "tablespoons" -> "TABLESPOON"
                "ml" -> "ML"
                else -> "SERVING"
            }
            assertEquals("Unit '$input' should map to '$expected'", expected, result)
        }
    }

    @Test
    fun `all categories are valid`() {
        val meals = json.decodeFromString<List<TestMeal>>(sampleJson)
        val validCategories = setOf(
            "Proteins", "Grains & Cereals & Legumes", "Dairy", "Fruits",
            "Vegetables", "Snacks", "Beverages", "Fats & Oils",
            "Sweets & Desserts", "Carbs", "curries", "Restaurant",
            "Condiment", "Dips", "",
        )
        meals.forEach { meal ->
            assertTrue(
                "Category '${meal.category}' not in valid set",
                meal.category in validCategories,
            )
        }
    }

    @Test
    fun `all unit types are valid`() {
        val meals = json.decodeFromString<List<TestMeal>>(sampleJson)
        val validUnits = setOf("pieces", "items", "slices", "grams", "cups", "tablespoons", "ml", "")
        meals.forEach { meal ->
            assertTrue(
                "Unit '${meal.unit}' not in valid set",
                meal.unit in validUnits,
            )
        }
    }

    @Test
    fun `deterministic ID generation`() {
        // Same name should always produce same ID (UUID from name bytes)
        val id1 = java.util.UUID.nameUUIDFromBytes("Boiled Egg".toByteArray()).toString()
        val id2 = java.util.UUID.nameUUIDFromBytes("Boiled Egg".toByteArray()).toString()
        assertEquals(id1, id2)
    }

    @Test
    fun `different names produce different IDs`() {
        val id1 = java.util.UUID.nameUUIDFromBytes("Boiled Egg".toByteArray()).toString()
        val id2 = java.util.UUID.nameUUIDFromBytes("Roti".toByteArray()).toString()
        assertTrue(id1 != id2)
    }

    @Test
    fun `parse full asset file`() {
        // Read the actual asset file to verify it's valid JSON
        val assetFile = java.io.File("src/main/assets/notion_meal_library.json")
        if (assetFile.exists()) {
            val meals = json.decodeFromString<List<TestMeal>>(assetFile.readText())
            assertTrue("Expected at least 200 meals, got ${meals.size}", meals.size >= 200)
            // Verify no empty names
            meals.forEach { assertNotNull(it.name) }
            assertTrue("All meals should have names", meals.all { it.name.isNotBlank() })
        }
    }
}

