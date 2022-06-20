package com.meegoo.quizproject.server.security

import com.meegoo.quizproject.server.security.acl.AclPermissionEvaluatorWithOwner
import com.meegoo.quizproject.server.security.acl.ssel.QuizMethodSecurityExpressionHandler
import com.meegoo.quizproject.server.security.jwt.JwtTokenFilter
import com.meegoo.quizproject.server.security.jwt.JwtTokenProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter


@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
class WebSecurityConfig(
    @param:Qualifier("jwtUserDetailsService") private val customUserDetailsService: UserDetailsService,
    private val jwtTokenProvider: JwtTokenProvider,
    private val passwordEncoder: PasswordEncoder
) : WebSecurityConfigurerAdapter() {
    @Autowired
    public override fun configure(auth: AuthenticationManagerBuilder) {
        auth.userDetailsService(customUserDetailsService)
            .passwordEncoder(passwordEncoder)
    }

    @Throws(Exception::class)
    public override fun configure(http: HttpSecurity) {

        http
            .httpBasic().disable()
            .csrf().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
//            .authorizeRequests()
//            .antMatchers("/api/v1/account/*").permitAll()
//            .anyRequest().authenticated()
//            .and()
            .addFilterAfter(JwtTokenFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter::class.java)
//            .apply(JwtConfigurer(jwtTokenProvider))
    }

    @Bean
    @Throws(Exception::class)
    override fun authenticationManagerBean(): AuthenticationManager {
        return super.authenticationManagerBean()
    }


}