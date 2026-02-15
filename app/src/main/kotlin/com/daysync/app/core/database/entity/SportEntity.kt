package com.daysync.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sports")
data class SportEntity(
    @PrimaryKey val id: String,
    val name: String,
    val sportType: String, // TEAM, INDIVIDUAL, RACING
    val icon: String? = null,
)
