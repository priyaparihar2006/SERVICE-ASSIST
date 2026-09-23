package com.example.data.remote.booking

import com.example.data.remote.supabase.SupabaseBookingDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface BookingApiService {

    @POST("functions/v1/booking-create")
    suspend fun createBooking(
        @Body request: SupabaseBookingDto
    ): Response<SupabaseBookingDto>

    @POST("functions/v1/booking-update-status")
    suspend fun updateBookingStatus(
        @Body request: BookingUpdateStatusRequest
    ): Response<SupabaseBookingDto>
}
