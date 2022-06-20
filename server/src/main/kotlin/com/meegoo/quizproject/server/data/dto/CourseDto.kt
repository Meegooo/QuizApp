package com.meegoo.quizproject.server.data.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonView
import com.meegoo.quizproject.server.data.JacksonView
import com.meegoo.quizproject.server.data.entity.Course
import java.util.*

@JsonInclude(value = JsonInclude.Include.NON_NULL)
class CourseDto() {

    constructor(
        id: UUID? = null,
        name: String? = null,
        quizzes: MutableSet<QuizDto> = HashSet(),
        permissions: List<GrantedPermission> = emptyList()
    ) : this() {
        this.id = id
        this.name = name
        this.quizzes = quizzes
        this.permissions = permissions
    }

    constructor(
        course: Course, permissions: List<GrantedPermission>,
        includeQuizzes: Boolean = true
    ) : this(course.id, course.name, permissions = permissions) {
        if (includeQuizzes) {
            this.quizzes = course.quizzes.map { QuizDto(it, emptyList()) }.toMutableSet()
        }
    }

    @JsonView(JacksonView.Overview::class)
    var id: UUID? = null

    @JsonView(JacksonView.Overview::class)
    var name: String? = null

    @JsonView(JacksonView.Overview::class)
    @JsonIgnoreProperties("courses, permissions")
    var quizzes: MutableSet<QuizDto> = HashSet()

    @JsonView(JacksonView.Overview::class)
    var permissions: List<GrantedPermission> = emptyList()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CourseDto

        if (id != other.id) return false
        if (name != other.name) return false
        if (quizzes != other.quizzes) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + (name?.hashCode() ?: 0)
        result = 31 * result + quizzes.hashCode()
        return result
    }


}