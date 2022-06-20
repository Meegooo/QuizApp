package com.meegoo.quizproject.server.data.repositories

import com.meegoo.quizproject.server.data.entity.Account
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface AccountRepository : JpaRepository<Account, UUID> {

    fun findByUsername(username: String): Optional<Account>

    fun existsByUsername(username: String): Boolean
}