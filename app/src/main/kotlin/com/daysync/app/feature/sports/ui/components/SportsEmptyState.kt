package com.daysync.app.feature.sports.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.Scoreboard
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.daysync.app.feature.sports.ui.SportsTab

@Composable
fun SportsEmptyState(
    tab: SportsTab,
    modifier: Modifier = Modifier,
) {
    val (icon, title, subtitle) = when (tab) {
        SportsTab.UPCOMING -> Triple(
            Icons.Outlined.EmojiEvents,
            "No upcoming events",
            "Pull to refresh to load fixtures",
        )
        SportsTab.LIVE -> Triple(
            Icons.Outlined.LiveTv,
            "No live events",
            "Check back when matches are in progress",
        )
        SportsTab.RESULTS -> Triple(
            Icons.Outlined.Scoreboard,
            "No recent results",
            "Results will appear after matches complete",
        )
        SportsTab.WATCHLIST -> Triple(
            Icons.Outlined.Star,
            "Your watchlist is empty",
            "Star events to add them to your watchlist",
        )
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
            )
        }
    }
}
