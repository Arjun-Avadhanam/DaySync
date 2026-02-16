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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.daysync.app.feature.nutrition.domain.model.MealTime
import com.daysync.app.feature.nutrition.ui.viewmodel.MealTemplateEvent
import com.daysync.app.feature.nutrition.ui.viewmodel.MealTemplatesViewModel
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionLogTemplateScreen(
    templateId: String,
    date: String,
    mealTime: String,
    onNavigateBack: () -> Unit,
    viewModel: MealTemplatesViewModel = hiltViewModel(),
) {
    val selectedTemplate by viewModel.selectedTemplate.collectAsStateWithLifecycle()
    val amountOverrides = remember { mutableStateMapOf<String, Double>() }

    LaunchedEffect(templateId) {
        viewModel.loadTemplateWithItems(templateId)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is MealTemplateEvent.Logged -> onNavigateBack()
                else -> {}
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Log Template") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { paddingValues ->
        val template = selectedTemplate

        if (template == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = template.template.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    template.template.description?.let { desc ->
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }
                }

                item {
                    Text(
                        text = "Adjust quantities:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                items(template.items, key = { it.item.id }) { itemWithFood ->
                    val currentAmount = amountOverrides[itemWithFood.item.id]
                        ?: itemWithFood.item.defaultAmount

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
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
                                    text = itemWithFood.food.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                )
                                Text(
                                    text = "${itemWithFood.food.caloriesPerUnit.toInt()} cal/${itemWithFood.food.unitType.symbol}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                )
                            }
                            OutlinedTextField(
                                value = if (currentAmount == currentAmount.toLong().toDouble()) {
                                    currentAmount.toLong().toString()
                                } else {
                                    currentAmount.toString()
                                },
                                onValueChange = { text ->
                                    text.toDoubleOrNull()?.let { amt ->
                                        amountOverrides[itemWithFood.item.id] = amt
                                    }
                                },
                                label = { Text(itemWithFood.food.unitType.symbol) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(0.4f).padding(start = 8.dp),
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            viewModel.logTemplate(
                                templateId = templateId,
                                date = LocalDate.parse(date),
                                mealTime = MealTime.fromDbValue(mealTime),
                                amountMultipliers = amountOverrides.toMap().ifEmpty { null },
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Log to ${MealTime.fromDbValue(mealTime).displayName}")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
