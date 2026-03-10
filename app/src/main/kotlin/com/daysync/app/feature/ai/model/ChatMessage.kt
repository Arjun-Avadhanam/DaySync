package com.daysync.app.feature.ai.model

import kotlin.time.Clock
import kotlin.time.Instant

data class ChatMessage(
    val id: String,
    val role: Role,
    val content: String,
    val timestamp: Instant = Clock.System.now(),
    val isStreaming: Boolean = false,
)

enum class Role { USER, ASSISTANT }
