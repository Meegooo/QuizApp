package com.meegoo.quizproject.server.security.acl

import org.apache.commons.logging.LogFactory
import org.springframework.core.log.LogMessage
import org.springframework.security.access.PermissionEvaluator
import org.springframework.security.acls.domain.DefaultPermissionFactory
import org.springframework.security.acls.domain.ObjectIdentityRetrievalStrategyImpl
import org.springframework.security.acls.domain.PermissionFactory
import org.springframework.security.acls.domain.SidRetrievalStrategyImpl
import org.springframework.security.acls.model.*
import org.springframework.security.core.Authentication
import java.io.Serializable
import java.util.*

class AclPermissionEvaluatorWithOwner(private val aclService: AclService) : PermissionEvaluator {

    private val logger = LogFactory.getLog(javaClass)

    var objectIdentityRetrievalStrategy: ObjectIdentityRetrievalStrategy = ObjectIdentityRetrievalStrategyImpl()

    var objectIdentityGenerator: ObjectIdentityGenerator = ObjectIdentityRetrievalStrategyImpl()

    var sidRetrievalStrategy: SidRetrievalStrategy = SidRetrievalStrategyImpl()

    var permissionFactory: PermissionFactory = DefaultPermissionFactory()


    override fun hasPermission(authentication: Authentication, domainObject: Any?, permission: Any): Boolean {
        if (domainObject == null) {
            return false
        }
        val objectIdentity = objectIdentityRetrievalStrategy.getObjectIdentity(domainObject)
        return checkPermission(authentication, objectIdentity, permission)
    }

    override fun hasPermission(
        authentication: Authentication, targetId: Serializable, targetType: String,
        permission: Any
    ): Boolean {
        val objectIdentity = objectIdentityGenerator.createObjectIdentity(targetId, targetType)
        return checkPermission(authentication, objectIdentity, permission)
    }


    private fun checkPermission(authentication: Authentication, oid: ObjectIdentity, permission: Any): Boolean {
        // Obtain the SIDs applicable to the principal
        val sids = sidRetrievalStrategy.getSids(authentication)
        logger.debug(LogMessage.of { "Checking permission '$permission' for object '$oid'" })
        try {
            // Lookup only ACLs for SIDs we're interested in
            val acl = aclService.readAclById(oid, sids)
            if (acl.owner in sids) {
                logger.debug("Access is granted for owner")
                return true
            }
            if (permission == "OWNER") {
                return false
            }
            val requiredPermission = resolvePermission(permission)
            if (acl.isGranted(requiredPermission, sids, false)) {
                logger.debug("Access is granted")
                return true
            }
            logger.debug("Returning false - ACLs returned, but insufficient permissions for this principal")
        } catch (nfe: NotFoundException) {
            logger.debug("Returning false - no ACLs apply for this principal")
        }
        return false
    }

    private fun resolvePermission(permission: Any): List<Permission> {
        if (permission is Int) {
            return listOf(permissionFactory.buildFromMask(permission))
        }
        if (permission is Permission) {
            return listOf(permission)
        }
        if (permission is Array<*> && permission.isArrayOf<Permission>()) {
            return listOf(*permission as Array<Permission>)
        }
        if (permission is String) {
            val p = buildPermission(permission)
            if (p != null) {
                return listOf(p)
            }
        }
        throw IllegalArgumentException("Unsupported permission: $permission")
    }


    private fun buildPermission(permString: String): Permission? {
        return try {
            permissionFactory.buildFromName(permString)
        } catch (notfound: IllegalArgumentException) {
            permissionFactory.buildFromName(permString.toUpperCase(Locale.ENGLISH))
        }
    }
}