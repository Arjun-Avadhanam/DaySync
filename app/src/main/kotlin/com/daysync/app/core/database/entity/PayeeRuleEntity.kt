package com.daysync.app.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlin.time.Clock
import kotlin.time.Instant

@Entity(
    tableName = "payee_rules",
    indices = [Index("payeeName", unique = true)]
)
data class PayeeRuleEntity(
    @PrimaryKey val id: String,
    val payeeName: String,
    val category: String,
    val defaultTitle: String? = null,
    val createdAt: Instant = Clock.System.now(),
)
