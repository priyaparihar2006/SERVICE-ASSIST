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
        val api = paymentApi ?: return@withContext Result.failure(Exception("Payment service not available"))
        try {
            val localBooking = bookingDao.getBookingByIdSync(bookingId)
            val code = localBooking?.bookingCode

            var response = api.initPayment(PaymentInitRequest(bookingId, code))
            if (!response.isSuccessful && response.code() == 404 && localBooking != null) {
                try {
                    android.util.Log.i("PaymentRepository", "Booking #$bookingId not found remotely. Auto-pushing local booking to Supabase...")
                    val pushResp = SupabaseClient.bookingApiService?.createBooking(localBooking.toSupabaseDto())
                    val createdRemote = pushResp?.body()
                    val remoteId = createdRemote?.id
                    if (remoteId != null && remoteId != localBooking.id) {
                        bookingDao.updateBooking(localBooking.copy(id = remoteId))
                    }
                } catch (e: Exception) {
                    android.util.Log.w("PaymentRepository", "Auto-push booking before initPayment failed: ${e.message}")
                }
                response = api.initPayment(PaymentInitRequest(bookingId, code))
            }

            if (response.isSuccessful && response.body() != null) {
                val booking = bookingDao.getBookingByIdSync(bookingId)
                if (booking != null && (booking.status == BookingStatus.ARRIVED || booking.status == BookingStatus.STARTED)) {
                    bookingDao.updateStatus(bookingId, BookingStatus.AWAITING_PAYMENT)
                }
                Result.success(response.body()!!)
            } else {
                val raw = response.errorBody()?.string()
                val errorMsg = extractErrorMessage(raw, response.code(), "Failed to initialize payment")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun collectCash(bookingId: Long): Result<PaymentCollectCashResponse> = withContext(Dispatchers.IO) {
        val api = paymentApi ?: return@withContext Result.failure(Exception("Payment service not available"))
        try {
            val localBooking = bookingDao.getBookingByIdSync(bookingId)
            val code = localBooking?.bookingCode

            var response = api.collectCash(PaymentCollectCashRequest(bookingId, code))
            if (!response.isSuccessful) {
                val raw = response.errorBody()?.string()
                val errorMsg = extractErrorMessage(raw, response.code(), "Failed to record cash payment")
                if (response.code() == 404 && localBooking != null) {
                    try {
                        SupabaseClient.bookingApiService?.createBooking(localBooking.toSupabaseDto())
                    } catch (e: Exception) { }
                    initPayment(bookingId)
                    response = api.collectCash(PaymentCollectCashRequest(bookingId, code))
                } else if (response.code() == 400 && errorMsg.contains("status", ignoreCase = true)) {
                    android.util.Log.i("PaymentRepository", "Status mismatch in collectCash for booking $bookingId ($errorMsg). Attempting auto-repair via initPayment...")
                    initPayment(bookingId)
                    response = api.collectCash(PaymentCollectCashRequest(bookingId, code))
                }
            }

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val booking = bookingDao.getBookingByIdSync(bookingId)
                if (booking != null) {
                    bookingDao.updateBooking(
                        booking.copy(
                            status = BookingStatus.COMPLETED,
                            isPaid = true,
                            paymentMethod = "CASH",
                            paidAt = body.paidAt ?: System.currentTimeMillis()
                        )
                    )
                }
                Result.success(body)
            } else {
                val raw = response.errorBody()?.string()
                val errorMsg = extractErrorMessage(raw, response.code(), "Failed to record cash payment")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun collectUpi(bookingId: Long, utr: String): Result<PaymentCollectUpiResponse> = withContext(Dispatchers.IO) {
        val api = paymentApi ?: return@withContext Result.failure(Exception("Payment service not available"))
        try {
            val localBooking = bookingDao.getBookingByIdSync(bookingId)
            val code = localBooking?.bookingCode

            var response = api.collectUpi(PaymentCollectUpiRequest(bookingId, utr, code))
            if (!response.isSuccessful) {
                val raw = response.errorBody()?.string()
                val errorMsg = extractErrorMessage(raw, response.code(), "Failed to confirm UPI payment")
                if (response.code() == 404 && localBooking != null) {
                    try {
                        SupabaseClient.bookingApiService?.createBooking(localBooking.toSupabaseDto())
                    } catch (e: Exception) { }
                    initPayment(bookingId)
                    response = api.collectUpi(PaymentCollectUpiRequest(bookingId, utr, code))
                } else if (response.code() == 400 && errorMsg.contains("status", ignoreCase = true)) {
                    android.util.Log.i("PaymentRepository", "Status mismatch in collectUpi for booking $bookingId ($errorMsg). Attempting auto-repair via initPayment...")
                    initPayment(bookingId)
                    response = api.collectUpi(PaymentCollectUpiRequest(bookingId, utr, code))
                }
            }

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val booking = bookingDao.getBookingByIdSync(bookingId)
                if (booking != null) {
                    bookingDao.updateBooking(
                        booking.copy(
                            status = BookingStatus.COMPLETED,
                            isPaid = true,
                            paymentMethod = "UPI",
                            paymentReference = utr,
                            paidAt = body.paidAt ?: System.currentTimeMillis()
                        )
                    )
                }
                Result.success(body)
            } else {
                val raw = response.errorBody()?.string()
                val errorMsg = extractErrorMessage(raw, response.code(), "Failed to confirm UPI payment")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
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
