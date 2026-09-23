package com.example.data.remote.supabase

import android.util.Log
import com.example.data.db.AddressDao
import com.example.data.db.BookingDao
import com.example.data.db.ReviewDao
import com.example.data.db.UserDao
import com.example.data.model.Booking
import com.example.data.model.BookingStatus
import com.example.data.model.CustomerReview
import com.example.data.model.SavedAddress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response

private fun <T> Response<T>.logIfFailed(tag: String, action: String) {
    if (!isSuccessful) {
        val err = errorBody()?.string()?.take(500) ?: "no error body"
        Log.w(tag, "$action failed HTTP ${code()}: $err")
    }
}

data class SupabaseSyncState(
    val isSyncing: Boolean = false,
    val isConnected: Boolean = false,
    val lastSyncTime: Long? = null,
    val lastMessage: String = "Ready",
    val error: String? = null
)

class SupabaseSyncManager(
    private val bookingDao: BookingDao,
    private val addressDao: AddressDao,
    private val reviewDao: ReviewDao,
    private val userDao: UserDao
) {
    private val TAG = "SupabaseSyncManager"

    private val _syncState = MutableStateFlow(
        SupabaseSyncState(
            isConnected = SupabaseConfig.isConfigured,
            lastMessage = if (SupabaseConfig.isConfigured) "Connected to Supabase" else "Supabase not configured"
        )
    )
    val syncState: StateFlow<SupabaseSyncState> = _syncState.asStateFlow()

    /**
     * Performs a full two-way synchronization between local Room DB and Supabase
     */
    suspend fun performFullSync(): Boolean = withContext(Dispatchers.IO) {
        if (!SupabaseConfig.isConfigured) {
            _syncState.value = _syncState.value.copy(
                isSyncing = false,
                lastMessage = "Supabase not configured in .env",
                error = "Missing SUPABASE_URL or SUPABASE_ANON_KEY"
            )
            return@withContext false
        }

        val api = SupabaseClient.apiService ?: run {
            _syncState.value = _syncState.value.copy(
                isSyncing = false,
                lastMessage = "Failed to initialize Supabase HTTP client",
                error = "Client initialization failed"
            )
            return@withContext false
        }

        _syncState.value = _syncState.value.copy(isSyncing = true, error = null)

        try {
            // 1. PUSH User profile
            val currentUser = userDao.getCurrentUserSync()
            if (currentUser != null) {
                try {
                    val resp = api.upsertUserProfile(profile = currentUser.toSupabaseDto())
                    resp.logIfFailed(TAG, "upsertUserProfile")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to upsert user profile: ${e.message}")
                }
            }

            // 2. SYNC BOOKINGS (Two-way)
            try {
                val remoteBookingsResponse = api.getBookings()
                remoteBookingsResponse.logIfFailed(TAG, "getBookings")
                if (remoteBookingsResponse.isSuccessful) {
                    val remoteList = remoteBookingsResponse.body() ?: emptyList()
                    // Merge remote bookings into local Room DB
                    remoteList.forEach { remoteDto ->
                        val existing = bookingDao.getBookingByCode(remoteDto.bookingCode)
                        if (existing == null) {
                            bookingDao.insertBooking(remoteDto.toDomainBooking())
                        } else {
                            val remoteStatus = try {
                                BookingStatus.valueOf(remoteDto.status)
                            } catch (e: Exception) {
                                existing.status
                            }
                            if (existing.status != remoteStatus) {
                                bookingDao.updateStatus(existing.id, remoteStatus)
                            }
                        }
                    }

                    // Push any local bookings that don't exist remotely
                    val remoteCodes = remoteList.map { it.bookingCode }.toSet()
                    val localList = bookingDao.getAllBookingsList()
                    val bookingApi = SupabaseClient.bookingApiService
                    localList.forEach { local ->
                        if (!remoteCodes.contains(local.bookingCode)) {
                            try {
                                val pushResp = bookingApi?.createBooking(local.toSupabaseDto())
                                pushResp?.logIfFailed(TAG, "pushLocalBooking (${local.bookingCode})")
                            } catch (e: Exception) {
                                Log.w(TAG, "Could not push local booking ${local.bookingCode}: ${e.message}")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Booking sync failed: ${e.message}")
            }

            // 3. SYNC REVIEWS
            try {
                val remoteReviewsResponse = api.getReviews()
                remoteReviewsResponse.logIfFailed(TAG, "getReviews")
                if (remoteReviewsResponse.isSuccessful) {
                    val remoteReviews = remoteReviewsResponse.body() ?: emptyList()
                    val localReviews = reviewDao.getAllReviewsList()
                    val localComments = localReviews.map { it.comment }.toSet()

                    remoteReviews.forEach { remoteDto ->
                        if (!localComments.contains(remoteDto.comment)) {
                            reviewDao.insertReview(remoteDto.toDomainReview())
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Review sync failed: ${e.message}")
            }

            // 4. SYNC ADDRESSES
            try {
                val remoteAddressesResponse = api.getAddresses()
                remoteAddressesResponse.logIfFailed(TAG, "getAddresses")
                if (remoteAddressesResponse.isSuccessful) {
                    val remoteAddresses = remoteAddressesResponse.body() ?: emptyList()
                    val localAddresses = addressDao.getAllAddressesList()
                    val localAddrs = localAddresses.map { it.fullAddress }.toSet()

                    remoteAddresses.forEach { remoteDto ->
                        if (!localAddrs.contains(remoteDto.fullAddress)) {
                            addressDao.insertAddress(remoteDto.toDomainAddress())
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Address sync failed: ${e.message}")
            }

            // 5. SYNC SERVICE CATEGORIES
            try {
                val remoteCategoriesResponse = api.getServiceCategories()
                remoteCategoriesResponse.logIfFailed(TAG, "getServiceCategories")
                if (remoteCategoriesResponse.isSuccessful) {
                    val remoteCats = remoteCategoriesResponse.body() ?: emptyList()
                    if (remoteCats.isNotEmpty()) {
                        _remoteCategories.value = remoteCats.map { it.toDomainCategory() }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Category sync failed: ${e.message}")
            }

            _syncState.value = SupabaseSyncState(
                isSyncing = false,
                isConnected = true,
                lastSyncTime = System.currentTimeMillis(),
                lastMessage = "Synced with Supabase at ${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}",
                error = null
            )
            true
        } catch (e: Exception) {
            Log.e(TAG, "Sync exception: ${e.message}", e)
            _syncState.value = _syncState.value.copy(
                isSyncing = false,
                isConnected = false,
                error = e.message ?: "Unknown sync error",
                lastMessage = "Sync failed: ${e.localizedMessage}"
            )
            false
        }
    }

    private val _remoteCategories = MutableStateFlow<List<com.example.data.model.ServiceCategory>>(emptyList())
    val remoteCategories: StateFlow<List<com.example.data.model.ServiceCategory>> = _remoteCategories.asStateFlow()

    /**
     * Synchronous/awaited update booking status on Supabase via Edge Function
     */
    suspend fun updateBookingStatus(
        bookingCode: String,
        status: BookingStatus,
        reason: String? = null,
        feedback: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val bookingApi = SupabaseClient.bookingApiService ?: return@withContext false
            val resp = bookingApi.updateBookingStatus(
                com.example.data.remote.booking.BookingUpdateStatusRequest(
                    bookingCode = bookingCode,
                    status = status.name,
                    cancellationReason = reason,
                    cancellationFeedback = feedback
                )
            )
            val success = resp.isSuccessful
            if (!success) {
                resp.logIfFailed(TAG, "updateBookingStatus ($bookingCode)")
            }
            success
        } catch (e: Exception) {
            Log.w(TAG, "Failed update booking status ($bookingCode): ${e.message}")
            false
        }
    }

    /**
     * Light-weight single booking refresh from Supabase (for live customer tracking)
     */
    suspend fun refreshBookingStatus(bookingId: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            val api = SupabaseClient.apiService ?: return@withContext false
            val resp = api.getBookingById(idFilter = "eq.$bookingId")
            if (resp.isSuccessful && !resp.body().isNullOrEmpty()) {
                val remoteDto = resp.body()!!.first()
                val existing = bookingDao.getBookingByIdSync(bookingId)
                if (existing != null) {
                    val remoteStatus = try {
                        BookingStatus.valueOf(remoteDto.status)
                    } catch (e: Exception) {
                        existing.status
                    }
                    val updated = existing.copy(
                        status = remoteStatus,
                        isPaid = remoteDto.isPaid,
                        paymentMethod = remoteDto.paymentMethod,
                        paymentReference = remoteDto.paymentReference,
                        paidAt = remoteDto.paidAt
                    )
                    bookingDao.updateBooking(updated)
                } else {
                    bookingDao.insertBooking(remoteDto.toDomainBooking())
                }
                true
            } else {
                resp.logIfFailed(TAG, "refreshBookingStatus ($bookingId)")
                false
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to refresh booking status ($bookingId): ${e.message}")
            false
        }
    }

    /**
     * Synchronous/awaited push on booking creation via Edge Function
     */
    suspend fun pushBooking(booking: Booking): Boolean = withContext(Dispatchers.IO) {
        try {
            val bookingApi = SupabaseClient.bookingApiService ?: return@withContext false
            val resp = bookingApi.createBooking(booking.toSupabaseDto())
            val success = resp.isSuccessful
            if (!success) {
                resp.logIfFailed(TAG, "pushBooking (${booking.bookingCode})")
            }
            success
        } catch (e: Exception) {
            Log.w(TAG, "Failed push booking (${booking.bookingCode}): ${e.message}")
            false
        }
    }

    /**
     * Async push on booking creation
     */
    fun pushBookingAsync(booking: Booking, scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            pushBooking(booking)
        }
    }

    /**
     * Async push on address creation
     */
    fun pushAddressAsync(address: SavedAddress, scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            try {
                val resp = SupabaseClient.apiService?.insertAddress(address.toSupabaseDto())
                resp?.logIfFailed(TAG, "pushAddressAsync")
            } catch (e: Exception) {
                Log.w(TAG, "Failed async push address: ${e.message}")
            }
        }
    }

    /**
     * Async push on review creation
     */
    fun pushReviewAsync(review: CustomerReview, scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            try {
                val resp = SupabaseClient.apiService?.insertReview(review.toSupabaseDto())
                resp?.logIfFailed(TAG, "pushReviewAsync")
            } catch (e: Exception) {
                Log.w(TAG, "Failed async push review: ${e.message}")
            }
        }
    }

    /**
     * Async push on user profile update
     */
    fun pushUserProfileAsync(user: com.example.data.model.UserProfile, scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            try {
                val resp = SupabaseClient.apiService?.upsertUserProfile(user.toSupabaseDto())
                resp?.logIfFailed(TAG, "pushUserProfileAsync")
            } catch (e: Exception) {
                Log.w(TAG, "Failed async push user profile: ${e.message}")
            }
        }
    }

    /**
     * Async delete on address removal
     */
    fun deleteAddressAsync(id: Long, scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            try {
                val resp = SupabaseClient.apiService?.deleteAddress("eq.$id")
                resp?.logIfFailed(TAG, "deleteAddressAsync ($id)")
            } catch (e: Exception) {
                Log.w(TAG, "Failed async delete address: ${e.message}")
            }
        }
    }
}
