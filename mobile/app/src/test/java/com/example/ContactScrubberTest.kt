package com.example

import com.example.data.remote.chat.ContactScrubber
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactScrubberTest {

    data class TestCase(
        val input: String,
        val shouldBlock: Boolean,
        val expectedReason: ContactScrubber.ScrubberReason? = null,
        val description: String
    )

    @Test
    fun testContactScrubberComprehensive() {
        val cases = listOf(
            // 1-10: Direct Indian Mobile formats
            TestCase("Call me on 9876543210 please", true, ContactScrubber.ScrubberReason.PHONE, "Plain 10 digit number starting with 9"),
            TestCase("My number is +91 98765 43210", true, ContactScrubber.ScrubberReason.PHONE, "+91 with spaces"),
            TestCase("+91-98765-43210", true, ContactScrubber.ScrubberReason.PHONE, "+91 with dashes"),
            TestCase("00919876543210", true, ContactScrubber.ScrubberReason.PHONE, "0091 prefix"),
            TestCase("9 8 7 6 5 4 3 2 1 0", true, ContactScrubber.ScrubberReason.PHONE, "Spaced out 10 digits"),
            TestCase("9.8.7.6.5.4.3.2.1.0", true, ContactScrubber.ScrubberReason.PHONE, "Dotted 10 digits"),
            TestCase("8123456789", true, ContactScrubber.ScrubberReason.PHONE, "Plain 10 digit starting with 8"),
            TestCase("7987654321", true, ContactScrubber.ScrubberReason.PHONE, "Plain 10 digit starting with 7"),
            TestCase("6234567890", true, ContactScrubber.ScrubberReason.PHONE, "Plain 10 digit starting with 6"),
            TestCase("Reach me at 98765-43210 tomorrow", true, ContactScrubber.ScrubberReason.PHONE, "Hyphenated mobile number"),

            // 11-18: Spelled out numbers (English & Hindi)
            TestCase("nine eight seven six five four three two one zero", true, ContactScrubber.ScrubberReason.PHONE, "Spelled out english digits"),
            TestCase("9 eight 7 six 5 four 3 two 1 zero", true, ContactScrubber.ScrubberReason.PHONE, "Mixed digits and words"),
            TestCase("nau aath saat chhe paanch chaar teen do ek shunya", true, ContactScrubber.ScrubberReason.PHONE, "Hindi transliterated digits"),
            TestCase("mera number hai nine eight seven six five four three two one zero", true, ContactScrubber.ScrubberReason.PHONE, "Hindi phrase with english digits"),
            TestCase("Call me: 98 seven 65 four 3210", true, ContactScrubber.ScrubberReason.PHONE, "Interleaved word digits"),
            TestCase("eight one two three four five six seven eight nine", true, ContactScrubber.ScrubberReason.PHONE, "Spelled out starting with 8"),
            TestCase("seven nine eight seven six five four three two one", true, ContactScrubber.ScrubberReason.PHONE, "Spelled out starting with 7"),
            TestCase("six two three four five six seven eight nine zero", true, ContactScrubber.ScrubberReason.PHONE, "Spelled out starting with 6"),

            // 19-25: Emails
            TestCase("Email me at user@example.com", true, ContactScrubber.ScrubberReason.EMAIL, "Standard email"),
            TestCase("contact: priya.sharma99@gmail.com", true, ContactScrubber.ScrubberReason.EMAIL, "Gmail address"),
            TestCase("reach out to test.partner@yahoo.co.in", true, ContactScrubber.ScrubberReason.EMAIL, "Yahoo co in"),
            TestCase("service.lead@outlook.com is my mail", true, ContactScrubber.ScrubberReason.EMAIL, "Outlook email"),
            TestCase("admin@servora.com", true, ContactScrubber.ScrubberReason.EMAIL, "Custom domain email"),
            TestCase("contact me rajesh_tech@rediffmail.com", true, ContactScrubber.ScrubberReason.EMAIL, "Rediffmail"),
            TestCase("send bill to invoices+tax@mycompany.org", true, ContactScrubber.ScrubberReason.EMAIL, "Plus tag email"),

            // 26-32: UPI & Payment Handles
            TestCase("pay me on priya@okhdfcbank", true, ContactScrubber.ScrubberReason.EXTERNAL_APP, "HDFC UPI"),
            TestCase("send money to rajesh@okaxis", true, ContactScrubber.ScrubberReason.EXTERNAL_APP, "Axis UPI"),
            TestCase("amit@okicici", true, ContactScrubber.ScrubberReason.EXTERNAL_APP, "ICICI UPI"),
            TestCase("payment handle: 9876543210@paytm", true, ContactScrubber.ScrubberReason.PHONE, "Phone based Paytm UPI"),
            TestCase("dinesh@ybl", true, ContactScrubber.ScrubberReason.EXTERNAL_APP, "PhonePe YBL handle"),
            TestCase("send tip to test@upi", true, ContactScrubber.ScrubberReason.EXTERNAL_APP, "Generic UPI"),
            TestCase("my id is pro@oksbi", true, ContactScrubber.ScrubberReason.EXTERNAL_APP, "SBI UPI"),

            // 33-38: External Apps & Solicitations
            TestCase("Ping me on whatsapp please", true, ContactScrubber.ScrubberReason.EXTERNAL_APP, "WhatsApp mention"),
            TestCase("Join my telegram channel t.me/servora", true, ContactScrubber.ScrubberReason.EXTERNAL_APP, "Telegram link"),
            TestCase("Message me on insta id: @cleaner_pro", true, ContactScrubber.ScrubberReason.EXTERNAL_APP, "Instagram reference"),
            TestCase("Snapchat me later", true, ContactScrubber.ScrubberReason.EXTERNAL_APP, "Snapchat reference"),
            TestCase("Please call me directly outside app", true, ContactScrubber.ScrubberReason.EXTERNAL_APP, "Call me solicitation"),
            TestCase("my number is private", true, ContactScrubber.ScrubberReason.EXTERNAL_APP, "My number phrase"),

            // 39-48: Benign texts (MUST NOT BE BLOCKED)
            TestCase("Gate no 9876 street 5", false, null, "Street and gate numbers"),
            TestCase("I live in Flat 402, Royal Residency", false, null, "Apartment address"),
            TestCase("Please bring 2 packets of cleaning powder and 1 mop", false, null, "Item counts"),
            TestCase("Service booking SRV-21493", false, null, "Booking code"),
            TestCase("The door bell is not working, please knock loudly", false, null, "Instructions"),
            TestCase("ETA is 15 minutes, coming via MG Road", false, null, "ETA and transit note"),
            TestCase("Please keep the AC outdoor unit accessible", false, null, "Technical instructions"),
            TestCase("OTP for start is 4829", false, null, "4-digit OTP"),
            TestCase("PINCODE: 282001 Agra", false, null, "Indian Postal Pincode"),
            TestCase("Total estimate is 499 rupees only", false, null, "Price quote")
        )

        assertEquals("Should have at least 40 test cases", true, cases.size >= 40)

        for ((index, tc) in cases.withIndex()) {
            val result = ContactScrubber.scrub(tc.input)
            assertEquals(
                "Case #$index failed: '${tc.description}' (Input: '${tc.input}')",
                tc.shouldBlock,
                result.isBlocked
            )
            if (tc.shouldBlock && tc.expectedReason != null) {
                // If expectedReason is specified, verify it matches
                assertEquals(
                    "Case #$index reason mismatch for '${tc.input}'",
                    tc.expectedReason,
                    result.reason
                )
            }
        }
    }
}
