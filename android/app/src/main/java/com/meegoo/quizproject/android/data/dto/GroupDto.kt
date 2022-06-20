package com.meegoo.quizproject.server.data.dto

import com.meegoo.quizproject.android.data.dto.GrantedPermission
import java.util.*
import kotlin.collections.HashMap

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

    var id: UUID? = null
    var name: String? = null
    var users: Map<String, GrantedPermission> = HashMap()
    var permissions: List<GrantedPermission> = emptyList()

}