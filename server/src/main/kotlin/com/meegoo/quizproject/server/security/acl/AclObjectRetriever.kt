package com.meegoo.quizproject.server.security.acl

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.security.acls.domain.GrantedAuthoritySid
import org.springframework.security.acls.domain.PrincipalSid

import org.springframework.security.acls.model.Sid
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Service
import java.util.*

@Service
class AclObjectRetriever(
    private val jdbcTemplate: JdbcTemplate,
    private val groupSidRetrievalStrategy: GroupSidRetrievalStrategy
) {
    private val FIND_OBJECTS_WITH_ACCESS =
        """SELECT obj.object_id_identity AS obj_id,
       class.class            AS class,
       entry.mask
FROM (acl_entry as entry
         LEFT JOIN acl_object_identity as obj
                   ON entry.acl_object_identity = obj.id
         LEFT JOIN acl_class as class
                   ON obj.object_id_class = class.id)
WHERE
    class.class = :class AND
    entry.granting = true AND
    (
        entry.sid in (SELECT id FROM acl_sid WHERE sid IN (:sids))
        or obj.owner_sid in (SELECT id FROM acl_sid WHERE sid IN (:sids))
    );""".trimMargin()

    val namedJdbcTemplate = NamedParameterJdbcTemplate(jdbcTemplate)


    fun getObjectsWithAccess(clazz: Class<*>, authentication: Authentication): List<AclRetrieverResponse> {
        val sids = groupSidRetrievalStrategy.getSids(authentication)
        val args =
            mapOf("class" to clazz.name, "sids" to sids.map { sidToString(it) })
        return namedJdbcTemplate.query(FIND_OBJECTS_WITH_ACCESS, args, getRowMapper())
    }

    private fun sidToString(sid: Sid): String {
        return if (sid is GroupSid) {
            val sidName = sid.group
            "group:$sidName"
        } else if (sid is PrincipalSid) {
            val sidName = sid.principal
            "user:$sidName"
        } else if (sid is GrantedAuthoritySid) {
            val sidName = sid.grantedAuthority
            sidName
        } else ""
    }

    data class AclRetrieverResponse(val uuid: UUID, val mask: Int)

    private fun getRowMapper(): RowMapper<AclRetrieverResponse> {
        return RowMapper<AclRetrieverResponse> { rs, rowNum ->
            val identifier: UUID = UUID.fromString(rs.getString("obj_id"))
            val mask: Int = rs.getInt("mask")
            AclRetrieverResponse(identifier, mask)
        }
    }
}