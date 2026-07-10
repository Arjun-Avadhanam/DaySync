package com.daysync.app.feature.expenses.budget

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.daysync.app.core.config.UserPreferences
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlin.time.Clock
import kotlinx.datetime.todayIn

@HiltWorker
class BudgetCheckWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val evaluator: BudgetAlertEvaluator,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val prefs = UserPreferences(applicationContext)
        val today = Clock.System.todayIn(prefs.kotlinTimeZone)
        evaluator.onExpenseChanged(setOf(today))
        return Result.success()
    }
}
