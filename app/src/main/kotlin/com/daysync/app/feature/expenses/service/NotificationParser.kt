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
    val currency: String = "INR",
    val timestamp: Instant = Clock.System.now(),
)

object NotificationParser {

    val MONITORED_PACKAGES = setOf(
        "com.google.android.apps.messaging",           // Google Messages (bank SMS)
        "com.google.android.apps.nbu.paisa.user",      // GPay (fallback)
        "com.phonepe.app",                              // PhonePe (fallback)
        "net.one97.paytm",                              // Paytm (fallback)
        "com.snapwork.hdfc",                            // HDFC Bank app
        "in.org.npci.upiapp",                           // BHIM (fallback)
    )

    // ── Bank SMS patterns (content-based, not sender ID based) ──────

    // HDFC UPI: "Sent Rs.540.00\nFrom HDFC Bank A/C *3082\nTo MERCHANT\nOn DD/MM/YY\nRef 123456"
    // HDFC UPI Mandate: "Sent Rs.299.00\nfrom HDFC Bank A/c 3082\nTo Google Play\n25/02/26\nRef 123456"
    private val HDFC_UPI_SENT = Regex(
        """Sent\s+Rs\.([\d,]+(?:\.\d{1,2})?)\s*\n.*HDFC Bank.*\n\s*To\s+(.+?)\s*\n.*?(?:Ref\s+(\d+))?""",
        RegexOption.DOT_MATCHES_ALL,
    )

    // HDFC Card spend: "Rs.540 spent on HDFC Bank Card xx1279 at MERCHANT on DD-MM-YY"
    private val HDFC_CARD_SPENT = Regex(
        """Rs\.([\d,]+(?:\.\d{1,2})?)\s+spent\s+on\s+HDFC Bank Card.*?at\s+(.+?)\s+on\s+\d""",
    )

    // HDFC Card foreign currency debit: "USD.118.00 has been debited from Card 1279 for Anthropic"
    // Note: "will be debited" (pre-debit alerts) are SKIPPED to avoid duplicates
    private val HDFC_CARD_FOREIGN = Regex(
        """(\w{3})\.([\d,]+(?:\.\d{1,2})?)\s+(?:has been )?debited.*?(?:Card|card)\s+\d+.*?(?:for|at)\s+(.+?)(?:\n|ID:|$)""",
        RegexOption.DOT_MATCHES_ALL,
    )

    // HDFC generic debit: "Rs.X debited from A/c"
    private val HDFC_GENERIC_DEBIT = Regex(
        """Rs\.([\d,]+(?:\.\d{1,2})?)\s+(?:has been )?debited\s+from""",
    )

    // Reference number extraction
    private val REF_REGEX = Regex("""Ref\s+(\d{6,})""")

    // Credit/skip patterns
    private val SKIP_KEYWORDS = listOf("received", "credited", "OTP", "otp", "One Time Password")

    // ── Main parse entry point ──────────────────────────────────────

    fun parse(packageName: String, title: String?, text: String?): ParsedTransaction? {
        val fullText = text ?: title ?: return null

        // Primary: Bank SMS via Google Messages
        if (packageName !in MONITORED_PACKAGES) return null

        if (packageName == "com.google.android.apps.messaging") {
            return parseBankSms(fullText, title, packageName)
        }

        if (packageName == "com.snapwork.hdfc") {
            return parseBankSms(fullText, title, packageName)
        }

        return parsePaymentAppNotification(fullText, packageName)
    }

    // ── Bank SMS parser (primary source) ────────────────────────────

    private fun parseBankSms(text: String, title: String?, pkg: String): ParsedTransaction? {
        if (text.contains("will be debited", ignoreCase = true)) return null

        if (SKIP_KEYWORDS.any { text.contains(it, ignoreCase = true) }) {
            if (!text.contains("Sent Rs.", ignoreCase = true) &&
                !text.contains("debited", ignoreCase = true) &&
                !text.contains("spent", ignoreCase = true)
            ) {
                return null
            }
        }

        val isHdfc = text.contains("HDFC Bank", ignoreCase = true) ||
            text.contains("HDFCBK", ignoreCase = true) ||
            title?.contains("HDFCBK", ignoreCase = true) == true

        if (!isHdfc) return null

        // Try HDFC UPI pattern (most common)
        HDFC_UPI_SENT.find(text)?.let { match ->
            val amount = parseAmount(match.groupValues[1]) ?: return null
            val merchant = match.groupValues[2].trim()
            val ref = match.groupValues.getOrNull(3)?.takeIf { it.isNotBlank() }
                ?: REF_REGEX.find(text)?.groupValues?.get(1)
            return ParsedTransaction(
                amount = amount,
                merchantName = cleanMerchantName(merchant),
                isDebit = true,
                isP2P = false,
                payeeName = null,
                referenceId = ref,
                rawText = text,
                packageName = pkg,
            )
        }

        // Try HDFC Card spend pattern
        HDFC_CARD_SPENT.find(text)?.let { match ->
            val amount = parseAmount(match.groupValues[1]) ?: return null
            val merchant = match.groupValues[2].trim()
            return ParsedTransaction(
                amount = amount,
                merchantName = cleanMerchantName(merchant),
                isDebit = true,
                isP2P = false,
                payeeName = null,
                referenceId = REF_REGEX.find(text)?.groupValues?.get(1),
                rawText = text,
                packageName = pkg,
            )
        }

        HDFC_CARD_FOREIGN.find(text)?.let { match ->
            val currency = match.groupValues[1] // USD, EUR, etc.
            val amount = parseAmount(match.groupValues[2]) ?: return null
            val merchant = match.groupValues[3].trim()
            return ParsedTransaction(
                amount = amount,
                merchantName = cleanMerchantName(merchant),
                isDebit = true,
                isP2P = false,
                payeeName = null,
                referenceId = null,
                rawText = text,
                packageName = pkg,
                currency = currency,
            )
        }

        // Try HDFC generic debit
        HDFC_GENERIC_DEBIT.find(text)?.let { match ->
            val amount = parseAmount(match.groupValues[1]) ?: return null
            // Try to extract merchant from "to VPA" or "To" pattern
            val merchantPattern = Regex("""[Tt]o\s+(?:VPA\s+)?(.+?)(?:\s+on\s+|\s*\n|$)""")
            val merchant = merchantPattern.find(text)?.groupValues?.get(1)?.trim()
            return ParsedTransaction(
                amount = amount,
                merchantName = merchant?.let { cleanMerchantName(it) },
                isDebit = true,
                isP2P = false,
                payeeName = null,
                referenceId = REF_REGEX.find(text)?.groupValues?.get(1),
                rawText = text,
                packageName = pkg,
            )
        }

        return null
    }

    // ── Payment app notification parser (fallback) ──────────────────

    private fun parsePaymentAppNotification(text: String, pkg: String): ParsedTransaction? {
        // "Paid ₹X to Merchant" / "Sent ₹X to Person"
        val pattern1 = Regex(
            """(?:[Pp]aid|[Ss]ent)\s+(?:Rs\.?|INR|₹)\s*([\d,]+(?:\.\d{1,2})?)\s+to\s+(.+?)(?:\s*$|\s+UPI|\s+Ref|\.)"""
        )
        // "₹X paid to Merchant" / "₹X sent to Person" (Paytm style)
        val pattern2 = Regex(
            """(?:Rs\.?|INR|₹)\s*([\d,]+(?:\.\d{1,2})?)\s+(?:paid|sent)\s+to\s+(.+?)(?:\s*$|\s+UPI|\s+Ref|\.)"""
        )
        val match = pattern1.find(text) ?: pattern2.find(text)
        match?.let {
            val amount = parseAmount(it.groupValues[1]) ?: return null
            val payee = it.groupValues[2].trim()
            return ParsedTransaction(
                amount = amount,
                merchantName = cleanMerchantName(payee),
                isDebit = true,
                isP2P = false,
                payeeName = null,
                referenceId = extractUpiRef(text),
                rawText = text,
                packageName = pkg,
            )
        }

        // Skip credits
        if (text.contains("received", ignoreCase = true) ||
            text.contains("credited", ignoreCase = true)
        ) {
            return null
        }

        // Generic amount extraction (last resort)
        val amount = extractAmount(text) ?: return null
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

    // ── Helpers ──────────────────────────────────────────────────────

    private val AMOUNT_REGEX = Regex("""(?:Rs\.?|INR|₹)\s*([\d,]+(?:\.\d{1,2})?)""")
    private val UPI_REF_REGEX = Regex("""(?:UPI\s*(?:Ref|ref|ID|id)[:\s]*|Ref\s*No[:\s]*)(\d+)""")

    private fun extractAmount(text: String): Double? {
        return AMOUNT_REGEX.find(text)?.let { parseAmount(it.groupValues[1]) }
    }

    private fun parseAmount(amountStr: String): Double? {
        return amountStr.replace(",", "").toDoubleOrNull()
    }

    private fun extractUpiRef(text: String): String? {
        return UPI_REF_REGEX.find(text)?.groupValues?.get(1)
    }

    private fun cleanMerchantName(name: String): String {
        // Remove trailing junk like "Not You?", "Call 180...", newlines
        return name
            .split("\n").first()
            .replace(Regex("""\s*Not You\?.*"""), "")
            .replace(Regex("""\s*Call\s+\d+.*"""), "")
            .replace(Regex("""\s*SMS BLOCK.*"""), "")
            .trim()
    }
}
