package com.meegoo.quizproject.android.network.interceptors

import android.util.Log
import android.widget.Toast
import androidx.compose.material.ExperimentalMaterialApi
import com.google.accompanist.pager.ExperimentalPagerApi
import com.meegoo.quizproject.android.QuizApplication
import com.meegoo.quizproject.android.R
import com.meegoo.quizproject.android.data.LoginRepository
import com.meegoo.quizproject.android.network.LoginExpiredException
import com.meegoo.quizproject.android.network.api.AccountService
import com.meegoo.quizproject.android.security.TokenManager
import kotlinx.coroutines.*
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

import android.os.ConditionVariable

object RefreshTokenInterceptor : Interceptor {


    private val LOCK = ConditionVariable(true)
    private val mIsRefreshing = AtomicBoolean(false)

    override fun intercept(chain: Interceptor.Chain): Response {
        val request: Request = chain.request()
        // try the request
        val response: Response = chain.proceed(request)
        if (response.code == 401) {
            response.close()


            if (mIsRefreshing.compareAndSet(false, true)) {
                LOCK.close()
                val refreshSuccessful = runBlocking {
                    LoginRepository.refreshTokens()
                }
                LOCK.open();
                mIsRefreshing.set(false);

                if (refreshSuccessful) {
                    val newRequest = request.newBuilder()
                        .removeHeader("Authorization")
                        .addHeader("Authorization", String.format("Bearer %s", TokenManager.authToken))
                        .build()
                    return chain.proceed(newRequest)
                } else {
                    MainScope().launch {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(QuizApplication.appContext,R.string.login_expired,Toast.LENGTH_LONG).show()
                        }
                        QuizApplication.startLoginActivity()
                    }
                    LoginRepository.tokenExpired()
                }

            } else {
                val conditionOpened = LOCK.block(5000)
                if (conditionOpened) {
                    val newRequest = request.newBuilder()
                        .removeHeader("Authorization")
                        .addHeader("Authorization", String.format("Bearer %s", TokenManager.authToken))
                        .build()
                    return chain.proceed(newRequest)
                } else {
                    MainScope().launch {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(QuizApplication.appContext,R.string.login_expired,Toast.LENGTH_LONG).show()
                        }
                        QuizApplication.startLoginActivity()
                    }
                    LoginRepository.tokenExpired()
                }
            }
        }
        return response
    }
}