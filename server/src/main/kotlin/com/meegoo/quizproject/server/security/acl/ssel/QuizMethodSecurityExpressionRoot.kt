package com.meegoo.quizproject.server.security.acl.ssel

import com.meegoo.quizproject.server.data.repositories.QuizRepository
import com.meegoo.quizproject.server.security.acl.AclPermissionEvaluatorWithOwner
import org.springframework.security.access.PermissionEvaluator
import org.springframework.security.access.expression.SecurityExpressionRoot
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations
import org.springframework.security.core.Authentication
import java.util.*

class QuizMethodSecurityExpressionRoot(
    authentication: Authentication,
    private val permissionEvaluator: PermissionEvaluator,
    private val quizRepository: QuizRepository
) : SecurityExpressionRoot(authentication), MethodSecurityExpressionOperations {

    fun hasPermissionThroughCourse(quizId: UUID): Boolean {
        val findById = quizRepository.findById(quizId)
        return if (findById.isEmpty) {
            false
        } else {
            val courses = findById.get().courses
            courses.any { course ->
                permissionEvaluator.hasPermission(authentication, course, "READ")
            }
        }
    }

    override fun setFilterObject(filterObject: Any?) {
        TODO("Not yet implemented")
    }

    override fun getFilterObject(): Any {
        TODO("Not yet implemented")
    }

    override fun setReturnObject(returnObject: Any?) {
        TODO("Not yet implemented")
    }

    override fun getReturnObject(): Any {
        TODO("Not yet implemented")
    }

    override fun getThis(): Any {
        TODO("Not yet implemented")
    }
}