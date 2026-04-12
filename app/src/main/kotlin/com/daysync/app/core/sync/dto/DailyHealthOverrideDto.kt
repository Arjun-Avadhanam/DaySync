package com.daysync.app.core.sync.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DailyHealthOverrideDto(
    val date: String,
    @SerialName("total_calories") val totalCalories: Double?,
    @SerialName("weight_morning") val weightMorning: Double?,
    @SerialName("weight_evening") val weightEvening: Double?,
    @SerialName("weight_night") val weightNight: Double?,
    @SerialName("last_modified") val lastModified: Long,
    @SerialName("is_deleted") val isDeleted: Boolean,
)
