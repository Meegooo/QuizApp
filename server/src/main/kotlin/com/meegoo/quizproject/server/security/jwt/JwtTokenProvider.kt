package com.meegoo.quizproject.server.security.jwt

import com.meegoo.quizproject.server.controllers.hooks.UnauthorizedException
import com.meegoo.quizproject.server.data.entity.Account
import com.meegoo.quizproject.server.data.entity.Session
import com.meegoo.quizproject.server.data.repositories.SessionRepository
import io.jsonwebtoken.*
import io.jsonwebtoken.security.Keys
import io.jsonwebtoken.security.SignatureException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.security.Key
import java.security.MessageDigest
import java.time.Duration
import java.util.*
import javax.servlet.http.HttpServletRequest

@Component
class JwtTokenProvider @Autowired constructor(
    @param:Qualifier("jwtUserDetailsService") private val userDetailsService: UserDetailsService,
    private val sessionRepository: SessionRepository
) {
    private val secret =
        "096C55A44C890ACD0B8E4FA32415C18E0B6ED1D213B454373593EF3677D32969DBA998C9C437E88C82536957E93BC675DB50647182A232EB4E6C30026504C43D"
    private var key: Key? = Keys.hmacShaKeyFor(secret.toByteArray())
    private val tokenLifetime: Long = 5
    private val refreshTokenLifetime: Long = 60 * 24 * 3

    val parser = Jwts.parserBuilder().setSigningKey(key).build()
//    @PostConstruct
//    protected fun init() {
//        key = Keys.hmacShaKeyFor(secret.toByteArray())
//    }

    fun createAuthToken(username: String): String {
        val claims = Jwts.claims().setSubject(username)
        claims["auth"] = "true"
        val now = Date()
        val validUntil = now.toInstant().plus(Duration.ofMinutes(tokenLifetime))
        return Jwts.builder()
            .setClaims(claims)
            .setIssuedAt(now)
            .setExpiration(Date.from(validUntil))
            .signWith(key, SignatureAlgorithm.HS512)
            .compact()
    }

    fun createRefreshToken(username: String, deviceId: String): String {
        val claims = Jwts.claims().setSubject(username)
        claims["refresh"] = "true"
        claims["device_id"] = deviceId
        val now = Date()
        val validUntil = now.toInstant().plus(Duration.ofMinutes(refreshTokenLifetime))

        return Jwts.builder()
            .setClaims(claims)
            .setIssuedAt(now)
            .setExpiration(Date.from(validUntil))
            .signWith(key, SignatureAlgorithm.HS512)
            .compact()
    }

    @Transactional
    fun saveRefreshToken(token: String, account: Account, deviceId: String) {
        val hash = hashToken(token)
        sessionRepository.deleteAllByAccountAndDeviceId(account, deviceId)
        sessionRepository.save(Session(account, hash, Date(), deviceId))

    }

    private fun hashToken(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(token.encodeToByteArray())
        return Base64.getEncoder().encodeToString(hashBytes)
    }

    fun parseAuthenticationToken(token: String): Authentication {
        try {
            val claims = parser.parseClaimsJws(token);
            if (claims.body["auth"] == "true") {
                val username = claims.body.subject
                val userDetails = userDetailsService.loadUserByUsername(username)
                return UsernamePasswordAuthenticationToken(userDetails, token, userDetails.authorities)
            }
            throw UnauthorizedException("Wrong token type")
        } catch (e: UnsupportedJwtException) {
            throw UnauthorizedException("Unsupported token")
        } catch (e: MalformedJwtException) {
            throw UnauthorizedException("Malformed token")
        } catch (e: SignatureException) {
            throw UnauthorizedException("Signature exception")
        } catch (e: ExpiredJwtException) {
            throw UnauthorizedException("Token Expired")
        }
    }


    fun parseRefreshToken(token: String): Session {
        try {
            val claims = parser.parseClaimsJws(token)
            if (claims.body["refresh"] == "true") {
                val hash = hashToken(token)
                return sessionRepository.getByToken(hash).orElseThrow { AccessDeniedException("Token Expired") }
            }
            throw AccessDeniedException("Wrong token type")
        } catch (e: UnsupportedJwtException) {
            throw AccessDeniedException("Unsupported token")
        } catch (e: MalformedJwtException) {
            throw AccessDeniedException("Malformed token")
        } catch (e: SignatureException) {
            throw AccessDeniedException("Signature exception")
        } catch (e: ExpiredJwtException) {
            throw AccessDeniedException("Token Expired")
        }
    }

    fun resolveToken(req: HttpServletRequest): String? {
        val bearerToken = req.getHeader("Authorization")
        return if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            bearerToken.substring(7)
        } else null
    }
}