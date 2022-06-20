package com.meegoo.quizproject.android.network.interceptors

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object ErrorInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        return try {
            chain.proceed(request)
        } catch (e: SocketTimeoutException) {
            Response.Builder().code(408).request(request)
                .body("Timeout".toResponseBody())
                .message("Timeout").protocol(Protocol.HTTP_1_1).build()
        } catch (e: SocketException) {
            Log.e("Connection/Retrofit", "Socket exception", e);
            Response.Builder().code(491).request(request)
                .body("Connection error".toResponseBody())
                .message(e.message.orEmpty())
                .protocol(Protocol.HTTP_1_1).build()
        } catch (e: UnknownHostException) {
            Log.e("Connection/Retrofit", "Unknown host (no internet?)", e);
            Response.Builder().code(492).request(request)
                .body("Unknown host (no internet?)".toResponseBody())
                .message(e.message.orEmpty())
                .protocol(Protocol.HTTP_1_1).build()
        }
    }
}