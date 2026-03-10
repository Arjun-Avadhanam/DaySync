package com.daysync.app.core.sync.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExpenseDto(
    val id: String,
    val title: String?,
    val item: String?,
    val date: String,
    val category: String?,
    val frequency: String?,
    @SerialName("unit_cost") val unitCost: Double,
    val quantity: Double,
    @SerialName("delivery_charge") val deliveryCharge: Double,
    @SerialName("total_amount") val totalAmount: Double,
    val notes: String?,
    val source: String,
    @SerialName("merchant_name") val merchantName: String?,
    @SerialName("last_modified") val lastModified: Long,
    @SerialName("is_deleted") val isDeleted: Boolean,
)
