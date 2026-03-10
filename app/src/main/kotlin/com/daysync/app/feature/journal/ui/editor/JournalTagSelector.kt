package com.daysync.app.feature.journal.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JournalTagSelector(
    selectedTags: List<String>,
    availableTags: List<String>,
    onTagToggle: (String) -> Unit,
    onCustomTagAdd: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDialog by remember { mutableStateOf(false) }
    var customTagInput by remember { mutableStateOf("") }

    Column(modifier = modifier) {
        Text(
            text = "Tags",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            availableTags.forEach { tag ->
                FilterChip(
                    selected = tag in selectedTags,
                    onClick = { onTagToggle(tag) },
                    label = { Text(tag) },
                )
            }
            InputChip(
                selected = false,
                onClick = { showDialog = true },
                label = { Text("Custom") },
                avatar = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add custom tag",
                    )
                },
            )
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                showDialog = false
                customTagInput = ""
            },
            title = { Text("Add Custom Tag") },
            text = {
                OutlinedTextField(
                    value = customTagInput,
                    onValueChange = { customTagInput = it },
                    label = { Text("Tag name") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onCustomTagAdd(customTagInput)
                        customTagInput = ""
                        showDialog = false
                    },
                    enabled = customTagInput.isNotBlank(),
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDialog = false
                        customTagInput = ""
                    },
                ) {
                    Text("Cancel")
                }
            },
        )
    }
}
