package com.example.data.remote.supabase

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query

interface SupabaseApiService {

    // USER PROFILES
    @GET("rest/v1/user_profiles?select=*")
    suspend fun getUserProfiles(): Response<List<SupabaseUserProfileDto>>

    @Headers("Prefer: resolution=merge-duplicates, return=representation")
    @POST("rest/v1/user_profiles")
    suspend fun upsertUserProfile(@Body profile: SupabaseUserProfileDto): Response<List<SupabaseUserProfileDto>>

    // BOOKINGS
    @GET("rest/v1/bookings?select=*&order=created_at.desc")
    suspend fun getBookings(): Response<List<SupabaseBookingDto>>

    @GET("rest/v1/bookings?select=*")
    suspend fun getBookingById(@Query("id") idFilter: String): Response<List<SupabaseBookingDto>>

    @Headers("Prefer: return=representation")
    @POST("rest/v1/bookings")
    suspend fun insertBooking(@Body booking: SupabaseBookingDto): Response<List<SupabaseBookingDto>>

    @Headers("Prefer: return=representation")
    @PATCH("rest/v1/bookings")
    suspend fun updateBookingStatus(
        @Query("id") idFilter: String,
        @Body updates: Map<String, String>
    ): Response<List<SupabaseBookingDto>>

    @Headers("Prefer: return=representation")
    @PATCH("rest/v1/bookings")
    suspend fun updateBookingStatusByCode(
        @Query("booking_code") codeFilter: String,
        @Body updates: Map<String, String>
    ): Response<List<SupabaseBookingDto>>

    // SAVED ADDRESSES
    @GET("rest/v1/saved_addresses?select=*&order=created_at.desc")
    suspend fun getAddresses(): Response<List<SupabaseSavedAddressDto>>

    @Headers("Prefer: return=representation")
    @POST("rest/v1/saved_addresses")
    suspend fun insertAddress(@Body address: SupabaseSavedAddressDto): Response<List<SupabaseSavedAddressDto>>

    @DELETE("rest/v1/saved_addresses")
    suspend fun deleteAddress(@Query("id") idFilter: String): Response<Unit>

    // REVIEWS
    @GET("rest/v1/reviews?select=*&order=created_at.desc")
    suspend fun getReviews(): Response<List<SupabaseReviewDto>>

    @Headers("Prefer: return=representation")
    @POST("rest/v1/reviews")
    suspend fun insertReview(@Body review: SupabaseReviewDto): Response<List<SupabaseReviewDto>>

    // SERVICE CATEGORIES
    @GET("rest/v1/service_categories?select=*")
    suspend fun getServiceCategories(): Response<List<SupabaseServiceCategoryDto>>

    @Headers("Prefer: resolution=merge-duplicates, return=representation")
    @POST("rest/v1/service_categories")
    suspend fun upsertCategory(@Body category: SupabaseServiceCategoryDto): Response<List<SupabaseServiceCategoryDto>>
}
