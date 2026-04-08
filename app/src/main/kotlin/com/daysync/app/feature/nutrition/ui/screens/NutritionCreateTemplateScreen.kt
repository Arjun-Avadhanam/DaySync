package com.daysync.app.feature.nutrition.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.daysync.app.feature.nutrition.domain.model.FoodItem
import com.daysync.app.feature.nutrition.domain.model.MealTemplateItemInput
import com.daysync.app.feature.nutrition.ui.components.FoodSearchBar
import com.daysync.app.feature.nutrition.ui.viewmodel.MealTemplateEvent
import com.daysync.app.feature.nutrition.ui.viewmodel.MealTemplatesViewModel

private data class TemplateItemState(
    val food: FoodItem,
    val amount: Double = 1.0,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionCreateTemplateScreen(
    onNavigateBack: () -> Unit,
    viewModel: MealTemplatesViewModel = hiltViewModel(),
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    val selectedItems = remember { mutableStateListOf<TemplateItemState>() }
    val allFoods by viewModel.allFoodItems.collectAsStateWithLifecycle()

    val filteredFoods = if (searchQuery.isBlank()) {
        allFoods
    } else {
        allFoods.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is MealTemplateEvent.Created -> onNavigateBack()
                else -> {}
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Template") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        bottomBar = {
            // Sticky "Create Template" button always visible at bottom
            Button(
                onClick = {
                    val items = selectedItems.map { item ->
                        MealTemplateItemInput(foodId = item.food.id, defaultAmount = item.amount)
                    }
                    viewModel.createTemplate(
                        name = name.trim(),
                        description = description.trim().ifEmpty { null },
                        items = items,
                    )
                },
                enabled = name.isNotBlank() && selectedItems.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text("Create Template (${selectedItems.size} items)")
            }
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Template name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (selectedItems.isNotEmpty()) {
                item {
                    Text(
                        text = "Selected items (${selectedItems.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                items(selectedItems.size) { index ->
                    val item = selectedItems[index]
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.food.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                            OutlinedTextField(
                                value = if (item.amount == item.amount.toLong().toDouble()) {
                                    item.amount.toLong().toString()
                                } else {
                                    item.amount.toString()
                                },
                                onValueChange = { text ->
                                    text.toDoubleOrNull()?.let { amt ->
                                        selectedItems[index] = item.copy(amount = amt)
                                    }
                                },
                                label = { Text(item.food.unitType.symbol) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(0.5f).padding(horizontal = 8.dp),
                            )
                            IconButton(
                                onClick = { selectedItems.removeAt(index) },
                            ) {
                                Icon(
                                    Icons.Outlined.Delete,
                                    contentDescription = "Remove",
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Add food items",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            item {
                FoodSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholder = "Search food to add...",
                )
            }

            items(filteredFoods, key = { it.id }) { food ->
                val isSelected = selectedItems.any { it.food.id == food.id }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { checked ->
                            if (checked) {
                                selectedItems.add(TemplateItemState(food = food))
                            } else {
                                selectedItems.removeAll { it.food.id == food.id }
                            }
                        },
                    )
                    Text(
                        text = food.name,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "${food.caloriesPerUnit.toInt()} cal/${food.unitType.symbol}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
