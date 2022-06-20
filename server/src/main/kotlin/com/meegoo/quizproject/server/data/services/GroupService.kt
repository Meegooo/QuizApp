package com.meegoo.quizproject.server.data.services

import com.meegoo.quizproject.server.controllers.hooks.BadRequestException
import com.meegoo.quizproject.server.controllers.hooks.GroupTakenException
import com.meegoo.quizproject.server.controllers.hooks.NotFoundException
import com.meegoo.quizproject.server.data.dto.GrantedPermission
import com.meegoo.quizproject.server.data.dto.GroupDto
import com.meegoo.quizproject.server.data.entity.Group
import com.meegoo.quizproject.server.data.repositories.AccountRepository
import com.meegoo.quizproject.server.data.repositories.GroupRepository
import com.meegoo.quizproject.server.security.acl.AclPermissionGranter
import com.meegoo.quizproject.server.security.acl.GroupSidRetrievalStrategy
import com.meegoo.quizproject.server.security.jwt.JwtUserDetailsService
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.acls.domain.ObjectIdentityImpl
import org.springframework.security.acls.domain.PrincipalSid
import org.springframework.security.acls.model.MutableAclService
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class GroupService(
    private val jwtUserDetailsService: JwtUserDetailsService,
    private val groupRepository: GroupRepository,
    private val permissionService: PermissionService,
    private val groupSidRetrievalStrategy: GroupSidRetrievalStrategy,
    private val aclPermissionGranter: AclPermissionGranter,
    private val aclService: MutableAclService,
    private val accountRepository: AccountRepository
) {
    fun getGroup(uuid: UUID): GroupDto {
        val auth = SecurityContextHolder.getContext().authentication
            ?: throw IllegalStateException("No authentication available")
        val group = groupRepository.findById(uuid).orElseThrow { NotFoundException("Group", uuid) }
        return convertGroupToDto(auth, group)
    }

    fun getGroups(): List<GroupDto> {
        val auth = SecurityContextHolder.getContext().authentication
            ?: throw IllegalStateException("No authentication available")
        return if (auth.authorities.contains(SimpleGrantedAuthority("ROLE_ADMIN"))) {
            groupRepository.findAll().map { convertGroupToDto(auth, it) }
        } else {
            val account = jwtUserDetailsService.loadUserByUsername(auth.name)
            account.groups.map { convertGroupToDto(auth, it) }
        }
    }

    @Transactional
    fun createGroup(name: String): GroupDto {
        val auth = SecurityContextHolder.getContext().authentication
            ?: throw IllegalStateException("No authentication available")

        if (groupRepository.existsByName(name)) {
            throw GroupTakenException()
        }
        val account = jwtUserDetailsService.loadUserByUsername(auth.name)
        var newGroup = Group(name)
        newGroup = groupRepository.save(newGroup)
        account.groups.add(newGroup)
        accountRepository.save(account)
        aclPermissionGranter.createObject(newGroup)
        return GroupDto(
            newGroup.id,
            newGroup.name,
            mapOf(account.username to GrantedPermission.OWNER),
            listOf(GrantedPermission.OWNER, GrantedPermission.WRITE, GrantedPermission.READ)
        )
    }

    @Transactional
    fun deleteGroup(groupId: UUID) {
        if (groupRepository.existsById(groupId)) {
            aclService.deleteAcl(ObjectIdentityImpl(Group::class.java, groupId), true)
            groupRepository.deleteById(groupId)
        }
    }

    @Transactional
    fun changeGroupName(groupUuid: UUID, name: String): GroupDto {
        val auth = SecurityContextHolder.getContext().authentication
            ?: throw IllegalStateException("No authentication available")
        val group = groupRepository.findById(groupUuid).orElseThrow { NotFoundException("Group", groupUuid) }
        group.name = name
        return convertGroupToDto(auth, groupRepository.save(group))
    }

    @Transactional
    fun addUserToGroup(groupUuid: UUID, username: String): GroupDto {
        val auth = SecurityContextHolder.getContext().authentication
            ?: throw IllegalStateException("No authentication available")
        val account = jwtUserDetailsService.loadUserByUsername(username)
        val group = groupRepository.findById(groupUuid).orElseThrow { NotFoundException("Group", groupUuid) }
        account.groups.add(group)
        group.accounts.add(account)
        accountRepository.save(account)
        return convertGroupToDto(auth, group)
    }

    @Transactional
    fun deleteUserFromGroup(groupUuid: UUID, username: String): GroupDto {
        val auth = SecurityContextHolder.getContext().authentication
            ?: throw IllegalStateException("No authentication available")
        val account = jwtUserDetailsService.loadUserByUsername(username)
        val group = groupRepository.findById(groupUuid).orElseThrow { NotFoundException("Group", groupUuid) }

        val sid = PrincipalSid(account.username)
        val requesterSid = groupSidRetrievalStrategy.getSids(auth)
        val acl = aclService.readAclById(ObjectIdentityImpl(group), listOf(sid))
        if (acl.owner == sid) {
            throw AccessDeniedException("Can't remove owner from group")
        } else if (acl.entries.any { it.sid == sid && it.permission.mask != 0 } && acl.owner !in requesterSid) {
            throw AccessDeniedException("Only owner can remove moderators")
        }

        account.groups.remove(group)
        group.accounts.remove(account)
        accountRepository.save(account)
        return convertGroupToDto(auth, group)
    }

    @Transactional
    fun grantWritePermissions(groupId: UUID, username: String): GroupDto {
        val auth = SecurityContextHolder.getContext().authentication
            ?: throw IllegalStateException("No authentication available")
        val account = jwtUserDetailsService.loadUserByUsername(username)
        val group = groupRepository.findById(groupId).orElseThrow { NotFoundException("Group", groupId) }
        if (!group.accounts.contains(account)) {
            throw BadRequestException("User $username is not in group")
        }
        permissionService.grantGroupWritePermission(group, username)
        return convertGroupToDto(auth, group)
    }


    @Transactional
    fun revokeWritePermissions(groupId: UUID, username: String): GroupDto {
        val auth = SecurityContextHolder.getContext().authentication
            ?: throw IllegalStateException("No authentication available")
        val account = jwtUserDetailsService.loadUserByUsername(username)
        val group = groupRepository.findById(groupId).orElseThrow { NotFoundException("Group", groupId) }
        if (!group.accounts.contains(account)) {
            throw BadRequestException("User $username is not in group")
        }
        permissionService.revokeGroupWritePermission(group, username)
        return convertGroupToDto(auth, group)
    }

    private fun convertGroupToDto(
        auth: Authentication,
        group: Group
    ): GroupDto {
        val admins = permissionService.findAdminAccessorsForGroup(group.id).associateBy { it.sid }
        val requesterName = auth.name
        var requesterPermissions: GrantedPermission? = null
        val users = group.accounts.associate {
            val sid = "user:${it.username}"
            val permission = if (sid in admins) {
                if (admins[sid]!!.permissions.contains(GrantedPermission.OWNER)) {
                    GrantedPermission.OWNER
                } else if (admins[sid]!!.permissions.contains(GrantedPermission.WRITE)) {
                    GrantedPermission.WRITE
                } else {
                    throw RuntimeException("Impossible")
                }
            } else {
                GrantedPermission.READ
            }

            if (requesterName == it.username) {
                requesterPermissions = permission
            }
            it.username to permission
        }

        if (auth.authorities.contains(SimpleGrantedAuthority("ROLE_ADMIN"))) {
            requesterPermissions = GrantedPermission.OWNER
        }
        if (requesterPermissions == null) {
            throw AccessDeniedException("No access to group")
        }
        val permissions = when (requesterPermissions) {
            GrantedPermission.OWNER -> listOf(GrantedPermission.OWNER, GrantedPermission.WRITE, GrantedPermission.READ)
            GrantedPermission.WRITE -> listOf(GrantedPermission.WRITE, GrantedPermission.READ)
            GrantedPermission.READ -> listOf(GrantedPermission.READ)
            else -> throw RuntimeException("Unknown permission status")
        }
        return GroupDto(group.id, group.name, users, permissions)
    }
}