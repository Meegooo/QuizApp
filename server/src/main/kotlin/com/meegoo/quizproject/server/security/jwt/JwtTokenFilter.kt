package com.meegoo.quizproject.server.security.jwt

import com.meegoo.quizproject.server.controllers.hooks.UnauthorizedException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.util.matcher.AntPathRequestMatcher
import org.springframework.security.web.util.matcher.RequestMatcher
import org.springframework.web.filter.GenericFilterBean
import org.springframework.web.filter.OncePerRequestFilter
import java.io.PrintWriter
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


class JwtTokenFilter(private val jwtTokenProvider: JwtTokenProvider) : OncePerRequestFilter() {
    var customFilterUrl: RequestMatcher = AntPathRequestMatcher("/api/v1/account/**")

    override fun doFilterInternal(req: HttpServletRequest, res: HttpServletResponse, filterChain: FilterChain) {
        if (customFilterUrl.matches(req)) {
            filterChain.doFilter(req, res)
        } else {
            val token = jwtTokenProvider.resolveToken(req)
            try {
                if (token != null) {
                    val auth = jwtTokenProvider.parseAuthenticationToken(token)
                    SecurityContextHolder.getContext().authentication = auth
                    filterChain.doFilter(req, res)
                } else {
                    throw UnauthorizedException("Invalid authorization header")
                }
            } catch (e: UnauthorizedException) {
                res.sendError(HttpServletResponse.SC_UNAUTHORIZED, e.message)
                res.writer.println(e.message)

            }
        }
    }
}