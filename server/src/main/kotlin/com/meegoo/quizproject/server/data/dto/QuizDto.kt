package com.meegoo.quizproject.server.data.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonView
import com.meegoo.quizproject.server.data.JacksonView
import com.meegoo.quizproject.server.data.JacksonViewEnum
import com.meegoo.quizproject.server.data.entity.Quiz
import java.time.Instant
import java.util.*
import kotlin.collections.ArrayList

@JsonInclude(value = JsonInclude.Include.NON_NULL)
class QuizDto() {

    constructor(
        id: UUID? = null,
        name: String? = null,
        timeLimit: Int? = null,
        score: Double? = null,
        automaticScore: Boolean? = null,
        publishedAt: Instant? = null,
        questions: MutableList<QuestionDto> = ArrayList(),
        course: MutableList<CourseDto> = mutableListOf(),
        permissions: MutableList<GrantedPermission> = mutableListOf()
    ): this() {
        this.id = id
        this.name = name
        this.timeLimit = timeLimit
        this.score = score
        this.automaticScore = automaticScore
        this.publishedAt = publishedAt
        this.questions = questions
        this.courses = course
        this.permissions = permissions
    }

    constructor(entity: Quiz, permissions: List<GrantedPermission>,
                includeQuestions: Boolean = true, includeCourses: Boolean = false) : this(
        entity.id,
        entity.name,
        entity.timeLimit,
        entity.score,
        entity.automaticScore,
        entity.publishedAt,
    ) {
        if (includeQuestions) {
            this.questions = entity.questions.map { QuestionDto(it) }.toMutableList()
        }
        if (includeCourses) {
            this.courses = entity.courses.map { CourseDto(it, emptyList()) }.toMutableList()
        }
        this.permissions = permissions
    }

    @JsonView(JacksonView.Overview::class, JacksonView.Answers::class)
    var id: UUID? = null

    @JsonView(JacksonView.Overview::class, JacksonView.Answers::class)
    var name: String? = null

    @JsonView(JacksonView.Overview::class, JacksonView.Answers::class)
    var timeLimit: Int? = null

    @JsonView(JacksonView.Overview::class, JacksonView.Answers::class)
    var score: Double? = null

    @JsonView(JacksonView.Overview::class)
    var publishedAt: Instant? = null

    @JsonView(JacksonView.Write::class)
    var automaticScore: Boolean? = null

    @JsonView(JacksonView.Read::class)
    var questions: MutableList<QuestionDto> = ArrayList()
    set(value) {
        field = value
        questionCount = value.size
    }

    @JsonView(JacksonView.Overview::class)
    var questionCount: Int? = null

    @JsonIgnoreProperties("quizzes", "permissions")
    @JsonView(JacksonView.Overview::class)
    var courses: MutableList<CourseDto> = ArrayList()

    @JsonView(JacksonView.Overview::class)
    var loadLevel: JacksonViewEnum? = null

    @JsonView(JacksonView.Overview::class)
    var permissions: List<GrantedPermission> = emptyList()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as QuizDto

        if (id != other.id) return false
        if (name != other.name) return false
        if (timeLimit != other.timeLimit) return false
        if (score != other.score) return false
        if (publishedAt != other.publishedAt) return false
        if (automaticScore != other.automaticScore) return false
        if (questions != other.questions) return false
        if (questionCount != other.questionCount) return false
        if (courses != other.courses) return false
        if (loadLevel != other.loadLevel) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + (name?.hashCode() ?: 0)
        result = 31 * result + (timeLimit ?: 0)
        result = 31 * result + (score?.hashCode() ?: 0)
        result = 31 * result + (publishedAt?.hashCode() ?: 0)
        result = 31 * result + (automaticScore?.hashCode() ?: 0)
        result = 31 * result + questions.hashCode()
        result = 31 * result + (questionCount ?: 0)
        result = 31 * result + courses.hashCode()
        result = 31 * result + (loadLevel?.hashCode() ?: 0)
        return result
    }


}