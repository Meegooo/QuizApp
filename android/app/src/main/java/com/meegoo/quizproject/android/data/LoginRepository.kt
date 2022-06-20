package com.meegoo.quizproject.android.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.meegoo.quizproject.android.QuizApplication
import com.meegoo.quizproject.android.R
import com.meegoo.quizproject.android.network.api.AccountService
import com.meegoo.quizproject.android.network.api.RequestController
import com.meegoo.quizproject.android.security.TokenManager
import java.util.*


object LoginRepository {
    private const val TAG = "LoginRepository"

    // If user credentials will be cached in local storage, it is recommended it be encrypted
    // @see https://developer.android.com/training/articles/keystore
    private var userId: UUID? = null
    private val accountService: AccountService
    private val preferences: SharedPreferences
    private val deviceId: String?
    private val applicationContext: Context = QuizApplication.appContext

    var username: String? = null

    fun logout(context: Context) {
        userId = null
        username = null
        tokenExpired()
        QuizApplication.startLoginActivity(context)
    }

    fun tokenExpired() {
        TokenManager.authToken = ""
        TokenManager.refreshToken = ""
    }

    suspend fun login(username: String, password: String): Pair<Boolean, Int> {
        // handle login
        this.username = null
        this.userId = null
        val authenticate = accountService.authenticate(username, password, deviceId)
        return if (authenticate.isSuccessful) {
            TokenManager.authToken = authenticate.body()?.token
            TokenManager.refreshToken = authenticate.body()?.refreshToken
            userId = authenticate.body()?.userId!!
            this.username = authenticate.body()?.displayName
            this.userId = authenticate.body()?.userId
            true to 0
        } else if (authenticate.code() == 408) {
            false to R.string.connection_timeout
        } else {
            false to R.string.login_failed
        }
    }

    suspend fun register(username: String, password: String): Pair<Boolean, Int> {
        // handle login
        this.username = null
        this.userId = null
        val register = accountService.register(username, password)
        return if (register.isSuccessful) {
            true to 0
        } else if (register.code() == 408) {
            false to R.string.connection_timeout
        } else if(register.errorBody()?.string()?.contains("Username is taken") == true) {
            false to R.string.username_taken
        } else {
            false to R.string.register_failed
        }
    }

    suspend fun refreshTokens(): Boolean {
        this.username = null
        this.userId = null
        val response = accountService.refreshAuthToken(TokenManager.refreshToken)
        return if (response.isSuccessful && response.body() != null) {
            TokenManager.authToken = response.body()!!.token
            TokenManager.refreshToken = response.body()!!.refreshToken
            this.username = response.body()?.displayName
            this.userId = response.body()?.userId
            true
        } else {
            tokenExpired()
            false
        }
    }

    suspend fun checkLogin(): Boolean {
        return if (TokenManager.refreshToken == null) {
            Log.d(TAG, "checkLogin: token is null")
            false
        } else {
            Log.d(TAG, "checkLogin: refreshing token")
            refreshTokens()
        }
    }

    // private constructor : singleton access
    init {
        preferences = applicationContext.getSharedPreferences("account", Context.MODE_PRIVATE)
        accountService = RequestController.accountService
        if (!preferences.contains("device_id")) {
            preferences.edit().putString("device_id", UUID.randomUUID().toString()).apply()
        }
        deviceId = preferences.getString("device_id", null)
    }
}