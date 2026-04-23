package com.daysync.app.feature.dashboard.ui

import android.Manifest
import android.content.Context
import androidx.compose.foundation.clickable
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.remember
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.daysync.app.feature.expenses.ui.NotificationAccessHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenGuide: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Settings") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Permissions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            PermissionRow(
                title = "Notifications",
                description = "Required for expense classification prompts and sync alerts",
                isGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(
                        context, Manifest.permission.POST_NOTIFICATIONS,
                    ) == PackageManager.PERMISSION_GRANTED
                } else {
                    true
                },
                onManage = { openAppSettings(context) },
            )

            PermissionRow(
                title = "Camera",
                description = "Used for scanning nutrition labels and receipts",
                isGranted = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.CAMERA,
                ) == PackageManager.PERMISSION_GRANTED,
                onManage = { openAppSettings(context) },
            )

            PermissionRow(
                title = "Notification Listener",
                description = "Reads notifications for alerts",
                isGranted = NotificationAccessHelper.isNotificationAccessEnabled(context),
                onManage = { NotificationAccessHelper.openNotificationAccessSettings(context) },
            )

            PermissionRow(
                title = "Health Connect",
                description = "Reads steps, heart rate, sleep, workouts from your watch",
                isGranted = null, // Can't easily check all 16 HC permissions inline
                onManage = { openHealthConnectSettings(context) },
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Help",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenGuide() },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("App Guide", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.weight(1f))
                    Text("View tutorial", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Crash log (diagnostic — only visible if a crash was captured)
            val lastCrash = remember { com.daysync.app.core.CrashLogger.getLastCrash(context) }
            if (lastCrash != null) {
                Text(
                    text = "Diagnostics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Last Crash Log",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = lastCrash.take(1500),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // ── Configuration ─────────────────────────────────────────
            Text(
                text = "Configuration",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            val userPrefs = remember {
                com.daysync.app.core.config.UserPreferences(context)
            }
            var selectedTimezone by remember { mutableStateOf(userPrefs.timezoneId) }
            var selectedCurrency by remember { mutableStateOf(userPrefs.currencyCode) }
            var syncHour by remember { mutableIntStateOf(userPrefs.syncHour) }
            var syncMinute by remember { mutableIntStateOf(userPrefs.syncMinute) }
            var reminderHour by remember { mutableIntStateOf(userPrefs.reminderHour) }
            var reminderMinute by remember { mutableIntStateOf(userPrefs.reminderMinute) }
            var showTimezoneMenu by remember { mutableStateOf(false) }
            var showCurrencyMenu by remember { mutableStateOf(false) }
            var showSyncTimePicker by remember { mutableStateOf(false) }
            var showReminderTimePicker by remember { mutableStateOf(false) }

            // Timezone
            ConfigRow(
                label = "Timezone",
                value = selectedTimezone,
                onClick = { showTimezoneMenu = true },
            )
            DropdownMenu(expanded = showTimezoneMenu, onDismissRequest = { showTimezoneMenu = false }) {
                com.daysync.app.core.config.UserPreferences.SUPPORTED_TIMEZONES.forEach { tz ->
                    DropdownMenuItem(
                        text = { Text(tz) },
                        onClick = {
                            selectedTimezone = tz
                            userPrefs.timezoneId = tz
                            showTimezoneMenu = false
                        },
                    )
                }
            }

            // Currency
            ConfigRow(
                label = "Currency",
                value = "$selectedCurrency (${userPrefs.currencySymbol})",
                onClick = { showCurrencyMenu = true },
            )
            DropdownMenu(expanded = showCurrencyMenu, onDismissRequest = { showCurrencyMenu = false }) {
                com.daysync.app.core.config.UserPreferences.SUPPORTED_CURRENCIES.forEach { code ->
                    DropdownMenuItem(
                        text = { Text(code) },
                        onClick = {
                            selectedCurrency = code
                            userPrefs.currencyCode = code
                            showCurrencyMenu = false
                        },
                    )
                }
            }

            // Sync time
            ConfigRow(
                label = "Daily Sync Time",
                value = "%02d:%02d".format(syncHour, syncMinute),
                onClick = { showSyncTimePicker = true },
            )
            if (showSyncTimePicker) {
                TimePickerDialog(
                    initialHour = syncHour,
                    initialMinute = syncMinute,
                    onConfirm = { h, m ->
                        syncHour = h; syncMinute = m
                        userPrefs.syncHour = h; userPrefs.syncMinute = m
                        showSyncTimePicker = false
                    },
                    onDismiss = { showSyncTimePicker = false },
                )
            }

            // Reminder time
            ConfigRow(
                label = "Daily Reminder Time",
                value = "%02d:%02d".format(reminderHour, reminderMinute),
                onClick = { showReminderTimePicker = true },
            )
            if (showReminderTimePicker) {
                TimePickerDialog(
                    initialHour = reminderHour,
                    initialMinute = reminderMinute,
                    onConfirm = { h, m ->
                        reminderHour = h; reminderMinute = m
                        userPrefs.reminderHour = h; userPrefs.reminderMinute = m
                        showReminderTimePicker = false
                    },
                    onDismiss = { showReminderTimePicker = false },
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "About",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("DaySync", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "Personal daily tracker — health, nutrition, expenses, sports, journal, media.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionRow(
    title: String,
    description: String,
    isGranted: Boolean?,
    onManage: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                if (isGranted != null) {
                    Text(
                        text = if (isGranted) "Granted" else "Denied",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isGranted) Color(0xFF4CAF50) else Color(0xFFEF5350),
                    )
                }
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onManage) {
                Text("Manage")
            }
        }
    }
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    context.startActivity(intent)
}

private fun openHealthConnectSettings(context: Context) {
    try {
        val intent = Intent("androidx.health.ACTION_HEALTH_CONNECT_SETTINGS").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        openAppSettings(context)
    }
}

@Composable
private fun ConfigRow(
    label: String,
    value: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberTimePickerState(initialHour = initialHour, initialMinute = initialMinute)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select time") },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
