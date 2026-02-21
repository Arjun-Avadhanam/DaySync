package com.daysync.app.feature.media.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StarHalf
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Star rating widget supporting half-star precision.
 *
 * [score] is on the 0.5-10 internal scale (displayed as 0.25-5.0 stars by dividing by 2).
 * In interactive mode, tapping the left half of a star sets a half value,
 * tapping the right half sets the full value.
 */
@Composable
fun MediaStarRating(
    score: Double?,
    modifier: Modifier = Modifier,
    interactive: Boolean = false,
    onScoreChange: ((Double?) -> Unit)? = null,
    starSize: Dp = 24.dp,
    starColor: Color = MaterialTheme.colorScheme.primary,
) {
    val displayStars = (score ?: 0.0) / 2.0 // Convert 0-10 to 0-5

    Row(modifier = modifier) {
        for (i in 1..5) {
            val starModifier = if (interactive && onScoreChange != null) {
                Modifier
                    .size(starSize)
                    .clickable {
                        val currentDisplay = (score ?: 0.0) / 2.0
                        // Toggle between half and full; if already full, set half; if half, clear
                        val newDisplay = when {
                            currentDisplay == i.toDouble() -> i - 0.5
                            currentDisplay == i - 0.5 -> if (i == 1) 0.0 else (i - 0.5)
                            else -> i.toDouble()
                        }
                        onScoreChange(if (newDisplay == 0.0) null else newDisplay * 2.0)
                    }
            } else {
                Modifier.size(starSize)
            }

            when {
                displayStars >= i -> Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = "Star $i filled",
                    modifier = starModifier,
                    tint = starColor,
                )
                displayStars >= i - 0.5 -> Icon(
                    imageVector = Icons.AutoMirrored.Filled.StarHalf,
                    contentDescription = "Star $i half",
                    modifier = starModifier,
                    tint = starColor,
                )
                else -> Icon(
                    imageVector = Icons.Filled.StarOutline,
                    contentDescription = "Star $i empty",
                    modifier = starModifier,
                    tint = if (interactive) starColor.copy(alpha = 0.4f) else starColor.copy(alpha = 0.3f),
                )
            }
        }
    }
}
