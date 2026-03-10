package com.daysync.app.feature.sports.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.daysync.app.feature.sports.ui.SportsTab

@Composable
fun SportsTabRow(
    selectedTab: SportsTab,
    liveCount: Int,
    onTabSelected: (SportsTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    PrimaryTabRow(
        selectedTabIndex = selectedTab.ordinal,
        modifier = modifier,
    ) {
        SportsTab.entries.forEach { tab ->
            Tab(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                text = {
                    if (tab == SportsTab.LIVE && liveCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.error,
                                ) {
                                    Text("$liveCount")
                                }
                            }
                        ) {
                            Text(tab.label)
                        }
                    } else {
                        Text(tab.label)
                    }
                },
            )
        }
    }
}

private val SportsTab.label: String
    get() = when (this) {
        SportsTab.UPCOMING -> "Upcoming"
        SportsTab.LIVE -> "Live"
        SportsTab.RESULTS -> "Results"
        SportsTab.WATCHLIST -> "Watchlist"
    }
