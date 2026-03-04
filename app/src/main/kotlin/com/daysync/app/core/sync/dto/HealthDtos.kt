package com.daysync.app.core.sync.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HealthMetricDto(
    val id: String,
    val type: String,
    val value: Double,
    val unit: String,
    val timestamp: Long,
    val source: String,
    @SerialName("last_modified") val lastModified: Long,
    @SerialName("is_deleted") val isDeleted: Boolean,
)

@Serializable
data class SleepSessionDto(
    val id: String,
    @SerialName("start_time") val startTime: Long,
    @SerialName("end_time") val endTime: Long,
    @SerialName("total_minutes") val totalMinutes: Int,
    @SerialName("deep_minutes") val deepMinutes: Int,
    @SerialName("light_minutes") val lightMinutes: Int,
    @SerialName("rem_minutes") val remMinutes: Int,
    @SerialName("awake_minutes") val awakeMinutes: Int,
    val score: Int?,
    @SerialName("last_modified") val lastModified: Long,
    @SerialName("is_deleted") val isDeleted: Boolean,
)

@Serializable
data class ExerciseSessionDto(
    val id: String,
    @SerialName("exercise_type") val exerciseType: String,
    @SerialName("start_time") val startTime: Long,
    @SerialName("end_time") val endTime: Long,
    val calories: Double?,
    @SerialName("avg_heart_rate") val avgHeartRate: Int?,
    @SerialName("max_heart_rate") val maxHeartRate: Int?,
    val distance: Double?,
    @SerialName("elevation_gain") val elevationGain: Double?,
    val notes: String?,
    @SerialName("last_modified") val lastModified: Long,
    @SerialName("is_deleted") val isDeleted: Boolean,
)
