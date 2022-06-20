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
import com.fasterxml.jackson.databind.node.TextNode
import com.meegoo.quizproject.server.util.asUuidList
import org.springframework.boot.jackson.JsonComponent
import java.util.*

@JsonInclude(value = JsonInclude.Include.NON_NULL)
sealed class UserAnswerDto {
    class StringAnswer(val answer: String) : UserAnswerDto()
    class ChoiceAnswer(val answer: List<UUID>) : UserAnswerDto()

    @JsonComponent
    class DataDeserializer : JsonDeserializer<UserAnswerDto>() {
        override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): UserAnswerDto {
            return when (val value = p.readValueAsTree<TreeNode>()) {
                is TextNode -> StringAnswer(value.textValue())
                is ArrayNode -> ChoiceAnswer(value.asUuidList(p))
                else -> throw JsonParseException(p, "Unexpected value $value")
            }
        }
    }

    @JsonComponent
    class DataSerializer : JsonSerializer<UserAnswerDto>() {
        override fun serialize(value: UserAnswerDto?, gen: JsonGenerator, serializers: SerializerProvider?) {
            when (value) {
                is StringAnswer -> gen.writeString(value.answer)
                is ChoiceAnswer -> gen.writeArray(
                    value.answer.map { it.toString() }.toTypedArray(),
                    0,
                    value.answer.size
                )
            }

        }
    }

    companion object {
        fun parseFromEntity(data: Any): UserAnswerDto {
            return if (data is List<*>) {
                ChoiceAnswer(data.map { UUID.fromString(it.toString()) })
            } else {
                StringAnswer(data.toString())
            }
        }

        fun convertToEntity(data: UserAnswerDto): Any {
            return if (data is StringAnswer) {
                data.answer
            } else {
                (data as ChoiceAnswer).answer.map { it.toString() }
            }

        }

    }


}