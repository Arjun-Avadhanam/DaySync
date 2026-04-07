package com.daysync.app.feature.nutrition.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.daysync.app.feature.nutrition.domain.model.FoodItemInput
import com.daysync.app.feature.nutrition.domain.model.UnitType
import com.daysync.app.feature.nutrition.ui.components.NutritionFormFields
import com.daysync.app.feature.nutrition.ui.viewmodel.FoodLibraryEvent
import com.daysync.app.feature.nutrition.ui.viewmodel.FoodLibraryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionAddEditFoodScreen(
    foodId: String?,
    onNavigateBack: () -> Unit,
    viewModel: FoodLibraryViewModel = hiltViewModel(),
) {
    val foodItems by viewModel.foodItems.collectAsStateWithLifecycle()
    val existingFood = foodId?.let { id -> foodItems.firstOrNull { it.id == id } }
    val isEdit = existingFood != null

    var name by remember(existingFood) { mutableStateOf(existingFood?.name ?: "") }
    var category by remember(existingFood) { mutableStateOf(existingFood?.category ?: "") }
    var calories by remember(existingFood) {
        mutableStateOf(existingFood?.caloriesPerUnit?.toString() ?: "")
    }
    var protein by remember(existingFood) {
        mutableStateOf(existingFood?.proteinPerUnit?.toString() ?: "0")
    }
    var carbs by remember(existingFood) {
        mutableStateOf(existingFood?.carbsPerUnit?.toString() ?: "0")
    }
    var fat by remember(existingFood) {
        mutableStateOf(existingFood?.fatPerUnit?.toString() ?: "0")
    }
    var sugar by remember(existingFood) {
        mutableStateOf(existingFood?.sugarPerUnit?.toString() ?: "0")
    }
    var selectedUnitType by remember(existingFood) {
        mutableStateOf(existingFood?.unitType ?: UnitType.SERVING)
    }
    var servingDescription by remember(existingFood) {
        mutableStateOf(existingFood?.servingDescription ?: "")
    }
    var unitTypeExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is FoodLibraryEvent.FoodSaved -> onNavigateBack()
                is FoodLibraryEvent.FoodDeleted -> onNavigateBack()
                is FoodLibraryEvent.ImportComplete -> { /* Handled in library screen */ }
                is FoodLibraryEvent.Error -> { /* Could show snackbar */ }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) "Edit Food" else "Add Food") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isEdit && foodId != null) {
                        IconButton(onClick = {
                            viewModel.deleteFood(foodId)
                        }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Delete")
                        }
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Food name *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("Category") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ExposedDropdownMenuBox(
                    expanded = unitTypeExpanded,
                    onExpandedChange = { unitTypeExpanded = it },
                    modifier = Modifier.weight(1f),
                ) {
                    OutlinedTextField(
                        value = selectedUnitType.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Unit type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitTypeExpanded) },
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                    )
                    ExposedDropdownMenu(
                        expanded = unitTypeExpanded,
                        onDismissRequest = { unitTypeExpanded = false },
                    ) {
                        UnitType.entries.forEach { unitType ->
                            DropdownMenuItem(
                                text = { Text("${unitType.displayName} (${unitType.symbol})") },
                                onClick = {
                                    selectedUnitType = unitType
                                    unitTypeExpanded = false
                                },
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = servingDescription,
                    onValueChange = { servingDescription = it },
                    label = { Text("Serving desc.") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }

            NutritionFormFields(
                calories = calories,
                onCaloriesChange = { calories = it },
                protein = protein,
                onProteinChange = { protein = it },
                carbs = carbs,
                onCarbsChange = { carbs = it },
                fat = fat,
                onFatChange = { fat = it },
                sugar = sugar,
                onSugarChange = { sugar = it },
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    val input = FoodItemInput(
                        name = name.trim(),
                        category = category.trim().ifEmpty { null },
                        caloriesPerUnit = calories.toDoubleOrNull() ?: 0.0,
                        proteinPerUnit = protein.toDoubleOrNull() ?: 0.0,
                        carbsPerUnit = carbs.toDoubleOrNull() ?: 0.0,
                        fatPerUnit = fat.toDoubleOrNull() ?: 0.0,
                        sugarPerUnit = sugar.toDoubleOrNull() ?: 0.0,
                        unitType = selectedUnitType,
                        servingDescription = servingDescription.trim().ifEmpty { null },
                    )
                    if (isEdit && foodId != null) {
                        viewModel.updateFood(foodId, input)
                    } else {
                        viewModel.addFood(input)
                    }
                },
                enabled = name.isNotBlank() && (calories.toDoubleOrNull() ?: -1.0) >= 0,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (isEdit) "Update Food" else "Add Food")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
