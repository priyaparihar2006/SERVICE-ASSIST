package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM user_profiles LIMIT 1")
    fun getCurrentUser(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profiles LIMIT 1")
    suspend fun getCurrentUserSync(): UserProfile?

    @Query("SELECT * FROM user_profiles WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: String): UserProfile?

    @Query("SELECT * FROM user_profiles WHERE role = :role LIMIT 1")
    suspend fun getUserByRole(role: UserRole): UserProfile?

    @Query("SELECT * FROM user_profiles")
    suspend fun getAllUsers(): List<UserProfile>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserProfile)

    @Update
    suspend fun updateUser(user: UserProfile)

    @Query("UPDATE user_profiles SET role = :role")
    suspend fun updateRole(role: UserRole)

    @Query("UPDATE user_profiles SET city = :city, locality = :locality")
    suspend fun updateLocation(city: String, locality: String)

    @Query("DELETE FROM user_profiles")
    suspend fun clearUsers()
}

