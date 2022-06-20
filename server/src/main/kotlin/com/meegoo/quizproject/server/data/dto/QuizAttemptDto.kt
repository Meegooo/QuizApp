package com.meegoo.quizproject.server.data.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonView
import com.meegoo.quizproject.server.data.JacksonView
import com.meegoo.quizproject.server.data.JacksonViewEnum
import com.meegoo.quizproject.server.data.entity.QuizAttempt
import java.time.Instant
import java.util.*


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
    ): this() {
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

    constructor(entity: QuizAttempt) : this(
        entity.id,
        entity.startedAt,
        entity.score,
        entity.quiz.id,
        entity.userAnswers.mapValues { UserAnswerDto.parseFromEntity(it.value) } as LinkedHashMap<UUID, UserAnswerDto>,
        entity.closed,
        entity.timeTaken,
        entity.quiz.timeLimit
    )

    @JsonView(JacksonView.Overview::class)
    var id: UUID? = null

    @JsonView(JacksonView.Overview::class)
    var startedAt: Instant? = null

    @JsonView(JacksonView.Overview::class)
    var score: Double? = null

    @JsonView(JacksonView.Overview::class)
    var quizId: UUID? = null

    @JsonView(JacksonView.Overview::class)
    var timeLimit: Int? = null

    @JsonView(JacksonView.Read::class)
    var userAnswers: LinkedHashMap<UUID, UserAnswerDto>? = null

    @JsonView(JacksonView.Overview::class)
    var closed: Boolean? = null

    @JsonView(JacksonView.Overview::class)
    var timeTaken: Int? = null

    @JsonView(JacksonView.Write::class, JacksonView.Answers::class)
    var systemAnswers: Map<UUID, SystemAnswerDto>? = null

    @JsonView(JacksonView.Overview::class)
    var loadLevel: JacksonViewEnum? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as QuizAttemptDto

        if (id != other.id) return false
        if (startedAt != other.startedAt) return false
        if (score != other.score) return false
        if (quizId != other.quizId) return false
        if (timeLimit != other.timeLimit) return false
        if (userAnswers != other.userAnswers) return false
        if (closed != other.closed) return false
        if (timeTaken != other.timeTaken) return false
        if (systemAnswers != other.systemAnswers) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + (startedAt?.hashCode() ?: 0)
        result = 31 * result + (score?.hashCode() ?: 0)
        result = 31 * result + (quizId?.hashCode() ?: 0)
        result = 31 * result + (timeLimit ?: 0)
        result = 31 * result + (userAnswers?.hashCode() ?: 0)
        result = 31 * result + (closed?.hashCode() ?: 0)
        result = 31 * result + (timeTaken ?: 0)
        result = 31 * result + (systemAnswers?.hashCode() ?: 0)
        return result
    }


}