package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.CustomerReview
import kotlinx.coroutines.flow.Flow

@Dao
interface ReviewDao {
    @Query("SELECT * FROM reviews ORDER BY createdAt DESC")
    fun getAllReviews(): Flow<List<CustomerReview>>

    @Query("SELECT * FROM reviews ORDER BY createdAt DESC")
    suspend fun getAllReviewsList(): List<CustomerReview>

    @Query("SELECT * FROM reviews WHERE serviceId = :serviceId ORDER BY createdAt DESC")
    fun getReviewsForService(serviceId: String): Flow<List<CustomerReview>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: CustomerReview): Long
}
