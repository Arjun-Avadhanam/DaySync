package com.daysync.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.daysync.app.feature.dashboard.ui.AppGuideScreen
import com.daysync.app.feature.dashboard.ui.DashboardScreen
import com.daysync.app.feature.dashboard.ui.SettingsScreen
import com.daysync.app.feature.expenses.ui.CsvImportScreen
import com.daysync.app.feature.expenses.ui.ExpenseFormScreen
import com.daysync.app.feature.expenses.ui.ExpensesScreen
import com.daysync.app.feature.expenses.ui.PayeeRulesScreen
import com.daysync.app.feature.expenses.ui.ReceiptScanScreen
import com.daysync.app.feature.health.ui.HealthScreen
import com.daysync.app.feature.journal.ui.JournalScreen
import com.daysync.app.feature.media.ui.MediaScreen
import com.daysync.app.feature.nutrition.ui.NutritionScreen
import com.daysync.app.feature.sports.ui.SportsScreen

@Composable
fun DaySyncNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Dashboard,
        modifier = modifier,
    ) {
        composable<Dashboard> {
            DashboardScreen(
                onNavigateToSection = { route ->
                    // Use the same nav pattern as the bottom bar so we switch
                    // tabs instead of pushing onto the Home tab's back stack.
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onNavigateToSettings = { navController.navigate(AppSettings) },
            )
        }
        composable<AppSettings> {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenGuide = { navController.navigate(AppGuide) },
            )
        }
        composable<AppGuide> {
            AppGuideScreen(onBack = { navController.popBackStack() })
        }
        composable<Health> { HealthScreen() }
        composable<Nutrition> { NutritionScreen() }
        composable<Expenses> { ExpensesScreen(navController = navController) }
        composable<Sports> { SportsScreen() }
        composable<Journal> { JournalScreen() }
        composable<Media> { MediaScreen() }

        // Expense sub-screens
        composable<ExpenseAdd> {
            ExpenseFormScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable<ExpenseDetail> { backStackEntry ->
            val route = backStackEntry.toRoute<ExpenseDetail>()
            ExpenseFormScreen(
                expenseId = route.expenseId,
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable<ExpenseCsvImport> {
            CsvImportScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable<ExpensePayeeRules> {
            PayeeRulesScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable<ExpenseReceiptScan> {
            ReceiptScanScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
