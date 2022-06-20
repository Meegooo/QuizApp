package com.meegoo.quizproject.server.data.dto

import com.fasterxml.jackson.annotation.JsonIgnore
import com.meegoo.quizproject.server.security.acl.GroupSid
import com.meegoo.quizproject.server.security.acl.QuizPermission
import org.springframework.security.acls.domain.CumulativePermission
import org.springframework.security.acls.domain.PrincipalSid
import org.springframework.security.acls.model.Permission
import org.springframework.security.acls.model.Sid
import java.lang.IllegalArgumentException

class AclDto {
    constructor()

    constructor(sid: Sid, permission: Permission, isOwner: Boolean) {
        if (sid is PrincipalSid) {
            this.sid = "user:${sid.principal}"
        } else if (sid is GroupSid) {
            this.sid = "group:${sid.group}"
        } else throw IllegalArgumentException("Only GroupSid and PrincipalSid are supported")


        this.permissions = when {
            isOwner -> {
                GrantedPermission.OWNER.toListGranting()
            }
            permission.mask.and(GrantedPermission.ADMINISTRATION.permission!!.mask) > 0 -> {
                GrantedPermission.ADMINISTRATION.toListGranting()
            }
            permission.mask.and(GrantedPermission.WRITE.permission!!.mask) > 0 -> {
                GrantedPermission.WRITE.toListGranting()
            }
            permission.mask.and(GrantedPermission.SHARE.permission!!.mask) > 0 -> {
                GrantedPermission.SHARE.toListGranting()
            }
            permission.mask.and(GrantedPermission.READ.permission!!.mask) > 0 -> {
                GrantedPermission.READ.toListGranting()
            }
            else -> {
                emptyList()
            }
        }
    }
    @get:JsonIgnore
    val sidObject by lazy {
        if (sid.startsWith("user:")) {
            PrincipalSid(sid.substring(5))
        } else if (sid.startsWith("group:")) {
            GroupSid(sid.substring(6))
        } else {
            throw IllegalArgumentException("sid must start with 'user:' or 'group:")
        }
    }

    var sid: String = ""
    var permissions: List<GrantedPermission> = emptyList()
}

enum class GrantedPermission(val permission: Permission?) {
   READ(QuizPermission.READ),
   WRITE(QuizPermission.WRITE),
   SHARE(QuizPermission.SHARE),
   ADMINISTRATION(QuizPermission.ADMINISTRATION),
   OWNER(null);

    fun toListGranting(): List<GrantedPermission> {
        return when (this) {
            OWNER -> {
                listOf(OWNER, ADMINISTRATION, WRITE, SHARE, READ)
            }
            ADMINISTRATION -> {
                listOf(ADMINISTRATION, WRITE, SHARE, READ)
            }
            WRITE -> {
                listOf(WRITE, SHARE, READ)
            }
            SHARE -> {
                listOf(SHARE, READ)
            }
            READ -> {
                listOf(READ)
            }
        }
    }

    fun toListRevoking(): List<GrantedPermission> {
        return when (this) {
            OWNER -> {
                listOf()
            }
            ADMINISTRATION -> {
                listOf(ADMINISTRATION)
            }
            WRITE -> {
                listOf(ADMINISTRATION, WRITE)
            }
            SHARE -> {
                listOf(ADMINISTRATION, WRITE, SHARE)
            }
            READ -> {
                listOf(ADMINISTRATION, WRITE, SHARE, READ)
            }
        }
    }
}