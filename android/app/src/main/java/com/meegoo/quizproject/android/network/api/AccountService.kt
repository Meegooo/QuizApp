package com.meegoo.quizproject.android.network.api

import com.meegoo.quizproject.android.data.dto.Account
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface AccountService {
    @POST("api/v1/account/auth")
    suspend fun authenticate(@Query("username") username: String,
                     @Query("password") password: String,
                     @Query("device_id") deviceId: String?): Response<Account>

    @POST("api/v1/account/refresh")
    suspend fun refreshAuthToken(@Query("token") token: String?): Response<Account>

    @POST("api/v1/account/register")
    suspend fun register(@Query("username") username: String, @Query("password") password: String): Response<Unit>
}