package com.meegoo.quizproject.android.data.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.*

/**
 * Data class that captures user information for logged in users retrieved from LoginRepository
 */
class Account {
    @JsonProperty("uuid")
    var userId: UUID? = null

    @JsonProperty("username")
    var displayName: String? = null

    @JsonProperty("token")
    var token: String? = null

    @JsonProperty("refresh_token")
    var refreshToken: String? = null

    constructor(token: String?, refreshToken: String?) {
        this.token = token
        this.refreshToken = refreshToken
    }

    constructor(userId: UUID?, displayName: String?, token: String?, refreshToken: String?) {
        this.userId = userId
        this.displayName = displayName
        this.token = token
        this.refreshToken = refreshToken
    }

    constructor()
}