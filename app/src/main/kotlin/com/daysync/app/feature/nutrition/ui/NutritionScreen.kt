package com.daysync.app.feature.nutrition.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.daysync.app.feature.nutrition.ui.navigation.NutritionNavHost

@Composable
fun NutritionScreen(modifier: Modifier = Modifier) {
    NutritionNavHost(modifier = modifier)
}
