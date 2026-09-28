package com.example.data.repository

import com.example.data.db.BookingDao
import com.example.data.model.BookingStatus
import com.example.data.remote.payment.PaymentAdminListResponse
import com.example.data.remote.payment.PaymentAdminOverrideRequest
import com.example.data.remote.payment.PaymentAdminOverrideResponse
import com.example.data.remote.payment.PaymentApiService
import com.example.data.remote.payment.PaymentCollectCashRequest
import com.example.data.remote.payment.PaymentCollectCashResponse
import com.example.data.remote.payment.PaymentCollectUpiRequest
import com.example.data.remote.payment.PaymentCollectUpiResponse
import com.example.data.remote.payment.PaymentInitRequest
import com.example.data.remote.payment.PaymentInitResponse
import com.example.data.remote.payment.PaymentStatusResponse
import com.example.data.remote.supabase.SupabaseClient
import com.example.data.remote.supabase.toSupabaseDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PaymentRepository(
    private val paymentApi: PaymentApiService?,
    private val bookingDao: BookingDao
) {
    suspend fun initPayment(bookingId: Long): Result<PaymentInitResponse> = withContext(Dispatchers.IO) {
        val localBooking = bookingDao.getBookingByIdSync(bookingId)
        if (localBooking == null) {
            return@withContext Result.failure(Exception("Booking #$bookingId not found"))
        }

        val code = if (localBooking.bookingCode.isNotBlank()) localBooking.bookingCode else "SRV-$bookingId"

        // Auto-advance status in local DB if partner has arrived / started
        if (localBooking.status == BookingStatus.ARRIVED || localBooking.status == BookingStatus.STARTED) {
            try {
                bookingDao.updateStatus(bookingId, BookingStatus.AWAITING_PAYMENT)
            } catch (_: Exception) {}
        }

        // 1. Try remote Edge Function if available
        val api = paymentApi
        if (api != null) {
            try {
                var response = api.initPayment(PaymentInitRequest(bookingId, code))
                if (!response.isSuccessful && response.code() == 404) {
                    try {
                        android.util.Log.i("PaymentRepository", "Booking #$bookingId not found remotely. Auto-pushing local booking to Supabase...")
                        SupabaseClient.apiService?.insertBooking(localBooking.toSupabaseDto())
                    } catch (e: Exception) {
                        android.util.Log.w("PaymentRepository", "Auto-push booking before initPayment failed: ${e.message}")
                    }
                    response = api.initPayment(PaymentInitRequest(bookingId, code))
                }

                if (response.isSuccessful && response.body() != null) {
                    return@withContext Result.success(response.body()!!)
                }
            } catch (e: Exception) {
                android.util.Log.w("PaymentRepository", "Remote payment-init failed (${e.message}), using local fallback")
            }
        }

        // 2. Local Fallback: Always construct a valid PaymentInitResponse so user is NEVER blocked
        val upiVpa = "9105830551@upi"
        val upiName = "Servora"
        val amount = localBooking.totalAmount
        val encodedName = java.net.URLEncoder.encode(upiName, "UTF-8")
        val encodedNote = java.net.URLEncoder.encode("Servora booking $code", "UTF-8")
        val encodedCode = java.net.URLEncoder.encode(code, "UTF-8")
        val qrPayload = "upi://pay?pa=$upiVpa&pn=$encodedName&am=$amount.00&cu=INR&tn=$encodedNote&tr=$encodedCode"

        val fallbackResponse = PaymentInitResponse(
            paymentId = "local_pay_${bookingId}_${System.currentTimeMillis()}",
            bookingId = bookingId,
            amount = amount,
            currency = "INR",
            qrPayload = qrPayload,
            upiPayeeVpa = upiVpa,
            upiPayeeName = upiName,
            status = "INITIATED"
        )
        Result.success(fallbackResponse)
    }

    suspend fun collectCash(bookingId: Long): Result<PaymentCollectCashResponse> = withContext(Dispatchers.IO) {
        val localBooking = bookingDao.getBookingByIdSync(bookingId)
        val code = localBooking?.bookingCode ?: "SRV-$bookingId"
        val now = System.currentTimeMillis()

        // 1. Try syncing with remote backend Edge Function
        val api = paymentApi
        var remoteSuccess = false
        var remotePaymentId: String? = null
        if (api != null) {
            try {
                val response = api.collectCash(PaymentCollectCashRequest(bookingId, code))
                if (response.isSuccessful && response.body() != null) {
                    remoteSuccess = true
                    remotePaymentId = response.body()?.paymentId
                    android.util.Log.i("PaymentRepository", "Remote collectCash succeeded for booking #$bookingId ($code)")
                } else {
                    val err = response.errorBody()?.string()
                    android.util.Log.w("PaymentRepository", "Remote collectCash failed: HTTP ${response.code()} - $err")
                }
            } catch (e: Exception) {
                android.util.Log.w("PaymentRepository", "Remote collectCash exception: ${e.message}")
            }
        }

        // 2. Update local Room database immediately
        if (localBooking != null) {
            bookingDao.updateBooking(
                localBooking.copy(
                    status = BookingStatus.COMPLETED,
                    isPaid = true,
                    paymentMethod = "CASH",
                    paidAt = now,
                    pendingSync = !remoteSuccess
                )
            )
        }

        Result.success(
            PaymentCollectCashResponse(
                success = true,
                bookingId = bookingId,
                status = "COMPLETED",
                isPaid = true,
                paymentMethod = "CASH",
                paidAt = now,
                paymentId = remotePaymentId ?: "cash_${bookingId}_$now"
            )
        )
    }

    suspend fun collectUpi(bookingId: Long, utr: String): Result<PaymentCollectUpiResponse> = withContext(Dispatchers.IO) {
        val localBooking = bookingDao.getBookingByIdSync(bookingId)
        val code = localBooking?.bookingCode ?: "SRV-$bookingId"
        val now = System.currentTimeMillis()

        // 1. Try syncing with remote backend Edge Function
        val api = paymentApi
        var remoteSuccess = false
        var remotePaymentId: String? = null
        if (api != null) {
            try {
                val response = api.collectUpi(PaymentCollectUpiRequest(bookingId, utr, code))
                if (response.isSuccessful && response.body() != null) {
                    remoteSuccess = true
                    remotePaymentId = response.body()?.paymentId
                    android.util.Log.i("PaymentRepository", "Remote collectUpi succeeded for booking #$bookingId ($code)")
                } else {
                    val err = response.errorBody()?.string()
                    android.util.Log.w("PaymentRepository", "Remote collectUpi failed: HTTP ${response.code()} - $err")
                }
            } catch (e: Exception) {
                android.util.Log.w("PaymentRepository", "Remote collectUpi exception: ${e.message}")
            }
        }

        // 2. Update local Room database immediately
        if (localBooking != null) {
            bookingDao.updateBooking(
                localBooking.copy(
                    status = BookingStatus.COMPLETED,
                    isPaid = true,
                    paymentMethod = "UPI",
                    paymentReference = utr,
                    paidAt = now,
                    pendingSync = !remoteSuccess
                )
            )
        }

        Result.success(
            PaymentCollectUpiResponse(
                success = true,
                bookingId = bookingId,
                status = "COMPLETED",
                isPaid = true,
                paymentMethod = "UPI",
                paymentReference = utr,
                paidAt = now,
                paymentId = remotePaymentId ?: "upi_${bookingId}_$now"
            )
        )
    }

    suspend fun getPaymentStatus(bookingId: Long): Result<PaymentStatusResponse> = withContext(Dispatchers.IO) {
        val api = paymentApi ?: return@withContext Result.failure(Exception("Payment service not available"))
        try {
            val response = api.getPaymentStatus(bookingId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val raw = response.errorBody()?.string()
                val errorMsg = extractErrorMessage(raw, response.code(), "Failed to get payment status")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listAdminPayments(bookingId: Long? = null): Result<PaymentAdminListResponse> = withContext(Dispatchers.IO) {
        val api = paymentApi ?: return@withContext Result.failure(Exception("Payment service not available"))
        try {
            val response = api.listAdminPayments(bookingId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val raw = response.errorBody()?.string()
                val errorMsg = extractErrorMessage(raw, response.code(), "Failed to list admin payments")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adminOverride(bookingId: Long, action: String, note: String?): Result<PaymentAdminOverrideResponse> = withContext(Dispatchers.IO) {
        val api = paymentApi ?: return@withContext Result.failure(Exception("Payment service not available"))
        try {
            val response = api.adminOverride(PaymentAdminOverrideRequest(bookingId, action, note))
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (action == "MARK_PAID") {
                    val booking = bookingDao.getBookingByIdSync(bookingId)
                    if (booking != null) {
                        bookingDao.updateBooking(
                            booking.copy(
                                status = BookingStatus.COMPLETED,
                                isPaid = true
                            )
                        )
                    }
                }
                Result.success(body)
            } else {
                val raw = response.errorBody()?.string()
                val errorMsg = extractErrorMessage(raw, response.code(), "Failed to override payment")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extractErrorMessage(raw: String?, code: Int, defaultPrefix: String): String {
        if (raw.isNullOrBlank()) return "$defaultPrefix ($code)"
        return try {
            val json = org.json.JSONObject(raw)
            when {
                json.has("error") -> json.getString("error")
                json.has("message") -> json.getString("message")
                else -> raw
            }
        } catch (_: Exception) {
            raw
        }
    }
}
