package com.meegoo.quizproject.server.security.acl

import org.springframework.security.acls.model.Sid
import org.springframework.util.Assert

class GroupSid(group: String) : Sid {
    val group: String
    override fun equals(other: Any?): Boolean {
        return if (other !is GroupSid) {
            false
        } else other.group == group
        // Delegate to getPrincipal() to perform actual comparison (both should be
        // identical)
    }

    override fun hashCode(): Int {
        return group.hashCode()
    }

    override fun toString(): String {
        return "GroupSid[$group]"
    }

    init {
        Assert.hasText(group, "Group required")
        this.group = group
    }
}