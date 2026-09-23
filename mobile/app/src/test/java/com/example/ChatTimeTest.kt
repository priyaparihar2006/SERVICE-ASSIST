package com.example

import com.example.util.ChatTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class ChatTimeTest {

    private val fixedZone = ZoneId.of("Asia/Kolkata")
    // Fixed reference clock at 2026-09-21 15:30:00 IST (UTC: 10:00:00)
    private val fixedInstant = Instant.parse("2026-09-21T10:00:00Z")
    private val fixedClock = Clock.fixed(fixedInstant, fixedZone)

    @Test
    fun testParseIsoToEpochMillis() {
        val iso = "2026-09-21T10:00:00Z"
        val millis = ChatTime.parseIsoToEpochMillis(iso)
        assertNotNull(millis)
        assertEquals(fixedInstant.toEpochMilli(), millis)
    }

    @Test
    fun testFormatMessageTime_12HourFormat() {
        // 10:00 UTC is 15:30 IST -> "3:30 PM" (or "3:30 pm" depending on locale format)
        val formatted = ChatTime.formatMessageTime(fixedInstant.toEpochMilli(), fixedZone)
        assertTrue("Formatted time should contain 3:30: $formatted", formatted.startsWith("3:30"))
        assertTrue("Formatted time should contain AM/PM: $formatted", formatted.contains("PM", ignoreCase = true))
    }

    @Test
    fun testFormatDayDivider_todayYesterdayAndOlder() {
        val todayMillis = fixedInstant.toEpochMilli()
        val yesterdayMillis = fixedInstant.minusSeconds(86400).toEpochMilli()
        val olderMillis = Instant.parse("2026-09-15T10:00:00Z").toEpochMilli()

        val todayText = ChatTime.formatDayDivider(todayMillis, fixedClock, fixedZone)
        val yesterdayText = ChatTime.formatDayDivider(yesterdayMillis, fixedClock, fixedZone)
        val olderText = ChatTime.formatDayDivider(olderMillis, fixedClock, fixedZone)

        assertEquals("Today", todayText)
        assertEquals("Yesterday", yesterdayText)
        assertTrue("Older date should contain month: $olderText", olderText.contains("Sep"))
    }

    @Test
    fun testFormatConversationListTime() {
        val todayMillis = fixedInstant.toEpochMilli()
        val yesterdayMillis = fixedInstant.minusSeconds(86400).toEpochMilli()
        val olderMillis = Instant.parse("2026-09-15T10:00:00Z").toEpochMilli()

        val todayText = ChatTime.formatConversationListTime(todayMillis, fixedClock, fixedZone)
        val yesterdayText = ChatTime.formatConversationListTime(yesterdayMillis, fixedClock, fixedZone)
        val olderText = ChatTime.formatConversationListTime(olderMillis, fixedClock, fixedZone)

        assertTrue("Today in list shows time: $todayText", todayText.contains("3:30"))
        assertEquals("Yesterday", yesterdayText)
        assertTrue("Older in list shows day month: $olderText", olderText.contains("15 Sep"))
    }
}
