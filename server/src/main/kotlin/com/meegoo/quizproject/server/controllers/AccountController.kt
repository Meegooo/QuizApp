package com.meegoo.quizproject.server.controllers

import com.meegoo.quizproject.server.data.repositories.AccountRepository
import com.meegoo.quizproject.server.data.repositories.SessionRepository
import com.meegoo.quizproject.server.security.jwt.JwtTokenProvider
import com.meegoo.quizproject.server.security.jwt.JwtUserDetailsService
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/v1/account")
class AccountController(
    private val authenticationManager: AuthenticationManager,
    private val tokenProvider: JwtTokenProvider,
    private val userDetailsService: JwtUserDetailsService,
    private val accountRepository: AccountRepository,
    private val sessionRepository: SessionRepository
) {

    @PostMapping("/auth")
    fun authorize(
        @RequestParam("username") username: String,
        @RequestParam("password") password: String,
        @RequestParam("device_id") deviceId: String
    ): ResponseEntity<MutableMap<Any, Any>> {

        val account = userDetailsService.loadUserByUsername(username)
        authenticationManager.authenticate(UsernamePasswordAuthenticationToken(username, password))
        val authToken = tokenProvider.createAuthToken(account.username)
        val refreshToken = tokenProvider.createRefreshToken(account.username, deviceId)
        tokenProvider.saveRefreshToken(refreshToken, account, deviceId)

        val response: MutableMap<Any, Any> = HashMap()
        response["token"] = authToken
        response["refresh_token"] = refreshToken
        response["uuid"] = account.id
        response["username"] = account.username
        return ResponseEntity.ok(response)
    }

    @PostMapping("/refresh")
    fun refreshToken(@RequestParam("token") token: String): ResponseEntity<MutableMap<Any, Any>> {
        val response = synchronized(token) {
            println("Refreshing $token")
            val session = tokenProvider.parseRefreshToken(token)
            val authToken = tokenProvider.createAuthToken(session.account.username)
            val refreshToken = tokenProvider.createRefreshToken(session.account.username, session.deviceId)
            tokenProvider.saveRefreshToken(refreshToken, session.account, session.deviceId)

            val response: MutableMap<Any, Any> = HashMap()
            response["token"] = authToken
            response["refresh_token"] = refreshToken
            response["uuid"] = session.account.id
            response["username"] = session.account.username
            response
        }
        return ResponseEntity.ok(response)
    }

    @PostMapping("/register")
    fun register(
        @RequestParam("username") username: String,
        @RequestParam("password") password: String
    ): ResponseEntity<*> {
        userDetailsService.register(username, password)
        return ResponseEntity.ok(null)
    }

    @GetMapping("/check_available")
    fun checkUserExists(@RequestParam("username") username: String): Boolean {
        return accountRepository.existsByUsername(username)
    }

}