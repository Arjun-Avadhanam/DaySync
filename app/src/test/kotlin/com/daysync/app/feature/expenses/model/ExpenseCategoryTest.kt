package com.daysync.app.feature.expenses.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpenseCategoryTest {

    // ── Package-based suggestion ─────────────────────────

    @Test
    fun `Swiggy package suggests Food - Delivery`() {
        val result = ExpenseCategory.suggestFromMerchant(null, "in.swiggy.android")
        assertEquals("Food > Delivery", result)
    }

    @Test
    fun `Blinkit package suggests Food - Groceries`() {
        val result = ExpenseCategory.suggestFromMerchant(null, "com.grofers.customerapp")
        assertEquals("Food > Groceries", result)
    }

    @Test
    fun `Amazon package suggests Shopping`() {
        val result = ExpenseCategory.suggestFromMerchant(null, "in.amazon.mShop.android.shopping")
        assertEquals("Shopping", result)
    }

    @Test
    fun `CRED package suggests Bills`() {
        val result = ExpenseCategory.suggestFromMerchant(null, "com.dreamplug.androidapp")
        assertEquals("Bills", result)
    }

    @Test
    fun `unknown package returns null`() {
        val result = ExpenseCategory.suggestFromMerchant(null, "com.unknown.app")
        assertNull(result)
    }

    // ── Merchant-based suggestion ────────────────────────

    @Test
    fun `Swiggy merchant suggests Food - Delivery`() {
        val result = ExpenseCategory.suggestFromMerchant("Swiggy Delivery", null)
        assertEquals("Food > Delivery", result)
    }

    @Test
    fun `Zomato merchant suggests Food - Delivery`() {
        val result = ExpenseCategory.suggestFromMerchant("Zomato Order", null)
        assertEquals("Food > Delivery", result)
    }

    @Test
    fun `Uber merchant suggests Travel - Auto-Cab`() {
        val result = ExpenseCategory.suggestFromMerchant("Uber Trip", null)
        assertEquals("Travel > Auto/Cab", result)
    }

    @Test
    fun `Ola merchant suggests Travel - Auto-Cab`() {
        val result = ExpenseCategory.suggestFromMerchant("Ola Ride", null)
        assertEquals("Travel > Auto/Cab", result)
    }

    @Test
    fun `Netflix merchant suggests Bills - Subscriptions`() {
        val result = ExpenseCategory.suggestFromMerchant("Netflix Subscription", null)
        assertEquals("Bills > Subscriptions", result)
    }

    @Test
    fun `Flipkart merchant suggests Shopping`() {
        val result = ExpenseCategory.suggestFromMerchant("Flipkart Order", null)
        assertEquals("Shopping", result)
    }

    @Test
    fun `1mg merchant suggests Health - Medicine`() {
        val result = ExpenseCategory.suggestFromMerchant("1mg Pharmacy", null)
        assertEquals("Health > Medicine", result)
    }

    @Test
    fun `BookMyShow suggests Entertainment`() {
        val result = ExpenseCategory.suggestFromMerchant("BookMyShow Tickets", null)
        assertEquals("Entertainment", result)
    }

    @Test
    fun `unknown merchant returns null`() {
        val result = ExpenseCategory.suggestFromMerchant("Random Shop", null)
        assertNull(result)
    }

    @Test
    fun `package takes priority over merchant`() {
        val result = ExpenseCategory.suggestFromMerchant("Netflix", "in.swiggy.android")
        assertEquals("Food > Delivery", result)
    }

    // ── Category string parsing ──────────────────────────

    @Test
    fun `fromCategoryString parses top-level category`() {
        val result = ExpenseCategory.fromCategoryString("Food")
        assertNotNull(result)
        assertEquals(ExpenseCategory.FOOD, result!!.first)
        assertNull(result.second)
    }

    @Test
    fun `fromCategoryString parses category with subcategory`() {
        val result = ExpenseCategory.fromCategoryString("Food > Delivery")
        assertNotNull(result)
        assertEquals(ExpenseCategory.FOOD, result!!.first)
        assertEquals("Delivery", result.second)
    }

    @Test
    fun `fromCategoryString returns null for invalid category`() {
        val result = ExpenseCategory.fromCategoryString("NonExistent")
        assertNull(result)
    }

    // ── allCategoryStrings ───────────────────────────────

    @Test
    fun `allCategoryStrings includes subcategories`() {
        val all = ExpenseCategory.allCategoryStrings()
        assertTrue(all.contains("Food > Delivery"))
        assertTrue(all.contains("Travel > Metro"))
        assertTrue(all.contains("Bills > Subscriptions"))
    }

    @Test
    fun `allCategoryStrings includes categories without subcategories`() {
        val all = ExpenseCategory.allCategoryStrings()
        assertTrue(all.contains("Entertainment"))
        assertTrue(all.contains("Education"))
        assertTrue(all.contains("Other"))
    }
}
