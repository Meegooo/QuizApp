package com.meegoo.quizproject.server.controllers

import com.meegoo.quizproject.server.data.dto.GroupDto
import com.meegoo.quizproject.server.data.repositories.AccountRepository
import com.meegoo.quizproject.server.data.repositories.GroupRepository
import com.meegoo.quizproject.server.data.services.GroupService
import com.meegoo.quizproject.server.security.jwt.JwtUserDetailsService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.acls.model.MutableAclService
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/v1/group")
class GroupController(
    private val groupService: GroupService,
) {

    @GetMapping("/{groupId}")
    fun getGroup(@PathVariable groupId: UUID): GroupDto {
        return groupService.getGroup(groupId)
    }

    @GetMapping
    fun getAllGroups(): List<GroupDto> {
        return groupService.getGroups()
    }

    @PostMapping
    fun createGroup(@RequestParam name: String): GroupDto {
        return groupService.createGroup(name)
    }

    @PutMapping("/{group_id}")
    fun changeGroupName(@PathVariable("group_id") groupId: UUID, @RequestParam("name") name: String): GroupDto {
        return groupService.changeGroupName(groupId, name)
    }

    @DeleteMapping("/{group_id}")
    @PreAuthorize(
        "hasAuthority('ROLE_ADMIN') or " +
                "(hasAuthority('ROLE_USER') and hasPermission(#groupId, 'com.meegoo.quizproject.server.data.entity.Group', 'WRITE'))"
    )
    fun deleteGroup(@PathVariable("group_id") groupId: UUID) {
        return groupService.deleteGroup(groupId)
    }

    @DeleteMapping("/{group_id}/user/{username}")
    @PreAuthorize(
        "hasAuthority('ROLE_ADMIN') or " +
                "(hasAuthority('ROLE_USER') and hasPermission(#groupId, 'com.meegoo.quizproject.server.data.entity.Group', 'WRITE'))"
    )
    fun deleteUserFromGroup(
        @PathVariable("group_id") groupId: UUID,
        @PathVariable("username") username: String
    ): GroupDto {
        return groupService.deleteUserFromGroup(groupId, username)
    }

    @PostMapping("{group_id}/user/{username}")
    @PreAuthorize(
        "hasAuthority('ROLE_ADMIN') or " +
                "(hasAuthority('ROLE_USER') and hasPermission(#groupId, 'com.meegoo.quizproject.server.data.entity.Group', 'WRITE'))"
    )
    fun addUserToGroup(@PathVariable("group_id") groupId: UUID, @PathVariable("username") username: String): GroupDto {
        return groupService.addUserToGroup(groupId, username)
    }

    @PostMapping("{group_id}/user/{username}/writer")
    @PreAuthorize(
        "hasAuthority('ROLE_ADMIN') or " +
                "(hasAuthority('ROLE_USER') and hasPermission(#groupId, 'com.meegoo.quizproject.server.data.entity.Group', 'OWNER'))"
    )
    fun addUserToGroupWriter(@PathVariable("group_id") groupId: UUID, @PathVariable("username") username: String): GroupDto {
        return groupService.grantWritePermissions(groupId, username)
    }

    @DeleteMapping("{group_id}/user/{username}/writer")
    @PreAuthorize(
        "hasAuthority('ROLE_ADMIN') or " +
                "(hasAuthority('ROLE_USER') and hasPermission(#groupId, 'com.meegoo.quizproject.server.data.entity.Group', 'OWNER'))"
    )
    fun removeUserFromGroupWriter(@PathVariable("group_id") groupId: UUID, @PathVariable("username") username: String): GroupDto {
        return groupService.revokeWritePermissions(groupId, username)
    }
}