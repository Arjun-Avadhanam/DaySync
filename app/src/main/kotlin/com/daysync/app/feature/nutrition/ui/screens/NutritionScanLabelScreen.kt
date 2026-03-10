package com.daysync.app.feature.nutrition.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.daysync.app.feature.nutrition.domain.model.UnitType
import com.daysync.app.feature.nutrition.ui.components.NutritionFormFields
import com.daysync.app.feature.nutrition.ui.viewmodel.ExtractionState
import com.daysync.app.feature.nutrition.ui.viewmodel.ScanLabelEvent
import com.daysync.app.feature.nutrition.ui.viewmodel.ScanLabelViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionScanLabelScreen(
    onNavigateBack: () -> Unit,
    viewModel: ScanLabelViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val extractionState by viewModel.extractionState.collectAsStateWithLifecycle()
    val imageUri by viewModel.imageUri.collectAsStateWithLifecycle()
    val selectedUnitType by viewModel.selectedUnitType.collectAsStateWithLifecycle()
    val productName by viewModel.productName.collectAsStateWithLifecycle()
    val category by viewModel.category.collectAsStateWithLifecycle()
    val servingDescription by viewModel.servingDescription.collectAsStateWithLifecycle()
    val calories by viewModel.calories.collectAsStateWithLifecycle()
    val protein by viewModel.protein.collectAsStateWithLifecycle()
    val carbs by viewModel.carbs.collectAsStateWithLifecycle()
    val fat by viewModel.fat.collectAsStateWithLifecycle()
    val sugar by viewModel.sugar.collectAsStateWithLifecycle()

    // Camera URI for TakePicture contract
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.setImageUri(uri)
            viewModel.extractNutrition(context)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraUri?.let { uri ->
                viewModel.setImageUri(uri)
                viewModel.extractNutrition(context)
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = createTempImageUri(context)
            cameraUri = uri
            cameraLauncher.launch(uri)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ScanLabelEvent.FoodSaved -> onNavigateBack()
                is ScanLabelEvent.SaveError -> { /* Handled by extraction state */ }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan Nutrition Label") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { paddingValues ->
        when (val state = extractionState) {
            is ExtractionState.Idle -> {
                IdleContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    onTakePhoto = {
                        val hasCameraPermission = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasCameraPermission) {
                            val uri = createTempImageUri(context)
                            cameraUri = uri
                            cameraLauncher.launch(uri)
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    onChooseGallery = {
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                )
            }

            is ExtractionState.Extracting -> {
                ExtractingContent(
                    imageUri = imageUri,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                )
            }

            is ExtractionState.Success -> {
                SuccessContent(
                    imageUri = imageUri,
                    productName = productName,
                    onProductNameChange = viewModel::setProductName,
                    category = category,
                    onCategoryChange = viewModel::setCategory,
                    selectedUnitType = selectedUnitType,
                    onUnitTypeChange = viewModel::setUnitType,
                    servingDescription = servingDescription,
                    onServingDescriptionChange = viewModel::setServingDescription,
                    calories = calories,
                    onCaloriesChange = viewModel::setCalories,
                    protein = protein,
                    onProteinChange = viewModel::setProtein,
                    carbs = carbs,
                    onCarbsChange = viewModel::setCarbs,
                    fat = fat,
                    onFatChange = viewModel::setFat,
                    sugar = sugar,
                    onSugarChange = viewModel::setSugar,
                    hasPerServing = state.result.perServing != null,
                    onSave = viewModel::saveFood,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                )
            }

            is ExtractionState.Error -> {
                ErrorContent(
                    message = state.message,
                    onRetry = viewModel::retry,
                    onManualEntry = onNavigateBack,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                )
            }
        }
    }
}

@Composable
private fun IdleContent(
    onTakePhoto: () -> Unit,
    onChooseGallery: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Scan a Nutrition Label",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = "Take a photo or choose an image of a nutrition facts table to automatically extract values.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onTakePhoto,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Take Photo")
                }

                OutlinedButton(
                    onClick = onChooseGallery,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Choose from Gallery")
                }
            }
        }
    }
}

@Composable
private fun ExtractingContent(
    imageUri: Uri?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (imageUri != null) {
            AsyncImage(
                model = imageUri,
                contentDescription = "Selected image",
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .aspectRatio(0.75f)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop,
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Analyzing nutrition label...",
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SuccessContent(
    imageUri: Uri?,
    productName: String,
    onProductNameChange: (String) -> Unit,
    category: String,
    onCategoryChange: (String) -> Unit,
    selectedUnitType: UnitType,
    onUnitTypeChange: (UnitType) -> Unit,
    servingDescription: String,
    onServingDescriptionChange: (String) -> Unit,
    calories: String,
    onCaloriesChange: (String) -> Unit,
    protein: String,
    onProteinChange: (String) -> Unit,
    carbs: String,
    onCarbsChange: (String) -> Unit,
    fat: String,
    onFatChange: (String) -> Unit,
    sugar: String,
    onSugarChange: (String) -> Unit,
    hasPerServing: Boolean,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var unitTypeExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // Image thumbnail
        if (imageUri != null) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = "Scanned label",
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .aspectRatio(0.75f)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop,
                )
            }
        }

        OutlinedTextField(
            value = productName,
            onValueChange = onProductNameChange,
            label = { Text("Product name *") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = category,
            onValueChange = onCategoryChange,
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
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitTypeExpanded)
                    },
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
                                onUnitTypeChange(unitType)
                                unitTypeExpanded = false
                            },
                        )
                    }
                }
            }

            OutlinedTextField(
                value = servingDescription,
                onValueChange = onServingDescriptionChange,
                label = { Text("Serving desc.") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }

        // Info text about what values are shown
        val infoText = when {
            selectedUnitType == UnitType.ML -> "Values shown per 100ml"
            selectedUnitType == UnitType.GRAMS -> "Values shown per 100g"
            hasPerServing -> "Values shown per serving"
            else -> "Values shown per unit"
        }
        Text(
            text = infoText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        NutritionFormFields(
            calories = calories,
            onCaloriesChange = onCaloriesChange,
            protein = protein,
            onProteinChange = onProteinChange,
            carbs = carbs,
            onCarbsChange = onCarbsChange,
            fat = fat,
            onFatChange = onFatChange,
            sugar = sugar,
            onSugarChange = onSugarChange,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onSave,
            enabled = productName.isNotBlank() && (calories.toDoubleOrNull() ?: -1.0) >= 0,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Save to Food Library")
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    onManualEntry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Extraction Failed",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )

                Button(
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Try Again")
                }

                OutlinedButton(
                    onClick = onManualEntry,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Enter Manually")
                }
            }
        }
    }
}

private fun createTempImageUri(context: android.content.Context): Uri {
    val imagesDir = File(context.cacheDir, "images").apply { mkdirs() }
    val imageFile = File(imagesDir, "scan_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        imageFile,
    )
}
