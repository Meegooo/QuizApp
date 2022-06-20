package com.meegoo.quizproject.android.data.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.TreeNode
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.node.ObjectNode
import java.lang.IllegalArgumentException
import java.util.*

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(value = JsonInclude.Include.NON_NULL)
class QuestionDto() {

    constructor(
        id: UUID? = null,
        question: String? = null,
        type: QuestionType? = null,
        weight: Double? = null,
        answerDtos: MutableList<AnswerDto>? = null,
        index: Int? = null,
        baseScore: Double? = null,
        maxScore: Double? = null,
        ignoreCase: Boolean? = null,
        numericPrecision: Int? = null,
        trimPadding: Boolean? = null,
    ) : this() {
        this.id = id
        this.question = question
        this.type = type
        this.weight = weight
        this.index = index
        this.answers = answerDtos
        this.baseScore = baseScore
        this.maxScore = maxScore
        this.ignoreCase = ignoreCase
        this.numericPrecision = numericPrecision
        this.trimPadding = trimPadding
    }


    var id: UUID? = null

    var question: String? = null

    var type: QuestionType? = null

    var weight: Double? = null

    var answers: MutableList<AnswerDto>? = null

    var baseScore: Double? = null

    var maxScore: Double? = null

    var numericPrecision: Int? = null

    var index: Int? = null

    var ignoreCase: Boolean? = null

    var trimPadding: Boolean? = null


    enum class QuestionType {
        SINGLE_ANSWER,
        MULTIPLE_ANSWER,
        TEXT_FIELD,
        NUMERIC_FIELD
    }

    class AnswerDto() {
        constructor(
            id: UUID? = null,
            text: String = "",
            chosenScore: Double = 0.0,
            notChosenScore: Double = 0.0,
        ) : this() {
            this.id = id
            this.text = text
            this.chosenScoreAsString = chosenScore.toString()
            this.notChosenScoreAsString = notChosenScore.toString()
        }

        constructor(text: String, chosenScore: Double): this(id = null, text = text, chosenScore = chosenScore)

        var id: UUID? = null
        var text: String = ""

        val chosenScore: Double
            get() {
                return if (chosenScoreAsString.isEmpty() || chosenScoreAsString == "-") 0.0 else chosenScoreAsString.toDouble()
            }

        val notChosenScore: Double
        get() {
            return if (notChosenScoreAsString.isEmpty() || chosenScoreAsString == "-") 0.0 else notChosenScoreAsString.toDouble()
        }


        var chosenScoreAsString = "0.0"
        set(value) {
            val v = value.replace(",", ".").trim()
            if (v.isNotEmpty() && v != "-" && v.toDoubleOrNull() == null) {
                throw IllegalArgumentException("Score needs to represent a floating point number")
            }
            field = v
        }

        var notChosenScoreAsString = "0.0"
            set(value) {
                val v = value.replace(",", ".").trim()
                if (v.isNotEmpty() && v != "-" && v.toDoubleOrNull() == null) {
                    throw IllegalArgumentException("Score needs to represent a floating point number")
                }
                field = v
            }

        class DataDeserializer : JsonDeserializer<AnswerDto>() {
            override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): AnswerDto {
                when (val value = p.readValueAsTree<TreeNode>()) {
                    is ObjectNode -> {
                        val id = value["id"]?.asText(null)
                        val text = value["text"]?.asText("") ?: ""
                        val chosenScore = value["chosenScore"]?.asDouble(0.0) ?: 0.0
                        val notChosenScore = value["notChosenScore"]?.asDouble(0.0) ?: 0.0
                        val uuid = if (id == null) null else UUID.fromString(id)
                        return AnswerDto(uuid, text, chosenScore, notChosenScore)
                    }
                    else -> throw JsonParseException(p, "Unexpected value $value")
                }
            }

        }

        class DataSerializer : JsonSerializer<AnswerDto>() {
            override fun serialize(value: AnswerDto, gen: JsonGenerator, serializers: SerializerProvider?) {
                gen.writeStartObject()
                if (value.id != null) {
                    gen.writeStringField("id", value.id.toString())
                }
                gen.writeStringField("text", value.text)
                gen.writeNumberField("chosenScore", value.chosenScore)
                gen.writeNumberField("notChosenScore", value.notChosenScore)
                gen.writeEndObject()
            }
        }
    }

}