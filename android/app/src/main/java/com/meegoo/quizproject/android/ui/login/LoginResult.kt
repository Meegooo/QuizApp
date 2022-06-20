package com.meegoo.quizproject.android.ui.login

/**
 * Authentication result : success (user details) or error message.
 */
class LoginResult {
    var success: Boolean = false
        private set
    var error: Int? = null
        private set

    constructor(error: Int?) {
        this.error = error
    }

    constructor() {
        this.success = true
    }
}