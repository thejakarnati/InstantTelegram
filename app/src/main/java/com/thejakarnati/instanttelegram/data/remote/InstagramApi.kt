package com.thejakarnati.instanttelegram.data.remote

import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path
import retrofit2.http.Query

interface InstagramApi {
    @Headers(
        "User-Agent: Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36",
        "Accept: application/json",
        "X-IG-App-ID: 936619743392459"
    )
    @GET("{username}/?__a=1&__d=dis")
    suspend fun getProfilePageData(
        @Path("username") username: String
    ): InstagramPageResponse

    @Headers(
        "User-Agent: Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36",
        "Accept: application/json",
        "X-IG-App-ID: 936619743392459"
    )
    @GET("https://i.instagram.com/api/v1/users/web_profile_info/")
    suspend fun getWebProfileInfo(
        @Query("username") username: String
    ): InstagramWebProfileResponse
}
