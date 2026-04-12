package com.daysync.app.feature.dashboard.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private data class GuidePage(
    val icon: ImageVector,
    val title: String,
    val features: List<String>,
)

private val guidePages = listOf(
    GuidePage(
        icon = Icons.Default.Home,
        title = "Home",
        features = listOf(
            "Summary cards for every section — tap any card to jump directly to that section",
            "Cloud sync at the bottom — tap Sync Now to push all data to Supabase",
            "Settings gear icon (top-right) to manage permissions and view this guide",
        ),
    ),
    GuidePage(
        icon = Icons.Default.FavoriteBorder,
        title = "Health",
        features = listOf(
            "Daily summary with steps, calories, heart rate, SpO2, and floors",
            "Date navigation arrows to view any past day's health data",
            "Tap the Calories tile to manually override the value from OHealth",
            "Multiple sleep sessions shown with individual time windows",
            "Tap strength training or workout entries to assign sub-types (Push/Pull/Legs)",
            "Charts for steps, heart rate, sleep, and workout trends (7 Days / 30 Days)",
            "Workout by Type filter — select a type and optionally a sub-type to see per-workout charts",
            "Calorie deficit/surplus calculated from Health (burned) and Nutrition (consumed)",
        ),
    ),
    GuidePage(
        icon = Icons.Default.Restaurant,
        title = "Nutrition",
        features = listOf(
            "Food library with 257+ items imported live from Notion",
            "Import from Notion pulls your current Master Meal Library database",
            "Scan nutrition labels with AI (Gemini) — take a photo, get auto-extracted data",
            "Daily meal tracking with breakfast/lunch/dinner/snacks",
            "Meal templates for quick repeat entries",
            "Daily summary with calories, protein, carbs, fat, sugar, and water",
        ),
    ),
    GuidePage(
        icon = Icons.Default.AccountBalanceWallet,
        title = "Expenses",
        features = listOf(
            "Auto-tracks payments from bank SMS via notification listener",
            "Payee rules auto-categorize recurring payees — set once, applied to all future payments",
            "Manual entry, CSV import, and AI receipt scanning",
            "Monthly totals with category breakdown chips",
            "Dedup by reference ID — same-amount payments to different payees stay separate",
        ),
    ),
    GuidePage(
        icon = Icons.Default.EmojiEvents,
        title = "Sports",
        features = listOf(
            "Upcoming, Live, Results, and Watchlist tabs across football, basketball, tennis, F1, MMA",
            "Manage Following — toggle competitions on/off to control what's fetched and displayed",
            "Team search — search icon in top bar, find any team, view their matches, star to watchlist",
            "Star events to add to the Watchlist tab for quick access",
            "Football covers 16 competitions via ESPN with a ±7 day window",
            "Live polling via API-Football every 60 seconds when on the Live tab",
        ),
    ),
    GuidePage(
        icon = Icons.Default.Book,
        title = "Journal",
        features = listOf(
            "Rich text journal entries with mood and reflection tags",
            "Calendar and list views to browse past entries",
            "Save entries to your Notion Journal database",
        ),
    ),
    GuidePage(
        icon = Icons.Default.Movie,
        title = "Media",
        features = listOf(
            "Track books, movies, TV series, anime, manga, music, games, podcasts, and more",
            "Search powered by OMDb, Google Books, Jikan (MAL), Steam, iTunes, OpenLibrary",
            "Type-ahead creator suggestions from your existing library",
            "Star rating (0.5–10 scale), status tracking, and completion dates",
            "Save entries to your Notion Books/Movies database",
        ),
    ),
    GuidePage(
        icon = Icons.Default.AutoAwesome,
        title = "AI Assistant",
        features = listOf(
            "Tap the sparkle FAB (bottom-right) on any screen to open the AI chat",
            "Ask questions about your data: health trends, expenses, nutrition, workouts",
            "AI has full context of your stored data — steps, sleep, meals, expenses, media, journal",
            "Powered by Gemini 2.5 Flash with Groq/Llama fallback",
            "Nutrition label scanning and receipt scanning also use AI",
        ),
    ),
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AppGuideScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(pageCount = { guidePages.size })
    val scope = rememberCoroutineScope()

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("App Guide") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
        ) { page ->
            GuidePageContent(guidePages[page])
        }

        // Dot indicators + navigation
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Dots
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(guidePages.size) { index ->
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = if (index == pagerState.currentPage) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                },
                                shape = CircleShape,
                            ),
                    )
                }
            }

            // Next / Done button
            if (pagerState.currentPage < guidePages.size - 1) {
                Button(onClick = {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }) {
                    Text("Next")
                }
            } else {
                Button(onClick = onBack) {
                    Text("Done")
                }
            }
        }
    }
}

@Composable
private fun GuidePageContent(page: GuidePage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Icon(
            imageVector = page.icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            page.features.forEach { feature ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text("•  ", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = feature,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
