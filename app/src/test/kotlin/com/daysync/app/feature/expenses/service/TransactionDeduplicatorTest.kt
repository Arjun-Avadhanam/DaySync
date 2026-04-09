package com.daysync.app.feature.expenses.service

import com.daysync.app.core.database.dao.ExpenseDao
import com.daysync.app.core.database.entity.ExpenseEntity
import com.daysync.app.core.sync.SyncStatus
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.time.Clock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

class TransactionDeduplicatorTest {

    private lateinit var expenseDao: ExpenseDao
    private lateinit var deduplicator: TransactionDeduplicator

    private val testDate = LocalDate(2026, 3, 15)
    private val testEntity = ExpenseEntity(
        id = "existing-1",
        title = null,
        item = null,
        date = testDate,
        category = null,
        frequency = null,
        unitCost = 500.0,
        quantity = 1.0,
        deliveryCharge = 0.0,
        totalAmount = 500.0,
        notes = null,
        source = "NOTIFICATION",
        merchantName = null,
        syncStatus = SyncStatus.PENDING,
        lastModified = Clock.System.now(),
    )

    @Before
    fun setup() {
        expenseDao = mockk()
        deduplicator = TransactionDeduplicator(expenseDao)
    }

    // ── findDuplicate ────────────────────────────────────

    @Test
    fun `findDuplicate returns match when reference ID exists in notes`() = runTest {
        coEvery {
            expenseDao.findByNotes("%Ref: 121280823461%")
        } returns testEntity

        val result = deduplicator.findDuplicate("121280823461")
        assertNotNull(result)
        assertEquals("existing-1", result!!.id)
    }

    @Test
    fun `findDuplicate returns null when reference ID not found`() = runTest {
        coEvery {
            expenseDao.findByNotes(any())
        } returns null

        val result = deduplicator.findDuplicate("175809912454")
        assertNull(result)
    }

    @Test
    fun `findDuplicate returns null when reference ID is null`() = runTest {
        val result = deduplicator.findDuplicate(null)
        assertNull(result)
    }

    @Test
    fun `findDuplicate returns null when reference ID is blank`() = runTest {
        val result = deduplicator.findDuplicate("")
        assertNull(result)
    }

    // ── findDuplicateForCsv ──────────────────────────────

    @Test
    fun `findDuplicateForCsv uses full day window`() = runTest {
        coEvery {
            expenseDao.findDuplicate(500.0, testDate, 0L, Long.MAX_VALUE)
        } returns testEntity

        val result = deduplicator.findDuplicateForCsv(500.0, testDate)
        assertNotNull(result)
    }

    // ── shouldUpdateExisting ─────────────────────────────

    @Test
    fun `shouldUpdateExisting returns true when existing has no merchant`() {
        val existing = testEntity.copy(merchantName = null)
        assertTrue(deduplicator.shouldUpdateExisting(existing, "Swiggy"))
    }

    @Test
    fun `shouldUpdateExisting returns true when existing has blank merchant`() {
        val existing = testEntity.copy(merchantName = "")
        assertTrue(deduplicator.shouldUpdateExisting(existing, "Swiggy"))
    }

    @Test
    fun `shouldUpdateExisting returns true when new name is longer`() {
        val existing = testEntity.copy(merchantName = "Swiggy")
        assertTrue(deduplicator.shouldUpdateExisting(existing, "Swiggy Delivery Service"))
    }

    @Test
    fun `shouldUpdateExisting returns false when new name is shorter`() {
        val existing = testEntity.copy(merchantName = "Swiggy Delivery Service")
        assertFalse(deduplicator.shouldUpdateExisting(existing, "Swiggy"))
    }

    @Test
    fun `shouldUpdateExisting returns false when new name is null`() {
        assertFalse(deduplicator.shouldUpdateExisting(testEntity, null))
    }

    @Test
    fun `shouldUpdateExisting returns false when new name is blank`() {
        assertFalse(deduplicator.shouldUpdateExisting(testEntity, ""))
    }
}
