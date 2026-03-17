package com.daysync.app.feature.expenses.data

import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class CsvExpenseParserTest {

    private fun csvStream(content: String) = ByteArrayInputStream(content.toByteArray())

    // ── Basic parsing ────────────────────────────────────

    @Test
    fun `parses valid HDFC CSV with header`() {
        val csv = """
            Date,Narration,Chq/Ref No,Debit,Credit,Balance
            15/03/26,UPI-GPay-merchant@ybl-123456-Payment,123456,500.00,,45000.00
        """.trimIndent()

        val result = CsvExpenseParser.parseHdfcCsv(csvStream(csv))
        assertEquals(1, result.size)
        assertEquals(LocalDate(2026, 3, 15), result[0].date)
        assertEquals("UPI-GPay-merchant@ybl-123456-Payment", result[0].narration)
        assertEquals(500.0, result[0].debitAmount!!, 0.01)
        assertNull(result[0].creditAmount)
    }

    @Test
    fun `parses multiple rows`() {
        val csv = """
            Date,Narration,Chq/Ref No,Debit,Credit,Balance
            15/03/26,UPI-Swiggy-Payment,111,350.00,,45000.00
            16/03/26,UPI-Salary-Credit,222,,50000.00,95000.00
            17/03/26,POS 123456 CROMA MUMBAI,333,2500.00,,92500.00
        """.trimIndent()

        val result = CsvExpenseParser.parseHdfcCsv(csvStream(csv))
        assertEquals(3, result.size)
        assertEquals(350.0, result[0].debitAmount!!, 0.01)
        assertEquals(50000.0, result[1].creditAmount!!, 0.01)
        assertEquals(2500.0, result[2].debitAmount!!, 0.01)
    }

    @Test
    fun `skips rows before header`() {
        val csv = """
            HDFC Bank Statement
            Account: XXXXX1234

            Date,Narration,Chq/Ref No,Debit,Credit,Balance
            15/03/26,UPI-Payment,111,500.00,,45000.00
        """.trimIndent()

        val result = CsvExpenseParser.parseHdfcCsv(csvStream(csv))
        assertEquals(1, result.size)
    }

    @Test
    fun `empty CSV returns empty list`() {
        val result = CsvExpenseParser.parseHdfcCsv(csvStream(""))
        assertTrue(result.isEmpty())
    }

    @Test
    fun `CSV with only header returns empty list`() {
        val csv = "Date,Narration,Chq/Ref No,Debit,Credit,Balance"
        val result = CsvExpenseParser.parseHdfcCsv(csvStream(csv))
        assertTrue(result.isEmpty())
    }

    @Test
    fun `skips malformed rows`() {
        val csv = """
            Date,Narration,Chq/Ref No,Debit,Credit,Balance
            15/03/26,Valid Row,111,500.00,,45000.00
            bad,data
            17/03/26,Another Valid,333,200.00,,44800.00
        """.trimIndent()

        val result = CsvExpenseParser.parseHdfcCsv(csvStream(csv))
        assertEquals(2, result.size)
    }

    @Test
    fun `skips rows with no debit or credit`() {
        val csv = """
            Date,Narration,Chq/Ref No,Debit,Credit,Balance
            15/03/26,Some narration,111,,,45000.00
        """.trimIndent()

        val result = CsvExpenseParser.parseHdfcCsv(csvStream(csv))
        assertTrue(result.isEmpty())
    }

    // ── Date parsing ─────────────────────────────────────

    @Test
    fun `parses dd-MM-yy date format`() {
        val csv = """
            Date,Narration,Chq/Ref No,Debit,Credit,Balance
            05/01/26,Test,111,100.00,,45000.00
        """.trimIndent()

        val result = CsvExpenseParser.parseHdfcCsv(csvStream(csv))
        assertEquals(LocalDate(2026, 1, 5), result[0].date)
    }

    @Test
    fun `parses dd-MM-yyyy date format`() {
        val csv = """
            Date,Narration,Chq/Ref No,Debit,Credit,Balance
            05/01/2026,Test,111,100.00,,45000.00
        """.trimIndent()

        val result = CsvExpenseParser.parseHdfcCsv(csvStream(csv))
        assertEquals(LocalDate(2026, 1, 5), result[0].date)
    }

    @Test
    fun `invalid date skips row`() {
        val csv = """
            Date,Narration,Chq/Ref No,Debit,Credit,Balance
            99/99/99,Test,111,100.00,,45000.00
        """.trimIndent()

        val result = CsvExpenseParser.parseHdfcCsv(csvStream(csv))
        assertTrue(result.isEmpty())
    }

    // ── Merchant extraction ──────────────────────────────

    @Test
    fun `extracts merchant from UPI narration`() {
        val csv = """
            Date,Narration,Chq/Ref No,Debit,Credit,Balance
            15/03/26,UPI-GPay-merchant@ybl-123456-Payment,111,500.00,,45000.00
        """.trimIndent()

        val result = CsvExpenseParser.parseHdfcCsv(csvStream(csv))
        assertEquals("merchant@ybl", result[0].merchantName)
    }

    @Test
    fun `extracts merchant from NEFT narration`() {
        val csv = """
            Date,Narration,Chq/Ref No,Debit,Credit,Balance
            15/03/26,NEFT-REF123-EMPLOYER NAME,111,,50000.00,95000.00
        """.trimIndent()

        val result = CsvExpenseParser.parseHdfcCsv(csvStream(csv))
        assertEquals(1, result.size)
        assertEquals("EMPLOYER NAME", result[0].merchantName)
    }

    @Test
    fun `extracts merchant from POS narration`() {
        val csv = """
            Date,Narration,Chq/Ref No,Debit,Credit,Balance
            15/03/26,POS 123456 CROMA ELECTRONICS  MUMBAI,333,2500.00,,45000.00
        """.trimIndent()

        val result = CsvExpenseParser.parseHdfcCsv(csvStream(csv))
        assertNotNull(result[0].merchantName)
    }

    // ── Quoted fields ────────────────────────────────────

    @Test
    fun `handles quoted fields with commas`() {
        val csv = """
            Date,Narration,Chq/Ref No,Debit,Credit,Balance
            15/03/26,"UPI-Payment, for groceries",111,500.00,,45000.00
        """.trimIndent()

        val result = CsvExpenseParser.parseHdfcCsv(csvStream(csv))
        assertEquals(1, result.size)
        assertEquals("UPI-Payment, for groceries", result[0].narration)
    }
}
