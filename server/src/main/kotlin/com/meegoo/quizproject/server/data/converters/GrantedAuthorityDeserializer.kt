package com.meegoo.quizproject.server.data.converters

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.util.*


class GrantedAuthorityDeserializer : JsonDeserializer<List<GrantedAuthority>>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): List<GrantedAuthority> {
        val jsonNode: JsonNode = p.codec.readTree(p)
        val grantedAuthorities: MutableList<GrantedAuthority> = LinkedList()

        val elements: Iterator<JsonNode> = jsonNode.elements()
        while (elements.hasNext()) {
            val next: JsonNode = elements.next()
            val authority: JsonNode = next.get("authority")
            grantedAuthorities.add(SimpleGrantedAuthority(authority.asText()))
        }
        return grantedAuthorities
    }

}