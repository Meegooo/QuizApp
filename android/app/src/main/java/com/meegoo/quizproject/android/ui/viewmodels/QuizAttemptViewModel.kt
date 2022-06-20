package com.meegoo.quizproject.android.ui.viewmodels

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.meegoo.quizproject.android.data.dto.QuestionDto
import com.meegoo.quizproject.android.data.dto.QuizAttemptDto
import com.meegoo.quizproject.android.data.dto.QuizDto
import com.meegoo.quizproject.server.data.dto.SystemAnswerDto
import com.meegoo.quizproject.server.data.dto.UserAnswerDto
import java.lang.RuntimeException
import java.time.Duration
import java.time.Instant
import java.util.*
import kotlin.collections.LinkedHashMap
import kotlin.math.max


class QuizAttemptViewModel : ViewModel() {
    var id = mutableStateOf<UUID?>(null)
    var startedAt = mutableStateOf<Instant?>(null)
    var score = mutableStateOf<Double?>(null)
    var quizId = mutableStateOf<UUID?>(null)
    var userAnswers = mutableStateMapOf<UUID, UserAnswerDto>()
    var userAnswersOptimized = mutableStateMapOf<UUID, Any>()
    var closed = mutableStateOf<Boolean?>(null)
    var timeTaken = mutableStateOf<Int?>(null)
    var timeLimit = mutableStateOf<Int?>(null)
    var systemAnswers = mutableStateMapOf<UUID, SystemAnswerDto>()

    var newAttempt = mutableStateOf(false)

    lateinit var quiz: MutableState<QuizDto?>

    fun fromQuizAttemptDto(quizAttempt: QuizAttemptDto) {
        if (quizAttempt.id == null) { //new attempt
            newAttempt.value = true
        }
        this.id.value = quizAttempt.id
        this.startedAt.value = quizAttempt.startedAt
        this.score.value = quizAttempt.score
        this.quizId.value = quizAttempt.quizId
        this.closed.value = quizAttempt.closed
        this.timeTaken.value = quizAttempt.timeTaken
        this.timeLimit.value = quizAttempt.timeLimit


        this.userAnswers.clear()
        if (quizAttempt.userAnswers != null) {
            this.userAnswers.putAll(quizAttempt.userAnswers!!)
        }
        this.systemAnswers.clear()
        if (quizAttempt.systemAnswers != null) {
            this.systemAnswers.putAll(quizAttempt.systemAnswers!!)
        }
    }

    fun loadUserAnswers() {
        userAnswersOptimized.clear()
        val questionMap = quiz.value!!.questions.associateBy { it.id }
        for ((key, value) in questionMap) {
            userAnswersOptimized[key!!] = userAnswerToEfficientAnswer(value)
        }
    }

    private fun processUserAnswers() {
        val questionMap = quiz.value!!.questions.associateBy { it.id }
        for ((key, value) in questionMap) {
            efficientAnswerToUserAnswer(value, userAnswersOptimized[key]!!)
        }
    }

    fun toQuizAttemptDto(): QuizAttemptDto {
        return QuizAttemptDto(
            id.value,
            startedAt.value,
            score.value,
            quizId.value,
            LinkedHashMap(userAnswers),
            closed.value,
            timeTaken.value,
            timeLimit.value,
            LinkedHashMap(systemAnswers),
        )
    }

    val timeTakenFormatted by lazy {
        when (timeTaken.value) {
            null -> "00:00:00"
            else -> {
                val seconds = (timeTaken.value ?: 0) % 60
                val minutes = (timeTaken.value ?: 0) / 60 % 60
                val hours = (timeTaken.value ?: 0) / 3600
                String.format("%02d:%02d:%02d", hours, minutes, seconds)
            }
        }

    }

    val timeRemaining: Long?
        get() {
            return startedAt.value?.let {
                if (timeLimit.value == null || timeLimit.value!! < 0) {
                    return null
                }
                return max(Duration.between(Instant.now(), it.plusSeconds(timeLimit.value!!.toLong())).seconds, 0)
            }
        }

    val finishedAt by lazy {
        startedAt.value?.plusSeconds(timeTaken.value?.toLong() ?: 0)
    }

    fun isExpired(): Boolean? {
        val timeLimit = timeLimit.value ?: return null

        return if (closed.value == true) {
            true
        } else {
            if (timeLimit <= 0) {
                false
            } else {
                startedAt.value!!.plusSeconds(timeLimit.toLong()).isBefore(Instant.now())
            }
        }

    }

    var updateQuizAttemptError = mutableStateOf(false)
    suspend fun updateQuizAttempt(mainViewModel: MainViewModel): Boolean {
        processUserAnswers()
        val (updateQuiz, success) = mainViewModel.putQuizAttempt(toQuizAttemptDto(), quiz.value?.id!!)
        return if (updateQuiz != null && success) {
            fromQuizAttemptDto(updateQuiz)
            updateQuizAttemptError.value = false
            true
        } else {
            updateQuizAttemptError.value = true
            false
        }

    }

    var submittingAnswer = mutableStateOf(false)
    suspend fun finishAttempt(mainViewModel: MainViewModel): Boolean {
        submittingAnswer.value = true
        val (updateQuiz, success) = mainViewModel.finishAttempt(toQuizAttemptDto(), quiz.value?.id!!)
        return if (updateQuiz != null && success) {
            fromQuizAttemptDto(updateQuiz)
            updateQuizAttemptError.value = false
            submittingAnswer.value = false
            true
        } else {
            updateQuizAttemptError.value = true
            submittingAnswer.value = false
            false
        }
    }


    private var parsedGrades = HashMap<UUID, QuizAttemptDto.Grade>()
    fun parseGrade(question: QuestionDto): QuizAttemptDto.Grade? {
        requireNotNull(question.id)
        if (!parsedGrades.containsKey(question.id)) {
            val userAnswer = userAnswers[question.id]
            val systemAnswer = systemAnswers[question.id]
            if (systemAnswer != null) {
                val parsed = if (systemAnswer is SystemAnswerDto.ChoiceAnswer) {
                    userAnswer as UserAnswerDto.ChoiceAnswer?
                    QuizAttemptDto.Grade.ChoiceGrade(userAnswer, systemAnswer, question.weight!!)
                } else {
                    systemAnswer as SystemAnswerDto.StringAnswer
                    userAnswer as UserAnswerDto.StringAnswer?
                    QuizAttemptDto.Grade.StringGrade(userAnswer, systemAnswer, question.weight!!)
                }
                parsedGrades[question.id!!] = parsed;
            } else {
                return null
            }
        }
        return parsedGrades[question.id!!]
    }

    private fun userAnswerToEfficientAnswer(question: QuestionDto): Any {
        when (question.type) {
            QuestionDto.QuestionType.SINGLE_ANSWER,
            QuestionDto.QuestionType.MULTIPLE_ANSWER -> {
                val out = mutableListOf<Int>()
                if (!userAnswers.containsKey(question.id)) {
                    return out
                }
                val choiceAnswer = userAnswers[question.id]!! as UserAnswerDto.ChoiceAnswer
                if (choiceAnswer.answer.isEmpty()) {
                    return out
                }
                question.answers!!.forEachIndexed { idx, value ->
                    if (value.id in choiceAnswer.answer) {
                        out.add(idx)
                    }
                }
                return out

            }
            QuestionDto.QuestionType.NUMERIC_FIELD,
            QuestionDto.QuestionType.TEXT_FIELD -> {
                if (!userAnswers.containsKey(question.id)) {
                    return "";
                }
                val choiceAnswer = userAnswers[question.id]!! as UserAnswerDto.StringAnswer
                return choiceAnswer.answer

            }
        }
        throw RuntimeException("Should never happen")
    }

    private fun efficientAnswerToUserAnswer(question: QuestionDto, answer: Any) {
        when (question.type) {
            QuestionDto.QuestionType.MULTIPLE_ANSWER,
            QuestionDto.QuestionType.SINGLE_ANSWER-> {
                val idxs = answer as List<Int>
                val ids = idxs.map { question.answers!![it].id!! }.toMutableList()
                if (userAnswers.containsKey(question.id!!)) {
                    val t = (userAnswers[question.id]!! as UserAnswerDto.ChoiceAnswer).answer
                    t.clear()
                    t.addAll(ids)
                } else {
                    userAnswers[question.id!!] = UserAnswerDto.ChoiceAnswer(ids)
                }

            }
            QuestionDto.QuestionType.NUMERIC_FIELD, QuestionDto.QuestionType.TEXT_FIELD -> {
                val ans = answer as String
                if (userAnswers.containsKey(question.id)) {
                    (userAnswers[question.id]!! as UserAnswerDto.StringAnswer).answer = ans
                } else {
                    userAnswers[question.id!!] = UserAnswerDto.StringAnswer(ans)
                }

            }
        }

    }

    fun processUserInput(question: QuestionDto, it: Any) {
        userAnswersOptimized[question.id!!] = it
    }

}