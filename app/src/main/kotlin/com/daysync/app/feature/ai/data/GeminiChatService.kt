package com.daysync.app.feature.ai.data

import com.daysync.app.feature.ai.model.ChatMessage
import com.daysync.app.feature.ai.model.Role
import com.google.genai.Client
import com.google.genai.types.Content
import com.google.genai.types.GenerateContentConfig
import com.google.genai.types.Part
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class GeminiChatService(
    private val client: Client,
) {
    companion object {
        private const val MODEL = "gemini-2.5-flash"
    }

    fun generateStream(
        messages: List<ChatMessage>,
        systemPrompt: String,
    ): Flow<String> = flow {
        val contents = messages.map { msg ->
            Content.builder()
                .role(if (msg.role == Role.USER) "user" else "model")
                .parts(listOf(Part.fromText(msg.content)))
                .build()
        }

        val config = GenerateContentConfig.builder()
            .systemInstruction(Content.fromParts(Part.fromText(systemPrompt)))
            .build()

        val stream = client.models.generateContentStream(MODEL, contents, config)
        for (chunk in stream) {
            val text = chunk.text()
            if (!text.isNullOrEmpty()) {
                emit(text)
            }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun generate(
        messages: List<ChatMessage>,
        systemPrompt: String,
    ): String {
        val contents = messages.map { msg ->
            Content.builder()
                .role(if (msg.role == Role.USER) "user" else "model")
                .parts(listOf(Part.fromText(msg.content)))
                .build()
        }

        val config = GenerateContentConfig.builder()
            .systemInstruction(Content.fromParts(Part.fromText(systemPrompt)))
            .build()

        val response = client.models.generateContent(MODEL, contents, config)
        return response.text() ?: ""
    }
}
