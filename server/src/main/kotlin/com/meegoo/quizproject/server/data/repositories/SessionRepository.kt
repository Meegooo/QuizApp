package com.meegoo.quizproject.server.data.repositories

import com.meegoo.quizproject.server.data.entity.Account
import com.meegoo.quizproject.server.data.entity.Session
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface SessionRepository : JpaRepository<Session, UUID> {
    fun getByToken(token: String): Optional<Session>

    fun existsByTokenAndDeviceId(token: String, deviceId: String): Boolean

    fun deleteAllByAccountAndDeviceId(account: Account, deviceId: String)
}