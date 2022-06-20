package com.meegoo.quizproject.server.data.repositories

import com.meegoo.quizproject.server.data.entity.QuizAttempt
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface QuizAttemptRepository : JpaRepository<QuizAttempt, UUID> {

    fun findQuizAttemptsByQuizIdAndAccountId(quiz: UUID, account: UUID): List<QuizAttempt>
    fun existsByQuizIdAndAccountId(quiz: UUID, account: UUID): Boolean
}