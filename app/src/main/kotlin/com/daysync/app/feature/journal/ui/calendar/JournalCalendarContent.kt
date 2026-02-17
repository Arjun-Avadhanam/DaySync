package com.daysync.app.feature.journal.ui.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.daysync.app.feature.journal.domain.JournalEntry
import com.daysync.app.feature.journal.ui.list.JournalEntryCard
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalCalendarContent(
    yearMonth: YearMonth,
    entries: Map<LocalDate, JournalEntry>,
    onBack: () -> Unit,
    onMonthChange: (YearMonth) -> Unit,
    onDayClick: (LocalDate, Boolean) -> Unit,
    onEntryClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendar") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        modifier = modifier,
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Month navigation
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { onMonthChange(yearMonth.minusMonths(1)) }) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Previous month",
                        )
                    }
                    Text(
                        text = "${yearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${yearMonth.year}",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    IconButton(onClick = { onMonthChange(yearMonth.plusMonths(1)) }) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Next month",
                        )
                    }
                }
            }

            // Calendar grid
            item {
                JournalCalendarGrid(
                    yearMonth = yearMonth,
                    entries = entries,
                    onDayClick = { date ->
                        onDayClick(date, entries.containsKey(date))
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Entries for this month
            val sortedEntries = entries.entries.sortedByDescending { it.key }
            if (sortedEntries.isNotEmpty()) {
                item {
                    Text(
                        text = "Entries this month",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                items(sortedEntries, key = { it.value.id }) { (_, entry) ->
                    JournalEntryCard(
                        entry = entry,
                        onClick = { onEntryClick(entry.id) },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}
