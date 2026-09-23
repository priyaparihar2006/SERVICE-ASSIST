package com.example.data.remote.payment

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PaymentInitRequest(
    @Json(name = "booking_id") val bookingId: Long,
    @Json(name = "booking_code") val bookingCode: String? = null
)

@JsonClass(generateAdapter = true)
data class PaymentInitResponse(
    @Json(name = "payment_id") val paymentId: String?,
    @Json(name = "booking_id") val bookingId: Long,
    @Json(name = "amount") val amount: Int,
    @Json(name = "currency") val currency: String,
    @Json(name = "qr_payload") val qrPayload: String,
    @Json(name = "upi_payee_vpa") val upiPayeeVpa: String,
    @Json(name = "upi_payee_name") val upiPayeeName: String,
    @Json(name = "status") val status: String
)

@JsonClass(generateAdapter = true)
data class PaymentCollectCashRequest(
    @Json(name = "booking_id") val bookingId: Long,
    @Json(name = "booking_code") val bookingCode: String? = null
)

@JsonClass(generateAdapter = true)
data class PaymentCollectCashResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "booking_id") val bookingId: Long,
    @Json(name = "status") val status: String,
    @Json(name = "is_paid") val isPaid: Boolean,
    @Json(name = "payment_method") val paymentMethod: String,
    @Json(name = "paid_at") val paidAt: Long? = null,
    @Json(name = "payment_id") val paymentId: String? = null
)

@JsonClass(generateAdapter = true)
data class PaymentCollectUpiRequest(
    @Json(name = "booking_id") val bookingId: Long,
    @Json(name = "utr") val utr: String,
    @Json(name = "booking_code") val bookingCode: String? = null
)

@JsonClass(generateAdapter = true)
data class PaymentCollectUpiResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "booking_id") val bookingId: Long,
    @Json(name = "status") val status: String,
    @Json(name = "is_paid") val isPaid: Boolean,
    @Json(name = "payment_method") val paymentMethod: String,
    @Json(name = "payment_reference") val paymentReference: String? = null,
    @Json(name = "paid_at") val paidAt: Long? = null,
    @Json(name = "payment_id") val paymentId: String? = null
)

@JsonClass(generateAdapter = true)
data class PaymentDto(
    @Json(name = "id") val id: String,
    @Json(name = "booking_id") val bookingId: Long,
    @Json(name = "amount") val amount: Int,
    @Json(name = "currency") val currency: String,
    @Json(name = "method") val method: String,
    @Json(name = "status") val status: String,
    @Json(name = "upi_vpa") val upiVpa: String? = null,
    @Json(name = "upi_reference") val upiReference: String? = null,
    @Json(name = "qr_payload") val qrPayload: String? = null,
    @Json(name = "confirmed_by") val confirmedBy: String? = null,
    @Json(name = "confirmed_role") val confirmedRole: String? = null,
    @Json(name = "paid_at") val paidAt: String? = null,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class PaymentStatusResponse(
    @Json(name = "found") val found: Boolean,
    @Json(name = "booking_status") val bookingStatus: String? = null,
    @Json(name = "is_paid") val isPaid: Boolean? = null,
    @Json(name = "payment") val payment: PaymentDto? = null
)

@JsonClass(generateAdapter = true)
data class PaymentAdminAuditDto(
    @Json(name = "id") val id: String,
    @Json(name = "payment_id") val paymentId: String? = null,
    @Json(name = "booking_id") val bookingId: Long,
    @Json(name = "actor_id") val actorId: String,
    @Json(name = "actor_role") val actorRole: String,
    @Json(name = "action") val action: String,
    @Json(name = "detail") val detail: String? = null,
    @Json(name = "created_at") val createdAt: String
)

@JsonClass(generateAdapter = true)
data class PaymentAdminListResponse(
    @Json(name = "payments") val payments: List<PaymentDto> = emptyList(),
    @Json(name = "audit_logs") val auditLogs: List<PaymentAdminAuditDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class PaymentAdminOverrideRequest(
    @Json(name = "booking_id") val bookingId: Long,
    @Json(name = "action") val action: String,
    @Json(name = "note") val note: String? = null
)

@JsonClass(generateAdapter = true)
data class PaymentAdminOverrideResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "booking_id") val bookingId: Long,
    @Json(name = "action") val action: String,
    @Json(name = "status") val status: String? = null,
    @Json(name = "is_paid") val isPaid: Boolean? = null
)
