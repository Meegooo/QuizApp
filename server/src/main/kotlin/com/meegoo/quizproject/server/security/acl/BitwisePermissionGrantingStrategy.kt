package com.meegoo.quizproject.server.security.acl

import org.springframework.security.acls.domain.AuditLogger
import org.springframework.security.acls.domain.DefaultPermissionGrantingStrategy
import org.springframework.security.acls.model.AccessControlEntry
import org.springframework.security.acls.model.Permission

class BitwisePermissionGrantingStrategy(auditLogger: AuditLogger?) : DefaultPermissionGrantingStrategy(auditLogger) {

    override fun isGranted(ace: AccessControlEntry, p: Permission): Boolean {
        return ace.permission.mask and p.mask == p.mask
    }
}