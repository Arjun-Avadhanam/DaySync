package com.daysync.app.feature.journal.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.daysync.app.feature.journal.domain.Mood

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JournalMoodSelector(
    selectedMood: Mood?,
    onMoodSelected: (Mood) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = "Mood",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Mood.entries.forEach { mood ->
                val isSelected = mood == selectedMood
                FilterChip(
                    selected = isSelected,
                    onClick = { onMoodSelected(mood) },
                    label = { Text(mood.label) },
                    leadingIcon = {
                        Icon(
                            imageVector = mood.icon,
                            contentDescription = mood.label,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = mood.color.copy(alpha = 0.2f),
                        selectedLabelColor = mood.color,
                        selectedLeadingIconColor = mood.color,
                    ),
                )
            }
        }
    }
}
