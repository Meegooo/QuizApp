package com.meegoo.quizproject.server.controllers

import com.meegoo.quizproject.server.data.dto.AclDto
import com.meegoo.quizproject.server.data.dto.GroupDto
import com.meegoo.quizproject.server.data.entity.Question
import com.meegoo.quizproject.server.data.repositories.GroupRepository
import com.meegoo.quizproject.server.data.repositories.QuestionRepository
import com.meegoo.quizproject.server.data.services.PermissionService
import com.meegoo.quizproject.server.security.acl.CustomObjectIdentityRetrievalStrategy
import com.meegoo.quizproject.server.security.acl.GroupSid
import com.meegoo.quizproject.server.security.acl.QuizPermission
import com.meegoo.quizproject.server.security.acl.QuizPermission.Companion.parsePermissions
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.acls.domain.BasePermission
import org.springframework.security.acls.domain.CumulativePermission
import org.springframework.security.acls.domain.ObjectIdentityRetrievalStrategyImpl
import org.springframework.security.acls.model.*
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/v1/acl/")
class PermissionController(
    val permissionService: PermissionService,
) {

    @PutMapping("quiz/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or " +
            "(hasAuthority('ROLE_USER') and hasPermission(#quizId, 'com.meegoo.quizproject.server.data.entity.Quiz', 'ADMINISTRATION'))"
    )
    fun grantQuiz(@PathVariable("id") quizId: UUID, @RequestBody aclDto: AclDto): AclDto {
        return permissionService.grantQuizPermission(quizId, aclDto)
    }


    @DeleteMapping("quiz/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or " +
            "(hasAuthority('ROLE_USER') and hasPermission(#quizId, 'com.meegoo.quizproject.server.data.entity.Quiz', 'ADMINISTRATION'))"
    )
    fun revokeQuiz(@PathVariable("id") quizId: UUID, @RequestBody aclDto: AclDto): AclDto {
        return permissionService.revokeQuizPermission(quizId, aclDto)
    }

    @PutMapping("course/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or " +
            "(hasAuthority('ROLE_USER') and hasPermission(#courseId, 'com.meegoo.quizproject.server.data.entity.Course', 'ADMINISTRATION'))"
    )
    fun grantCourse(@PathVariable("id") courseId: UUID, @RequestBody aclDto: AclDto): AclDto {
        return permissionService.grantCoursePermission(courseId, aclDto)
    }


    @DeleteMapping("course/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or " +
            "(hasAuthority('ROLE_USER') and hasPermission(#courseId, 'com.meegoo.quizproject.server.data.entity.Course', 'ADMINISTRATION'))"
    )
    fun revokeCourse(@PathVariable("id") courseId: UUID, @RequestBody aclDto: AclDto): AclDto {
        return permissionService.revokeCoursePermission(courseId, aclDto)
    }

    @GetMapping("quiz/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or " +
            "(hasAuthority('ROLE_USER') and hasPermission(#quizId, 'com.meegoo.quizproject.server.data.entity.Quiz', 'ADMINISTRATION'))"
    )
    fun findAllForQuiz(@PathVariable("id") quizId: UUID): List<AclDto> {
        return permissionService.findAllAccessorsForQuiz(quizId)
    }

    @GetMapping("course/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or " +
            "(hasAuthority('ROLE_USER') and hasPermission(#courseId, 'com.meegoo.quizproject.server.data.entity.Course', 'ADMINISTRATION'))"
    )
    fun findAllForCourse(@PathVariable("id") courseId: UUID): List<AclDto> {
        return permissionService.findAllAccessorsForCourse(courseId)
    }
}