package com.daysync.app.feature.nutrition.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.daysync.app.feature.nutrition.ui.screens.NutritionAddEditFoodScreen
import com.daysync.app.feature.nutrition.ui.screens.NutritionCreateTemplateScreen
import com.daysync.app.feature.nutrition.ui.screens.NutritionDailySummaryScreen
import com.daysync.app.feature.nutrition.ui.screens.NutritionDailyTrackerScreen
import com.daysync.app.feature.nutrition.ui.screens.NutritionFoodLibraryScreen
import com.daysync.app.feature.nutrition.ui.screens.NutritionHistoryScreen
import com.daysync.app.feature.nutrition.ui.screens.NutritionLogTemplateScreen
import com.daysync.app.feature.nutrition.ui.screens.NutritionMealEntryScreen
import com.daysync.app.feature.nutrition.ui.screens.NutritionMealTemplatesScreen

@Composable
fun NutritionNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = DailyTracker,
        modifier = modifier,
    ) {
        composable<DailyTracker> {
            NutritionDailyTrackerScreen(
                onAddMealEntry = { date, mealTime ->
                    navController.navigate(MealEntry(date = date, mealTime = mealTime))
                },
                onViewSummary = { date ->
                    navController.navigate(DailySummary(date = date))
                },
                onNavigateToFoodLibrary = {
                    navController.navigate(FoodLibrary)
                },
                onNavigateToTemplates = {
                    navController.navigate(Templates)
                },
                onNavigateToHistory = {
                    navController.navigate(History)
                },
            )
        }

        composable<MealEntry> { backStackEntry ->
            val route = backStackEntry.toRoute<MealEntry>()
            NutritionMealEntryScreen(
                date = route.date,
                mealTime = route.mealTime,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAddFood = { navController.navigate(AddFood) },
            )
        }

        composable<FoodLibrary> {
            NutritionFoodLibraryScreen(
                onNavigateBack = { navController.popBackStack() },
                onAddFood = { navController.navigate(AddFood) },
                onEditFood = { foodId -> navController.navigate(EditFood(foodId = foodId)) },
            )
        }

        composable<AddFood> {
            NutritionAddEditFoodScreen(
                foodId = null,
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable<EditFood> { backStackEntry ->
            val route = backStackEntry.toRoute<EditFood>()
            NutritionAddEditFoodScreen(
                foodId = route.foodId,
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable<Templates> {
            NutritionMealTemplatesScreen(
                onNavigateBack = { navController.popBackStack() },
                onCreateTemplate = {
                    navController.navigate(CreateTemplate())
                },
                onLogTemplate = { templateId, date, mealTime ->
                    navController.navigate(
                        LogTemplate(templateId = templateId, date = date, mealTime = mealTime)
                    )
                },
            )
        }

        composable<CreateTemplate> {
            NutritionCreateTemplateScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable<LogTemplate> { backStackEntry ->
            val route = backStackEntry.toRoute<LogTemplate>()
            NutritionLogTemplateScreen(
                templateId = route.templateId,
                date = route.date,
                mealTime = route.mealTime,
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable<DailySummary> { backStackEntry ->
            val route = backStackEntry.toRoute<DailySummary>()
            NutritionDailySummaryScreen(
                date = route.date,
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable<History> {
            NutritionHistoryScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}
