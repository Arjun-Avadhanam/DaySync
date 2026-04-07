package com.daysync.app.feature.nutrition.ui.util

/**
 * Format a Double for display: show up to 2 decimal places if fractional,
 * otherwise show as whole number. Trims trailing zeros.
 * Examples: 1500.0 → "1500", 2.5 → "2.5", 3.14159 → "3.14"
 */
fun Double.fmtNutrition(): String {
    return if (this % 1.0 == 0.0) {
        "${this.toLong()}"
    } else {
        String.format("%.2f", this).trimEnd('0').trimEnd('.')
    }
}
