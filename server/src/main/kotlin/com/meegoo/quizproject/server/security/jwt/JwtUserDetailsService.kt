package com.meegoo.quizproject.server.security.jwt

import com.meegoo.quizproject.server.controllers.hooks.UsernameTakenException
import com.meegoo.quizproject.server.data.entity.Account
import org.springframework.beans.factory.annotation.Autowired
import com.meegoo.quizproject.server.data.repositories.AccountRepository
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import kotlin.Throws
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Service
import java.lang.RuntimeException

@Service
class JwtUserDetailsService @Autowired constructor(
    private val accountRepository: AccountRepository,
    private val passwordEncoder: BCryptPasswordEncoder
) : UserDetailsService {
    @Throws(UsernameNotFoundException::class)
    override fun loadUserByUsername(username: String): Account {
        val account = accountRepository.findByUsername(username)
        if (account.isEmpty) {
            throw UsernameNotFoundException("Username $username not found")
        }
        return account.get()
    }

    fun register(username: String, password: String) {
        if (accountRepository.findByUsername(username).isPresent)
            throw UsernameTakenException()
        val user = Account(username, passwordEncoder.encode(password), arrayListOf(Account.Role.ROLE_USER.name))
        accountRepository.save(user)
    }

}