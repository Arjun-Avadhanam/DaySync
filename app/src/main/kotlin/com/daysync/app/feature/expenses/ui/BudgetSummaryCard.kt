package com.daysync.app.feature.expenses.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.daysync.app.core.config.UserPreferences
import com.daysync.app.feature.expenses.budget.model.BudgetProgressItem

@Composable
fun BudgetSummaryCard(
    onSetBudget: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BudgetSummaryViewModel = hiltViewModel(),
) {
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val prefs = remember { UserPreferences(context) }
    var expanded by remember { mutableStateOf(false) }

    val current = summary
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable {
                if (current?.primary == null && current?.monthly == null) onSetBudget()
                else expanded = !expanded
            },
    ) {
        Column(Modifier.padding(16.dp)) {
            if (current?.primary == null && current?.monthly == null) {
                Text("Set a budget", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    "Tap to add a weekly, monthly, or custom budget",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@Column
            }
            current.primary?.let { ProgressRow(it, prefs, prominent = true) }
            current.monthly?.takeIf { it.instanceKey != current.primary?.instanceKey }?.let {
                ProgressRow(it, prefs, prominent = false)
            }
            if (expanded) {
                current.all
                    .filter { it.instanceKey != current.primary?.instanceKey && it.instanceKey != current.monthly?.instanceKey }
                    .forEach { ProgressRow(it, prefs, prominent = false) }
            } else if (current.all.size > 2) {
                Text(
                    "tap for all (${current.all.size})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun ProgressRow(item: BudgetProgressItem, prefs: UserPreferences, prominent: Boolean) {
    val over = item.remaining < 0
    val leftText = if (over) "Over by ${prefs.formatCurrency(-item.remaining)}"
    else "${prefs.formatCurrency(item.remaining)} left of ${prefs.formatCurrency(item.amount)}"
    val header = if (prominent) "Money left till ${item.end}" else item.label
    Column(Modifier.padding(vertical = 4.dp)) {
        Text(
            header,
            style = if (prominent) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            fontWeight = if (prominent) FontWeight.Bold else FontWeight.Normal,
        )
        Text(leftText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        LinearProgressIndicator(
            progress = { item.fraction.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            color = when {
                over || item.fraction >= 1f -> MaterialTheme.colorScheme.error
                item.fraction >= 0.75f -> Color(0xFFF59E0B)
                else -> MaterialTheme.colorScheme.primary
            },
        )
    }
}
