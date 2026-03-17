package com.daysync.app.feature.ai.data

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Method

/**
 * Tests for DataContextBuilder's pure logic methods via reflection.
 */
class DataContextBuilderTest {

    private lateinit var instance: Any
    private lateinit var inferDateRangeMethod: Method
    private lateinit var generateDateSequenceMethod: Method

    @Before
    fun setUp() {
        // Use Unsafe or direct instantiation bypassing constructor
        val clazz = DataContextBuilder::class.java
        val unsafe = sun.misc.Unsafe::class.java.getDeclaredField("theUnsafe").apply { isAccessible = true }.get(null) as sun.misc.Unsafe
        instance = unsafe.allocateInstance(clazz)

        // Set the tz field via reflection
        val tzField = clazz.getDeclaredField("tz")
        tzField.isAccessible = true
        tzField.set(instance, TimeZone.of("Asia/Kolkata"))

        inferDateRangeMethod = clazz.getDeclaredMethod("inferDateRange", String::class.java)
        inferDateRangeMethod.isAccessible = true

        generateDateSequenceMethod = clazz.getDeclaredMethod(
            "generateDateSequence", LocalDate::class.java, LocalDate::class.java,
        )
        generateDateSequenceMethod.isAccessible = true
    }

    @Suppress("UNCHECKED_CAST")
    private fun inferDateRange(question: String): Pair<LocalDate, LocalDate> {
        return inferDateRangeMethod.invoke(instance, question) as Pair<LocalDate, LocalDate>
    }

    @Suppress("UNCHECKED_CAST")
    private fun generateDateSequence(start: LocalDate, end: LocalDate): List<LocalDate> {
        return generateDateSequenceMethod.invoke(instance, start, end) as List<LocalDate>
    }

    private val tz = TimeZone.of("Asia/Kolkata")
    private val today: LocalDate
        get() = kotlin.time.Clock.System.now().toLocalDateTime(tz).date

    // ── inferDateRange: "today" ─────────────────────────

    @Test
    fun `today returns single day range`() {
        val (start, end) = inferDateRange("How did I do today?")
        assertEquals(today, start)
        assertEquals(today, end)
    }

    @Test
    fun `TODAY case insensitive`() {
        val (start, end) = inferDateRange("Show me TODAY's data")
        assertEquals(today, start)
        assertEquals(today, end)
    }

    // ── inferDateRange: "yesterday" ─────────────────────

    @Test
    fun `yesterday returns previous day`() {
        val yesterday = today.minus(DatePeriod(days = 1))
        val (start, end) = inferDateRange("What happened yesterday?")
        assertEquals(yesterday, start)
        assertEquals(yesterday, end)
    }

    // ── inferDateRange: "this week" ─────────────────────

    @Test
    fun `this week returns last 7 days`() {
        val (start, end) = inferDateRange("Show me this week's summary")
        assertEquals(today.minus(DatePeriod(days = 6)), start)
        assertEquals(today, end)
    }

    // ── inferDateRange: "last week" ─────────────────────

    @Test
    fun `last week returns previous 7 day window`() {
        val (start, end) = inferDateRange("How was last week?")
        assertEquals(today.minus(DatePeriod(days = 13)), start)
        assertEquals(today.minus(DatePeriod(days = 7)), end)
    }

    // ── inferDateRange: "this month" ────────────────────

    @Test
    fun `this month starts from first of month`() {
        val (start, end) = inferDateRange("This month expense summary")
        assertEquals(LocalDate(today.year, today.month, 1), start)
        assertEquals(today, end)
    }

    // ── inferDateRange: "last month" ────────────────────

    @Test
    fun `last month returns full previous month`() {
        val (start, end) = inferDateRange("Show last month data")
        val firstOfThisMonth = LocalDate(today.year, today.month, 1)
        val lastMonthEnd = firstOfThisMonth.minus(DatePeriod(days = 1))
        assertEquals(LocalDate(lastMonthEnd.year, lastMonthEnd.month, 1), start)
        assertEquals(lastMonthEnd, end)
    }

    // ── inferDateRange: "last N days" ───────────────────

    @Test
    fun `last 3 days`() {
        val (start, end) = inferDateRange("Show me last 3 days")
        assertEquals(today.minus(DatePeriod(days = 2)), start)
        assertEquals(today, end)
    }

    @Test
    fun `last 7 days`() {
        val (start, end) = inferDateRange("What about the last 7 days?")
        assertEquals(today.minus(DatePeriod(days = 6)), start)
        assertEquals(today, end)
    }

    @Test
    fun `past 30 days`() {
        val (start, end) = inferDateRange("past 30 days spending")
        assertEquals(today.minus(DatePeriod(days = 29)), start)
        assertEquals(today, end)
    }

    // ── inferDateRange: "last N weeks" ──────────────────

    @Test
    fun `last 2 weeks`() {
        val (start, end) = inferDateRange("last 2 weeks health data")
        assertEquals(today.minus(DatePeriod(days = 13)), start)
        assertEquals(today, end)
    }

    @Test
    fun `past 4 weeks`() {
        val (start, end) = inferDateRange("past 4 weeks")
        assertEquals(today.minus(DatePeriod(days = 27)), start)
        assertEquals(today, end)
    }

    // ── inferDateRange: "last N months" ─────────────────

    @Test
    fun `last 3 months`() {
        val (start, end) = inferDateRange("trends for last 3 months")
        assertEquals(today.minus(DatePeriod(months = 3)), start)
        assertEquals(today, end)
    }

    @Test
    fun `past 1 month`() {
        val (start, end) = inferDateRange("past 1 month")
        assertEquals(today.minus(DatePeriod(months = 1)), start)
        assertEquals(today, end)
    }

    // ── inferDateRange: default ─────────────────────────

    @Test
    fun `unrecognized question defaults to last 7 days`() {
        val (start, end) = inferDateRange("How am I doing?")
        assertEquals(today.minus(DatePeriod(days = 6)), start)
        assertEquals(today, end)
    }

    @Test
    fun `empty question defaults to last 7 days`() {
        val (start, end) = inferDateRange("")
        assertEquals(today.minus(DatePeriod(days = 6)), start)
        assertEquals(today, end)
    }

    // ── inferDateRange: priority ────────────────────────

    @Test
    fun `last N days pattern takes priority over this week`() {
        val (start, end) = inferDateRange("last 5 days this week")
        assertEquals(today.minus(DatePeriod(days = 4)), start)
        assertEquals(today, end)
    }

    // ── generateDateSequence ────────────────────────────

    @Test
    fun `generates single date for same start and end`() {
        val date = LocalDate(2026, 3, 15)
        val result = generateDateSequence(date, date)
        assertEquals(1, result.size)
        assertEquals(date, result[0])
    }

    @Test
    fun `generates 7 dates for a week range`() {
        val start = LocalDate(2026, 3, 10)
        val end = LocalDate(2026, 3, 16)
        val result = generateDateSequence(start, end)
        assertEquals(7, result.size)
        assertEquals(start, result.first())
        assertEquals(end, result.last())
    }

    @Test
    fun `generates dates across month boundary`() {
        val start = LocalDate(2026, 2, 27)
        val end = LocalDate(2026, 3, 2)
        val result = generateDateSequence(start, end)
        assertEquals(4, result.size)
    }

    @Test
    fun `empty result when start is after end`() {
        val start = LocalDate(2026, 3, 15)
        val end = LocalDate(2026, 3, 10)
        val result = generateDateSequence(start, end)
        assertTrue(result.isEmpty())
    }
}
