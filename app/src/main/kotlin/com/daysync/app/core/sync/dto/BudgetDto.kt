package com.daysync.app.core.sync.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BudgetDto(
    val id: String,
    val type: String,
    val category: String? = null,
    val amount: Double,
    val recurring: Boolean,
    @SerialName("year_month") val yearMonth: String? = null,
    @SerialName("week_block") val weekBlock: Int? = null,
    @SerialName("start_date") val startDate: String? = null,
    @SerialName("end_date") val endDate: String? = null,
    val label: String? = null,
    @SerialName("last_modified") val lastModified: Long,
    @SerialName("is_deleted") val isDeleted: Boolean,
)
