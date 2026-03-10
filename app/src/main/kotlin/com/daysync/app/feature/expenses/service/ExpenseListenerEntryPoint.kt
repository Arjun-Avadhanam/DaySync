package com.daysync.app.feature.expenses.service

import com.daysync.app.feature.expenses.data.ExpenseRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ExpenseListenerEntryPoint {
    fun expenseRepository(): ExpenseRepository
}
