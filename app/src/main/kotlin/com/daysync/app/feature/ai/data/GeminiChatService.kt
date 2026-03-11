package com.daysync.app.feature.ai.data

import com.daysync.app.feature.ai.model.ChatMessage
import com.daysync.app.feature.ai.model.Role
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class GeminiChatService(
    private val apiKey: String,
) {
    companion object {
        private const val MODEL = "gemini-2.5-flash"
    }

    private fun createModel(systemPrompt: String): GenerativeModel {
        return GenerativeModel(
            modelName = MODEL,
            apiKey = apiKey,
            systemInstruction = content { text(systemPrompt) },
        )
    }

    fun generateStream(
        messages: List<ChatMessage>,
        systemPrompt: String,
    ): Flow<String> = flow {
        val model = createModel(systemPrompt)

        val contents = messages.map { msg ->
            content(role = if (msg.role == Role.USER) "user" else "model") {
                text(msg.content)
            }
        }

        model.generateContentStream(*contents.toTypedArray()).collect { chunk ->
            chunk.text?.let { text ->
                if (text.isNotEmpty()) {
                    emit(text)
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun generate(
        messages: List<ChatMessage>,
        systemPrompt: String,
    ): String {
        val model = createModel(systemPrompt)

        val contents = messages.map { msg ->
            content(role = if (msg.role == Role.USER) "user" else "model") {
                text(msg.content)
            }
        }

        val response = model.generateContent(*contents.toTypedArray())
        return response.text ?: ""
    }
}
