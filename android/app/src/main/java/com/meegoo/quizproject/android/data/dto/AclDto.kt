package com.meegoo.quizproject.android.data.dto

import java.lang.IllegalArgumentException

class AclDto() {

    constructor(sid: String, permission: GrantedPermission) : this() {
        this.sid = sid
        this.permissions = listOf(permission)
    }
    constructor(sid: String, permissions: List<GrantedPermission>) : this() {
        this.sid = sid
        this.permissions = ArrayList(permissions)
    }
    var sid: String = ""
    var permissions: List<GrantedPermission> = emptyList()
}

enum class GrantedPermission(val level: Int, val label: String) {
    NONE(0, "None"),
    READ(1, "Read"),
    SHARE(2, "Share"),
    WRITE(3, "Write"),
    ADMINISTRATION(4, "Admin"),
    OWNER(5, "Owner");

    companion object {
        fun from(label: String): GrantedPermission {
            return when (label) {
                "None" -> NONE
                "Read" -> READ
                "Share" -> SHARE
                "Write" -> WRITE
                "Admin" -> ADMINISTRATION
                "Owner" -> OWNER
                else -> throw IllegalArgumentException("Label for GrantedPermission not found")
            }
        }
    }
}