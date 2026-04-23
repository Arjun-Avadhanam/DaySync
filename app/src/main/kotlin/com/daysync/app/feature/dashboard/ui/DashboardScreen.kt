package com.daysync.app.feature.dashboard.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.daysync.app.core.notion.WeeklySummary
import com.daysync.app.core.sync.SyncState
import java.text.NumberFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToSection: (Any) -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val summary by viewModel.summary.collectAsState()
    val weeklySummary by viewModel.weeklySummary.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val isRestoring by viewModel.isRestoring.collectAsState()
    val restoreMessage by viewModel.restoreMessage.collectAsState()

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("DaySync") },
            actions = {
                IconButton(onClick = onNavigateToSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
            },
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Weekly AI Summary (from Claude via Notion)
            weeklySummary?.let { ws ->
                WeeklySummaryCard(summary = ws)
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Section summary cards
            SectionCard(
                icon = Icons.Default.FavoriteBorder,
                title = "Health",
                onClick = { onNavigateToSection(com.daysync.app.ui.navigation.Health) },
            ) {
                val steps = summary.steps?.let { NumberFormat.getIntegerInstance().format(it) } ?: "--"
                val cals = summary.calories?.let { "${it.toInt()} kcal" } ?: "--"
                val sleep = summary.sleepMinutes?.let { "${it / 60}h ${it % 60}m" } ?: "--"
                Text("Steps: $steps  •  Calories: $cals")
                Text("Last sleep: $sleep")
            }

            SectionCard(
                icon = Icons.Default.Restaurant,
                title = "Nutrition",
                onClick = { onNavigateToSection(com.daysync.app.ui.navigation.Nutrition) },
            ) {
                val consumed = summary.caloriesConsumed?.let { "${it.toInt()} kcal" } ?: "No entries"
                val protein = summary.proteinConsumed?.let { "${it.toInt()}g protein" } ?: ""
                Text("Today: $consumed${if (protein.isNotEmpty()) "  •  $protein" else ""}")
            }

            SectionCard(
                icon = Icons.Default.AccountBalanceWallet,
                title = "Expenses",
                onClick = { onNavigateToSection(com.daysync.app.ui.navigation.Expenses) },
            ) {
                val userPrefs = remember { com.daysync.app.core.config.UserPreferences(context) }
                val monthly = userPrefs.formatCurrency(summary.monthlyExpenses)
                Text("This month: $monthly")
            }

            SectionCard(
                icon = Icons.Default.EmojiEvents,
                title = "Sports",
                onClick = { onNavigateToSection(com.daysync.app.ui.navigation.Sports) },
            ) {
                Text("${summary.upcomingMatches} upcoming matches")
            }

            SectionCard(
                icon = Icons.Default.Book,
                title = "Journal",
                onClick = { onNavigateToSection(com.daysync.app.ui.navigation.Journal) },
            ) {
                Text("${summary.journalEntries} entries")
            }

            SectionCard(
                icon = Icons.Default.Movie,
                title = "Media",
                onClick = { onNavigateToSection(com.daysync.app.ui.navigation.Media) },
            ) {
                Text("${summary.mediaInProgress} in progress  •  ${summary.mediaDone} completed")
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(4.dp))

            // Sync section
            SyncSection(
                syncState = syncState,
                isSyncing = isSyncing,
                onSyncNow = viewModel::syncNow,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Restore from cloud
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Cloud Restore",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = restoreMessage ?: "Download data from Supabase after reinstall",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedButton(
                    onClick = viewModel::restoreFromCloud,
                    enabled = !isRestoring && !isSyncing,
                ) {
                    if (isRestoring) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text("Restore")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionCard(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                content()
            }
        }
    }
}

@Composable
private fun SyncSection(
    syncState: SyncState,
    isSyncing: Boolean,
    onSyncNow: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Cloud Sync",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            val statusText = when (syncState) {
                is SyncState.Idle -> "Ready to sync"
                is SyncState.Syncing -> "Syncing ${syncState.currentTable}..."
                is SyncState.Completed -> "${syncState.successCount} synced, ${syncState.failureCount} failed"
                is SyncState.Failed -> "Failed: ${syncState.error}"
            }
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Button(
            onClick = onSyncNow,
            enabled = !isSyncing,
        ) {
            if (isSyncing) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Sync Now")
        }
    }
}

@Composable
private fun WeeklySummaryCard(
    summary: WeeklySummary,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Weekly Insights",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                    Text(
                        text = summary.title,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
                    )
                }
                Text(
                    text = if (expanded) "Collapse" else "View",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.2f),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = summary.content,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
            }
        }
    }
}
