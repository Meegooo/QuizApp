package com.meegoo.quizproject.server.data.services

import com.meegoo.quizproject.server.controllers.hooks.BadRequestException
import com.meegoo.quizproject.server.controllers.hooks.NotFoundException
import com.meegoo.quizproject.server.data.QuestionType
import com.meegoo.quizproject.server.data.dto.GrantedPermission
import com.meegoo.quizproject.server.data.dto.QuestionDto
import com.meegoo.quizproject.server.data.dto.QuizDto
import com.meegoo.quizproject.server.data.entity.Course
import com.meegoo.quizproject.server.data.entity.Question
import com.meegoo.quizproject.server.data.entity.Quiz
import com.meegoo.quizproject.server.data.repositories.CourseRepository
import com.meegoo.quizproject.server.data.repositories.QuestionRepository
import com.meegoo.quizproject.server.data.repositories.QuizRepository
import com.meegoo.quizproject.server.security.acl.AclObjectRetriever
import com.meegoo.quizproject.server.security.acl.AclPermissionGranter
import com.meegoo.quizproject.server.security.acl.QuizPermission
import org.springframework.security.acls.domain.ObjectIdentityImpl
import org.springframework.security.acls.model.MutableAclService
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

@Service
class QuizService(
    private val quizRepository: QuizRepository,
    private val courseRepository: CourseRepository,
    private val courseService: CourseService,
    private val questionRepository: QuestionRepository,
    private val aclPermissionGranter: AclPermissionGranter,
    private val aclObjectRetriever: AclObjectRetriever,
    private val aclService: MutableAclService,
    private val permissionService: PermissionService,
) {

    @Transactional
    fun createNewQuiz(name: String): QuizDto {
        var newQuiz = Quiz(name)
        validateNewQuiz(newQuiz)
        newQuiz = quizRepository.save(newQuiz)
        aclPermissionGranter.createObject(newQuiz)
        return QuizDto(newQuiz, GrantedPermission.OWNER.toListGranting())
    }


    @Transactional
    fun updateQuiz(quizDto: QuizDto): QuizDto {
        val quiz = quizRepository.findById(quizDto.id!!).orElseThrow { NotFoundException("Quiz", quizDto.id!!) }

        val questions = quiz.questions.associateBy { it.id }
        quiz.name = quizDto.name ?: quiz.name
        quiz.timeLimit = quizDto.timeLimit ?: quiz.timeLimit
        quiz.automaticScore = quizDto.automaticScore ?: quiz.automaticScore
        if (!quiz.automaticScore) {
            quiz.score = quizDto.score ?: quiz.score
        }
        if (quiz.timeLimit < 0) quiz.timeLimit = -1

        //Delete removed questions
        val quizDtoIds = quizDto.questions.mapNotNull { it.id }.toSet()
        val toRemove = quiz.questions.filter { it.id !in quizDtoIds }
        questionRepository.deleteAll(toRemove)
        quiz.questions.removeAll { it in toRemove  }

        for (newQuestion in quizDto.questions) {
            if (newQuestion.id != null && questions.containsKey(newQuestion.id!!)) {
                //Existing question
                val originalQuestion = questions[newQuestion.id!!]!!
                originalQuestion.question = newQuestion.question ?: originalQuestion.question
                if (newQuestion.type != originalQuestion.type) {
                    originalQuestion.type = newQuestion.type!!
                    originalQuestion.answers.clear()
                }
                originalQuestion.weight = newQuestion.weight ?: originalQuestion.weight
                originalQuestion.index = newQuestion.index ?: originalQuestion.index
                if (newQuestion.answers != null) {
                    originalQuestion.answers = parseAnswers(originalQuestion, newQuestion)
                }

                if (newQuestion.baseScore != null)
                    originalQuestion.options["base_score"] = newQuestion.baseScore.toString()
                if (newQuestion.maxScore != null) {
                    if (newQuestion.maxScore!! <= 0) {
                        throw IllegalArgumentException("Max question score has to be bigger than 0")
                    }
                    originalQuestion.options["max_score"] = newQuestion.maxScore.toString()
                }
                if (newQuestion.numericPrecision != null)
                    originalQuestion.options["numeric_precision"] = newQuestion.numericPrecision.toString()
                if (newQuestion.trimPadding != null)
                    originalQuestion.options["trim_padding"] = newQuestion.trimPadding.toString()
                if (newQuestion.ignoreCase != null)
                    originalQuestion.options["ignore_case"] = newQuestion.ignoreCase.toString()

            } else {
                //New question
                parseAndAddNewQuestion(newQuestion, quiz)
            }
        }


        //Check for index uniqueness
        if (quiz.questions.map { it.index }.toSet().size != quiz.questions.size) {
            throw IllegalArgumentException("Question indexes have to be unique for one quiz")
        }

        if (quiz.score == null) {
            quiz.automaticScore = true
        }

        if (quiz.automaticScore) {
            quiz.score = quiz.questions.sumOf { it.weight }
        }

        questionRepository.saveAll(quiz.questions)
        quizRepository.save(quiz)

        return QuizDto(
            quiz, permissionService.findPermissionsForQuiz(quiz.id),
            includeQuestions = true
        )
    }

    private fun parseAndAddNewQuestion(newQuestion: QuestionDto, quiz: Quiz) {
        requireNotNull(newQuestion.question) { "question can't be null" }
        requireNotNull(newQuestion.type) { "type can't be null" }
        requireNotNull(newQuestion.answers) { "answers can't be null" }
        requireNotNull(newQuestion.index) { "index can't be null" }

        requireNotNull(newQuestion.baseScore) { "baseScore can't be null" }
        requireNotNull(newQuestion.maxScore) { "maxScore can't be null" }

        if (newQuestion.maxScore!! <= 0) {
            throw IllegalArgumentException("Max question score has to be bigger than 0")
        }
        if (newQuestion.weight!! <= 0) {
            throw IllegalArgumentException("Weight has to be bigger than 0")
        }

        val createdQuestion =
            Question(
                newQuestion.question!!,
                newQuestion.type!!,
                quiz,
                newQuestion.index!!,
                newQuestion.weight ?: 1.0
            )
        createdQuestion.answers = parseAnswers(createdQuestion, newQuestion)

        createdQuestion.options["base_score"] = newQuestion.baseScore.toString()
        createdQuestion.options["max_score"] = newQuestion.maxScore.toString()

        if (createdQuestion.type == QuestionType.NUMERIC_FIELD) {
            requireNotNull(newQuestion.numericPrecision) { "numericPrecision can't be null" }
            createdQuestion.options["numeric_precision"] = newQuestion.numericPrecision.toString()
        } else if (createdQuestion.type == QuestionType.TEXT_FIELD) {
            requireNotNull(newQuestion.trimPadding) { "trimPadding can't be null" }
            requireNotNull(newQuestion.ignoreCase) { "ignoreCase can't be null" }
            createdQuestion.options["ignore_case"] = newQuestion.trimPadding.toString()
            createdQuestion.options["trim_padding"] = newQuestion.ignoreCase.toString()
        }

        quiz.questions.add(createdQuestion)
    }

    private fun parseAnswers(
        originalQuestion: Question,
        newQuestion: QuestionDto
    ): MutableList<Question.Answer> {

        val type = originalQuestion.type
        val originalAnswers = originalQuestion.answers.map { it.id }.toSet()
        try {
            return newQuestion.answers!!.map {
                requireNotNull(it.text) { "text" }
                requireNotNull(it.chosenScore) { "chosenScore" }

                val answer = when (type) {
                    QuestionType.SINGLE_ANSWER -> {
                        Question.Answer(text = it.text, chosenScore = it.chosenScore)
                    }

                    QuestionType.MULTIPLE_ANSWER -> {
                        Question.Answer(
                            text = it.text,
                            chosenScore = it.chosenScore,
                            notChosenScore = it.notChosenScore
                        )
                    }

                    QuestionType.NUMERIC_FIELD -> {
                        if (it.text.isEmpty() || it.text == "-") {
                            it.text = "0.0"
                        }
                        it.text = it.text.replace(",", ".")

                        if (it.text.toDoubleOrNull() == null) {
                            throw IllegalArgumentException("Provided value is not numeric")
                        }
                        Question.Answer(text = it.text, chosenScore = it.chosenScore)

                    }

                    QuestionType.TEXT_FIELD -> {
                        Question.Answer(text = it.text, chosenScore = it.chosenScore)
                    }

                }
                if (it.id != null && it.id!! in originalAnswers) {
                    answer.id = it.id!!
                }
                answer
            }.toMutableList()
        } catch (e: java.lang.IllegalArgumentException) {
            throw BadRequestException(e.message ?: "")
        }
    }

    fun getOne(uuid: UUID): QuizDto {
        val entity = quizRepository.findById(uuid).orElseThrow { NotFoundException("Quiz", uuid) }
        return QuizDto(
            entity,
            permissionService.findPermissionsForQuiz(uuid),
            includeQuestions = true,
            includeCourses = true
        ).apply {
            val courseAccess = courseService.getAllCourses().map { it.id }.toSet()
            courses.retainAll { it.id in courseAccess }
        }
    }

    fun getAllQuizzes(): List<QuizDto> {
        val authentication = SecurityContextHolder.getContext().authentication
        return if (authentication.authorities.contains(SimpleGrantedAuthority("ROLE_ADMIN"))) {
            quizRepository.findAll()
                .map { QuizDto(it, GrantedPermission.OWNER.toListGranting(), includeQuestions = true) }
        } else {
            val found = aclObjectRetriever.getObjectsWithAccess(Quiz::class.java, authentication)
                .asSequence()
                .filter {
                    (it.mask and QuizPermission.READ.mask) != 0
                }
                .mapNotNull { quizRepository.findById(it.uuid).orElse(null) }
                .map { QuizDto(it, permissionService.findPermissionsForQuiz(it.id), includeQuestions = true) }
                .associateBy { it.id } as MutableMap
            aclObjectRetriever.getObjectsWithAccess(Course::class.java, authentication)
                .asSequence()
                .filter {
                    (it.mask and QuizPermission.READ.mask) != 0
                }
                .map { courseRepository.findById(it.uuid) }
                .flatMap { it.orElseGet { null }?.quizzes ?: emptyList() }
                .map { QuizDto(it, GrantedPermission.READ.toListGranting(), includeQuestions = true) }
                .forEach {
                    if (!found.containsKey(it.id)) {
                        found[it.id] = it
                    }

                }

            found.values.toList()
        }
    }

    @Transactional
    fun publishQuiz(uuid: UUID): QuizDto {
        val quiz = quizRepository.findById(uuid).orElseThrow { NotFoundException("Quiz", uuid) }
        quiz.publishedAt = Instant.now()
        quizRepository.save(quiz)
        return QuizDto(
            quiz,
            permissionService.findPermissionsForQuiz(uuid)
        )
    }


    @Transactional
    fun deleteQuiz(uuid: UUID) {
        if (quizRepository.existsById(uuid)) {
            aclService.deleteAcl(ObjectIdentityImpl(Quiz::class.java, uuid), true)
            quizRepository.deleteById(uuid)
        }
    }


    @Transactional
    fun exportQuiz(uuid: UUID): QuizDto {
        val quizDto = QuizDto(
            quizRepository.findById(uuid).orElseThrow { NotFoundException("Quiz", uuid) },
            emptyList(),
            includeQuestions = true,
            includeCourses = false
        )
        if (quizDto.automaticScore == true) {
            quizDto.score = null
        }
        quizDto.loadLevel = null
        quizDto.questions.forEach {
            it.answers!!.forEach { answer ->
                answer.id = null
            }
        }
        return quizDto
    }

    @Transactional
    fun importQuiz(quizDto: QuizDto): QuizDto {
        quizDto.id = null
        requireNotNull(quizDto.name) { "quiz name can't be null" }
        requireNotNull(quizDto.timeLimit) { "quiz timeLimit can't be null" }
        requireNotNull(quizDto.automaticScore) { "quiz automaticScore can't be null" }
        if (quizDto.automaticScore == false) {
            requireNotNull(quizDto.score) { "quiz score can't be null" }
        }

        val quiz = Quiz(quizDto.name!!)
        quiz.automaticScore = quizDto.automaticScore!!
        quiz.score = quizDto.score
        quiz.timeLimit = quizDto.timeLimit!!
        quiz.publishedAt = null

        quizRepository.save(quiz)
        for (newQuestion in quizDto.questions) {
            parseAndAddNewQuestion(newQuestion, quiz)
        }

        if (quiz.questions.map { it.index }.toSet().size != quiz.questions.size) {
            throw IllegalArgumentException("Question indexes have to be unique for one quiz")
        }
        if (quiz.score == null) {
            quiz.automaticScore = true
        }
        if (quiz.automaticScore) {
            quiz.score = quiz.questions.sumOf { it.weight }
        }
        questionRepository.saveAll(quiz.questions)
        aclPermissionGranter.createObject(quiz)
        return QuizDto(quiz, GrantedPermission.OWNER.toListGranting(), includeQuestions = true)

    }

    private fun validateNewQuiz(quiz: Quiz) {
        if (quiz.name.isEmpty()) {
            throw BadRequestException("Quiz name cannot be empty")
        }
    }
}