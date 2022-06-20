package com.meegoo.quizproject.server.data.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonView
import com.meegoo.quizproject.server.data.JacksonView
import com.meegoo.quizproject.server.data.QuestionType
import com.vladmihalcea.hibernate.type.json.JsonBinaryType
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import java.util.*
import javax.persistence.*

@Entity
@TypeDef(name = "jsonb", typeClass = JsonBinaryType::class)
@Table(name = "questions")
class Question(
    @Id
    var id: UUID,

    var question: String,

    var type: QuestionType,

    var index: Int,

    @ManyToOne
    @JoinColumn(name = "quiz_id")
    var quiz: Quiz,

    var weight: Double,
) {

    constructor(
        question: String,
        type: QuestionType,
        quiz: Quiz,
        index: Int,
        weight: Double = 1.0
    ) : this(UUID.randomUUID(), question, type, index, quiz, weight)

    @Type(type = "jsonb")
    var answers: MutableList<Answer> = mutableListOf()

    @Type(type = "jsonb")
    var options: MutableMap<String, String> = mutableMapOf()

    val baseScore: Double
        get() = options["base_score"]?.toDoubleOrNull() ?: 0.0

    val maxScore: Double
        get() = options["max_score"]?.toDoubleOrNull() ?: 1.0

    val numericPrecision: Int
        get() = options["numeric_precision"]?.toIntOrNull() ?: 2

    val ignoreCase: Boolean
        get() = options["ignore_case"]?.toBoolean() ?: true

    val trimPadding: Boolean
        get() = options["trim_padding"]?.toBoolean() ?: true

    data class Answer(
        var id: UUID = UUID.randomUUID(),
        var text: String,
        var chosenScore: Double,
        var notChosenScore: Double = 0.0
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Question

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}