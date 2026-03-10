package com.daysync.app.feature.media.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.daysync.app.feature.media.domain.MediaStatus
import com.daysync.app.feature.media.domain.MediaType

private data class TypeFilterOption(
    val label: String,
    val types: Set<MediaType>?,
)

private val typeFilterOptions = listOf(
    TypeFilterOption("All", null),
    TypeFilterOption("Books", setOf(MediaType.BOOK)),
    TypeFilterOption("Film+TV", MediaType.FILM_AND_TV),
    TypeFilterOption("Articles", setOf(MediaType.ARTICLE)),
    TypeFilterOption("Manga+Anime", setOf(MediaType.MANGA, MediaType.ANIME)),
    TypeFilterOption("Games", setOf(MediaType.GAME)),
    TypeFilterOption("Podcasts", setOf(MediaType.PODCAST)),
    TypeFilterOption("Music", setOf(MediaType.MUSIC)),
    TypeFilterOption("Comics", setOf(MediaType.COMIC)),
)

@Composable
fun MediaFilterChips(
    selectedTypes: Set<MediaType>?,
    selectedStatus: MediaStatus?,
    onTypeFilterChange: (Set<MediaType>?) -> Unit,
    onStatusFilterChange: (MediaStatus?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        ) {
            typeFilterOptions.forEach { option ->
                FilterChip(
                    selected = selectedTypes == option.types,
                    onClick = { onTypeFilterChange(option.types) },
                    label = { Text(option.label) },
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        ) {
            FilterChip(
                selected = selectedStatus == null,
                onClick = { onStatusFilterChange(null) },
                label = { Text("All") },
            )
            MediaStatus.entries.forEach { status ->
                FilterChip(
                    selected = selectedStatus == status,
                    onClick = { onStatusFilterChange(status) },
                    label = { Text(status.displayName) },
                )
            }
        }
    }
}
