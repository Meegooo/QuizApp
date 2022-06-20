package com.meegoo.quizproject.android.network.interceptors

import com.meegoo.quizproject.android.security.TokenManager
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

object AccessTokenInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val authToken: String = TokenManager.authToken!!
        val request: Request = chain.request().newBuilder().addHeader("Authorization", "Bearer $authToken").build()
        return chain.proceed(request)
    }
}