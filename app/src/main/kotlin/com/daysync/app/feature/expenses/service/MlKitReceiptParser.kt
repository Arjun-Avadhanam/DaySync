package com.daysync.app.feature.expenses.service

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class MlKitReceiptParser {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun extractFromBitmap(bitmap: Bitmap): ReceiptData? {
        val image = InputImage.fromBitmap(bitmap, 0)

        val result = suspendCoroutine { continuation ->
            recognizer.process(image)
                .addOnSuccessListener { text -> continuation.resume(text) }
                .addOnFailureListener { e -> continuation.resumeWithException(e) }
        }

        val fullText = result.text
        if (fullText.isBlank()) return null

        val amount = extractTotalAmount(fullText) ?: return null
        val date = extractDate(fullText)
        val merchantName = extractMerchantName(result.textBlocks.map { it.text })

        return ReceiptData(
            merchantName = merchantName,
            date = date,
            totalAmount = amount,
            category = null,
        )
    }

    private fun extractTotalAmount(text: String): Double? {
        val patterns = listOf(
            Regex("""(?:TOTAL|GRAND\s*TOTAL|NET\s*(?:AMOUNT|PAYABLE)|AMOUNT\s*(?:DUE|PAYABLE)|BILL\s*AMOUNT)[:\s]*(?:Rs\.?|₹|INR)?\s*([\d,]+\.?\d*)""", RegexOption.IGNORE_CASE),
            Regex("""(?:Rs\.?|₹|INR)\s*([\d,]+\.\d{2})"""),
        )

        for (pattern in patterns) {
            val matches = pattern.findAll(text).toList()
            if (matches.isNotEmpty()) {
                val lastMatch = matches.last()
                val amountStr = lastMatch.groupValues[1].replace(",", "")
                amountStr.toDoubleOrNull()?.let { return it }
            }
        }

        return null
    }

    private fun extractDate(text: String): String? {
        val patterns = listOf(
            Regex("""(\d{4})-(\d{2})-(\d{2})"""),
            Regex("""(\d{1,2})/(\d{1,2})/(\d{4})"""),
            Regex("""(\d{1,2})-(\d{1,2})-(\d{4})"""),
            Regex("""(\d{1,2})/(\d{1,2})/(\d{2})"""),
            Regex("""(\d{1,2})-(\d{1,2})-(\d{2})"""),
        )

        for (pattern in patterns) {
            val match = pattern.find(text) ?: continue
            return try {
                when {
                    // YYYY-MM-DD
                    match.groupValues[1].length == 4 -> match.value
                    // DD/MM/YYYY or DD-MM-YYYY
                    match.groupValues[3].length == 4 -> {
                        val day = match.groupValues[1].padStart(2, '0')
                        val month = match.groupValues[2].padStart(2, '0')
                        val year = match.groupValues[3]
                        "$year-$month-$day"
                    }
                    // DD/MM/YY or DD-MM-YY
                    else -> {
                        val day = match.groupValues[1].padStart(2, '0')
                        val month = match.groupValues[2].padStart(2, '0')
                        val year = "20${match.groupValues[3]}"
                        "$year-$month-$day"
                    }
                }
            } catch (_: Exception) {
                null
            }
        }

        return null
    }

    private fun extractMerchantName(textBlocks: List<String>): String? {
        return textBlocks.firstOrNull { block ->
            block.isNotBlank() &&
                block.length in 2..60 &&
                !block.matches(Regex("""^[\d\s.,:/-]+$""")) &&
                !block.contains("TOTAL", ignoreCase = true) &&
                !block.contains("TAX", ignoreCase = true) &&
                !block.contains("GST", ignoreCase = true)
        }?.trim()
    }
}
