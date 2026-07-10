package com.daysync.app.feature.expenses.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.daysync.app.core.database.entity.BudgetEntity
import com.daysync.app.feature.expenses.budget.model.CalendarWeek

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetSetupScreen(
    onNavigateBack: () -> Unit,
    viewModel: BudgetSetupViewModel = hiltViewModel(),
) {
    val budgets by viewModel.budgets.collectAsStateWithLifecycle()
    val today = viewModel.today

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Budgets") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Monthly
            val monthly = budgets.firstOrNull { it.type == "MONTHLY" && it.recurring }
            AmountSection(
                title = "Monthly budget",
                initial = monthly?.amount,
                onSave = { viewModel.setMonthly(it) },
            )
            HorizontalDivider()

            // Flat weekly
            val flatWeekly = budgets.firstOrNull { it.type == "WEEKLY" && it.recurring && it.weekBlock == null }
            AmountSection(
                title = "Weekly budget (every week)",
                initial = flatWeekly?.amount,
                onSave = { viewModel.setFlatWeekly(it) },
            )
            HorizontalDivider()

            // Vary by week (per-week overrides for weeks touching this month)
            val weekOverrides = budgets
                .filter { it.type == "WEEKLY" && !it.recurring && it.startDate != null }
                .associate { it.startDate!! to it.amount }
            VaryByWeekSection(
                weeks = viewModel.weeksForCurrentMonth(),
                overrideAmounts = weekOverrides,
                onSaveWeek = { monday, amount -> viewModel.setWeekOverride(monday, amount) },
            )
            HorizontalDivider()

            // Custom budgets
            CustomBudgetsSection(
                customs = budgets.filter { it.type == "CUSTOM" },
                onAdd = { start, end, amount, label -> viewModel.addCustom(today.year, today.monthNumber, start, end, amount, label) },
                onDelete = { viewModel.deleteBudget(it) },
            )
        }
    }
}

@Composable
private fun AmountSection(title: String, initial: Double?, onSave: (Double?) -> Unit) {
    var text by remember(initial) { mutableStateOf(initial?.let { it.toInt().toString() } ?: "") }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = text,
            onValueChange = { text = it.filter { c -> c.isDigit() } },
            label = { Text("Amount") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = { onSave(text.toDoubleOrNull()) }, modifier = Modifier.align(Alignment.End)) {
            Text("Save")
        }
    }
}

@Composable
private fun VaryByWeekSection(
    weeks: List<CalendarWeek>,
    overrideAmounts: Map<kotlinx.datetime.LocalDate, Double>,
    onSaveWeek: (kotlinx.datetime.LocalDate, Double?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Vary by week (this month)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Switch(checked = expanded, onCheckedChange = { expanded = it })
        }
        if (expanded) {
            Text(
                "Override a specific week; blank falls back to your weekly budget.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            weeks.forEach { week ->
                var text by remember(week.start) {
                    mutableStateOf(overrideAmounts[week.start]?.let { it.toInt().toString() } ?: "")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it.filter { c -> c.isDigit() } },
                        label = { Text("${week.start.monthNumber}/${week.start.dayOfMonth} – ${week.end.monthNumber}/${week.end.dayOfMonth}") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    Button(
                        onClick = { onSaveWeek(week.start, text.toDoubleOrNull()) },
                        modifier = Modifier.padding(start = 8.dp),
                    ) { Text("Save") }
                }
            }
        }
    }
}

@Composable
private fun CustomBudgetsSection(
    customs: List<BudgetEntity>,
    onAdd: (kotlinx.datetime.LocalDate, kotlinx.datetime.LocalDate, Double, String?) -> Unit,
    onDelete: (String) -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }
    var pendingRange by remember { mutableStateOf<Pair<kotlinx.datetime.LocalDate, kotlinx.datetime.LocalDate>?>(null) }
    var amountText by remember { mutableStateOf("") }
    var labelText by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Custom budgets", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        customs.forEach { c ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(c.label ?: "${c.startDate} – ${c.endDate}", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "${c.startDate} – ${c.endDate} · ${c.amount.toInt()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { onDelete(c.id) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        }
        Button(onClick = { showPicker = true }) { Text("Add custom budget") }

        if (showPicker) {
            CustomDateRangeDialog(
                onConfirm = { start, end -> pendingRange = start to end; showPicker = false },
                onDismiss = { showPicker = false },
            )
        }
        pendingRange?.let { (start, end) ->
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it.filter { c -> c.isDigit() } },
                label = { Text("Amount for $start – $end") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = labelText,
                onValueChange = { labelText = it },
                label = { Text("Label (optional)") },
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull()
                    if (amt != null && amt > 0) {
                        onAdd(start, end, amt, labelText.ifBlank { null })
                        pendingRange = null; amountText = ""; labelText = ""
                    }
                },
                modifier = Modifier.align(Alignment.End),
            ) { Text("Add") }
        }
    }
}
