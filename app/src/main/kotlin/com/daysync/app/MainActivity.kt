package com.daysync.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.daysync.app.ui.navigation.AiFab
import com.daysync.app.ui.navigation.BottomNavBar
import com.daysync.app.ui.navigation.DaySyncNavHost
import com.daysync.app.ui.theme.DaySyncTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* ignore result */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        maybeRequestNotificationPermission()
        val navigateTo = intent?.getStringExtra("navigate_to")

        setContent {
            DaySyncTheme {
                val navController = rememberNavController()

                // Handle deep link from expense classification notification
                androidx.compose.runtime.LaunchedEffect(navigateTo) {
                    if (navigateTo == "expense_detail") {
                        navController.navigate(com.daysync.app.ui.navigation.Expenses) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                        }
                    }
                }

                Scaffold(
                    bottomBar = { BottomNavBar(navController) },
                    floatingActionButton = { AiFab() },
                ) { innerPadding ->
                    DaySyncNavHost(
                        navController = navController,
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }

    // On Android 13+ POST_NOTIFICATIONS is a runtime permission; without it the
    // expense classification prompts (and any other app notifications) are
    // silently dropped by the system. Ask on every launch until granted — the
    // OS handles the "don't ask again" state automatically.
    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
