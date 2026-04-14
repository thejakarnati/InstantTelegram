package com.thejakarnati.instanttelegram.domain

data class CreatorProfile(
    val username: String,
    val fullName: String,
    val biography: String,
    val profilePicUrl: String,
    val isFavorite: Boolean
)

data class FeedItem(
    val id: String,
    val username: String,
    val caption: String,
    val mediaUrl: String,
    val thumbnailUrl: String,
    val isVideo: Boolean,
    val permalink: String
)
