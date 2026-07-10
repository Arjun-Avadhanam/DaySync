package com.daysync.app.feature.expenses.budget

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface BudgetAlertLevels {
    fun getLevel(key: String): Int
    fun setLevel(key: String, level: Int)
    fun keys(): Set<String>
    fun prune(validKeys: Set<String>)
}

/**
 * Device-local record of the highest alert threshold already fired per budget instance.
 * Never synced — alerts are per-device. Keys are ResolvedBudget.instanceKey.
 */
@Singleton
class BudgetAlertStore @Inject constructor(
    @ApplicationContext context: Context,
) : BudgetAlertLevels {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("daysync_budget_alerts", Context.MODE_PRIVATE)

    override fun getLevel(key: String): Int = prefs.getInt(key, 0)

    override fun setLevel(key: String, level: Int) {
        prefs.edit().putInt(key, level).apply()
    }

    override fun keys(): Set<String> = prefs.all.keys.toSet()

    override fun prune(validKeys: Set<String>) {
        val editor = prefs.edit()
        for (k in prefs.all.keys) if (k !in validKeys) editor.remove(k)
        editor.apply()
    }
}
