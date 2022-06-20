package com.meegoo.quizproject.server.security.acl

import org.springframework.security.acls.domain.GrantedAuthoritySid
import org.springframework.security.acls.domain.PrincipalSid
import org.springframework.security.acls.jdbc.JdbcMutableAclService
import org.springframework.security.acls.jdbc.LookupStrategy
import org.springframework.security.acls.model.AclCache
import org.springframework.security.acls.model.Sid
import org.springframework.util.Assert
import javax.sql.DataSource

class GroupJdbcMutableAclService(dataSource: DataSource?, lookupStrategy: LookupStrategy?, aclCache: AclCache?) :
    JdbcMutableAclService(dataSource, lookupStrategy, aclCache) {
    override fun createOrRetrieveSidPrimaryKey(sid: Sid?, allowCreate: Boolean): Long {
        Assert.notNull(sid, "Sid required")
        if (sid is GroupSid) {
            val sidName = sid.group
            return createOrRetrieveSidPrimaryKey("group:$sidName", true, allowCreate)
        }
        if (sid is PrincipalSid) {
            val sidName = sid.principal
            return createOrRetrieveSidPrimaryKey("user:$sidName", true, allowCreate)
        }
        if (sid is GrantedAuthoritySid) {
            val sidName = sid.grantedAuthority
            return createOrRetrieveSidPrimaryKey(sidName, false, allowCreate)
        }
        throw IllegalArgumentException("Unsupported implementation of Sid")
    }
}