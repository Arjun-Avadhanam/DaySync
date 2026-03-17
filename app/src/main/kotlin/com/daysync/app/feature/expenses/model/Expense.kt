package com.daysync.app.feature.expenses.model

import com.daysync.app.core.database.entity.ExpenseEntity
import com.daysync.app.core.sync.SyncStatus
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.datetime.LocalDate

data class Expense(
    val id: String,
    val title: String? = null,
    val item: String? = null,
    val date: LocalDate,
    val category: String? = null,
    val frequency: String? = null,
    val unitCost: Double,
    val quantity: Double = 1.0,
    val deliveryCharge: Double = 0.0,
    val totalAmount: Double,
    val notes: String? = null,
    val source: String = "MANUAL",
    val merchantName: String? = null,
) {
    val displayTitle: String
        get() = title ?: merchantName ?: item ?: "Expense"

    val formattedAmount: String
        get() = formatIndianCurrency(totalAmount)
}

fun ExpenseEntity.toDomain(): Expense = Expense(
    id = id,
    title = title,
    item = item,
    date = date,
    category = category,
    frequency = frequency,
    unitCost = unitCost,
    quantity = quantity,
    deliveryCharge = deliveryCharge,
    totalAmount = totalAmount,
    notes = notes,
    source = source,
    merchantName = merchantName,
)

fun Expense.toEntity(
    syncStatus: SyncStatus = SyncStatus.PENDING,
): ExpenseEntity = ExpenseEntity(
    id = id,
    title = title,
    item = item,
    date = date,
    category = category,
    frequency = frequency,
    unitCost = unitCost,
    quantity = quantity,
    deliveryCharge = deliveryCharge,
    totalAmount = totalAmount,
    notes = notes,
    source = source,
    merchantName = merchantName,
    syncStatus = syncStatus,
    lastModified = Clock.System.now(),
)

@OptIn(ExperimentalUuidApi::class)
fun generateExpenseId(): String = Uuid.random().toString()

fun formatIndianCurrency(amount: Double): String {
    val isNegative = amount < 0
    val absAmount = kotlin.math.abs(amount)
    val wholePart = absAmount.toLong()
    val decimalPart = ((absAmount - wholePart) * 100).toLong()

    val wholeStr = wholePart.toString()
    val formatted = buildString {
        if (wholeStr.length <= 3) {
            append(wholeStr)
        } else {
            val lastThree = wholeStr.takeLast(3)
            val remaining = wholeStr.dropLast(3)
            val grouped = remaining.reversed().chunked(2)
                .reversed()
                .joinToString(",") { it.reversed() }
            append(grouped)
            append(",")
            append(lastThree)
        }
    }

    val prefix = if (isNegative) "-" else ""
    return if (decimalPart > 0) {
        "$prefix₹$formatted.${decimalPart.toString().padStart(2, '0')}"
    } else {
        "$prefix₹$formatted"
    }
}
