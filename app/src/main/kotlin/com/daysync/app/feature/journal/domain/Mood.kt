package com.daysync.app.feature.journal.domain

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MoodBad
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class Mood(
    val intValue: Int,
    val label: String,
    val icon: ImageVector,
    val color: Color,
) {
    SAD(1, "Sad", Icons.Default.WaterDrop, Color(0xFF5C6BC0)),
    GUILTY(2, "Guilty", Icons.Default.MoodBad, Color(0xFF7E57C2)),
    STRESSED(3, "Stressed", Icons.Default.FlashOn, Color(0xFFEF5350)),
    UNMOTIVATED(4, "Unmotivated", Icons.AutoMirrored.Filled.TrendingDown, Color(0xFFFF7043)),
    NEUTRAL(5, "Neutral", Icons.Default.Circle, Color(0xFF78909C)),
    MOTIVATED(6, "Motivated", Icons.Default.LocalFireDepartment, Color(0xFFFFA726)),
    HAPPY(7, "Happy", Icons.Default.WbSunny, Color(0xFF66BB6A));

    companion object {
        fun fromInt(value: Int?): Mood? = value?.let { v -> entries.find { it.intValue == v } }
    }
}
