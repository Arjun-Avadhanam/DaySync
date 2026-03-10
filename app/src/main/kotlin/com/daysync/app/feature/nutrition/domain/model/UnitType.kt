package com.daysync.app.feature.nutrition.domain.model

enum class UnitType(val displayName: String, val symbol: String) {
    PIECES("Pieces", "pcs"),
    GRAMS("Grams", "g"),
    CUPS("Cups", "cup"),
    ML("Millilitres", "ml"),
    TABLESPOON("Tablespoon", "tbsp"),
    TEASPOON("Teaspoon", "tsp"),
    BOWL("Bowl", "bowl"),
    PLATE("Plate", "plate"),
    SLICE("Slice", "slice"),
    SERVING("Serving", "serving");

    companion object {
        fun fromDbValue(value: String): UnitType =
            entries.firstOrNull { it.name == value } ?: SERVING
    }
}
