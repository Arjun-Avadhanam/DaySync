package com.daysync.app.feature.nutrition.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.daysync.app.feature.nutrition.ui.components.FoodItemCard
import com.daysync.app.feature.nutrition.ui.components.FoodSearchBar
import com.daysync.app.feature.nutrition.ui.components.QuantityInput
import com.daysync.app.feature.nutrition.ui.viewmodel.MealEntryEvent
import com.daysync.app.feature.nutrition.ui.viewmodel.MealEntryViewModel
import com.daysync.app.feature.nutrition.ui.util.fmtNutrition

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionMealEntryScreen(
    date: String,
    mealTime: String,
    onNavigateBack: () -> Unit,
    onNavigateToAddFood: () -> Unit,
    viewModel: MealEntryViewModel = hiltViewModel(),
) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val selectedFood by viewModel.selectedFood.collectAsStateWithLifecycle()
    val amount by viewModel.amount.collectAsStateWithLifecycle()
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is MealEntryEvent.Saved -> onNavigateBack()
                is MealEntryEvent.Error -> { /* Could show snackbar */ }
            }
        }
    }

    LaunchedEffect(selectedFood) {
        if (selectedFood != null) showBottomSheet = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add ${viewModel.mealTime.displayName}") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToAddFood) {
                        Icon(Icons.Default.Add, contentDescription = "Add food item")
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            FoodSearchBar(
                query = searchQuery,
                onQueryChange = viewModel::searchFood,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            if (searchResults.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "No food items found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onNavigateToAddFood) {
                        Text("Add a new food item")
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(searchResults, key = { it.id }) { food ->
                        FoodItemCard(
                            food = food,
                            onClick = { viewModel.selectFood(food) },
                        )
                    }
                }
            }
        }

        if (showBottomSheet && selectedFood != null) {
            val food = selectedFood!!
            ModalBottomSheet(
                onDismissRequest = {
                    showBottomSheet = false
                    viewModel.clearSelection()
                },
                sheetState = sheetState,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = food.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )

                    QuantityInput(
                        amount = amount,
                        onAmountChange = viewModel::setAmount,
                        unitSymbol = food.unitType.symbol,
                    )

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            NutritionPreviewItem("Cal", (amount * food.caloriesPerUnit).fmtNutrition().toString())
                            NutritionPreviewItem("Protein", "${(amount * food.proteinPerUnit).fmtNutrition()}g")
                            NutritionPreviewItem("Carbs", "${(amount * food.carbsPerUnit).fmtNutrition()}g")
                            NutritionPreviewItem("Fat", "${(amount * food.fatPerUnit).fmtNutrition()}g")
                        }
                    }

                    Button(
                        onClick = viewModel::saveMealEntry,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Add to ${viewModel.mealTime.displayName}")
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun NutritionPreviewItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
    }
}
