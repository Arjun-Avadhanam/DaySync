package com.daysync.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "venues")
data class VenueEntity(
    @PrimaryKey val id: String,
    val name: String,
    val city: String? = null,
    val country: String? = null,
    val capacity: Int? = null,
    val imageUrl: String? = null,
)
