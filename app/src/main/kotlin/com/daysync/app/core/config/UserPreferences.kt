package com.daysync.app.core.config

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Runtime-configurable user preferences. Backed by SharedPreferences
 * so values persist across app restarts and are editable from Settings.
 *
 * Defaults mirror the original hardcoded values (IST, INR, 23:59/23:30)
 * so existing users see no change until they explicitly configure.
 */
@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("daysync_user_prefs", Context.MODE_PRIVATE)

    // ── Timezone ────────────────────────────────────────────────────

    var timezoneId: String
        get() = prefs.getString(KEY_TIMEZONE, "Asia/Kolkata") ?: "Asia/Kolkata"
        set(value) = prefs.edit().putString(KEY_TIMEZONE, value).apply()

    val javaZoneId: java.time.ZoneId get() = java.time.ZoneId.of(timezoneId)
    val kotlinTimeZone: kotlinx.datetime.TimeZone get() = kotlinx.datetime.TimeZone.of(timezoneId)

    // ── Currency ────────────────────────────────────────────────────

    var currencyCode: String
        get() = prefs.getString(KEY_CURRENCY, "INR") ?: "INR"
        set(value) = prefs.edit().putString(KEY_CURRENCY, value).apply()

    val currencySymbol: String
        get() = CURRENCY_SYMBOLS[currencyCode] ?: currencyCode

    val currencyLocale: java.util.Locale
        get() = CURRENCY_LOCALES[currencyCode] ?: java.util.Locale("en", "IN")

    fun formatCurrency(amount: Double): String {
        return java.text.NumberFormat.getCurrencyInstance(currencyLocale).format(amount)
    }

    // ── Sync & Reminder times ───────────────────────────────────────

    var syncHour: Int
        get() = prefs.getInt(KEY_SYNC_HOUR, 23)
        set(value) = prefs.edit().putInt(KEY_SYNC_HOUR, value).apply()

    var syncMinute: Int
        get() = prefs.getInt(KEY_SYNC_MINUTE, 59)
        set(value) = prefs.edit().putInt(KEY_SYNC_MINUTE, value).apply()

    var reminderHour: Int
        get() = prefs.getInt(KEY_REMINDER_HOUR, 23)
        set(value) = prefs.edit().putInt(KEY_REMINDER_HOUR, value).apply()

    var reminderMinute: Int
        get() = prefs.getInt(KEY_REMINDER_MINUTE, 30)
        set(value) = prefs.edit().putInt(KEY_REMINDER_MINUTE, value).apply()

    // ── Notion (optional) ───────────────────────────────────────────

    val isNotionConfigured: Boolean
        get() = com.daysync.app.BuildConfig.NOTION_API_KEY.isNotBlank()

    var notionSummaryPageId: String
        get() = prefs.getString(KEY_NOTION_SUMMARY_PAGE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_NOTION_SUMMARY_PAGE, value).apply()

    // ── Calorie deficit baseline ────────────────────────────────────

    var calorieDeficitBaseline: Int
        get() = prefs.getInt(KEY_CALORIE_BASELINE, com.daysync.app.BuildConfig.CALORIE_DEFICIT_BASELINE)
        set(value) = prefs.edit().putInt(KEY_CALORIE_BASELINE, value).apply()

    companion object {
        private const val KEY_TIMEZONE = "timezone"
        private const val KEY_CURRENCY = "currency"
        private const val KEY_SYNC_HOUR = "sync_hour"
        private const val KEY_SYNC_MINUTE = "sync_minute"
        private const val KEY_REMINDER_HOUR = "reminder_hour"
        private const val KEY_REMINDER_MINUTE = "reminder_minute"
        private const val KEY_NOTION_SUMMARY_PAGE = "notion_summary_page_id"
        private const val KEY_CALORIE_BASELINE = "calorie_deficit_baseline"

        val SUPPORTED_CURRENCIES = listOf("INR", "USD", "EUR", "GBP", "AED", "SGD", "AUD", "CAD", "JPY")

        val SUPPORTED_TIMEZONES = listOf(
            "Asia/Kolkata", "America/New_York", "America/Chicago", "America/Denver",
            "America/Los_Angeles", "Europe/London", "Europe/Paris", "Europe/Berlin",
            "Asia/Dubai", "Asia/Singapore", "Asia/Tokyo", "Australia/Sydney",
            "Pacific/Auckland",
        )

        private val CURRENCY_SYMBOLS = mapOf(
            "INR" to "₹", "USD" to "$", "EUR" to "€", "GBP" to "£",
            "AED" to "د.إ", "SGD" to "S$", "AUD" to "A$", "CAD" to "C$", "JPY" to "¥",
        )

        private val CURRENCY_LOCALES = mapOf(
            "INR" to java.util.Locale("en", "IN"),
            "USD" to java.util.Locale.US,
            "EUR" to java.util.Locale.FRANCE,
            "GBP" to java.util.Locale.UK,
            "AED" to java.util.Locale("en", "AE"),
            "SGD" to java.util.Locale("en", "SG"),
            "AUD" to java.util.Locale("en", "AU"),
            "CAD" to java.util.Locale.CANADA,
            "JPY" to java.util.Locale.JAPAN,
        )
    }
}
