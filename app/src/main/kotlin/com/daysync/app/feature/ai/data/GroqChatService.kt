package com.daysync.app.feature.ai.data

import com.daysync.app.BuildConfig
import com.daysync.app.feature.ai.model.ChatMessage
import com.daysync.app.feature.ai.model.Role
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class GroqChatService(
    private val httpClient: HttpClient,
) {
    companion object {
        private const val ENDPOINT = "https://api.groq.com/openai/v1/chat/completions"
        private const val MODEL = "llama-3.3-70b-versatile"
    }

    suspend fun generate(
        messages: List<ChatMessage>,
        systemPrompt: String,
    ): String {
        val requestMessages = buildList {
            add(GroqMessage(role = "system", content = systemPrompt))
            messages.forEach { msg ->
                add(GroqMessage(
                    role = if (msg.role == Role.USER) "user" else "assistant",
                    content = msg.content,
                ))
            }
        }

        val request = GroqRequest(
            model = MODEL,
            messages = requestMessages,
        )

        val response: GroqResponse = httpClient.post(ENDPOINT) {
            contentType(ContentType.Application.Json)
            headers {
                append(HttpHeaders.Authorization, "Bearer ${BuildConfig.GROQ_API_KEY}")
            }
            setBody(request)
        }.body()

        return response.choices.firstOrNull()?.message?.content ?: ""
    }
}

@Serializable
private data class GroqRequest(
    val model: String,
    val messages: List<GroqMessage>,
)

@Serializable
private data class GroqMessage(
    val role: String,
    val content: String,
)

@Serializable
private data class GroqResponse(
    val choices: List<GroqChoice>,
)

@Serializable
private data class GroqChoice(
    val message: GroqResponseMessage,
)

@Serializable
private data class GroqResponseMessage(
    val role: String,
    val content: String,
)
