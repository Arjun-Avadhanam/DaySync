package com.daysync.app.feature.expenses.di

import com.daysync.app.core.database.dao.ExpenseDao
import com.daysync.app.core.database.dao.PayeeRuleDao
import com.daysync.app.feature.expenses.data.ExpenseRepository
import com.daysync.app.feature.expenses.data.ExpenseRepositoryImpl
import com.daysync.app.feature.expenses.service.GeminiReceiptService
import com.daysync.app.feature.expenses.service.MlKitReceiptParser
import com.daysync.app.feature.expenses.service.TransactionDeduplicator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ExpenseModule {

    @Provides
    @Singleton
    fun provideTransactionDeduplicator(expenseDao: ExpenseDao): TransactionDeduplicator {
        return TransactionDeduplicator(expenseDao)
    }

    @Provides
    @Singleton
    fun provideGeminiReceiptService(): GeminiReceiptService {
        return GeminiReceiptService()
    }

    @Provides
    @Singleton
    fun provideMlKitReceiptParser(): MlKitReceiptParser {
        return MlKitReceiptParser()
    }

    @Provides
    @Singleton
    fun provideExpenseRepository(
        expenseDao: ExpenseDao,
        payeeRuleDao: PayeeRuleDao,
        deduplicator: TransactionDeduplicator,
    ): ExpenseRepository {
        return ExpenseRepositoryImpl(expenseDao, payeeRuleDao, deduplicator)
    }
}
