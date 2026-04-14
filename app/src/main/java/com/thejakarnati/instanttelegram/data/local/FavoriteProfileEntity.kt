package com.thejakarnati.instanttelegram.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_profiles")
data class FavoriteProfileEntity(
    @PrimaryKey val username: String,
    val displayName: String,
    val profilePicUrl: String
)
