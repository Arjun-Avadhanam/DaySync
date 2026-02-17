package com.daysync.app.feature.expenses.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.daysync.app.core.ui.ErrorMessage
import com.daysync.app.core.ui.LoadingIndicator
import com.daysync.app.feature.expenses.model.Expense
import com.daysync.app.feature.expenses.model.formatIndianCurrency
import com.daysync.app.feature.expenses.ui.components.ExpenseSourceBadge
import com.daysync.app.ui.navigation.ExpenseAdd
import com.daysync.app.ui.navigation.ExpenseCsvImport
import com.daysync.app.ui.navigation.ExpenseDetail
import com.daysync.app.ui.navigation.ExpensePayeeRules
import kotlinx.datetime.LocalDate
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExpensesScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: ExpensesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val notificationAccessEnabled = remember {
        NotificationAccessHelper.isNotificationAccessEnabled(context)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Expenses") },
                actions = {
                    var menuExpanded by remember { mutableStateOf(false) }
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Import CSV") },
                            onClick = {
                                menuExpanded = false
                                navController.navigate(ExpenseCsvImport)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Payee Rules") },
                            onClick = {
                                menuExpanded = false
                                navController.navigate(ExpensePayeeRules)
                            },
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate(ExpenseAdd) }) {
                Icon(Icons.Default.Add, contentDescription = "Add expense")
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // Notification access banner
            if (!notificationAccessEnabled) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable { NotificationAccessHelper.openNotificationAccessSettings(context) },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    ),
                ) {
                    Text(
                        text = "Enable notification access to auto-track expenses from payment apps",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }

            when (val state = uiState) {
                is ExpensesListUiState.Loading -> LoadingIndicator()
                is ExpensesListUiState.Error -> ErrorMessage(state.message)
                is ExpensesListUiState.Success -> {
                    // Month selector
                    MonthSelector(
                        year = state.selectedYear,
                        month = state.selectedMonth,
                        monthlyTotal = state.monthlyTotal,
                        onPrevious = viewModel::previousMonth,
                        onNext = viewModel::nextMonth,
                    )

                    // Category chips
                    if (state.categoryTotals.isNotEmpty()) {
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            state.categoryTotals.take(5).forEach { cat ->
                                AssistChip(
                                    onClick = {},
                                    label = {
                                        val label = cat.category.substringBefore(" > ")
                                        Text(
                                            "$label: ${formatIndianCurrency(cat.total)}",
                                            style = MaterialTheme.typography.labelSmall,
                                        )
                                    },
                                )
                            }
                        }
                    }

                    // Expense list grouped by date
                    if (state.expenses.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                "No expenses this month",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            state.expenses.toSortedMap(compareByDescending { it })
                                .forEach { (date, expenses) ->
                                    item(key = "header_$date") {
                                        DayHeader(
                                            date = date,
                                            total = state.dailyTotals[date] ?: 0.0,
                                        )
                                    }
                                    items(
                                        items = expenses,
                                        key = { it.id },
                                    ) { expense ->
                                        ExpenseItem(
                                            expense = expense,
                                            onClick = {
                                                navController.navigate(
                                                    ExpenseDetail(expense.id)
                                                )
                                            },
                                            onDelete = { viewModel.deleteExpense(expense) },
                                        )
                                    }
                                }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthSelector(
    year: Int,
    month: Int,
    monthlyTotal: Double,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val monthName = Month.of(month).getDisplayName(TextStyle.FULL, Locale.getDefault())

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = onPrevious) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous month")
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$monthName $year",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = formatIndianCurrency(monthlyTotal),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            IconButton(onClick = onNext) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Next month")
            }
        }
    }
}

@Composable
private fun DayHeader(date: LocalDate, total: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${date.dayOfMonth} ${Month.of(date.monthNumber).getDisplayName(TextStyle.SHORT, Locale.getDefault())}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = formatIndianCurrency(total),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseItem(
    expense: Expense,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        },
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        },
        enableDismissFromStartToEnd = false,
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp)
                .clickable(onClick = onClick),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = expense.displayTitle,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (expense.category != null) {
                            Text(
                                text = expense.category,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        ExpenseSourceBadge(source = expense.source)
                    }
                }
                Text(
                    text = expense.formattedAmount,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
