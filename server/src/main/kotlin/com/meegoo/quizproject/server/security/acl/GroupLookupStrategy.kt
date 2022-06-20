package com.meegoo.quizproject.server.security.acl

import org.springframework.security.acls.domain.*
import org.springframework.security.acls.jdbc.BasicLookupStrategy
import org.springframework.security.acls.model.AclCache
import org.springframework.security.acls.model.PermissionGrantingStrategy
import org.springframework.security.acls.model.Sid
import java.lang.IllegalArgumentException
import javax.sql.DataSource

class GroupLookupStrategy(
    dataSource: DataSource?, aclCache: AclCache?,
    aclAuthorizationStrategy: AclAuthorizationStrategy?, grantingStrategy: PermissionGrantingStrategy?
) : BasicLookupStrategy(dataSource, aclCache, aclAuthorizationStrategy, grantingStrategy) {

    constructor(
        dataSource: DataSource?, aclCache: AclCache?,
        aclAuthorizationStrategy: AclAuthorizationStrategy?, auditLogger: AuditLogger
    ) : this(dataSource, aclCache, aclAuthorizationStrategy, DefaultPermissionGrantingStrategy(auditLogger))

    override fun createSid(isPrincipal: Boolean, sid: String?): Sid {
        return if (isPrincipal) {
            if (sid?.startsWith("user:") == true) {
                PrincipalSid(sid.substring(5))
            } else if (sid?.startsWith("group:") == true) {
                GroupSid(sid.substring(6))
            } else {
                throw IllegalArgumentException("sid must start with 'user:' or 'group:")
            }
        } else GrantedAuthoritySid(sid)
    }
}