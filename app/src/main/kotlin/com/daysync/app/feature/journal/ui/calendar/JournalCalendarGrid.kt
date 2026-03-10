package com.daysync.app.feature.journal.ui.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.daysync.app.feature.journal.domain.JournalEntry
import java.time.DayOfWeek
import java.time.YearMonth
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun JournalCalendarGrid(
    yearMonth: YearMonth,
    entries: Map<LocalDate, JournalEntry>,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }
    val dayHeaders = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    val firstDayOfMonth = yearMonth.atDay(1)
    // Monday = 1, Sunday = 7
    val startOffset = (firstDayOfMonth.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7
    val daysInMonth = yearMonth.lengthOfMonth()

    Column(modifier = modifier) {
        // Day-of-week headers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            dayHeaders.forEach { header ->
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = header,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        // Calendar grid rows
        val totalCells = startOffset + daysInMonth
        val rows = (totalCells + 6) / 7

        for (row in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    val dayOfMonth = cellIndex - startOffset + 1

                    if (dayOfMonth in 1..daysInMonth) {
                        val date = LocalDate(yearMonth.year, yearMonth.monthValue, dayOfMonth)
                        val entry = entries[date]

                        JournalDayCell(
                            dayOfMonth = dayOfMonth,
                            isToday = date == today,
                            mood = entry?.mood,
                            hasEntry = entry != null,
                            onClick = { onDayClick(date) },
                        )
                    } else {
                        // Empty cell
                        Box(modifier = Modifier.size(40.dp))
                    }
                }
            }
        }
    }
}
