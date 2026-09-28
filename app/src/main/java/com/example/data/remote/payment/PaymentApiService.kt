package com.example.data.remote.payment

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface PaymentApiService {

    @POST("functions/v1/payment-init")
    suspend fun initPayment(
        @Body request: PaymentInitRequest
    ): Response<PaymentInitResponse>

    @POST("functions/v1/payment-collect-cash")
    suspend fun collectCash(
        @Body request: PaymentCollectCashRequest
    ): Response<PaymentCollectCashResponse>

    @POST("functions/v1/payment-collect-upi")
    suspend fun collectUpi(
        @Body request: PaymentCollectUpiRequest
    ): Response<PaymentCollectUpiResponse>

    @GET("functions/v1/payment-status")
    suspend fun getPaymentStatus(
        @Query("booking_id") bookingId: Long
    ): Response<PaymentStatusResponse>

    @GET("functions/v1/payment-admin-list")
    suspend fun listAdminPayments(
        @Query("booking_id") bookingId: Long? = null,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): Response<PaymentAdminListResponse>

    @POST("functions/v1/payment-admin-override")
    suspend fun adminOverride(
        @Body request: PaymentAdminOverrideRequest
    ): Response<PaymentAdminOverrideResponse>
}
