package com.meegoo.quizproject.server.data.services

import com.meegoo.quizproject.server.controllers.hooks.BadRequestException
import com.meegoo.quizproject.server.controllers.hooks.NotFoundException
import com.meegoo.quizproject.server.data.JacksonView
import com.meegoo.quizproject.server.data.JacksonViewEnum
import com.meegoo.quizproject.server.data.QuestionType
import com.meegoo.quizproject.server.data.dto.QuizAttemptDto
import com.meegoo.quizproject.server.data.dto.SystemAnswerDto
import com.meegoo.quizproject.server.data.dto.UserAnswerDto
import com.meegoo.quizproject.server.data.entity.Account
import com.meegoo.quizproject.server.data.entity.Question
import com.meegoo.quizproject.server.data.entity.QuizAttempt
import com.meegoo.quizproject.server.data.repositories.QuizAttemptRepository
import com.meegoo.quizproject.server.data.repositories.QuizRepository
import org.springframework.http.converter.json.MappingJacksonValue
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.access.PermissionEvaluator
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.util.*
import kotlin.collections.HashMap
import kotlin.concurrent.schedule
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow

@Service
class QuizAttemptService(
    private val attemptRepository: QuizAttemptRepository,
    private val quizRepository: QuizRepository,
    private val permissionEvaluator: PermissionEvaluator
) {

    fun getQuizAttempts(uuid: UUID): List<QuizAttemptDto> {
        if (quizRepository.existsById(uuid)) {
            val principal = SecurityContextHolder.getContext().authentication
            val attempts =
                attemptRepository.findQuizAttemptsByQuizIdAndAccountId(uuid, (principal.principal as Account).id)
            return attempts.map { QuizAttemptDto(it) }
        } else throw NotFoundException("Quiz", uuid)
    }

    @Transactional
    fun modifyAttempt(attemptUuid: UUID, submittedAttempt: QuizAttemptDto, principal: Authentication): QuizAttemptDto {
        val storedAttempt = findAttemptById(attemptUuid)
        authorizeAttemptModification(storedAttempt, principal)
        if (storedAttempt.isExpired()) {
            throw AccessDeniedException("Attempt is closed")
        }

        if (submittedAttempt.userAnswers == null) {
            throw BadRequestException("Answers field must be present")
        } else {
            val storedAnswers = storedAttempt.userAnswers
            val questions = storedAttempt.quiz.questions.associateBy { it.id }
            for ((uuid, userAnswer) in submittedAttempt.userAnswers!!.entries) {
                if (questions.containsKey(uuid)) {
                    validateSubmittedAnswer(userAnswer, questions[uuid]!!)
                    storedAnswers[uuid] = UserAnswerDto.convertToEntity(userAnswer)
                } else {
                    throw BadRequestException("Unknown question UUID $uuid")
                }
            }
            storedAttempt.userAnswers = storedAnswers
            attemptRepository.save(storedAttempt)
            return QuizAttemptDto(storedAttempt)
        }
    }

    private fun validateSubmittedAnswer(userAnswer: UserAnswerDto, question: Question) {
        val possibleAnswers = question.answers.map { it.id }.toSet()
        when (question.type) {
            QuestionType.SINGLE_ANSWER -> {
                if (userAnswer is UserAnswerDto.ChoiceAnswer &&
                    userAnswer.answer.size > 1
                ) {
                    throw BadRequestException("Multiple answers for single answer question")
                }

                if (userAnswer !is UserAnswerDto.ChoiceAnswer) {
                    throw BadRequestException("String answer for single answer question")
                } else {
                    if (userAnswer.answer.isNotEmpty() && !possibleAnswers.contains(userAnswer.answer[0])) {
                        throw BadRequestException("Unknown answer ${userAnswer.answer[0]} for question ${question.id}")
                    }
                }
            }

            QuestionType.MULTIPLE_ANSWER -> {
                if (userAnswer !is UserAnswerDto.ChoiceAnswer) {
                    throw BadRequestException("String answer for single answer question")
                } else {

                    for (uuid in userAnswer.answer) {
                        if (!possibleAnswers.contains(uuid)) {
                            throw BadRequestException("Unknown answer ${uuid} for question ${question.id}")
                        }
                    }

                }
            }

            QuestionType.TEXT_FIELD -> {
                if (userAnswer !is UserAnswerDto.StringAnswer) {
                    throw BadRequestException("Choice answer for field question")
                }
            }

            QuestionType.NUMERIC_FIELD -> {
                if (userAnswer !is UserAnswerDto.StringAnswer) {
                    throw BadRequestException("Choice answer for field question")
                }
                try {
                    if (userAnswer.answer.isNotEmpty() && userAnswer.answer != "-") {
                        userAnswer.answer.replace(',', '.').toDouble()
                    }
                } catch (e: NumberFormatException) {
                    throw BadRequestException(e.message.toString())
                }
            }
        }
    }

    fun authorizeAttemptModification(attempt: QuizAttempt, principal: Authentication) {
        if (attempt.account != principal.principal) {
            throw AccessDeniedException("Access denied to attempt ${attempt.id}")
        }
    }

    fun authorizeAttemptAccess(attempt: QuizAttempt, principal: Authentication) {
        if (attempt.account != principal.principal &&
                permissionEvaluator.hasPermission(principal, attempt.quiz, "WRITE")) {
            throw AccessDeniedException("Access denied to attempt ${attempt.id}")
        }
    }

    @Transactional
    fun beginQuizAttempt(quizUuid: UUID, principal: Authentication): QuizAttemptDto {
        val quiz = quizRepository.findById(quizUuid).orElseThrow { NotFoundException("Quiz", quizUuid) }
        if (quiz.publishedAt == null) {
            throw BadRequestException("Quiz is not published")
        }
        if (attemptRepository.existsByQuizIdAndAccountId(quizUuid, (principal.principal as Account).id)) {
            throw BadRequestException("Attempt already exists")
        }
        var attempt = QuizAttempt(quiz, Instant.now(), principal.principal as Account)
        attempt = attemptRepository.save(attempt)

        if (attempt.quiz.timeLimit >= 0) {
            Timer().schedule(attempt.quiz.timeLimit.toLong().plus(2) * 1000) {
                endAttemptUnconditionally(attempt.id)
            }
        }

        return QuizAttemptDto(attempt)
    }

    @Transactional
    fun endAttempt(uuid: UUID): QuizAttemptDto {

        val principal = SecurityContextHolder.getContext().authentication

        val attempt = findAttemptById(uuid)
        authorizeAttemptModification(attempt, principal)
        return if (!attempt.closed) {
            attempt.closed = true
            attempt.timeTaken = Duration.between(attempt.startedAt, Instant.now()).toSeconds().toInt()
            val grade = gradeAttempt(attempt)
            attempt.score = grade.score
            attemptRepository.save(attempt)
            grade
        } else gradeAttempt(attempt)
    }

    private fun endAttemptUnconditionally(uuid: UUID) {
        val attempt = findAttemptById(uuid)
        if (!attempt.closed) {
            attempt.closed = true
            attempt.timeTaken = Duration.between(attempt.startedAt, Instant.now()).toSeconds().toInt()
            attemptRepository.save(attempt)
        }
    }

    fun getAttemptDetails(attemptUuid: UUID): MappingJacksonValue {

        val principal = SecurityContextHolder.getContext().authentication
        val attempt = findAttemptById(attemptUuid)
        authorizeAttemptAccess(attempt, principal)

        return if (!attempt.isExpired() && !permissionEvaluator.hasPermission(principal, attempt.quiz, "WRITE")) {
            val mappingJacksonValue = MappingJacksonValue(QuizAttemptDto(attempt).apply { loadLevel = JacksonViewEnum.READ })
            mappingJacksonValue.serializationView = JacksonView.Read::class.java
            mappingJacksonValue
        } else {
            val mappingJacksonValue = MappingJacksonValue(gradeAttempt(attempt).apply { loadLevel = JacksonViewEnum.ANSWER })
            mappingJacksonValue.serializationView = JacksonView.Answers::class.java
            mappingJacksonValue
        }
    }

    private fun gradeAnswer(userAnswer: UserAnswerDto?, question: Question): SystemAnswerDto {
        when (question.type) {
            QuestionType.SINGLE_ANSWER -> {
                val answers = question.answers.associateBy { it.id }
                userAnswer as UserAnswerDto.ChoiceAnswer?

                return if (userAnswer == null || userAnswer.answer.isEmpty()) {
                    SystemAnswerDto.ChoiceAnswer(0.0, emptyList())
                } else {
                    val correct = answers.maxByOrNull { it.value.chosenScore }?.key
                    val userScore = question.weight / question.maxScore *
                            (answers[userAnswer.answer[0]]?.chosenScore ?: 0.0)
                    SystemAnswerDto.ChoiceAnswer(
                        userScore,
                        if (correct == null) emptyList() else listOf(correct)
                    )
                }
            }

            QuestionType.MULTIPLE_ANSWER -> {
                userAnswer as UserAnswerDto.ChoiceAnswer?
                val grade = if (userAnswer != null) {
                    val userAnswerSet = userAnswer.answer.toSet()
                    var grade = question.baseScore
                    for (answer in question.answers) {
                        grade += if (answer.id in userAnswerSet) {
                            answer.chosenScore
                        } else {
                            answer.notChosenScore
                        }
                    }

                    grade = grade.coerceIn(0.0, question.maxScore)
                    grade *= question.weight / question.maxScore
                    grade
                } else {
                    0.0
                }
                val correctAnswers = question.answers.filter { it.chosenScore > it.notChosenScore }.map { it.id }
                return SystemAnswerDto.ChoiceAnswer(grade, correctAnswers)
            }
            QuestionType.NUMERIC_FIELD -> {
                val grade = if (userAnswer != null) {
                    userAnswer as UserAnswerDto.StringAnswer
                    var grade: Double = question.baseScore
                    if (userAnswer.answer.isNotBlank()) {
                        val replaced = userAnswer.answer.replace(',', '.')
                        val userAnswerDouble = if (replaced == "-" || replaced == ".") {
                            0.0
                        } else {
                            replaced.toDouble()
                        }

                        val eps = 0.1.pow(question.numericPrecision)

                        for (answer in question.answers) {
                            if (abs(answer.text.toDouble() - userAnswerDouble) < eps) {
                                grade = max(grade, answer.chosenScore)
                            }
                        }
                    }

                    grade *= question.weight / question.maxScore
                    grade
                } else {
                    0.0
                }
                val correct = question.answers.maxByOrNull { it.chosenScore }?.text
                return SystemAnswerDto.StringAnswer(
                    grade,
                    correct ?: ""
                )

            }

            QuestionType.TEXT_FIELD -> {
                val grade = if (userAnswer != null) {
                    userAnswer as UserAnswerDto.StringAnswer
                    var userAnswerString = userAnswer.answer
                    if (question.trimPadding) {
                        userAnswerString = userAnswerString.trim().replace("\\s+", " ")
                    }
                    if (question.ignoreCase) {
                        userAnswerString = userAnswerString.toLowerCase()
                    }

                    var grade: Double = question.baseScore

                    for (answer in question.answers) {
                        var modifiedAnswer = answer.text
                        if (question.trimPadding) {
                            modifiedAnswer = modifiedAnswer.trim().replace("\\s+", " ")
                        }
                        if (question.ignoreCase) {
                            modifiedAnswer = modifiedAnswer.toLowerCase()
                        }

                        if (modifiedAnswer == userAnswerString) {
                            grade = max(grade, answer.chosenScore)
                        }
                    }

                    grade = grade.coerceIn(0.0, question.maxScore)
                    grade *= question.weight / question.maxScore
                    grade
                } else {
                    0.0
                }
                val correct = question.answers.maxByOrNull { it.chosenScore }?.text
                return SystemAnswerDto.StringAnswer(
                    grade,
                    correct ?: ""
                )
            }
        }
    }

    fun gradeAttempt(attempt: QuizAttempt): QuizAttemptDto {
        val questions = attempt.quiz.questions.associateBy { it.id }
        val result = HashMap<UUID, SystemAnswerDto>()
        var gradeSum = 0.0
        var quizSum = 0.0

        for ((uuid,question) in questions) {
            val userAnswer = attempt.userAnswers[uuid]
            val gradeAnswer = if (userAnswer != null) {
                gradeAnswer(UserAnswerDto.parseFromEntity(userAnswer), questions[uuid]!!)
            } else {
                gradeAnswer(null, questions[uuid]!!)
            }
            result[uuid] = gradeAnswer
            gradeSum += gradeAnswer.userScore
            quizSum += question.weight
        }
        val attemptDto = QuizAttemptDto(attempt)
        attemptDto.systemAnswers = result
        attemptDto.score = gradeSum / quizSum * attempt.quiz.score!!
        return attemptDto
    }


    private fun findAttemptById(uuid: UUID): QuizAttempt {
        return attemptRepository.findById(uuid).orElseThrow { NotFoundException("Attempt", uuid) }
    }

}