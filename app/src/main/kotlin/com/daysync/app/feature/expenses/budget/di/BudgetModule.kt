package com.daysync.app.feature.expenses.budget.di

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.daysync.app.core.config.UserPreferences
import com.daysync.app.core.database.dao.BudgetDao
import com.daysync.app.core.database.dao.ExpenseDao
import com.daysync.app.feature.expenses.budget.BudgetAlertEvaluator
import com.daysync.app.feature.expenses.budget.BudgetAlertStore
import com.daysync.app.feature.expenses.budget.BudgetNotificationChannel
import com.daysync.app.feature.expenses.budget.data.BudgetRepository
import com.daysync.app.feature.expenses.budget.data.BudgetRepositoryImpl
import com.daysync.app.feature.expenses.budget.model.ResolvedBudget
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BudgetModule {

    @Provides
    @Singleton
    fun provideBudgetAlertEvaluator(
        @ApplicationContext context: Context,
        budgetDao: BudgetDao,
        expenseDao: ExpenseDao,
        alertStore: BudgetAlertStore,
        userPreferences: UserPreferences,
    ): BudgetAlertEvaluator {
        val post: (ResolvedBudget, Int) -> Unit = { rb, level ->
            BudgetNotificationChannel.createChannel(context)
            val title = when (level) {
                100 -> "Budget reached: ${rb.label}"
                else -> "$level% of budget used: ${rb.label}"
            }
            val body = if (level >= 100) {
                "You've hit your ${userPreferences.formatCurrency(rb.amount)} budget for ${rb.label}."
            } else {
                "You've used $level% of your ${userPreferences.formatCurrency(rb.amount)} budget for ${rb.label}."
            }
            val notification = NotificationCompat.Builder(context, BudgetNotificationChannel.CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .build()
            val id = ("budget_" + rb.instanceKey + "_" + level).hashCode()
            context.getSystemService(NotificationManager::class.java).notify(id, notification)
        }
        return BudgetAlertEvaluator(budgetDao, expenseDao, alertStore, post)
    }

    @Provides
    @Singleton
    fun provideBudgetRepository(
        budgetDao: BudgetDao,
        expenseDao: ExpenseDao,
    ): BudgetRepository {
        return BudgetRepositoryImpl(budgetDao, expenseDao)
    }
}
