package com.daysync.app.feature.media.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.daysync.app.feature.media.domain.MediaItem
import com.daysync.app.feature.media.domain.MediaStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaStatusBottomSheet(
    item: MediaItem,
    onStatusChange: (MediaStatus) -> Unit,
    onScoreChange: (Double?) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            Text(
                text = "Status",
                style = MaterialTheme.typography.labelLarge,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                MediaStatus.entries.forEach { status ->
                    FilterChip(
                        selected = item.status == status,
                        onClick = { onStatusChange(status) },
                        label = { Text(status.displayName) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Rating",
                style = MaterialTheme.typography.labelLarge,
            )
            Spacer(modifier = Modifier.height(8.dp))
            MediaStarRating(
                score = item.score,
                interactive = true,
                onScoreChange = onScoreChange,
                starSize = 36.dp,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}
