package com.meegoo.quizproject.server.data.services

import com.meegoo.quizproject.server.controllers.hooks.NotFoundException
import com.meegoo.quizproject.server.data.dto.CourseDto
import com.meegoo.quizproject.server.data.dto.GrantedPermission
import com.meegoo.quizproject.server.data.entity.Course
import com.meegoo.quizproject.server.data.repositories.CourseRepository
import com.meegoo.quizproject.server.data.repositories.QuizRepository
import com.meegoo.quizproject.server.security.acl.AclObjectRetriever
import com.meegoo.quizproject.server.security.acl.AclPermissionGranter
import com.meegoo.quizproject.server.security.acl.QuizPermission
import org.springframework.security.acls.domain.ObjectIdentityImpl
import org.springframework.security.acls.model.MutableAclService
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class CourseService(
    private val quizRepository: QuizRepository,
    private val courseRepository: CourseRepository,
    private val permissionService: PermissionService,
    private val aclService: MutableAclService,
    private val aclPermissionGranter: AclPermissionGranter,
    private val aclObjectRetriever: AclObjectRetriever,
) {
    @Transactional
    fun addQuizToCourse(quizUuid: UUID, courseUuid: UUID): CourseDto {
        val course = courseRepository.findById(courseUuid).orElseThrow { NotFoundException("Course", courseUuid) }
        val quiz = quizRepository.findById(quizUuid).orElseThrow { NotFoundException("Quiz", courseUuid) }
        //Add course
        quiz.courses.add(course)
        course.quizzes.add(quiz)
        quizRepository.save(quiz)
        return CourseDto(course, permissionService.findPermissionsForCourse(courseUuid))
    }

    @Transactional
    fun deleteQuizFromCourse(quizUuid: UUID, courseUuid: UUID): CourseDto {
        val course = courseRepository.findById(courseUuid).orElseThrow { NotFoundException("Course", courseUuid) }
        val quiz = quizRepository.findById(quizUuid).orElseThrow { NotFoundException("Quiz", courseUuid) }
        //Delete course
        quiz.courses.remove(course)
        quizRepository.save(quiz)
        return CourseDto(course, permissionService.findPermissionsForCourse(courseUuid))
    }

    fun getCourse(courseUuid: UUID): CourseDto {
        return CourseDto(
            courseRepository.findById(courseUuid).orElseThrow { NotFoundException("Course", courseUuid) },
            permissionService.findPermissionsForCourse(courseUuid)
        )
    }

    @Transactional
    fun createCourse(name: String): CourseDto {
        var course = Course(name)
        course = courseRepository.save(course)
        aclPermissionGranter.createObject(course)
        return CourseDto(course, GrantedPermission.OWNER.toListGranting())
    }


    @Transactional
    fun changeCourseName(courseUuid: UUID, name: String): CourseDto {
        val course = courseRepository.findById(courseUuid).orElseThrow { NotFoundException("Course", courseUuid) }
        course.name = name
        courseRepository.save(course)
        return CourseDto(course, permissionService.findPermissionsForCourse(courseUuid))
    }

    @Transactional
    fun deleteCourse(courseUuid: UUID) {
        if (courseRepository.existsById(courseUuid)) {
            aclService.deleteAcl(ObjectIdentityImpl(Course::class.java, courseUuid), true)
            courseRepository.deleteById(courseUuid)
        }
    }

    fun getAllCourses(authentication: Authentication? = null): List<CourseDto> {
        val auth = authentication ?: SecurityContextHolder.getContext().authentication ?: throw IllegalStateException("No authentication available")
        return if (auth.authorities.contains(SimpleGrantedAuthority("ROLE_ADMIN"))) {
            courseRepository.findAll().map { CourseDto(it, GrantedPermission.OWNER.toListGranting()) }
        } else {
            aclObjectRetriever.getObjectsWithAccess(Course::class.java, auth)
                .filter {(it.mask and QuizPermission.READ.mask) != 0 }
                .mapNotNull { courseRepository.findById(it.uuid).orElse(null) }
                .map { CourseDto(it, permissionService.findPermissionsForCourse(it.id)) }
        }
    }
}