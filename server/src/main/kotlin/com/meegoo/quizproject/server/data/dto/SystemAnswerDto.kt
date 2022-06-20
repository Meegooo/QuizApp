package com.meegoo.quizproject.server.data.dto
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.TreeNode
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.NumericNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import com.meegoo.quizproject.server.util.asUuidList
import org.springframework.boot.jackson.JsonComponent
import java.util.*

@JsonInclude(value = JsonInclude.Include.NON_NULL)
sealed class SystemAnswerDto(val userScore: Double) {
    class StringAnswer(userScore: Double, val answer: String) : SystemAnswerDto(userScore)
    class ChoiceAnswer(userScore: Double, val answer: List<UUID>) : SystemAnswerDto(userScore)

    @JsonComponent
    class DataDeserializer : JsonDeserializer<SystemAnswerDto>() {
        override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): SystemAnswerDto {
            when (val value = p.readValueAsTree<TreeNode>()) {
                is ObjectNode -> {
                    val valueNode = value["value"] ?: throw JsonParseException(p, "value field cannot be null")
                    val userScoreNode = value["userScore"] as? NumericNode ?: throw JsonParseException(
                        p,
                        "userScore field cannot be null"
                    )

                    return when (valueNode) {
                        is TextNode -> StringAnswer(userScoreNode.doubleValue(), valueNode.textValue())
                        is ArrayNode -> ChoiceAnswer(userScoreNode.doubleValue(), valueNode.asUuidList(p))
                        else -> throw JsonParseException(p, "Unexpected value $value")
                    }
                }
                else -> throw JsonParseException(p, "Unexpected value $value")
            }
        }

    }

    @JsonComponent
    class DataSerializer : JsonSerializer<SystemAnswerDto>() {
        override fun serialize(value: SystemAnswerDto, gen: JsonGenerator, serializers: SerializerProvider?) {
            gen.writeStartObject()
            gen.writeNumberField("userScore", value.userScore)
            gen.writeFieldName("value")
            when (value) {
                is StringAnswer -> gen.writeString(value.answer)
                is ChoiceAnswer -> gen.writeArray(value.answer.map { it.toString() }.toTypedArray(), 0, value.answer.size)
            }
            gen.writeEndObject()
        }
    }
}