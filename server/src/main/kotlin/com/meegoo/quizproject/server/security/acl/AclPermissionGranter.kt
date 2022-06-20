package com.meegoo.quizproject.server.security.acl

import com.meegoo.quizproject.server.security.acl.QuizPermission.Companion.parsePermissions
import org.springframework.security.acls.domain.ObjectIdentityImpl
import org.springframework.security.acls.domain.PrincipalSid
import org.springframework.security.acls.model.MutableAclService
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import java.util.*

@Service
class AclPermissionGranter(private val aclService: MutableAclService) {
    fun createObject(obj: Any) {
        val acl = aclService.createAcl(ObjectIdentityImpl(obj))
        val authentication = SecurityContextHolder.getContext().authentication
        val sid = PrincipalSid(authentication)
        val permissions = parsePermissions(listOf("r", "w", "s", "a"))
        acl.insertAce(0, permissions, sid, true)
        aclService.updateAcl(acl)
    }

}