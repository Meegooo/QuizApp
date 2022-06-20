package com.meegoo.quizproject.server.security.acl.ssel

import com.meegoo.quizproject.server.data.repositories.QuizRepository
import com.meegoo.quizproject.server.security.acl.AclPermissionEvaluatorWithOwner
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations
import org.aopalliance.intercept.MethodInvocation
import org.springframework.security.core.Authentication

class QuizMethodSecurityExpressionHandler(
    private val quizRepository: QuizRepository
) : DefaultMethodSecurityExpressionHandler() {

    override fun createSecurityExpressionRoot(
        authentication: Authentication, invocation: MethodInvocation
    ): MethodSecurityExpressionOperations {
        val root = QuizMethodSecurityExpressionRoot(authentication, permissionEvaluator, quizRepository)
        root.setPermissionEvaluator(permissionEvaluator)
        root.setTrustResolver(trustResolver)
        root.setRoleHierarchy(roleHierarchy)
        return root
    }
}