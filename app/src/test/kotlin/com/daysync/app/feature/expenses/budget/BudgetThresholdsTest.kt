package com.daysync.app.feature.expenses.budget

import org.junit.Assert.assertEquals
import org.junit.Test

class BudgetThresholdsTest {
    @Test
    fun `crossedLevel returns highest reached band`() {
        assertEquals(0, BudgetThresholds.crossedLevel(10.0, 100.0))
        assertEquals(50, BudgetThresholds.crossedLevel(50.0, 100.0))
        assertEquals(75, BudgetThresholds.crossedLevel(80.0, 100.0))
        assertEquals(100, BudgetThresholds.crossedLevel(100.0, 100.0))
        assertEquals(100, BudgetThresholds.crossedLevel(140.0, 100.0))
    }

    @Test
    fun `zero or negative amount never crosses`() {
        assertEquals(0, BudgetThresholds.crossedLevel(50.0, 0.0))
    }

    @Test
    fun `notifies only when crossing a higher band`() {
        assertEquals(AlertDecision(50, 50), BudgetThresholds.evaluate(60.0, 100.0, lastNotified = 0))
        // already notified 50, now at 80 -> notify 75
        assertEquals(AlertDecision(75, 75), BudgetThresholds.evaluate(80.0, 100.0, lastNotified = 50))
        // still at 80, already notified 75 -> no notify, stays 75
        assertEquals(AlertDecision(null, 75), BudgetThresholds.evaluate(80.0, 100.0, lastNotified = 75))
    }

    @Test
    fun `re-arms downward without notifying when spend drops`() {
        // was at 100 (notified 100), an edit drops spend to 60% -> stored re-arms to 50, no notify
        assertEquals(AlertDecision(null, 50), BudgetThresholds.evaluate(60.0, 100.0, lastNotified = 100))
        // then it climbs back to 100 -> notifies 100 again
        assertEquals(AlertDecision(100, 100), BudgetThresholds.evaluate(100.0, 100.0, lastNotified = 50))
    }
}
