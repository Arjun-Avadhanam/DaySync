package com.daysync.app.feature.nutrition.ui.util

import org.junit.Assert.assertEquals
import org.junit.Test

class NumberFormatTest {

    @Test
    fun `whole number shows no decimals`() {
        assertEquals("1500", 1500.0.fmtNutrition())
    }

    @Test
    fun `zero shows as zero`() {
        assertEquals("0", 0.0.fmtNutrition())
    }

    @Test
    fun `one decimal place preserved`() {
        assertEquals("2.5", 2.5.fmtNutrition())
    }

    @Test
    fun `two decimal places preserved`() {
        assertEquals("3.14", 3.14.fmtNutrition())
    }

    @Test
    fun `trailing zeros trimmed`() {
        assertEquals("2.5", 2.50.fmtNutrition())
    }

    @Test
    fun `more than 2 decimals truncated`() {
        assertEquals("3.14", 3.14159.fmtNutrition())
    }

    @Test
    fun `small fraction shows correctly`() {
        assertEquals("0.25", 0.25.fmtNutrition())
    }

    @Test
    fun `large whole number`() {
        assertEquals("10000", 10000.0.fmtNutrition())
    }

    @Test
    fun `negative value`() {
        assertEquals("-1.5", (-1.5).fmtNutrition())
    }

    @Test
    fun `value like 1 point 0 shows as whole`() {
        assertEquals("1", 1.0.fmtNutrition())
    }

    @Test
    fun `value like 100 point 10 trims trailing zero`() {
        assertEquals("100.1", 100.1.fmtNutrition())
    }
}
