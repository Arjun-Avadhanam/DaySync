package com.daysync.app.feature.expenses.service

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReceiptData(
    @SerialName("merchant_name") val merchantName: String? = null,
    val date: String? = null,
    @SerialName("total_amount") val totalAmount: Double,
    val tax: Double? = null,
    @SerialName("payment_method") val paymentMethod: String? = null,
    val category: String? = null,
    @SerialName("line_items") val lineItems: List<ReceiptLineItem>? = null,
)

@Serializable
data class ReceiptLineItem(
    val name: String,
    val quantity: Double? = null,
    @SerialName("unit_price") val unitPrice: Double? = null,
    @SerialName("total_price") val totalPrice: Double,
)
