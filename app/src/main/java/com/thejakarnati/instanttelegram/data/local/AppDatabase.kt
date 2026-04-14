package com.thejakarnati.instanttelegram.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [FavoriteProfileEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteProfileDao(): FavoriteProfileDao
}
