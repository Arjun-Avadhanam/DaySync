package com.daysync.app.feature.expenses.service

import android.util.Base64
import com.daysync.app.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

class GeminiReceiptService {

    private val client = HttpClient(OkHttp)

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun extractFromImage(
        imageBytes: ByteArray,
        mimeType: String = "image/jpeg",
    ): ReceiptData {
        val apiKey = BuildConfig.GEMINI_API_KEY
        require(apiKey.isNotBlank()) { "GEMINI_API_KEY not configured in local.properties" }

        val base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

        val requestBody = buildJsonObject {
            putJsonArray("contents") {
                add(buildJsonObject {
                    putJsonArray("parts") {
                        add(buildJsonObject {
                            put("text", EXTRACTION_PROMPT)
                        })
                        add(buildJsonObject {
                            putJsonObject("inline_data") {
                                put("mime_type", mimeType)
                                put("data", base64Image)
                            }
                        })
                    }
                })
            }
            putJsonObject("generationConfig") {
                put("responseMimeType", "application/json")
            }
        }.toString()

        val response = client.post(
            "$BASE_URL/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
        ) {
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }

        val responseText = response.bodyAsText()
        val responseJson = json.parseToJsonElement(responseText) as JsonObject

        val candidates = responseJson["candidates"]?.jsonArray
            ?: throw IllegalStateException("No candidates in Gemini response")
        val content = candidates[0].jsonObject["content"]?.jsonObject
            ?: throw IllegalStateException("No content in Gemini response")
        val parts = content["parts"]?.jsonArray
            ?: throw IllegalStateException("No parts in Gemini response")
        val text = parts[0].jsonObject["text"]?.jsonPrimitive?.content
            ?: throw IllegalStateException("No text in Gemini response")

        return json.decodeFromString<ReceiptData>(text)
    }

    companion object {
        private const val BASE_URL = "https://generativelanguage.googleapis.com"

        private val EXTRACTION_PROMPT = """
            Extract expense data from this receipt/bill image. Return a JSON object with these fields:

            - "merchant_name": store or business name (string or null)
            - "date": transaction date in YYYY-MM-DD format (string or null)
            - "total_amount": final total paid (number, required)
            - "tax": total tax amount combining CGST+SGST+GST if present (number or null)
            - "payment_method": "UPI", "Card", "Cash", or "Online" if detectable (string or null)
            - "category": expense category from this list: "Food", "Food > Dining Out", "Food > Groceries", "Food > Snacks", "Food > Beverages", "Food > Delivery", "Travel", "Travel > Auto/Cab", "Travel > Fuel", "Shopping", "Shopping > Clothing", "Shopping > Electronics", "Health", "Health > Medicine", "Bills", "Bills > Subscriptions", "Entertainment", "Education", "Personal Care", "Other" (string or null)
            - "line_items": array of items, each with "name" (string), "quantity" (number or null), "unit_price" (number or null), "total_price" (number). Null if no items visible.

            Use the exact total shown on the receipt. For Indian receipts, amounts are in INR (ignore the currency symbol).
        """.trimIndent()
    }
}
