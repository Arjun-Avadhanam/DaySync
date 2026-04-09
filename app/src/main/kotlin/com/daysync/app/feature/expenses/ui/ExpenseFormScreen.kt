package com.daysync.app.feature.expenses.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.daysync.app.feature.expenses.ui.components.ExpenseCategoryPicker
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseFormScreen(
    onNavigateBack: () -> Unit,
    expenseId: String? = null,
    modifier: Modifier = Modifier,
    viewModel: ExpenseFormViewModel = hiltViewModel(),
) {
    val formState by viewModel.formState.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(if (formState.isEditing) "Edit Expense" else "Add Expense") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            // Amount
            OutlinedTextField(
                value = formState.amount,
                onValueChange = viewModel::updateAmount,
                label = { Text("Amount") },
                prefix = { Text("₹ ") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.headlineSmall,
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Title
            OutlinedTextField(
                value = formState.title,
                onValueChange = viewModel::updateTitle,
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Merchant name
            OutlinedTextField(
                value = formState.merchantName,
                onValueChange = viewModel::updateMerchantName,
                label = { Text("Merchant / Payee") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Date
            val dateDisplay = formState.date?.let {
                "${it.dayOfMonth}/${it.monthNumber}/${it.year}"
            } ?: "Today"
            OutlinedTextField(
                value = dateDisplay,
                onValueChange = {},
                label = { Text("Date") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                enabled = false,
            )
            TextButton(onClick = { showDatePicker = true }) {
                Text("Change date")
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Notes
            OutlinedTextField(
                value = formState.notes,
                onValueChange = viewModel::updateNotes,
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Category picker
            ExpenseCategoryPicker(
                selectedCategory = formState.category,
                onCategorySelected = viewModel::updateCategory,
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Save as payee rule — or show indicator if one already exists
            if (formState.merchantName.isNotBlank()) {
                val existingRule = formState.existingPayeeRule
                if (existingRule != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Payee rule saved (${existingRule.category})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = formState.saveAsPayeeRule,
                            onCheckedChange = viewModel::updateSaveAsPayeeRule,
                        )
                        Text(
                            text = "Save as payee rule for \"${formState.merchantName}\"",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Save button
            Button(
                onClick = { viewModel.save(onSuccess = onNavigateBack) },
                modifier = Modifier.fillMaxWidth(),
                enabled = formState.amount.toDoubleOrNull() != null && !formState.isSaving,
            ) {
                Text(if (formState.isSaving) "Saving..." else "Save")
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = formState.date
                    ?.atStartOfDayIn(TimeZone.UTC)
                    ?.toEpochMilliseconds(),
            )
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val instant = Instant.fromEpochMilliseconds(millis)
                            val date = instant.toLocalDateTime(TimeZone.UTC).date
                            viewModel.updateDate(date)
                        }
                        showDatePicker = false
                    }) {
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
}
