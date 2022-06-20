package com.meegoo.quizproject.server.data.repositories

import com.meegoo.quizproject.server.data.entity.Question
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface QuestionRepository : JpaRepository<Question, UUID>