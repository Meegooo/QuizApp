package com.meegoo.quizproject.server.controllers

import com.meegoo.quizproject.server.data.entity.Question
import com.meegoo.quizproject.server.data.repositories.QuestionRepository
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.security.Principal
import java.util.*

@RestController
@RequestMapping("/api/v1/question/")
class QuestionController(private val questionRepository: QuestionRepository) {

    @GetMapping("/{uuid}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or " +
            "(hasAuthority('ROLE_USER') and hasPermission(#uuid, 'com.meegoo.quizproject.server.data.entity.Question', 'WRITE')) "
    )
    fun getQuestion(@PathVariable uuid: UUID, principal: Principal): Question {
        return questionRepository.getOne(uuid)
    }
}