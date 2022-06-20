package com.meegoo.quizproject.server.controllers

import com.fasterxml.jackson.annotation.JsonView
import com.meegoo.quizproject.server.data.JacksonView
import com.meegoo.quizproject.server.data.JacksonViewEnum
import com.meegoo.quizproject.server.data.dto.QuizAttemptDto
import com.meegoo.quizproject.server.data.services.QuizAttemptService
import org.springframework.http.converter.json.MappingJacksonValue
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/v1/attempt/")
class AttemptController(val service: QuizAttemptService) {

    @GetMapping("/quiz/{uuid}")
    @JsonView(JacksonView.Read::class)
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or " +
            "(hasAuthority('ROLE_USER') and hasPermission(#uuid, 'com.meegoo.quizproject.server.data.entity.Quiz', 'READ')) or " +
            "(hasAuthority('ROLE_USER') and hasPermissionThroughCourse(#uuid))")
    fun getQuizAttempts(@PathVariable uuid: UUID): List<QuizAttemptDto> {
        return service.getQuizAttempts(uuid).onEach { it.loadLevel = JacksonViewEnum.READ }
    }

    @GetMapping("/{uuid}")
    fun getAttempt(@PathVariable uuid: UUID, principal: Authentication): MappingJacksonValue {
        return service.getAttemptDetails(uuid)
    }


    @PutMapping("/{uuid}")
    @JsonView(JacksonView.Read::class)
    fun putQuizAttempt(
        @PathVariable uuid: UUID,
        @RequestBody submittedAttempt: QuizAttemptDto,
        principal: Authentication
    ): QuizAttemptDto {
        return service.modifyAttempt(uuid, submittedAttempt, principal).apply { loadLevel = JacksonViewEnum.READ }
    }


    @PostMapping("/quiz/{uuid}")
    @JsonView(JacksonView.Read::class)
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or " +
            "(hasAuthority('ROLE_USER') and hasPermission(#uuid, 'com.meegoo.quizproject.server.data.entity.Quiz', 'READ')) or " +
            "(hasAuthority('ROLE_USER') and hasPermissionThroughCourse(#uuid))")
    fun beginQuizAttempt(@PathVariable uuid: UUID, principal: Authentication): QuizAttemptDto {
        return service.beginQuizAttempt(uuid, principal).apply { loadLevel = JacksonViewEnum.READ }
    }

    @PostMapping("/{uuid}/end")
    @JsonView(JacksonView.Answers::class)
    fun endQuizAttempt(@PathVariable uuid: UUID): QuizAttemptDto {
        return service.endAttempt(uuid).apply { loadLevel = JacksonViewEnum.WRITE }
    }

}