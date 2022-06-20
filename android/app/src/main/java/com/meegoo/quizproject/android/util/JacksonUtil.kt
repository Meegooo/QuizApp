package com.meegoo.quizproject.server.util

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.TextNode
import java.util.*
import kotlin.collections.ArrayList

fun ArrayNode.asUuidList(p: JsonParser): ArrayList<UUID> {
    val list = ArrayList<UUID>()
    for (element in this.elements()) {
        if (element is TextNode) {
            try {
                list.add(UUID.fromString(element.textValue()))
            } catch (e: IllegalArgumentException) {
                throw JsonParseException(p, "Expected UUID, found ${element.textValue()}")
            }
        } else {
            throw JsonParseException(p, "Expected UUID, found $this")
        }
    }
    return list
}