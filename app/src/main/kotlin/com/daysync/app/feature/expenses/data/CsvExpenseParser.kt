package com.daysync.app.feature.expenses.data

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import kotlinx.datetime.LocalDate

data class CsvExpenseEntry(
    val date: LocalDate,
    val narration: String,
    val debitAmount: Double?,
    val creditAmount: Double?,
    val merchantName: String?,
)

object CsvExpenseParser {

    fun parseHdfcCsv(inputStream: InputStream): List<CsvExpenseEntry> {
        val entries = mutableListOf<CsvExpenseEntry>()
        val reader = BufferedReader(InputStreamReader(inputStream))

        reader.useLines { lines ->
            var headerFound = false
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.isEmpty()) continue

                // Detect header row
                if (!headerFound) {
                    if (trimmed.contains("Date", ignoreCase = true) &&
                        trimmed.contains("Narration", ignoreCase = true)
                    ) {
                        headerFound = true
                    }
                    continue
                }

                // Skip non-data rows after header
                val columns = parseCsvLine(trimmed)
                if (columns.size < 5) continue

                try {
                    val date = parseHdfcDate(columns[0].trim()) ?: continue
                    val narration = columns[1].trim()
                    val debit = columns[3].trim().toDoubleOrNull()
                    val credit = columns[4].trim().toDoubleOrNull()

                    if (debit == null && credit == null) continue

                    entries.add(
                        CsvExpenseEntry(
                            date = date,
                            narration = narration,
                            debitAmount = debit,
                            creditAmount = credit,
                            merchantName = extractMerchantFromNarration(narration),
                        )
                    )
                } catch (_: Exception) {
                    // Skip malformed rows
                }
            }
        }

        return entries
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false

        for (char in line) {
            when {
                char == '"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current.clear()
                }
                else -> current.append(char)
            }
        }
        result.add(current.toString())
        return result
    }

    private fun parseHdfcDate(dateStr: String): LocalDate? {
        // HDFC uses dd/MM/yy or dd/MM/yyyy
        val parts = dateStr.split("/")
        if (parts.size != 3) return null

        return try {
            val day = parts[0].trim().toInt()
            val month = parts[1].trim().toInt()
            var year = parts[2].trim().toInt()
            if (year < 100) year += 2000
            LocalDate(year, month, day)
        } catch (_: Exception) {
            null
        }
    }

    private fun extractMerchantFromNarration(narration: String): String? {
        // UPI narration: "UPI-AppName-VPA-RefNo-Description"
        if (narration.startsWith("UPI", ignoreCase = true)) {
            val parts = narration.split("-")
            if (parts.size >= 3) {
                return parts[2].trim() // VPA or merchant name
            }
        }
        // NEFT/IMPS: "NEFT/IMPS-RefNo-Payee"
        if (narration.startsWith("NEFT", ignoreCase = true) ||
            narration.startsWith("IMPS", ignoreCase = true)
        ) {
            val parts = narration.split("-")
            if (parts.size >= 3) {
                return parts.last().trim()
            }
        }
        // Card: "POS XXXXXXX MERCHANT CITY"
        if (narration.startsWith("POS", ignoreCase = true)) {
            return narration.removePrefix("POS").trim().split("  ").firstOrNull()?.trim()
        }
        return null
    }
}
