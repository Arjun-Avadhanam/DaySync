package com.daysync.app.feature.expenses.budget

data class AlertDecision(val notifyLevel: Int?, val newStoredLevel: Int)

object BudgetThresholds {
    val LEVELS = listOf(50, 75, 100)

    fun crossedLevel(spent: Double, amount: Double): Int {
        if (amount <= 0.0) return 0
        val pct = spent / amount * 100.0
        return LEVELS.filter { pct >= it }.maxOrNull() ?: 0
    }

    fun evaluate(spent: Double, amount: Double, lastNotified: Int): AlertDecision {
        val current = crossedLevel(spent, amount)
        return if (current > lastNotified) AlertDecision(current, current)
        else AlertDecision(null, current) // re-arm downward, never notify on a drop
    }
}
