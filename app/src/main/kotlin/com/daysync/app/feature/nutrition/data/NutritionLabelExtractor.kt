package com.daysync.app.feature.nutrition.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import com.daysync.app.core.ai.GeminiRestClient
import com.daysync.app.feature.ai.data.GroqChatService
import com.daysync.app.feature.ai.model.ChatMessage
import com.daysync.app.feature.ai.model.Role
import com.daysync.app.feature.nutrition.domain.model.NutritionLabelResult
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NutritionLabelExtractor @Inject constructor(
    private val geminiClient: GeminiRestClient,
    private val groqChatService: GroqChatService,
) {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    suspend fun extractFromImage(imageBytes: ByteArray): NutritionLabelResult =
        withContext(Dispatchers.IO) {
            val compressed = compressBitmapForApi(imageBytes, maxDim = 1024)
            val base64 = Base64.encodeToString(compressed, Base64.NO_WRAP)

            val text = try {
                geminiClient.generateWithImage(
                    prompt = VISION_PROMPT,
                    imageBase64 = base64,
                    mimeType = "image/jpeg",
                    jsonMode = true,
                )
            } catch (e: Exception) {
                Log.w(TAG, "Gemini failed, falling back to ML Kit OCR + Groq", e)
                ocrThenGroq(compressed)
            }

            val cleaned = text.trim()
                .removePrefix("```json").removePrefix("```")
                .removeSuffix("```")
                .trim()

            json.decodeFromString<NutritionLabelResult>(cleaned)
        }

    /**
     * Fallback: ML Kit OCR extracts raw text from the label image,
     * then Groq parses the text into structured nutrition JSON.
     */
    private suspend fun ocrThenGroq(imageBytes: ByteArray): String {
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            ?: throw IllegalStateException("Cannot decode image for OCR")
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        val visionText = suspendCoroutine { cont ->
            recognizer.process(inputImage)
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }
        bitmap.recycle()

        val ocrText = visionText.text
        if (ocrText.isBlank()) {
            throw IllegalStateException("OCR extracted no text from label image")
        }
        Log.d(TAG, "OCR extracted ${ocrText.length} chars, sending to Groq")

        return groqChatService.generate(
            messages = listOf(ChatMessage(
                id = java.util.UUID.randomUUID().toString(),
                role = Role.USER,
                content = "Here is the text extracted from a nutrition facts label via OCR:\n\n$ocrText\n\n$TEXT_PARSE_PROMPT",
            )),
            systemPrompt = "You are a nutrition data extraction assistant. Always respond with valid JSON only, no markdown fences or explanations.",
        )
    }

    companion object {
        private const val TAG = "NutritionLabelExtractor"

        fun compressBitmapForApi(imageBytes: ByteArray, maxDim: Int = 1024): ByteArray {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)

            val origWidth = options.outWidth
            val origHeight = options.outHeight

            if (origWidth <= maxDim && origHeight <= maxDim) {
                return imageBytes
            }

            var sampleSize = 1
            while (origWidth / sampleSize > maxDim || origHeight / sampleSize > maxDim) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, decodeOptions)
                ?: return imageBytes

            val scale = maxDim.toFloat() / maxOf(bitmap.width, bitmap.height)
            val scaled = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * scale).toInt(),
                    (bitmap.height * scale).toInt(),
                    true,
                ).also { if (it !== bitmap) bitmap.recycle() }
            } else {
                bitmap
            }

            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
            scaled.recycle()
            return out.toByteArray()
        }

        private val VISION_PROMPT = """
Extract ALL nutritional information from this nutrition facts label image.

Rules:
1. Always calculate/extract per-100g or per-100ml values into "per_100". If the label only has a non-100 amount (e.g. "per 200g"), divide all values to get per-100 values.
2. If a separate serving column exists (e.g. "per glass 250ml"), extract those into "per_serving" with serving_size, serving_amount, and serving_unit.
3. Set "detected_unit" to "g" for solid foods or "ml" for liquids/beverages.
4. Extract the product name if visible on the label.
5. Suggest a food category (e.g. "Dairy", "Snacks", "Beverages", "Cereal", "Meat").

Return ONLY valid JSON with this exact structure:
{
  "product_name": "string",
  "category": "string or null",
  "per_100": {
    "calories": number,
    "protein_g": number,
    "carbs_g": number,
    "fat_g": number,
    "sugar_g": number
  },
  "per_serving": null or {
    "calories": number,
    "protein_g": number,
    "carbs_g": number,
    "fat_g": number,
    "sugar_g": number
  },
  "serving_size": "string or null",
  "serving_amount": number or null,
  "serving_unit": "string or null",
  "detected_unit": "g" or "ml"
}

Use 0 for any nutrient not found on the label. All numeric values should be plain numbers (no units).
""".trimIndent()

        private val TEXT_PARSE_PROMPT = """
Parse the OCR text above into this exact JSON structure:
{
  "product_name": "string",
  "category": "string or null",
  "per_100": {"calories": number, "protein_g": number, "carbs_g": number, "fat_g": number, "sugar_g": number},
  "per_serving": null or {"calories": number, "protein_g": number, "carbs_g": number, "fat_g": number, "sugar_g": number},
  "serving_size": "string or null",
  "serving_amount": number or null,
  "serving_unit": "string or null",
  "detected_unit": "g" or "ml"
}

Rules: Extract per-100g/100ml values. If label shows a different amount, divide to get per-100.
Use 0 for missing nutrients. Return ONLY valid JSON.
""".trimIndent()
    }
}
