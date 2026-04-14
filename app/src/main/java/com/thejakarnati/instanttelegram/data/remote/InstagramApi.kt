package com.thejakarnati.instanttelegram.data.remote

import retrofit2.http.GET
import retrofit2.http.Path

interface InstagramApi {
    @GET("{username}/?__a=1&__d=dis")
    suspend fun getProfilePageData(
        @Path("username") username: String
    ): InstagramPageResponse
}
