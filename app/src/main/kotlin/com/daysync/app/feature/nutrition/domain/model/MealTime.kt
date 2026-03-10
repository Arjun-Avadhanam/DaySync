package com.daysync.app.feature.nutrition.domain.model

enum class MealTime(val displayName: String, val dbValue: String) {
    BREAKFAST("Breakfast", "BREAKFAST"),
    LUNCH("Lunch", "LUNCH"),
    DINNER("Dinner", "DINNER"),
    SNACKS("Snacks", "SNACKS");

    companion object {
        fun fromDbValue(value: String): MealTime =
            entries.firstOrNull { it.dbValue == value } ?: SNACKS
    }
}
