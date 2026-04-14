package com.thejakarnati.instanttelegram.data.remote

import retrofit2.http.GET
import retrofit2.http.Path

interface BridgeApi {
    @GET("api/v1/instagram/{username}")
    suspend fun getCreatorFeed(
        @Path("username") username: String
    ): BridgeProfileResponse
}

data class BridgeProfileResponse(
    val profile: BridgeProfileDto?,
    val items: List<BridgeFeedItemDto>?
)

data class BridgeProfileDto(
    val username: String?,
    val fullName: String?,
    val biography: String?,
    val profilePicUrl: String?
)

data class BridgeFeedItemDto(
    val id: String?,
    val username: String?,
    val caption: String?,
    val mediaUrl: String?,
    val thumbnailUrl: String?,
    val isVideo: Boolean?,
    val permalink: String?
)
