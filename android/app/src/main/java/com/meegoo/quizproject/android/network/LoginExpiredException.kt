package com.meegoo.quizproject.android.network

import okio.IOException
import java.lang.RuntimeException

class LoginExpiredException: IOException {
    override val message: String?
    constructor() : super() {
        message = null
    }
    constructor(message: String) : super() {
        this.message = message;
    }
}