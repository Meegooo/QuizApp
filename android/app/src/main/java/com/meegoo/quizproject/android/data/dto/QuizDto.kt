package com.meegoo.quizproject.android.data.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant
import java.util.*

@JsonIgnoreProperties(ignoreUnknown = true)
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
        course: MutableList<CourseDto> = mutableListOf()
    ): this() {
        this.id = id
        this.name = name
        this.timeLimit = timeLimit
        this.score = score
        this.automaticScore = automaticScore
        this.publishedAt = publishedAt
        this.questions = questions
        this.courses = course
    }

    var id: UUID? = null

    var name: String? = null

    var timeLimit: Int? = null

    var score: Double? = null

    var automaticScore: Boolean? = null

    var publishedAt: Instant? = null

    var questions: MutableList<QuestionDto> = ArrayList()

    var questionCount: Int? = null

    var courses: MutableList<CourseDto> = ArrayList()

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    var permissions: List<GrantedPermission> = emptyList()

    val timeLimitFormatted by lazy {
        when {
            timeLimit == null -> "00:00:00"
            timeLimit!! < 0 -> "Unlimited"
            else -> {
                val seconds = (timeLimit ?: 0) % 60
                val minutes = (timeLimit ?: 0) / 60 % 60
                val hours = (timeLimit ?: 0) / 3600
                String.format("%02d:%02d:%02d", hours, minutes, seconds)
            }
        }

    }

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    var loadLevel = LoadLevel.MISSING
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as QuizDto

        if (id != other.id) return false
        if (name != other.name) return false
        if (timeLimit != other.timeLimit) return false
        if (score != other.score) return false
        if (automaticScore != other.automaticScore) return false
        if (publishedAt != other.publishedAt) return false
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
        result = 31 * result + (automaticScore?.hashCode() ?: 0)
        result = 31 * result + (publishedAt?.hashCode() ?: 0)
        result = 31 * result + questions.hashCode()
        result = 31 * result + (questionCount ?: 0)
        result = 31 * result + courses.hashCode()
        result = 31 * result + loadLevel.hashCode()
        return result
    }


}