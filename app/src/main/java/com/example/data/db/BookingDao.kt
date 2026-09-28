package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Booking
import com.example.data.model.BookingStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface BookingDao {
    @Query("SELECT * FROM bookings ORDER BY createdAt DESC")
    fun getAllBookings(): Flow<List<Booking>>

    @Query("SELECT * FROM bookings ORDER BY createdAt DESC")
    suspend fun getAllBookingsList(): List<Booking>

    @Query("SELECT * FROM bookings WHERE bookingCode = :code LIMIT 1")
    suspend fun getBookingByCode(code: String): Booking?

    @Query("SELECT * FROM bookings WHERE id = :id LIMIT 1")
    fun getBookingById(id: Long): Flow<Booking?>

    @Query("SELECT * FROM bookings WHERE id = :id LIMIT 1")
    suspend fun getBookingByIdSync(id: Long): Booking?

    @Query("SELECT * FROM bookings WHERE status != 'COMPLETED' AND status != 'CANCELLED' ORDER BY createdAt DESC LIMIT 1")
    fun getActiveBooking(): Flow<Booking?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooking(booking: Booking): Long

    @Update
    suspend fun updateBooking(booking: Booking)

    @Query("UPDATE bookings SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: BookingStatus)

    @Query("UPDATE bookings SET status = :status, pendingSync = 1, localUpdatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatusWithPendingSync(id: Long, status: BookingStatus, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE bookings SET status = :status, cancellationReason = :reason, cancellationFeedback = :feedback, cancelledAt = :cancelledAt WHERE id = :id")
    suspend fun updateCancellation(id: Long, status: BookingStatus, reason: String?, feedback: String?, cancelledAt: Long)

    @Query("UPDATE bookings SET status = :status, cancellationReason = :reason, cancellationFeedback = :feedback, cancelledAt = :cancelledAt, pendingSync = 1, localUpdatedAt = :updatedAt WHERE id = :id")
    suspend fun updateCancellationWithPendingSync(id: Long, status: BookingStatus, reason: String?, feedback: String?, cancelledAt: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE bookings SET acceptedAt = :acceptedAt WHERE id = :id")
    suspend fun updateAcceptedAt(id: Long, acceptedAt: Long)

    @Query("UPDATE bookings SET status = 'ASSIGNED', acceptedAt = :acceptedAt WHERE id = :id")
    suspend fun markAccepted(id: Long, acceptedAt: Long)

    @Query("UPDATE bookings SET status = :status, cancellationReason = :reason, cancellationFeedback = :feedback, cancelledAt = :cancelledAt, cancelledBy = :cancelledBy WHERE id = :id")
    suspend fun updatePartnerCancellation(id: Long, status: BookingStatus, reason: String?, feedback: String?, cancelledAt: Long, cancelledBy: String?)

    @Query("UPDATE bookings SET status = :status, cancellationReason = :reason, cancellationFeedback = :feedback, cancelledAt = :cancelledAt, cancelledBy = :cancelledBy, pendingSync = 1, localUpdatedAt = :updatedAt WHERE id = :id")
    suspend fun updatePartnerCancellationWithPendingSync(id: Long, status: BookingStatus, reason: String?, feedback: String?, cancelledAt: Long, cancelledBy: String?, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM bookings WHERE id = :id")
    suspend fun deleteBooking(id: Long)
}
