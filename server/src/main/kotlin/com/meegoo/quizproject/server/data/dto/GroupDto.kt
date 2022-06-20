package com.meegoo.quizproject.server.data.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonView
import com.meegoo.quizproject.server.data.JacksonView
import com.meegoo.quizproject.server.data.entity.Group
import java.util.*
import kotlin.collections.HashMap

@JsonInclude(value = JsonInclude.Include.NON_NULL)
class GroupDto() {

    constructor(
        id: UUID? = null,
        name: String? = null,
        users: Map<String, GrantedPermission> = mutableMapOf(),
        permissions: List<GrantedPermission> = emptyList()
    ) : this() {
        this.id = id
        this.name = name
        this.users = users
        this.permissions = permissions
    }

    @JsonView(JacksonView.Overview::class)
    var id: UUID? = null

    @JsonView(JacksonView.Overview::class)
    var name: String? = null

    @JsonView(JacksonView.Overview::class)
    var users: Map<String, GrantedPermission> = HashMap()

    @JsonView(JacksonView.Overview::class)
    var permissions: List<GrantedPermission> = emptyList()

}