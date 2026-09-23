package com.example.data.remote.booking

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BookingUpdateStatusRequest(
    @Json(name = "booking_id") val bookingId: Long? = null,
    @Json(name = "booking_code") val bookingCode: String? = null,
    @Json(name = "status") val status: String,
    @Json(name = "special_notes") val specialNotes: String? = null,
    @Json(name = "start_otp") val startOtp: String? = null,
    @Json(name = "cancellation_reason") val cancellationReason: String? = null,
    @Json(name = "cancellation_feedback") val cancellationFeedback: String? = null
)
