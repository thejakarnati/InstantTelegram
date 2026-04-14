package com.thejakarnati.instanttelegram.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteProfileDao {
    @Query("SELECT * FROM favorite_profiles ORDER BY username ASC")
    fun observeFavorites(): Flow<List<FavoriteProfileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: FavoriteProfileEntity)

    @Query("DELETE FROM favorite_profiles WHERE username = :username")
    suspend fun deleteByUsername(username: String)
}
