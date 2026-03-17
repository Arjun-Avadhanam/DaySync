package com.daysync.app.feature.expenses.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ExpenseModelTest {

    // ── formatIndianCurrency ─────────────────────────────

    @Test
    fun `formats small amount`() {
        assertEquals("₹50", formatIndianCurrency(50.0))
    }

    @Test
    fun `formats hundreds`() {
        assertEquals("₹500", formatIndianCurrency(500.0))
    }

    @Test
    fun `formats thousands with comma`() {
        assertEquals("₹1,500", formatIndianCurrency(1500.0))
    }

    @Test
    fun `formats lakhs with Indian grouping`() {
        assertEquals("₹1,25,000", formatIndianCurrency(125000.0))
    }

    @Test
    fun `formats crores with Indian grouping`() {
        assertEquals("₹1,00,00,000", formatIndianCurrency(10000000.0))
    }

    @Test
    fun `formats decimal amount`() {
        assertEquals("₹1,250.50", formatIndianCurrency(1250.50))
    }

    @Test
    fun `formats negative amount`() {
        assertEquals("-₹500", formatIndianCurrency(-500.0))
    }

    @Test
    fun `formats zero`() {
        assertEquals("₹0", formatIndianCurrency(0.0))
    }

    // ── Expense.displayTitle ─────────────────────────────

    @Test
    fun `displayTitle prefers title`() {
        val expense = Expense(
            id = "1", date = kotlinx.datetime.LocalDate(2026, 3, 15),
            unitCost = 100.0, totalAmount = 100.0,
            title = "Lunch", merchantName = "Swiggy", item = "Biryani",
        )
        assertEquals("Lunch", expense.displayTitle)
    }

    @Test
    fun `displayTitle falls back to merchantName`() {
        val expense = Expense(
            id = "1", date = kotlinx.datetime.LocalDate(2026, 3, 15),
            unitCost = 100.0, totalAmount = 100.0,
            title = null, merchantName = "Swiggy", item = "Biryani",
        )
        assertEquals("Swiggy", expense.displayTitle)
    }

    @Test
    fun `displayTitle falls back to item`() {
        val expense = Expense(
            id = "1", date = kotlinx.datetime.LocalDate(2026, 3, 15),
            unitCost = 100.0, totalAmount = 100.0,
            title = null, merchantName = null, item = "Biryani",
        )
        assertEquals("Biryani", expense.displayTitle)
    }

    @Test
    fun `displayTitle defaults to Expense`() {
        val expense = Expense(
            id = "1", date = kotlinx.datetime.LocalDate(2026, 3, 15),
            unitCost = 100.0, totalAmount = 100.0,
        )
        assertEquals("Expense", expense.displayTitle)
    }

    @Test
    fun `formattedAmount uses Indian currency format`() {
        val expense = Expense(
            id = "1", date = kotlinx.datetime.LocalDate(2026, 3, 15),
            unitCost = 1250.50, totalAmount = 1250.50,
        )
        assertEquals("₹1,250.50", expense.formattedAmount)
    }
}
