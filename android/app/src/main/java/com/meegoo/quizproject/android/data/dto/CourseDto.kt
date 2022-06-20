package com.meegoo.quizproject.android.data.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import java.util.*

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(value = JsonInclude.Include.NON_NULL)
class CourseDto() {

    constructor(id: UUID? = null,
                name: String? = null,
                quizzes: MutableSet<QuizDto> = HashSet()) :this() {
        this.id = id
        this.name = name
        this.quizzes = quizzes
    }


    var id: UUID? = null
    var name: String? = null
    var quizzes: MutableSet<QuizDto> = HashSet()

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
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