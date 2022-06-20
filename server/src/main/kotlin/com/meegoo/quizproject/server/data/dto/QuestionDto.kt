package com.meegoo.quizproject.server.data.dto

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonView
import com.meegoo.quizproject.server.data.JacksonView
import com.meegoo.quizproject.server.data.QuestionType
import com.meegoo.quizproject.server.data.entity.Question
import java.util.*

@JsonInclude(value = JsonInclude.Include.NON_NULL)
class QuestionDto() {

    constructor(
        id: UUID? = null,
        question: String? = null,
        type: QuestionType? = null,
        weight: Double? = null,
        answerDtos: MutableList<AnswerDto>? = null,
        index: Int? = null,
        options: MutableMap<String, String> = mutableMapOf()
    ) : this() {
        this.id = id
        this.question = question
        this.type = type
        this.weight = weight
        this.answers = answerDtos
        this.index = index
        this.options = options

        this.baseScore = options["base_score"]?.toDoubleOrNull()
        this.maxScore = options["max_score"]?.toDoubleOrNull()
        this.numericPrecision = options["numeric_precision"]?.toIntOrNull()
        this.trimPadding = options["trim_padding"]?.toBoolean()
        this.ignoreCase = options["ignore_case"]?.toBoolean()

    }

    constructor(question: Question) : this(
        question.id,
        question.question,
        question.type,
        question.weight,
        null,
        question.index,
        HashMap(question.options)
    ) {
        this.answers = question.answers.map { AnswerDto(it) }.toMutableList()
    }


    @JsonView(JacksonView.Overview::class)
    var id: UUID? = null

    @JsonView(JacksonView.Read::class)
    var question: String? = null

    @JsonView(JacksonView.Read::class)
    var type: QuestionType? = null

    @JsonView(JacksonView.Read::class)
    var weight: Double? = null

    @JsonView(JacksonView.Read::class)
    var index: Int? = null

    @JsonView(JacksonView.Read::class)
    var answers: MutableList<AnswerDto>? = null

    @JsonIgnore
    var options: MutableMap<String, String> = mutableMapOf()

    @JsonView(JacksonView.Write::class)
    var baseScore: Double? = null

    @JsonView(JacksonView.Write::class)
    var maxScore: Double? = null

    @JsonView(JacksonView.Write::class)
    var numericPrecision: Int? = null

    @JsonView(JacksonView.Write::class)
    var ignoreCase: Boolean? = null

    @JsonView(JacksonView.Write::class)
    var trimPadding: Boolean? = null

    class AnswerDto() {
        constructor(
            id: UUID?,
            text: String = "",
            chosenScore: Double = 0.0,
            notChosenScore: Double = 0.0
        ) : this() {
            this.id = id
            this.text = text
            this.chosenScore = chosenScore
            this.notChosenScore = notChosenScore
        }

        constructor(answer: Question.Answer) : this(
            answer.id,
            answer.text,
            answer.chosenScore,
            answer.notChosenScore,
        )

        @JsonView(JacksonView.Read::class)
        var id: UUID? = null

        @JsonView(JacksonView.Read::class)
        var text: String

        @JsonView(JacksonView.Write::class)
        var chosenScore: Double

        @JsonView(JacksonView.Write::class)
        var notChosenScore: Double

        init {
            this.text = ""
            this.chosenScore = 0.0
            this.notChosenScore = 0.0
        }
    }

}