package com.daysync.app.feature.expenses.service

import kotlin.time.Clock
import kotlin.time.Instant

data class ParsedTransaction(
    val amount: Double,
    val merchantName: String?,
    val isDebit: Boolean,
    val isP2P: Boolean,
    val payeeName: String?,
    val referenceId: String?,
    val rawText: String,
    val packageName: String,
    val timestamp: Instant = Clock.System.now(),
)

object NotificationParser {

    val MONITORED_PACKAGES = setOf(
        "com.google.android.apps.nbu.paisa.user", // GPay
        "com.phonepe.app",                         // PhonePe
        "net.one97.paytm",                         // Paytm
        "com.snapwork.hdfc",                       // HDFC Bank
        "in.swiggy.android",                       // Swiggy
        "com.grofers.customerapp",                  // Blinkit
        "com.dreamplug.androidapp",                 // CRED
        "in.amazon.mShop.android.shopping",         // Amazon
        "in.org.npci.upiapp",                       // BHIM
    )

    private val BUSINESS_KEYWORDS = listOf(
        "pvt", "ltd", "llp", "store", "shop", "mart", "restaurant",
        "cafe", "hotel", "hospital", "clinic", "pharmacy", "petrol",
        "fuel", "airlines", "travels", "services", "solutions",
        "enterprise", "industries", "technologies", "tech", "digital",
        "online", "foods", "retail", "traders", "merchant",
    )

    private val AMOUNT_REGEX = Regex("""(?:Rs\.?|INR|₹)\s*([\d,]+(?:\.\d{1,2})?)""")
    private val UPI_REF_REGEX = Regex("""(?:UPI\s*(?:Ref|ref|ID|id)[:\s]*|Ref\s*No[:\s]*)(\d+)""")

    fun parse(packageName: String, title: String?, text: String?): ParsedTransaction? {
        val fullText = text ?: title ?: return null

        return when (packageName) {
            "com.google.android.apps.nbu.paisa.user" -> parseGPay(fullText, packageName)
            "com.phonepe.app" -> parsePhonePe(fullText, packageName)
            "net.one97.paytm" -> parsePaytm(fullText, packageName)
            "com.snapwork.hdfc" -> parseHDFC(fullText, packageName)
            "in.swiggy.android" -> parseSwiggy(fullText, packageName)
            "com.grofers.customerapp" -> parseBlinkit(fullText, packageName)
            "com.dreamplug.androidapp" -> parseCRED(fullText, packageName)
            "in.amazon.mShop.android.shopping" -> parseAmazon(fullText, packageName)
            "in.org.npci.upiapp" -> parseBHIM(fullText, packageName)
            else -> null
        }
    }

    private fun parseGPay(text: String, pkg: String): ParsedTransaction? {
        // "Paid ₹500 to Merchant Name" or "Paid Rs. 500.00 to Merchant"
        val paidPattern = Regex(
            """[Pp]aid\s+(?:Rs\.?|INR|₹)\s*([\d,]+(?:\.\d{1,2})?)\s+to\s+(.+?)(?:\s*$|\s+UPI|\s+Ref)"""
        )
        paidPattern.find(text)?.let { match ->
            val amount = parseAmount(match.groupValues[1]) ?: return null
            val payee = match.groupValues[2].trim()
            return buildTransaction(amount, payee, true, text, pkg)
        }

        // "Received ₹500 from Person" — credit, skip
        if (text.contains("received", ignoreCase = true) ||
            text.contains("credited", ignoreCase = true)
        ) {
            return null
        }

        // Fallback: just extract amount
        return extractGenericDebit(text, pkg)
    }

    private fun parsePhonePe(text: String, pkg: String): ParsedTransaction? {
        // "Paid ₹500 to Merchant" or "Sent ₹500 to Person"
        val paidPattern = Regex(
            """(?:[Pp]aid|[Ss]ent)\s+(?:Rs\.?|INR|₹)\s*([\d,]+(?:\.\d{1,2})?)\s+to\s+(.+?)(?:\s*$|\s+UPI|\s+Ref|\.)"""
        )
        paidPattern.find(text)?.let { match ->
            val amount = parseAmount(match.groupValues[1]) ?: return null
            val payee = match.groupValues[2].trim()
            return buildTransaction(amount, payee, true, text, pkg)
        }

        if (text.contains("received", ignoreCase = true) ||
            text.contains("credited", ignoreCase = true)
        ) {
            return null
        }

        return extractGenericDebit(text, pkg)
    }

    private fun parsePaytm(text: String, pkg: String): ParsedTransaction? {
        // "Rs. 500 paid to Merchant" or "₹500 sent to Person"
        val paidPattern = Regex(
            """(?:Rs\.?|INR|₹)\s*([\d,]+(?:\.\d{1,2})?)\s+(?:paid|sent)\s+to\s+(.+?)(?:\s*$|\s+UPI|\s+Ref|\.)"""
        )
        paidPattern.find(text)?.let { match ->
            val amount = parseAmount(match.groupValues[1]) ?: return null
            val payee = match.groupValues[2].trim()
            return buildTransaction(amount, payee, true, text, pkg)
        }

        if (text.contains("received", ignoreCase = true) ||
            text.contains("credited", ignoreCase = true)
        ) {
            return null
        }

        return extractGenericDebit(text, pkg)
    }

    private fun parseHDFC(text: String, pkg: String): ParsedTransaction? {
        // "Rs XXX debited from a/c **1234 on DD-MM-YY"
        val debitPattern = Regex(
            """(?:Rs\.?|INR)\s*([\d,]+(?:\.\d{1,2})?)\s+debited"""
        )
        // "INR XXX spent on HDFC Bank Card xx1234 at MERCHANT on DD-MM-YY"
        val cardPattern = Regex(
            """(?:Rs\.?|INR)\s*([\d,]+(?:\.\d{1,2})?)\s+spent.+?at\s+(.+?)\s+on\s+\d"""
        )

        cardPattern.find(text)?.let { match ->
            val amount = parseAmount(match.groupValues[1]) ?: return null
            val merchant = match.groupValues[2].trim()
            return ParsedTransaction(
                amount = amount,
                merchantName = merchant,
                isDebit = true,
                isP2P = false,
                payeeName = null,
                referenceId = extractUpiRef(text),
                rawText = text,
                packageName = pkg,
            )
        }

        debitPattern.find(text)?.let { match ->
            val amount = parseAmount(match.groupValues[1]) ?: return null
            return ParsedTransaction(
                amount = amount,
                merchantName = extractMerchantFromHdfc(text),
                isDebit = true,
                isP2P = false,
                payeeName = null,
                referenceId = extractUpiRef(text),
                rawText = text,
                packageName = pkg,
            )
        }

        // Skip credits
        if (text.contains("credited", ignoreCase = true)) return null

        return null
    }

    private fun parseSwiggy(text: String, pkg: String): ParsedTransaction? {
        val amount = extractAmount(text) ?: return null
        return ParsedTransaction(
            amount = amount,
            merchantName = "Swiggy",
            isDebit = true,
            isP2P = false,
            payeeName = null,
            referenceId = null,
            rawText = text,
            packageName = pkg,
        )
    }

    private fun parseBlinkit(text: String, pkg: String): ParsedTransaction? {
        val amount = extractAmount(text) ?: return null
        return ParsedTransaction(
            amount = amount,
            merchantName = "Blinkit",
            isDebit = true,
            isP2P = false,
            payeeName = null,
            referenceId = null,
            rawText = text,
            packageName = pkg,
        )
    }

    private fun parseCRED(text: String, pkg: String): ParsedTransaction? {
        val amount = extractAmount(text) ?: return null
        return ParsedTransaction(
            amount = amount,
            merchantName = "CRED",
            isDebit = true,
            isP2P = false,
            payeeName = null,
            referenceId = null,
            rawText = text,
            packageName = pkg,
        )
    }

    private fun parseAmazon(text: String, pkg: String): ParsedTransaction? {
        val amount = extractAmount(text) ?: return null
        return ParsedTransaction(
            amount = amount,
            merchantName = "Amazon",
            isDebit = true,
            isP2P = false,
            payeeName = null,
            referenceId = null,
            rawText = text,
            packageName = pkg,
        )
    }

    private fun parseBHIM(text: String, pkg: String): ParsedTransaction? {
        val paidPattern = Regex(
            """(?:[Pp]aid|[Ss]ent)\s+(?:Rs\.?|INR|₹)\s*([\d,]+(?:\.\d{1,2})?)\s+to\s+(.+?)(?:\s*$|\s+UPI|\s+Ref|\.)"""
        )
        paidPattern.find(text)?.let { match ->
            val amount = parseAmount(match.groupValues[1]) ?: return null
            val payee = match.groupValues[2].trim()
            return buildTransaction(amount, payee, true, text, pkg)
        }

        if (text.contains("received", ignoreCase = true) ||
            text.contains("credited", ignoreCase = true)
        ) {
            return null
        }

        return extractGenericDebit(text, pkg)
    }

    private fun buildTransaction(
        amount: Double,
        payee: String,
        isDebit: Boolean,
        rawText: String,
        packageName: String,
    ): ParsedTransaction {
        val isP2P = isLikelyP2P(payee)
        return ParsedTransaction(
            amount = amount,
            merchantName = if (isP2P) null else payee,
            isDebit = isDebit,
            isP2P = isP2P,
            payeeName = if (isP2P) payee else null,
            referenceId = extractUpiRef(rawText),
            rawText = rawText,
            packageName = packageName,
        )
    }

    private fun extractGenericDebit(text: String, pkg: String): ParsedTransaction? {
        val amount = extractAmount(text) ?: return null
        if (text.contains("received", ignoreCase = true) ||
            text.contains("credited", ignoreCase = true)
        ) {
            return null
        }
        return ParsedTransaction(
            amount = amount,
            merchantName = null,
            isDebit = true,
            isP2P = false,
            payeeName = null,
            referenceId = extractUpiRef(text),
            rawText = text,
            packageName = pkg,
        )
    }

    private fun extractAmount(text: String): Double? {
        return AMOUNT_REGEX.find(text)?.let { parseAmount(it.groupValues[1]) }
    }

    private fun parseAmount(amountStr: String): Double? {
        return amountStr.replace(",", "").toDoubleOrNull()
    }

    private fun extractUpiRef(text: String): String? {
        return UPI_REF_REGEX.find(text)?.groupValues?.get(1)
    }

    private fun extractMerchantFromHdfc(text: String): String? {
        // Try to extract VPA or merchant from HDFC format
        // "to VPA xxxxxxx@okaxis" or "Info: MERCHANT NAME"
        val vpaPattern = Regex("""(?:to\s+VPA|VPA)\s+(\S+)""")
        val infoPattern = Regex("""Info:\s*(.+?)(?:\s*$|\s+Ref)""")

        infoPattern.find(text)?.let { return it.groupValues[1].trim() }
        vpaPattern.find(text)?.let { return it.groupValues[1].trim() }
        return null
    }

    private fun isLikelyP2P(@Suppress("UNUSED_PARAMETER") payeeName: String): Boolean {
        // Default to merchant — most notification payments are to merchants.
        // Misclassifying a merchant as P2P loses auto-categorization.
        // Users can manually reclassify P2P transfers if needed.
        return false
    }
}
