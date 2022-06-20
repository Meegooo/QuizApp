package com.meegoo.quizproject.server.controllers

import com.fasterxml.jackson.annotation.JsonView
import com.meegoo.quizproject.server.data.JacksonView
import com.meegoo.quizproject.server.data.JacksonViewEnum
import com.meegoo.quizproject.server.data.dto.QuizDto
import com.meegoo.quizproject.server.data.entity.Quiz
import com.meegoo.quizproject.server.data.services.QuizService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.security.Principal
import java.util.*

@RestController
@RequestMapping("/api/v1/quiz")
class QuizController(private val quizService: QuizService) {

    @GetMapping("/{uuid}")
    @JsonView(JacksonView.Read::class)
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or " +
            "(hasAuthority('ROLE_USER') and hasPermission(#uuid, 'com.meegoo.quizproject.server.data.entity.Quiz', 'READ')) or " +
            "(hasAuthority('ROLE_USER') and hasPermissionThroughCourse(#uuid))"
    )
    fun getQuiz(@PathVariable uuid: UUID, principal: Principal): QuizDto {
        return quizService.getOne(uuid).apply { loadLevel = JacksonViewEnum.READ }
    }

    @GetMapping("/{uuid}", params = ["editable"])
    @JsonView(JacksonView.Write::class)
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or " +
            "(hasAuthority('ROLE_USER') and hasPermission(#uuid, 'com.meegoo.quizproject.server.data.entity.Quiz', 'WRITE'))"
    )
    fun getQuizEditable(@RequestParam("editable") overview: Any, @PathVariable uuid: UUID, principal: Principal): QuizDto {
        return quizService.getOne(uuid).apply { loadLevel = JacksonViewEnum.WRITE }
    }

    @GetMapping
    @JsonView(JacksonView.Overview::class)
    fun getAllQuizzes(): List<QuizDto> {
        return quizService.getAllQuizzes().onEach { it.loadLevel = JacksonViewEnum.OVERVIEW}
    }

    @PostMapping
    @JsonView(JacksonView.Read::class)
    fun createQuiz(@RequestParam("name") name: String): QuizDto {
        return quizService.createNewQuiz(name).apply { loadLevel = JacksonViewEnum.READ }
    }

    @PutMapping("/{uuid}")
    @JsonView(JacksonView.Write::class)
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or " +
            "(hasAuthority('ROLE_USER') and hasPermission(#uuid, 'com.meegoo.quizproject.server.data.entity.Quiz', 'WRITE'))")
    fun updateQuiz(@PathVariable uuid: UUID, @RequestBody quiz: QuizDto): QuizDto {
        quiz.id = uuid
        return quizService.updateQuiz(quiz).apply { loadLevel = JacksonViewEnum.WRITE }
    }

    @PostMapping("/{uuid}")
    @JsonView(JacksonView.Write::class)
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or " +
            "(hasAuthority('ROLE_USER') and hasPermission(#uuid, 'com.meegoo.quizproject.server.data.entity.Quiz', 'WRITE'))")
    fun publishQuiz(@PathVariable uuid: UUID): QuizDto {
        return quizService.publishQuiz(uuid).apply { loadLevel = JacksonViewEnum.WRITE }
    }

    @DeleteMapping("/{uuid}")
    @JsonView(JacksonView.Write::class)
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or " +
            "(hasAuthority('ROLE_USER') and hasPermission(#uuid, 'com.meegoo.quizproject.server.data.entity.Quiz', 'WRITE'))")
    fun deleteQuiz(@PathVariable uuid: UUID) {
        return quizService.deleteQuiz(uuid)
    }

    @GetMapping("/{uuid}/export")
    @JsonView(JacksonView.Write::class)
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or " +
            "(hasAuthority('ROLE_USER') and hasPermission(#uuid, 'com.meegoo.quizproject.server.data.entity.Quiz', 'WRITE'))")
    fun exportQuiz(@PathVariable uuid: UUID): QuizDto {
        return quizService.exportQuiz(uuid)
    }

    @PostMapping("/import")
    @JsonView(JacksonView.Write::class)
    fun importQuiz(@RequestBody quiz: QuizDto): QuizDto {
        return quizService.importQuiz(quiz)
    }
}