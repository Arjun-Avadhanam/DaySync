package com.daysync.app.feature.ai.data

import com.daysync.app.BuildConfig
import com.daysync.app.feature.ai.model.ChatMessage
import com.daysync.app.feature.ai.model.Role
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

class GroqChatService(
    private val httpClient: HttpClient,
) {
    companion object {
        private const val ENDPOINT = "https://api.groq.com/openai/v1/chat/completions"
        private const val MODEL = "llama-3.3-70b-versatile"
        private const val VISION_MODEL = "llama-3.2-90b-vision-preview"
    }

    private val json = Json { ignoreUnknownKeys = true }

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

    /**
     * Vision model — sends an image + text prompt to Groq's Llama 3.2 Vision.
     * Used as fallback when Gemini fails for nutrition label / receipt scanning.
     */
    suspend fun generateWithImage(
        prompt: String,
        imageBase64: String,
        mimeType: String = "image/jpeg",
    ): String {
        // Groq vision uses OpenAI-compatible multimodal content format
        val body = buildJsonObject {
            put("model", VISION_MODEL)
            put("max_tokens", 2048)
            putJsonArray("messages") {
                add(buildJsonObject {
                    put("role", "user")
                    putJsonArray("content") {
                        add(buildJsonObject {
                            put("type", "text")
                            put("text", prompt)
                        })
                        add(buildJsonObject {
                            put("type", "image_url")
                            putJsonObject("image_url") {
                                put("url", "data:$mimeType;base64,$imageBase64")
                            }
                        })
                    }
                })
            }
        }.toString()

        val responseText = httpClient.post(ENDPOINT) {
            contentType(ContentType.Application.Json)
            headers {
                append(HttpHeaders.Authorization, "Bearer ${BuildConfig.GROQ_API_KEY}")
            }
            setBody(body)
        }.bodyAsText()

        val obj = json.parseToJsonElement(responseText) as JsonObject
        return obj["choices"]?.jsonArray
            ?.firstOrNull()?.jsonObject
            ?.get("message")?.jsonObject
            ?.get("content")?.jsonPrimitive?.content
            ?: throw IllegalStateException("No content in Groq vision response: ${responseText.take(300)}")
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
