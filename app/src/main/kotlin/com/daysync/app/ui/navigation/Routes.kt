package com.daysync.app.ui.navigation

import kotlinx.serialization.Serializable

@Serializable data object Dashboard
@Serializable data object Health
@Serializable data object Nutrition
@Serializable data object Expenses
@Serializable data object Sports
@Serializable data object Journal
@Serializable data object Media

// Expense sub-screens
@Serializable data object ExpenseAdd
@Serializable data class ExpenseDetail(val expenseId: String)
@Serializable data object ExpenseCsvImport
@Serializable data object ExpensePayeeRules
@Serializable data object ExpenseReceiptScan
