package com.meegoo.quizproject.android.data.dto

import android.util.Log
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.meegoo.quizproject.android.util.symmetricDifference
import com.meegoo.quizproject.server.data.dto.SystemAnswerDto
import com.meegoo.quizproject.server.data.dto.UserAnswerDto
import java.lang.RuntimeException
import java.time.Duration
import java.time.Instant
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.abs
import kotlin.math.max

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(value = JsonInclude.Include.NON_NULL)
class QuizAttemptDto() {

    constructor(
        id: UUID? = null,
        startedAt: Instant? = null,
        score: Double? = null,
        quizId: UUID? = null,
        userAnswers: LinkedHashMap<UUID, UserAnswerDto>? = null,
        closed: Boolean? = null,
        timeTaken: Int? = null,
        timeLimit: Int? = null,
        systemAnswers: Map<UUID, SystemAnswerDto>? = null
    ) : this() {
        this.id = id
        this.startedAt = startedAt
        this.score = score
        this.quizId = quizId
        this.userAnswers = userAnswers
        this.closed = closed
        this.timeTaken = timeTaken
        this.timeLimit = timeLimit
        this.systemAnswers = systemAnswers
    }

    var id: UUID? = null
    var startedAt: Instant? = null
    var score: Double? = null

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    var quizId: UUID? = null

    var userAnswers: LinkedHashMap<UUID, UserAnswerDto>? = null
    var closed: Boolean? = null
    var timeTaken: Int? = null
    var timeLimit: Int? = null

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    var systemAnswers: Map<UUID, SystemAnswerDto>? = null

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    var loadLevel = LoadLevel.MISSING

    val timeRemaining: Long?
        get() {
            return startedAt?.let {
                if (timeLimit == null || timeLimit!! < 0) {
                    return null
                }
                return max(Duration.between(Instant.now(), it.plusSeconds(timeLimit!!.toLong())).seconds, 0)
            }
        }
    fun isExpired(): Boolean? {
        val timeLimit = timeLimit ?: return null

        return if (closed == true) {
            true
        } else {
            if (timeLimit <= 0) {
                false
            } else {
                startedAt!!.plusSeconds(timeLimit.toLong() + 3).isBefore(Instant.now())
            }
        }

    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as QuizAttemptDto

        if (id != other.id) return false
        if (startedAt != other.startedAt) return false
        if (score != other.score) return false
        if (quizId != other.quizId) return false
        if (userAnswers != other.userAnswers) return false
        if (closed != other.closed) return false
        if (timeTaken != other.timeTaken) return false
        if (timeLimit != other.timeLimit) return false
        if (systemAnswers != other.systemAnswers) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + (startedAt?.hashCode() ?: 0)
        result = 31 * result + (score?.hashCode() ?: 0)
        result = 31 * result + (quizId?.hashCode() ?: 0)
        result = 31 * result + (userAnswers?.hashCode() ?: 0)
        result = 31 * result + (closed?.hashCode() ?: 0)
        result = 31 * result + (timeTaken ?: 0)
        result = 31 * result + (timeLimit ?: 0)
        result = 31 * result + (systemAnswers?.hashCode() ?: 0)
        return result
    }

    sealed class Grade(val grade: Double, val maxGrade: Double) {
        class ChoiceGrade(
            val userAnswer: UserAnswerDto.ChoiceAnswer?,
            systemAnswers: SystemAnswerDto.ChoiceAnswer,
            maxGrade: Double
        ) : Grade(systemAnswers.userScore, maxGrade) {

            val correctAnswers: Set<UUID> = (userAnswer?.answer?: emptySet()).intersect(systemAnswers.answer)

            val wrongAnswers = (userAnswer?.answer?: emptySet()).symmetricDifference(systemAnswers.answer)
        }

        class StringGrade(
            val userAnswer: UserAnswerDto.StringAnswer?,
            val systemAnswer: SystemAnswerDto.StringAnswer,
            maxGrade: Double
        ) : Grade(systemAnswer.userScore, maxGrade)

        fun isFullMark(): Boolean {
            return abs(grade - maxGrade) < EPS
        }

        fun isWrong(): Boolean {
            return grade < EPS
        }

        fun isPartiallyCorrect(): Boolean {
            return !isFullMark() && !isWrong()
        }
    }
}


private const val EPS = 1e-10;