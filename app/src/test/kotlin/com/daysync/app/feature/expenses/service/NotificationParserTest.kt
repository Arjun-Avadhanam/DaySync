package com.daysync.app.feature.expenses.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class NotificationParserTest {

    // ── GPay ──────────────────────────────────────────────

    @Test
    fun `GPay - paid to merchant without business keywords`() {
        val result = NotificationParser.parse(
            "com.google.android.apps.nbu.paisa.user",
            null,
            "Paid ₹500 to Chai Point"
        )
        assertNotNull(result)
        assertEquals(500.0, result!!.amount, 0.01)
        assertEquals("Chai Point", result.merchantName)
        assertFalse(result.isP2P)
        assertTrue(result.isDebit)
    }

    @Test
    fun `GPay - paid with Rs dot`() {
        val result = NotificationParser.parse(
            "com.google.android.apps.nbu.paisa.user",
            null,
            "Paid Rs. 1,250.50 to Reliance Retail Store"
        )
        assertNotNull(result)
        assertEquals(1250.50, result!!.amount, 0.01)
        assertEquals("Reliance Retail Store", result.merchantName)
        assertFalse(result.isP2P)
    }

    @Test
    fun `GPay - paid with INR prefix`() {
        val result = NotificationParser.parse(
            "com.google.android.apps.nbu.paisa.user",
            null,
            "Paid INR 300 to DMart Store"
        )
        assertNotNull(result)
        assertEquals(300.0, result!!.amount, 0.01)
    }

    @Test
    fun `GPay - received should return null`() {
        val result = NotificationParser.parse(
            "com.google.android.apps.nbu.paisa.user",
            null,
            "Received ₹500 from Rahul"
        )
        assertNull(result)
    }

    @Test
    fun `GPay - credited should return null`() {
        val result = NotificationParser.parse(
            "com.google.android.apps.nbu.paisa.user",
            null,
            "₹1000 credited to your account"
        )
        assertNull(result)
    }

    @Test
    fun `GPay - payment to person defaults to merchant`() {
        val result = NotificationParser.parse(
            "com.google.android.apps.nbu.paisa.user",
            null,
            "Paid ₹200 to Rahul Kumar"
        )
        assertNotNull(result)
        assertEquals(200.0, result!!.amount, 0.01)
        // All payments default to merchant — user can manually reclassify P2P
        assertFalse(result.isP2P)
        assertEquals("Rahul Kumar", result.merchantName)
        assertNull(result.payeeName)
    }

    @Test
    fun `GPay - paid with UPI ref`() {
        val result = NotificationParser.parse(
            "com.google.android.apps.nbu.paisa.user",
            null,
            "Paid ₹150 to Big Bazaar Store UPI Ref 123456789"
        )
        assertNotNull(result)
        assertEquals(150.0, result!!.amount, 0.01)
        assertEquals("Big Bazaar Store", result.merchantName)
        assertEquals("123456789", result.referenceId)
    }

    @Test
    fun `GPay - fallback generic debit`() {
        val result = NotificationParser.parse(
            "com.google.android.apps.nbu.paisa.user",
            null,
            "₹750 debited for order #12345"
        )
        assertNotNull(result)
        assertEquals(750.0, result!!.amount, 0.01)
        assertTrue(result.isDebit)
    }

    @Test
    fun `GPay - uses text when title is null`() {
        val result = NotificationParser.parse(
            "com.google.android.apps.nbu.paisa.user",
            null,
            "Paid ₹100 to Coffee Shop"
        )
        assertNotNull(result)
        assertEquals(100.0, result!!.amount, 0.01)
    }

    @Test
    fun `GPay - falls back to title when text is null`() {
        val result = NotificationParser.parse(
            "com.google.android.apps.nbu.paisa.user",
            "Paid ₹100 to Coffee Shop",
            null
        )
        assertNotNull(result)
        assertEquals(100.0, result!!.amount, 0.01)
    }

    // ── PhonePe ──────────────────────────────────────────

    @Test
    fun `PhonePe - paid to merchant with keyword`() {
        val result = NotificationParser.parse(
            "com.phonepe.app",
            null,
            "Paid ₹320 to Dominos Restaurant"
        )
        assertNotNull(result)
        assertEquals(320.0, result!!.amount, 0.01)
        assertFalse(result.isP2P)
        assertEquals("Dominos Restaurant", result.merchantName)
    }

    @Test
    fun `PhonePe - sent to person defaults to merchant`() {
        val result = NotificationParser.parse(
            "com.phonepe.app",
            null,
            "Sent ₹500 to Priya"
        )
        assertNotNull(result)
        assertEquals(500.0, result!!.amount, 0.01)
        assertFalse(result.isP2P)
        assertEquals("Priya", result.merchantName)
    }

    @Test
    fun `PhonePe - received should return null`() {
        val result = NotificationParser.parse(
            "com.phonepe.app",
            null,
            "Received ₹1000 from Dad"
        )
        assertNull(result)
    }

    // ── Paytm ────────────────────────────────────────────

    @Test
    fun `Paytm - Rs amount paid to merchant`() {
        val result = NotificationParser.parse(
            "net.one97.paytm",
            null,
            "Rs. 450 paid to Metro Card Recharge"
        )
        assertNotNull(result)
        assertEquals(450.0, result!!.amount, 0.01)
    }

    @Test
    fun `Paytm - rupee symbol sent to person defaults to merchant`() {
        val result = NotificationParser.parse(
            "net.one97.paytm",
            null,
            "₹200 sent to Amit"
        )
        assertNotNull(result)
        assertEquals(200.0, result!!.amount, 0.01)
        assertFalse(result.isP2P)
        assertEquals("Amit", result.merchantName)
    }

    // ── HDFC Bank app (now routes through bank SMS parser) ──

    @Test
    fun `HDFC app - credited should return null`() {
        val result = NotificationParser.parse(
            "com.snapwork.hdfc",
            null,
            "Rs 50,000.00 credited to HDFC Bank a/c **1234"
        )
        assertNull(result)
    }

    // ── BHIM ─────────────────────────────────────────────

    @Test
    fun `BHIM - paid to merchant`() {
        val result = NotificationParser.parse(
            "in.org.npci.upiapp",
            null,
            "Paid ₹150 to Medical Store"
        )
        assertNotNull(result)
        assertEquals(150.0, result!!.amount, 0.01)
        assertFalse(result.isP2P)
    }

    @Test
    fun `BHIM - received should return null`() {
        val result = NotificationParser.parse(
            "in.org.npci.upiapp",
            null,
            "Received ₹2000 from employer"
        )
        assertNull(result)
    }

    // ── Edge cases ───────────────────────────────────────

    @Test
    fun `unknown package returns null`() {
        val result = NotificationParser.parse(
            "com.unknown.app",
            null,
            "₹500 paid"
        )
        assertNull(result)
    }

    @Test
    fun `null text and null title returns null`() {
        val result = NotificationParser.parse(
            "com.google.android.apps.nbu.paisa.user",
            null,
            null
        )
        assertNull(result)
    }

    @Test
    fun `amount with commas parsed correctly`() {
        val result = NotificationParser.parse(
            "com.google.android.apps.nbu.paisa.user",
            null,
            "Paid ₹1,25,000.50 to Car Dealer Services"
        )
        assertNotNull(result)
        assertEquals(125000.50, result!!.amount, 0.01)
    }

    @Test
    fun `MONITORED_PACKAGES contains all expected packages`() {
        assertEquals(6, NotificationParser.MONITORED_PACKAGES.size)
        assertTrue(NotificationParser.MONITORED_PACKAGES.contains("com.google.android.apps.messaging"))
        assertTrue(NotificationParser.MONITORED_PACKAGES.contains("com.google.android.apps.nbu.paisa.user"))
        assertTrue(NotificationParser.MONITORED_PACKAGES.contains("com.phonepe.app"))
        assertTrue(NotificationParser.MONITORED_PACKAGES.contains("net.one97.paytm"))
        assertTrue(NotificationParser.MONITORED_PACKAGES.contains("com.snapwork.hdfc"))
    }

    @Test
    fun `all payments default to merchant`() {
        val result = NotificationParser.parse(
            "com.google.android.apps.nbu.paisa.user",
            null,
            "Paid ₹1000 to ABC Technologies Pvt Ltd"
        )
        assertNotNull(result)
        assertFalse(result!!.isP2P)
        assertEquals("ABC Technologies Pvt Ltd", result.merchantName)
        assertNull(result.payeeName)
    }

    @Test
    fun `VPA address treated as merchant`() {
        val result = NotificationParser.parse(
            "com.google.android.apps.nbu.paisa.user",
            null,
            "Paid ₹200 to merchant@ybl"
        )
        assertNotNull(result)
        assertFalse(result!!.isP2P)
        assertEquals("merchant@ybl", result.merchantName)
    }

    @Test
    fun `person name still treated as merchant`() {
        val result = NotificationParser.parse(
            "com.google.android.apps.nbu.paisa.user",
            null,
            "Paid ₹500 to Amit Sharma"
        )
        assertNotNull(result)
        assertFalse(result!!.isP2P)
        assertEquals("Amit Sharma", result.merchantName)
    }
}
