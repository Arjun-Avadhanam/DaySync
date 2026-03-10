package com.daysync.app.feature.journal.ui.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.daysync.app.feature.journal.domain.Mood
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalEditorContent(
    title: String,
    content: String,
    mood: Mood?,
    tags: List<String>,
    date: LocalDate?,
    availableTags: List<String>,
    isSaving: Boolean,
    isEditing: Boolean,
    isDirty: Boolean,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onMoodChange: (Mood) -> Unit,
    onTagToggle: (String) -> Unit,
    onCustomTagAdd: (String) -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val canSave = title.isNotBlank() || content.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (isEditing) "Edit Entry" else "New Entry")
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (isDirty) showDiscardDialog = true else onBack()
                        },
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = onSave,
                        enabled = canSave && !isSaving,
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Save")
                    }
                },
            )
        },
        modifier = modifier,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            // Date picker row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true }
                    .padding(vertical = 12.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = "Select date",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = date?.toString() ?: "Select date",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            // Title
            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                label = { Text("Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Mood selector
            JournalMoodSelector(
                selectedMood = mood,
                onMoodSelected = onMoodChange,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Tag selector
            JournalTagSelector(
                selectedTags = tags,
                availableTags = availableTags,
                onTagToggle = onTagToggle,
                onCustomTagAdd = onCustomTagAdd,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Content
            OutlinedTextField(
                value = content,
                onValueChange = onContentChange,
                label = { Text("Write your thoughts...") },
                minLines = 8,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Discard confirmation
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard changes?") },
            text = { Text("You have unsaved changes. Are you sure you want to go back?") },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    onBack()
                }) {
                    Text("Discard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Keep editing")
                }
            },
        )
    }

    // Date picker
    if (showDatePicker) {
        val initialMillis = date?.atStartOfDayIn(TimeZone.UTC)?.toEpochMilliseconds()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val instant = Instant.fromEpochMilliseconds(millis)
                            val ld = instant.toLocalDateTime(TimeZone.UTC).date
                            onDateChange(ld)
                        }
                        showDatePicker = false
                    },
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
