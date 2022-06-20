package com.meegoo.quizproject.server.data.repositories

import com.meegoo.quizproject.server.data.entity.Quiz
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface QuizRepository : JpaRepository<Quiz, UUID>