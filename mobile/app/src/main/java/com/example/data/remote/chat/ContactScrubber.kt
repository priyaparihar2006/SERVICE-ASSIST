package com.example.data.remote.chat

/**
 * Client and Server Contact Scrubber.
 * Blocks attempts to share phone numbers, email addresses, UPI handles, or external messaging handles.
 */
object ContactScrubber {

    enum class ScrubberReason {
        PHONE, EMAIL, EXTERNAL_APP, ABUSE
    }

    data class ScrubberResult(
        val isBlocked: Boolean,
        val reason: ScrubberReason? = null,
        val userMessage: String? = null
    )

    private val wordToDigit = mapOf(
        "zero" to "0", "one" to "1", "two" to "2", "three" to "3", "four" to "4",
        "five" to "5", "six" to "6", "seven" to "7", "eight" to "8", "nine" to "9",
        "shunya" to "0", "ek" to "1", "do" to "2", "teen" to "3", "chaar" to "4",
        "paanch" to "5", "chhe" to "6", "saat" to "7", "aath" to "8", "nau" to "9"
    )

    private val emailPattern = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
    private val upiPattern = Regex("[a-zA-Z0-9.\\-_]{2,256}@(okhdfcbank|okaxis|okicici|oksbi|paytm|apl|ybl|upi|ibl|axl)", RegexOption.IGNORE_CASE)
    private val socialPatterns = listOf(
        Regex("\\b(whatsapp|wa\\.me|telegram|t\\.me|insta(gram)?|snapchat|fb|facebook)\\b", RegexOption.IGNORE_CASE),
        Regex("\\b(call\\s*me|my\\s*number|ping\\s*me|text\\s*me|phone\\s*pe\\s*baat)\\b", RegexOption.IGNORE_CASE)
    )

    fun scrub(input: String?): ScrubberResult {
        if (input.isNullOrBlank()) return ScrubberResult(isBlocked = false)

        val text = input.trim()
        val normalized = text.lowercase()

        // 1. Words for digits replacement
        var converted = normalized
        for ((word, digit) in wordToDigit) {
            converted = converted.replace(Regex("\\b$word\\b"), digit)
        }

        // 2. Explicit phone number check
        val explicitPhoneRegex = Regex("(?:^|\\D)(?:(?:\\+?91|0091)[\\s.-]?)?([6-9][\\s.-]*(?:\\d[\\s.-]*){9})(?:\\D|$)")
        if (explicitPhoneRegex.containsMatchIn(converted)) {
            return ScrubberResult(
                isBlocked = true,
                reason = ScrubberReason.PHONE,
                userMessage = "For your safety, contact details can't be shared here. Keep chatting in the app."
            )
        }

        val digitsOnly = converted.filter { it.isDigit() }
        if (digitsOnly.length >= 10 && Regex("(?:^|\\D)[6-9]\\d{9}(?:\\D|$)").containsMatchIn(digitsOnly)) {
            if (!converted.contains("pincode") && !converted.contains("pin code")) {
                return ScrubberResult(
                    isBlocked = true,
                    reason = ScrubberReason.PHONE,
                    userMessage = "For your safety, contact details can't be shared here. Keep chatting in the app."
                )
            }
        }

        // 3. Email check
        if (emailPattern.containsMatchIn(text)) {
            return ScrubberResult(
                isBlocked = true,
                reason = ScrubberReason.EMAIL,
                userMessage = "For your safety, contact details can't be shared here. Keep chatting in the app."
            )
        }

        // 4. UPI check
        if (upiPattern.containsMatchIn(text)) {
            return ScrubberResult(
                isBlocked = true,
                reason = ScrubberReason.EXTERNAL_APP,
                userMessage = "For your safety, contact details can't be shared here. Keep chatting in the app."
            )
        }

        // 5. Social and direct calling solicitation
        for (pattern in socialPatterns) {
            if (pattern.containsMatchIn(normalized)) {
                return ScrubberResult(
                    isBlocked = true,
                    reason = ScrubberReason.EXTERNAL_APP,
                    userMessage = "For your safety, contact details can't be shared here. Keep chatting in the app."
                )
            }
        }

        return ScrubberResult(isBlocked = false)
    }
}
