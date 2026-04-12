package com.daysync.app.core.ai

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
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

/**
 * Thin wrapper around the Gemini REST API. Replaces the deprecated
 * com.google.ai.client.generativeai SDK with direct Ktor HTTP calls.
 */
class GeminiRestClient(
    private val httpClient: HttpClient,
    private val apiKey: String,
    private val model: String = "gemini-2.5-flash",
) {
    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta"
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Single-shot generation. Returns the full text response.
     */
    suspend fun generate(
        messages: List<ContentMessage>,
        systemPrompt: String? = null,
    ): String {
        val body = buildRequestBody(messages, systemPrompt)
        val response = httpClient.post("$baseUrl/models/$model:generateContent?key=$apiKey") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        return extractText(response.bodyAsText())
    }

    /**
     * Streaming generation. Emits text chunks as they arrive.
     */
    fun generateStream(
        messages: List<ContentMessage>,
        systemPrompt: String? = null,
    ): Flow<String> = flow {
        val body = buildRequestBody(messages, systemPrompt)
        httpClient.preparePost("$baseUrl/models/$model:streamGenerateContent?key=$apiKey&alt=sse") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }.execute { response ->
            val channel: ByteReadChannel = response.body()
            while (!channel.isClosedForRead) {
                val line = channel.readUTF8Line() ?: break
                if (!line.startsWith("data: ")) continue
                val chunk = line.removePrefix("data: ").trim()
                if (chunk.isEmpty()) continue
                try {
                    val obj = json.parseToJsonElement(chunk) as? JsonObject ?: continue
                    val text = obj["candidates"]?.jsonArray
                        ?.firstOrNull()?.jsonObject
                        ?.get("content")?.jsonObject
                        ?.get("parts")?.jsonArray
                        ?.firstOrNull()?.jsonObject
                        ?.get("text")?.jsonPrimitive?.content
                    if (!text.isNullOrEmpty()) emit(text)
                } catch (_: Exception) {
                    // Skip malformed chunks
                }
            }
        }
    }

    /**
     * Single-shot generation with an inline image (for label/receipt scanning).
     */
    suspend fun generateWithImage(
        prompt: String,
        imageBase64: String,
        mimeType: String = "image/jpeg",
        jsonMode: Boolean = false,
    ): String {
        val body = buildJsonObject {
            putJsonArray("contents") {
                add(buildJsonObject {
                    putJsonArray("parts") {
                        add(buildJsonObject { put("text", prompt) })
                        add(buildJsonObject {
                            putJsonObject("inline_data") {
                                put("mime_type", mimeType)
                                put("data", imageBase64)
                            }
                        })
                    }
                })
            }
            if (jsonMode) {
                putJsonObject("generationConfig") {
                    put("responseMimeType", "application/json")
                }
            }
        }.toString()

        val response = httpClient.post("$baseUrl/models/$model:generateContent?key=$apiKey") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        return extractText(response.bodyAsText())
    }

    private fun buildRequestBody(
        messages: List<ContentMessage>,
        systemPrompt: String?,
    ): String = buildJsonObject {
        putJsonArray("contents") {
            messages.forEach { msg ->
                add(buildJsonObject {
                    put("role", msg.role)
                    putJsonArray("parts") {
                        add(buildJsonObject { put("text", msg.text) })
                    }
                })
            }
        }
        if (systemPrompt != null) {
            putJsonObject("systemInstruction") {
                putJsonArray("parts") {
                    add(buildJsonObject { put("text", systemPrompt) })
                }
            }
        }
    }.toString()

    private fun extractText(responseBody: String): String {
        val obj = json.parseToJsonElement(responseBody) as JsonObject
        return obj["candidates"]?.jsonArray
            ?.firstOrNull()?.jsonObject
            ?.get("content")?.jsonObject
            ?.get("parts")?.jsonArray
            ?.firstOrNull()?.jsonObject
            ?.get("text")?.jsonPrimitive?.content
            ?: throw IllegalStateException("No text in Gemini response: ${responseBody.take(300)}")
    }
}

data class ContentMessage(
    val role: String, // "user" or "model"
    val text: String,
)
