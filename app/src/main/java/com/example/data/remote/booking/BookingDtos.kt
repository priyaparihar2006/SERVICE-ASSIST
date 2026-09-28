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

@JsonClass(generateAdapter = true)
data class BookingAcceptRequest(
    @Json(name = "booking_id") val bookingId: Long
)

@JsonClass(generateAdapter = true)
data class BookingAcceptResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "booking_id") val bookingId: Long? = null,
    @Json(name = "accepted_at") val acceptedAt: String? = null,
    @Json(name = "error") val error: String? = null
)

@JsonClass(generateAdapter = true)
data class BookingPartnerCancelRequest(
    @Json(name = "booking_id") val bookingId: Long,
    @Json(name = "reason_code") val reasonCode: String,
    @Json(name = "reason_note") val reasonNote: String? = null
)

@JsonClass(generateAdapter = true)
data class BookingPartnerCancelResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "booking_id") val bookingId: Long? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "error") val error: String? = null
)
