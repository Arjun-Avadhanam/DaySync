package com.daysync.app.feature.expenses.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BankSmsParserTest {

    private val SMS_PKG = "com.google.android.apps.messaging"

    // ── HDFC UPI Payment (from real screenshot) ─────────────

    @Test
    fun `parse HDFC UPI payment via GPay`() {
        val sms = "Sent Rs.1.00\nFrom HDFC Bank A/C *3082\nTo DEEPA DEVARAJ\nOn 08/04/26\nRef 121277123802\nNot You?\nCall 18002586161/SMS BLOCK UPI to 7308080808"
        val result = NotificationParser.parse(SMS_PKG, "VM-HDFCBK-T", sms)
        assertNotNull(result)
        assertEquals(1.0, result!!.amount, 0.01)
        assertEquals("DEEPA DEVARAJ", result.merchantName)
        assertEquals("121277123802", result.referenceId)
        assertTrue(result.isDebit)
        assertEquals("INR", result.currency)
    }

    @Test
    fun `parse HDFC UPI payment via BHIM`() {
        val sms = "Sent Rs.1.00\nFrom HDFC Bank A/C *3082\nTo VIJAYA  SIMHA VIDYASAGAR\nOn 08/04/26\nRef 165411759561\nNot You?\nCall 18002586161/SMS BLOCK UPI to 7308080808"
        val result = NotificationParser.parse(SMS_PKG, "VM-HDFCBK-T", sms)
        assertNotNull(result)
        assertEquals(1.0, result!!.amount, 0.01)
        assertEquals("VIJAYA  SIMHA VIDYASAGAR", result.merchantName)
        assertEquals("165411759561", result.referenceId)
    }

    @Test
    fun `parse HDFC UPI payment large amount`() {
        val sms = "Sent Rs.540.00\nFrom HDFC Bank A/C *3082\nTo CALIFORNIA BURRITO\nOn 21/03/26\nRef 987654321012\nNot You?"
        val result = NotificationParser.parse(SMS_PKG, "VM-HDFCBK-T", sms)
        assertNotNull(result)
        assertEquals(540.0, result!!.amount, 0.01)
        assertEquals("CALIFORNIA BURRITO", result.merchantName)
    }

    @Test
    fun `parse HDFC UPI to metro`() {
        val sms = "Sent Rs.61.00\nFrom HDFC Bank A/C *3082\nTo L AND T METRO RAIL HYDERABAD LIMITED\nOn 21/03/26\nRef 111222333444"
        val result = NotificationParser.parse(SMS_PKG, "VM-HDFCBK-T", sms)
        assertNotNull(result)
        assertEquals(61.0, result!!.amount, 0.01)
        assertEquals("L AND T METRO RAIL HYDERABAD LIMITED", result.merchantName)
    }

    // ── HDFC UPI Mandate ────────────────────────────────────

    @Test
    fun `parse HDFC UPI mandate for Google Play`() {
        val sms = "Sent Rs.299.00\nfrom HDFC Bank A/c 3082\nTo Google Play\n25/02/26\nRef 352268320566\nNot You? Call 18002586161/SMS BLOCK UPI to 7308080808"
        val result = NotificationParser.parse(SMS_PKG, "AD-HDFCBK-S", sms)
        assertNotNull(result)
        assertEquals(299.0, result!!.amount, 0.01)
        assertEquals("Google Play", result.merchantName)
        assertEquals("352268320566", result.referenceId)
    }

    // ── Different sender IDs ────────────────────────────────

    @Test
    fun `parse with VM sender ID`() {
        val sms = "Sent Rs.100.00\nFrom HDFC Bank A/C *3082\nTo MERCHANT\nOn 01/01/26\nRef 111"
        val result = NotificationParser.parse(SMS_PKG, "VM-HDFCBK-T", sms)
        assertNotNull(result)
    }

    @Test
    fun `parse with AD sender ID`() {
        val sms = "Sent Rs.100.00\nfrom HDFC Bank A/c 3082\nTo MERCHANT\n01/01/26\nRef 222"
        val result = NotificationParser.parse(SMS_PKG, "AD-HDFCBK-S", sms)
        assertNotNull(result)
    }

    @Test
    fun `parse with JM sender ID`() {
        val sms = "Sent Rs.50.00\nFrom HDFC Bank A/C *3082\nTo STORE\nOn 01/01/26\nRef 333"
        val result = NotificationParser.parse(SMS_PKG, "JM-HDFCBK-X", sms)
        assertNotNull(result)
    }

    // ── Filtering ───────────────────────────────────────────

    @Test
    fun `skip credit SMS`() {
        val sms = "Received Rs.500.00\nTo HDFC Bank A/C *3082\nFrom SOMEONE\nOn 01/01/26"
        val result = NotificationParser.parse(SMS_PKG, "VM-HDFCBK-T", sms)
        assertNull(result)
    }

    @Test
    fun `skip OTP SMS`() {
        val sms = "Your OTP for HDFC Bank transaction is 123456"
        val result = NotificationParser.parse(SMS_PKG, "VM-HDFCBK-T", sms)
        assertNull(result)
    }

    @Test
    fun `skip pre-debit alert`() {
        val sms = "Alert:\nUSD.118.00 will be debited on 03/04/2026 from HDFC Bank Card 1279 for Anthropic\nID:YJyCvPAdch"
        val result = NotificationParser.parse(SMS_PKG, "VM-HDFCBK-T", sms)
        assertNull(result)
    }

    @Test
    fun `skip non-bank SMS`() {
        val sms = "Your Swiggy order is on the way! Arriving in 30 minutes."
        val result = NotificationParser.parse(SMS_PKG, "Swiggy", sms)
        assertNull(result)
    }

    @Test
    fun `skip promotional SMS`() {
        val sms = "Exclusive offer! Get 50% off on your next purchase. Shop now at Amazon."
        val result = NotificationParser.parse(SMS_PKG, "Amazon", sms)
        assertNull(result)
    }

    // ── Merchant name cleaning ──────────────────────────────

    @Test
    fun `merchant name cleaned of Not You junk`() {
        val sms = "Sent Rs.100.00\nFrom HDFC Bank A/C *3082\nTo MEDICAL STORE\nOn 01/01/26\nRef 444\nNot You?\nCall 18002586161"
        val result = NotificationParser.parse(SMS_PKG, "VM-HDFCBK-T", sms)
        assertEquals("MEDICAL STORE", result?.merchantName)
    }

    // ── Card spend ──────────────────────────────────────────

    @Test
    fun `parse HDFC card spend at merchant`() {
        val sms = "Rs.1500.00 spent on HDFC Bank Card xx1279 at AMAZON RETAIL on 15-03-26"
        val result = NotificationParser.parse(SMS_PKG, "VM-HDFCBK-T", sms)
        assertNotNull(result)
        assertEquals(1500.0, result!!.amount, 0.01)
        assertEquals("AMAZON RETAIL", result.merchantName)
    }

    // ── Payment app fallback ────────────────────────────────

    @Test
    fun `GPay notification still works as fallback`() {
        val result = NotificationParser.parse(
            "com.google.android.apps.nbu.paisa.user",
            "Payment",
            "Paid ₹200 to Chai Point"
        )
        assertNotNull(result)
        assertEquals(200.0, result!!.amount, 0.01)
        assertEquals("Chai Point", result.merchantName)
    }

    @Test
    fun `GPay credit notification filtered`() {
        val result = NotificationParser.parse(
            "com.google.android.apps.nbu.paisa.user",
            "Payment",
            "Received ₹500 from John"
        )
        assertNull(result)
    }
}
