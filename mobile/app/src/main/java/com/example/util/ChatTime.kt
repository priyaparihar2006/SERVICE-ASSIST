package com.example.util

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object ChatTime {

    private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
    private val dayMonthFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
    private val fullDateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())

    fun parseIsoToEpochMillis(isoString: String?): Long? {
        if (isoString.isNullOrBlank()) return null
        return try {
            Instant.parse(isoString).toEpochMilli()
        } catch (_: Exception) {
            // Fallback for timestamps missing Z or with local offsets
            try {
                val normalized = if (!isoString.endsWith("Z") && !isoString.contains("+")) {
                    "${isoString}Z"
                } else {
                    isoString
                }
                Instant.parse(normalized).toEpochMilli()
            } catch (_: Exception) {
                null
            }
        }
    }

    fun formatMessageTime(
        epochMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): String {
        return try {
            val instant = Instant.ofEpochMilli(epochMillis)
            val zonedDateTime = instant.atZone(zoneId)
            timeFormatter.format(zonedDateTime)
        } catch (_: Exception) {
            ""
        }
    }

    fun formatConversationListTime(
        epochMillis: Long?,
        clock: Clock = Clock.systemDefaultZone(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): String {
        if (epochMillis == null || epochMillis <= 0) return ""
        return try {
            val messageInstant = Instant.ofEpochMilli(epochMillis)
            val messageDate = messageInstant.atZone(zoneId).toLocalDate()
            val today = LocalDate.now(clock.withZone(zoneId))

            when {
                messageDate.isEqual(today) -> {
                    timeFormatter.format(messageInstant.atZone(zoneId))
                }
                messageDate.isEqual(today.minusDays(1)) -> {
                    "Yesterday"
                }
                messageDate.year == today.year -> {
                    dayMonthFormatter.format(messageDate)
                }
                else -> {
                    fullDateFormatter.format(messageDate)
                }
            }
        } catch (_: Exception) {
            ""
        }
    }

    fun formatDayDivider(
        epochMillis: Long,
        clock: Clock = Clock.systemDefaultZone(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): String {
        return try {
            val messageInstant = Instant.ofEpochMilli(epochMillis)
            val messageDate = messageInstant.atZone(zoneId).toLocalDate()
            val today = LocalDate.now(clock.withZone(zoneId))

            when {
                messageDate.isEqual(today) -> "Today"
                messageDate.isEqual(today.minusDays(1)) -> "Yesterday"
                else -> fullDateFormatter.format(messageDate)
            }
        } catch (_: Exception) {
            "Today"
        }
    }
}
