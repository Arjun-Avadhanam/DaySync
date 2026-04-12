package com.daysync.app.feature.ai.data

import com.daysync.app.core.ai.ContentMessage
import com.daysync.app.core.ai.GeminiRestClient
import com.daysync.app.feature.ai.model.ChatMessage
import com.daysync.app.feature.ai.model.Role
import kotlinx.coroutines.flow.Flow

class GeminiChatService(
    private val geminiClient: GeminiRestClient,
) {
    fun generateStream(
        messages: List<ChatMessage>,
        systemPrompt: String,
    ): Flow<String> {
        val contents = messages.map { msg ->
            ContentMessage(
                role = if (msg.role == Role.USER) "user" else "model",
                text = msg.content,
            )
        }
        return geminiClient.generateStream(contents, systemPrompt)
    }

    suspend fun generate(
        messages: List<ChatMessage>,
        systemPrompt: String,
    ): String {
        val contents = messages.map { msg ->
            ContentMessage(
                role = if (msg.role == Role.USER) "user" else "model",
                text = msg.content,
            )
        }
        return geminiClient.generate(contents, systemPrompt)
    }
}
