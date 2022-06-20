package com.meegoo.quizproject.server.data.services

import com.meegoo.quizproject.server.controllers.hooks.NotFoundException
import com.meegoo.quizproject.server.controllers.hooks.SidNotFoundException
import com.meegoo.quizproject.server.data.dto.AclDto
import com.meegoo.quizproject.server.data.dto.GrantedPermission
import com.meegoo.quizproject.server.data.entity.Course
import com.meegoo.quizproject.server.data.entity.Group
import com.meegoo.quizproject.server.data.entity.Quiz
import com.meegoo.quizproject.server.data.repositories.AccountRepository
import com.meegoo.quizproject.server.data.repositories.CourseRepository
import com.meegoo.quizproject.server.data.repositories.GroupRepository
import com.meegoo.quizproject.server.data.repositories.QuizRepository
import com.meegoo.quizproject.server.security.acl.GroupSid
import com.meegoo.quizproject.server.security.acl.GroupSidRetrievalStrategy
import com.meegoo.quizproject.server.security.acl.QuizPermission
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.acls.domain.CumulativePermission
import org.springframework.security.acls.domain.ObjectIdentityImpl
import org.springframework.security.acls.domain.ObjectIdentityRetrievalStrategyImpl
import org.springframework.security.acls.domain.PrincipalSid
import org.springframework.security.acls.model.*
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*
import kotlin.collections.ArrayList

@Service
class PermissionService(
    private val quizRepository: QuizRepository,
    private val courseRepository: CourseRepository,
    private val groupRepository: GroupRepository,
    private val userRepository: AccountRepository,
    private val groupSidRetrievalStrategy: GroupSidRetrievalStrategy,
    private val aclService: MutableAclService,
) {
    companion object {
        private lateinit var instance: PermissionService
        private val objectIdentityGenerator: ObjectIdentityGenerator = ObjectIdentityRetrievalStrategyImpl()
    }

    init {
        instance = this
    }

    @Transactional
    fun grantQuizPermission(quizId: UUID, aclDto: AclDto): AclDto {
        if (!quizRepository.existsById(quizId)) {
            throw NotFoundException("Quiz", quizId)
        }

        val sid = aclDto.sidObject
        if (!checkSidExists(sid)) {
            throw SidNotFoundException()
        }
        if (aclDto.permissions.contains(GrantedPermission.OWNER)) {
            throw AccessDeniedException("Owner privileges can't be granted")
        }
        if (aclDto.permissions.contains(GrantedPermission.ADMINISTRATION) && !isOwnerOrAdminOfQuiz(quizId)) {
            throw AccessDeniedException("Only owner can grant administration privileges")
        }

        val permission = aclDto.permissions.flatMap { it.toListGranting() }.toSet()
            .fold(CumulativePermission()) { acc, perm ->
                acc.set(perm.permission)
            }

        val identity = objectIdentityGenerator.createObjectIdentity(quizId, Quiz::class.qualifiedName)
        val (acl, idx) = findAcl(identity, aclDto.sidObject)

        if (idx == -1) {
            acl.insertAce(acl.entries.size, permission, sid, true)
        } else {
            acl.updateAce(idx, permission.set(acl.entries[idx].permission))
        }
        aclService.updateAcl(acl)
        return AclDto(sid, permission, acl.owner == sid)
    }

    @Transactional
    fun revokeQuizPermission(quizId: UUID, aclDto: AclDto): AclDto {
        if (!quizRepository.existsById(quizId)) {
            throw NotFoundException("Quiz", quizId)
        }
        val sid = aclDto.sidObject
        if (!checkSidExists(sid)) {
            throw SidNotFoundException()
        }

        if (aclDto.permissions.contains(GrantedPermission.OWNER)) {
            throw AccessDeniedException("Owner privileges can't be revoked")
        }

        val identity = objectIdentityGenerator.createObjectIdentity(quizId, Quiz::class.qualifiedName)
        val (acl, idx) = findAcl(identity, aclDto.sidObject)
        val permission = aclDto.permissions.flatMap { it.toListRevoking() }.toSet()
            .fold(CumulativePermission()) { acc, perm ->
                acc.set(perm.permission)
            }
        val newPermission = CumulativePermission()
        if (idx != -1 && acl.owner != sid) {
            newPermission.set(acl.entries[idx].permission)
            if (newPermission.mask.and(QuizPermission.ADMINISTRATION.mask) > 0 && !isOwnerOrAdminOfQuiz(
                    quizId
                )
            ) {
                throw AccessDeniedException("Only owner can revoke administration privileges")
            }
            newPermission.clear(permission)
            acl.updateAce(idx, newPermission)
            aclService.updateAcl(acl)
        }
        return AclDto(sid, newPermission, acl.owner == sid)
    }

    @Transactional
    fun grantCoursePermission(courseId: UUID, aclDto: AclDto): AclDto {
        if (!courseRepository.existsById(courseId)) {
            throw NotFoundException("Course", courseId)
        }
        val sid = aclDto.sidObject
        if (!checkSidExists(sid)) {
            throw SidNotFoundException()
        }

        if (aclDto.permissions.contains(GrantedPermission.OWNER)) {
            throw AccessDeniedException("Owner privileges can't be granted")
        }

        if (aclDto.permissions.contains(GrantedPermission.ADMINISTRATION) && !isOwnerOrAdminOfCourse(courseId)) {
            throw AccessDeniedException("Only owner can grant administration privileges")
        }

        val permission = aclDto.permissions.flatMap { it.toListGranting() }.toSet()
            .fold(CumulativePermission()) { acc, perm ->
                acc.set(perm.permission)
            }

        val identity = objectIdentityGenerator.createObjectIdentity(courseId, Course::class.qualifiedName)
        val (acl, idx) = findAcl(identity, aclDto.sidObject)

        if (idx == -1) {
            acl.insertAce(acl.entries.size, permission, sid, true)
        } else {
            acl.updateAce(idx, permission.set(acl.entries[idx].permission))
        }
        aclService.updateAcl(acl)
        return AclDto(sid, permission, acl.owner == sid)
    }

    @Transactional
    fun revokeCoursePermission(courseId: UUID, aclDto: AclDto): AclDto {
        if (!courseRepository.existsById(courseId)) {
            throw NotFoundException("Course", courseId)
        }
        val sid = aclDto.sidObject
        if (!checkSidExists(sid)) {
            throw SidNotFoundException()
        }

        if (aclDto.permissions.contains(GrantedPermission.OWNER)) {
            throw AccessDeniedException("Owner privileges can't be revoked")
        }

        val identity = objectIdentityGenerator.createObjectIdentity(courseId, Course::class.qualifiedName)
        val (acl, idx) = findAcl(identity, aclDto.sidObject)
        val permission = aclDto.permissions.flatMap { it.toListRevoking() }.toSet()
            .fold(CumulativePermission()) { acc, perm ->
                acc.set(perm.permission)
            }

        val newPermission = CumulativePermission()
        if (idx != -1 && acl.owner != sid) {
            newPermission.set(acl.entries[idx].permission)
            if (newPermission.mask.and(QuizPermission.ADMINISTRATION.mask) > 0 && !isOwnerOrAdminOfCourse(
                    courseId
                )
            ) {
                throw AccessDeniedException("Only owner can revoke administration privileges")
            }
            newPermission.clear(permission)
            acl.updateAce(idx, newPermission)
            aclService.updateAcl(acl)
        }
        return AclDto(sid, newPermission, acl.owner == sid)
    }



    fun grantGroupWritePermission(group: Group, username: String) {
        val identity = ObjectIdentityImpl(group)
        val sid = PrincipalSid(username)
        val (acl, idx) = findAcl(identity, sid)

        val permission = GrantedPermission.WRITE.toListGranting()
            .fold(CumulativePermission()) { acc, perm ->
                acc.set(perm.permission)
            }

        if (idx == -1) {
            acl.insertAce(acl.entries.size, permission, sid, true)
        } else {
            acl.updateAce(idx, permission.set(acl.entries[idx].permission))
        }
        aclService.updateAcl(acl)
    }

    fun revokeGroupWritePermission(group: Group, username: String) {
        val identity = ObjectIdentityImpl(group)
        val sid = PrincipalSid(username)
        val (acl, idx) = findAcl(identity, sid)

        acl.deleteAce(idx)
        aclService.updateAcl(acl)
    }


    private fun findAcl(identity: ObjectIdentity, sid: Sid): Pair<MutableAcl, Int> {

        val acl = try {
            aclService.readAclById(identity, listOf(sid))
        } catch (e: org.springframework.security.acls.model.NotFoundException) {
            aclService.createAcl(identity)
        }

        acl as MutableAcl

        var idx = -1;
        for ((index, entry) in acl.entries.withIndex()) {
            if (entry.sid == sid) {
                idx = index
                break
            }
            if (!entry.isGranting) {
                throw NotImplementedError("Non-granting permissions are currently not supported") //Fail fast
            }
        }
        return Pair(acl, idx)
    }

    fun findAllAccessorsForQuiz(quizId: UUID): List<AclDto> {
        if (!quizRepository.existsById(quizId)) {
            throw NotFoundException("Quiz", quizId)
        }
        val identity = objectIdentityGenerator.createObjectIdentity(quizId, Quiz::class.qualifiedName)
        val acls = aclService.readAclsById(listOf(identity))
        return acls[identity]!!.entries
            .filter { it.permission.mask > 0 }
            .map { AclDto(it.sid, it.permission, acls[identity]!!.owner == it.sid) }
    }

    fun findAllAccessorsForCourse(courseId: UUID): List<AclDto> {
        if (!courseRepository.existsById(courseId)) {
            throw NotFoundException("Course", courseId)
        }
        val identity = objectIdentityGenerator.createObjectIdentity(courseId, Course::class.qualifiedName)
        val acls = aclService.readAclsById(listOf(identity))
        return acls[identity]!!.entries
            .filter { it.permission.mask > 0 }
            .map { AclDto(it.sid, it.permission, acls[identity]!!.owner == it.sid) }
    }

    fun findAdminAccessorsForGroup(groupId: UUID): List<AclDto> {
        if (!groupRepository.existsById(groupId)) {
            throw NotFoundException("Group", groupId)
        }
        val identity = objectIdentityGenerator.createObjectIdentity(groupId, Group::class.qualifiedName)
        val acls = aclService.readAclsById(listOf(identity))
        return acls[identity]!!.entries
            .filter { it.permission.mask > 0 }
            .map { AclDto(it.sid, it.permission, acls[identity]!!.owner == it.sid) }
    }

    fun findPermissionsForQuiz(quizId: UUID, authentication: Authentication? = null): List<GrantedPermission> {
        val quiz = quizRepository.findById(quizId).orElseThrow { NotFoundException("Quiz", quizId) }
        val obj = objectIdentityGenerator.createObjectIdentity(quizId, Quiz::class.qualifiedName)
        val quizPermissions = findPermissionsFor(obj)
        val throughCourse = quiz.courses.flatMap{ findPermissionsForCourse(it.id, authentication) }.contains(GrantedPermission.READ)
        return if (!quizPermissions.contains(GrantedPermission.READ) && throughCourse) {
            ArrayList(quizPermissions).apply { add(GrantedPermission.READ) }
        } else {
            quizPermissions
        }
    }

    fun findPermissionsForCourse(courseId: UUID, authentication: Authentication? = null): List<GrantedPermission> {
        if (!courseRepository.existsById(courseId)) {
            throw NotFoundException("Course", courseId)
        }
        val identity = objectIdentityGenerator.createObjectIdentity(courseId, Course::class.qualifiedName)
        return findPermissionsFor(identity)
    }

    fun findPermissionsFor(identity: ObjectIdentity, authentication: Authentication? = null): List<GrantedPermission> {
        val auth = authentication ?: SecurityContextHolder.getContext().authentication ?: throw IllegalStateException("No authentication available")
        val sids = groupSidRetrievalStrategy.getSids(auth)
        val acl = aclService.readAclById(identity, sids)
        if (acl.owner in sids) {
            return GrantedPermission.OWNER.toListGranting()
        }
        val permission = acl.entries.filter { it.sid in sids }
            .foldRight(CumulativePermission()) { acl, acc ->
                acc.set(acl.permission)
            }

        return when {
            permission.mask.and(QuizPermission.ADMINISTRATION.mask) > 0 -> {
                GrantedPermission.ADMINISTRATION.toListGranting()
            }
            permission.mask.and(QuizPermission.WRITE.mask) > 0 -> {
                GrantedPermission.WRITE.toListGranting()
            }
            permission.mask.and(QuizPermission.SHARE.mask) > 0 -> {
                GrantedPermission.SHARE.toListGranting()
            }
            permission.mask.and(QuizPermission.READ.mask) > 0 -> {
                GrantedPermission.READ.toListGranting()
            }
            else -> {
                emptyList()
            }
        }
    }

    fun isOwnerOrAdminOfQuiz(quizId: UUID, authentication: Authentication? = null): Boolean {
        val auth = authentication ?: SecurityContextHolder.getContext().authentication
        ?: throw IllegalStateException("No authentication available")

        if (auth.authorities.any { it.authority == "ROLE_ADMIN" }) {
            return true
        }
        val sids = groupSidRetrievalStrategy.getSids(auth)
        val acl = aclService.readAclById(objectIdentityGenerator.createObjectIdentity(quizId, Quiz::class.qualifiedName), sids)
        return acl.owner in sids
    }

    fun isOwnerOrAdminOfCourse(courseId: UUID, authentication: Authentication? = null): Boolean {
        val auth = authentication ?: SecurityContextHolder.getContext().authentication
        ?: throw IllegalStateException("No authentication available")

        if (auth.authorities.any { it.authority == "ROLE_ADMIN" }) {
            return true
        }
        val sids = groupSidRetrievalStrategy.getSids(auth)
        val acl = aclService.readAclById(objectIdentityGenerator.createObjectIdentity(courseId, Course::class.qualifiedName,), sids)
        return acl.owner in sids
    }

    fun checkSidExists(sid: Sid): Boolean {
        return when (sid) {
            is PrincipalSid -> userRepository.existsByUsername(sid.principal)
            is GroupSid -> groupRepository.existsByName(sid.group)
            else -> false
        }
    }

}