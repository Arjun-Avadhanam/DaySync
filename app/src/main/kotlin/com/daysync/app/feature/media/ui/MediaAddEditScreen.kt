package com.daysync.app.feature.media.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.daysync.app.feature.media.domain.MediaItem
import com.daysync.app.feature.media.domain.MediaStatus
import com.daysync.app.feature.media.domain.MediaType
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MediaAddEditScreen(
    viewModel: MediaViewModel,
    editItemId: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val metadataResults by viewModel.metadataResults.collectAsState()
    val isSearchingMetadata by viewModel.isSearchingMetadata.collectAsState()
    val creatorSuggestions by viewModel.creatorSuggestions.collectAsState()

    var title by remember { mutableStateOf("") }
    var mediaType by remember { mutableStateOf(MediaType.BOOK) }
    var status by remember { mutableStateOf(MediaStatus.NOT_STARTED) }
    var score by remember { mutableStateOf<Double?>(null) }
    var creators by remember { mutableStateOf<List<String>>(emptyList()) }
    var completedDate by remember { mutableStateOf<LocalDate?>(null) }
    var notes by remember { mutableStateOf("") }
    var coverImageUrl by remember { mutableStateOf("") }
    var newCreator by remember { mutableStateOf("") }
    var showMetadataDropdown by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedExternalId by remember { mutableStateOf<String?>(null) }
    var isLoaded by remember { mutableStateOf(editItemId == null) }

    // Load existing item for editing
    LaunchedEffect(editItemId) {
        if (editItemId != null) {
            val item = viewModel.getItemById(editItemId)
            if (item != null) {
                title = item.title
                mediaType = item.mediaType
                status = item.status
                score = item.score
                creators = item.creators
                completedDate = item.completedDate
                notes = item.notes ?: ""
                coverImageUrl = item.coverImageUrl ?: ""
            }
            isLoaded = true
        }
    }

    if (!isLoaded) return

    val isEditing = editItemId != null

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(if (isEditing) "Edit Media" else "Add Media") },
            navigationIcon = {
                IconButton(onClick = {
                    viewModel.clearMetadataResults()
                    viewModel.clearCreatorQuery()
                    onBack()
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            // Media type selector
            Text("Type", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                MediaType.entries.forEach { type ->
                    FilterChip(
                        selected = mediaType == type,
                        onClick = { mediaType = type },
                        label = { Text(type.displayName) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title with metadata search
            Text("Title", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(4.dp))
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { newTitle ->
                        title = newTitle
                        if (newTitle.length >= 3) {
                            viewModel.searchMetadata(newTitle, mediaType)
                            showMetadataDropdown = true
                        } else {
                            showMetadataDropdown = false
                        }
                    },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = {
                        if (isSearchingMetadata) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        }
                    },
                )
                // Suggestion list rendered below text field (doesn't steal focus)
                if (showMetadataDropdown && metadataResults.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ),
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Column {
                            metadataResults.forEach { result ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            title = result.title
                                            coverImageUrl = result.coverImageUrl ?: ""
                                            selectedExternalId = result.externalId
                                            if (result.creators.isNotEmpty()) {
                                                creators = result.creators
                                            }
                                            showMetadataDropdown = false
                                            viewModel.clearMetadataResults()

                                            // Fetch creators if not already provided
                                            if (result.creators.isEmpty() && result.externalId != null) {
                                                scope.launch {
                                                    val fetched = viewModel.fetchCreators(
                                                        result.externalId,
                                                        mediaType,
                                                    )
                                                    if (fetched.isNotEmpty()) {
                                                        creators = fetched
                                                    }
                                                }
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    if (result.coverImageUrl != null) {
                                        AsyncImage(
                                            model = result.coverImageUrl,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(MaterialTheme.shapes.extraSmall),
                                            contentScale = ContentScale.Crop,
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(result.title, style = MaterialTheme.typography.bodyMedium)
                                        if (result.year != null) {
                                            Text(
                                                result.year,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Creators
            Text("Creators", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(4.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                creators.forEach { creator ->
                    InputChip(
                        selected = false,
                        onClick = { creators = creators - creator },
                        label = { Text(creator) },
                        trailingIcon = {
                            Icon(Icons.Filled.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp))
                        },
                    )
                }
            }
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedTextField(
                        value = newCreator,
                        onValueChange = {
                            newCreator = it
                            viewModel.setCreatorQuery(it)
                        },
                        label = { Text("Add creator") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            val value = newCreator.trim()
                            if (value.isNotBlank() && creators.none { it.equals(value, ignoreCase = true) }) {
                                creators = creators + value
                            }
                            newCreator = ""
                            viewModel.clearCreatorQuery()
                        },
                    ) { Text("Add") }
                }
                // Suggestion list rendered below text field (doesn't steal focus).
                // Matches the title metadata suggestions pattern so the keyboard
                // stays up and the user can keep typing while picking a match.
                val visibleSuggestions = creatorSuggestions.filter { suggestion ->
                    creators.none { it.equals(suggestion, ignoreCase = true) }
                }
                if (visibleSuggestions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ),
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Column {
                            visibleSuggestions.forEach { suggestion ->
                                Text(
                                    text = suggestion,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            creators = creators + suggestion
                                            newCreator = ""
                                            viewModel.clearCreatorQuery()
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Status
            Text("Status", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(8.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                MediaStatus.entries.forEachIndexed { index, s ->
                    SegmentedButton(
                        selected = status == s,
                        onClick = { status = s },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = MediaStatus.entries.size,
                        ),
                    ) { Text(s.displayName, style = MaterialTheme.typography.labelSmall) }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Rating
            Text("Rating", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(8.dp))
            MediaStarRating(
                score = score,
                interactive = true,
                onScoreChange = { score = it },
                starSize = 36.dp,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Completed date (visible when status=DONE)
            if (status == MediaStatus.DONE) {
                Text("Completed Date", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = completedDate?.toString() ?: "Not set",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Filled.DateRange, contentDescription = "Pick date")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Cover URL
            if (coverImageUrl.isNotBlank()) {
                Text("Cover", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(4.dp))
                AsyncImage(
                    model = coverImageUrl,
                    contentDescription = "Cover preview",
                    modifier = Modifier
                        .size(width = 100.dp, height = 150.dp)
                        .clip(MaterialTheme.shapes.medium),
                    contentScale = ContentScale.Crop,
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            OutlinedTextField(
                value = coverImageUrl,
                onValueChange = { coverImageUrl = it },
                label = { Text("Cover Image URL") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Notes
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Save button
            Button(
                onClick = {
                    val item = MediaItem(
                        id = editItemId ?: "",
                        title = title.trim(),
                        mediaType = mediaType,
                        status = status,
                        score = score,
                        creators = creators,
                        completedDate = completedDate,
                        notes = notes.ifBlank { null },
                        coverImageUrl = coverImageUrl.ifBlank { null },
                    )
                    if (isEditing) {
                        viewModel.updateItem(item)
                        onBack()
                    } else {
                        viewModel.addItem(item)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = title.isNotBlank(),
            ) {
                Text(if (isEditing) "Save Changes" else "Add Media")
            }

            // Save to Notion (edit mode only, hidden if Notion not configured)
            if (isEditing && com.daysync.app.BuildConfig.NOTION_API_KEY.isNotBlank()) {
                val notionStatus by viewModel.notionExportStatus.collectAsState()
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        viewModel.exportToNotion(
                            MediaItem(
                                id = editItemId ?: "",
                                title = title.trim(),
                                mediaType = mediaType,
                                status = status,
                                score = score,
                                creators = creators,
                                completedDate = completedDate,
                                notes = notes.ifBlank { null },
                                coverImageUrl = coverImageUrl.ifBlank { null },
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = notionStatus == null,
                ) {
                    Text(notionStatus ?: "Save to Notion")
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = completedDate?.let {
                kotlin.time.Instant.parse("${it}T00:00:00Z").toEpochMilliseconds()
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val instant = kotlin.time.Instant.fromEpochMilliseconds(millis)
                        val dateTime = instant.toLocalDateTime(TimeZone.UTC)
                        completedDate = dateTime.date
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
