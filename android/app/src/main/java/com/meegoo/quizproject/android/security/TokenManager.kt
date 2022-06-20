package com.meegoo.quizproject.android.security

import android.content.Context
import android.content.SharedPreferences
import com.meegoo.quizproject.android.QuizApplication

object TokenManager {
    private val sharedPreferences: SharedPreferences =
        QuizApplication.appContext.getSharedPreferences("account", Context.MODE_PRIVATE)

    //TODO switch to keychain
    var authToken: String?
        get() =//TODO switch to keychain
            sharedPreferences.getString("auth_token", null)
        set(authToken) {
            sharedPreferences.edit().putString("auth_token", authToken).apply()
        }

    //TODO switch to keychain
    var refreshToken: String?
        get() =//TODO switch to keychain
            sharedPreferences.getString("refresh_token", null)
        set(authToken) {
            sharedPreferences.edit().putString("refresh_token", authToken).apply()
        }
}