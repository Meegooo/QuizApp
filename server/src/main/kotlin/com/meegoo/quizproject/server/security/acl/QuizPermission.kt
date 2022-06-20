package com.meegoo.quizproject.server.security.acl

import org.springframework.security.acls.domain.AbstractPermission
import org.springframework.security.acls.domain.CumulativePermission
import org.springframework.security.acls.model.Permission

class QuizPermission : AbstractPermission {

    companion object {
        @JvmField
        var READ: Permission = QuizPermission(1 shl 0, 'R') // 1

        @JvmField
        var WRITE: Permission = QuizPermission(1 shl 1, 'W') // 2

        @JvmField
        var SHARE: Permission = QuizPermission(1 shl 2, 'S') // 4

        @JvmField
        var ADMINISTRATION: Permission = QuizPermission(1 shl 4, 'A') // 16

        fun parsePermissions(permissionList: List<String>): CumulativePermission {
            val permission = CumulativePermission()
            for (s in permissionList.map { it.toLowerCase() }) {
                if (s == "r" || s == "read") {
                    permission.set(READ)
                } else if (s == "w" || s == "write") {
                    permission.set(WRITE)
                } else if (s == "s" || s == "share") {
                    permission.set(SHARE)
                } else if (s == "a" || s == "administration") {
                    permission.set(ADMINISTRATION)
                }
            }
            return permission
        }
    }


    private constructor(mask: Int) : super(mask)

    private constructor(mask: Int, code: Char) : super(mask, code)

}