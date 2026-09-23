package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.SavedAddress
import kotlinx.coroutines.flow.Flow

@Dao
interface AddressDao {
    @Query("SELECT * FROM saved_addresses ORDER BY isDefault DESC, id DESC")
    fun getAllAddresses(): Flow<List<SavedAddress>>

    @Query("SELECT * FROM saved_addresses ORDER BY isDefault DESC, id DESC")
    suspend fun getAllAddressesList(): List<SavedAddress>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAddress(address: SavedAddress): Long

    @Update
    suspend fun updateAddress(address: SavedAddress)

    @Query("UPDATE saved_addresses SET isDefault = 0")
    suspend fun clearDefaults()

    @Query("UPDATE saved_addresses SET isDefault = 1 WHERE id = :id")
    suspend fun setDefaultAddress(id: Long)

    @Query("DELETE FROM saved_addresses WHERE id = :id")
    suspend fun deleteAddress(id: Long)
}
