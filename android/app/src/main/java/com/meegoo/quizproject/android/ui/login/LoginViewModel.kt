package com.meegoo.quizproject.android.ui.login

import android.util.Patterns
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.meegoo.quizproject.android.QuizApplication
import com.meegoo.quizproject.android.R
import com.meegoo.quizproject.android.data.LoginRepository
import java.net.ConnectException

class LoginViewModel : ViewModel() {
    val username = mutableStateOf("")
    val password = mutableStateOf("")
    val repeatPassword = mutableStateOf("")
    var loading = mutableStateOf(false)
    val loginResult = MutableLiveData<LoginResult?>()
    val error = mutableStateOf("")

    var registering = mutableStateOf(false)

    suspend fun login(username: String, password: String) {
        // can be launched in a separate asynchronous job
        loading.value = true
        val result = LoginRepository.login(username, password)
        if (result.first) {
            loginResult.postValue(LoginResult())
        } else {
            QuizApplication.showSortToast(result.second)
        }

        loading.value = false
    }

    suspend fun login() = login(username.value, password.value)

    fun onRegister() {
        registering.value = true
        username.value = ""
        password.value = ""
    }


    fun onLogin() {
        registering.value = false
        password.value = ""
    }

    suspend fun register() {
        loading.value = true
        val result = LoginRepository.register(username.value, password.value)
        if (result.first) {
            QuizApplication.showSortToast("Registration successful")
            onLogin()
        } else {
            QuizApplication.showSortToast(result.second)
        }

        loading.value = false
    }
}