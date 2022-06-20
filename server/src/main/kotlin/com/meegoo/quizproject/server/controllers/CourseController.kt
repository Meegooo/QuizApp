package com.meegoo.quizproject.server.controllers;

import com.fasterxml.jackson.annotation.JsonView
import com.meegoo.quizproject.server.data.JacksonView
import com.meegoo.quizproject.server.data.JacksonViewEnum
import com.meegoo.quizproject.server.data.dto.CourseDto
import com.meegoo.quizproject.server.data.dto.QuizDto
import com.meegoo.quizproject.server.data.repositories.GroupRepository;
import com.meegoo.quizproject.server.data.services.CourseService
import com.meegoo.quizproject.server.security.jwt.JwtUserDetailsService;
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/v1/course")
class CourseController(
    private val courseService: CourseService,
) {

    @GetMapping
    @JsonView(JacksonView.Overview::class)
    fun getAllCourses(): List<CourseDto> {
        return courseService.getAllCourses()
    }

    @GetMapping("/{course_id}")
    @JsonView(JacksonView.Read::class)
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or " +
            "(hasAuthority('ROLE_USER') and hasPermission(#courseUuid, 'com.meegoo.quizproject.server.data.entity.Course', 'READ'))"
    )
    fun getCourse(@PathVariable("course_id") courseUuid: UUID): CourseDto {
        return courseService.getCourse(courseUuid)
    }

    @PostMapping()
    @JsonView(JacksonView.Read::class)
    fun createCourse(@RequestParam("name") name: String): CourseDto {
        return courseService.createCourse(name)
    }

    @DeleteMapping("/{course_id}")
    @JsonView(JacksonView.Read::class)
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or " +
            "(hasAuthority('ROLE_USER') and hasPermission(#courseUuid, 'com.meegoo.quizproject.server.data.entity.Course', 'WRITE'))"
    )
    fun deleteCourse(@PathVariable("course_id") courseUuid: UUID) {
        courseService.deleteCourse(courseUuid)
    }


    @PutMapping("/{course_id}")
    @JsonView(JacksonView.Read::class)
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or " +
            "(hasAuthority('ROLE_USER') and hasPermission(#courseUuid, 'com.meegoo.quizproject.server.data.entity.Course', 'WRITE'))"
    )
    fun updateCourseName(@PathVariable("course_id") courseUuid: UUID, @RequestParam("name") name: String): CourseDto {
        return courseService.changeCourseName(courseUuid, name)
    }

    @PostMapping("/{course_id}/quiz/{quiz_id}")
    @JsonView(JacksonView.Read::class)
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or " +
            "(hasAuthority('ROLE_USER') " +
            "and hasPermission(#courseUuid, 'com.meegoo.quizproject.server.data.entity.Course', 'WRITE')" +
            "and hasPermission(#quizUuid, 'com.meegoo.quizproject.server.data.entity.Quiz', 'SHARE')" +
            ")"
    )
    fun addQuizToCourse(@PathVariable("course_id") courseUuid: UUID, @PathVariable("quiz_id") quizUuid: UUID): CourseDto {
        return courseService.addQuizToCourse(quizUuid, courseUuid)
    }

    @DeleteMapping("/{course_id}/quiz/{quiz_id}")
    @JsonView(JacksonView.Read::class)
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or " +
            "(hasAuthority('ROLE_USER') " +
            "and hasPermission(#courseUuid, 'com.meegoo.quizproject.server.data.entity.Course', 'WRITE')" +
            "and hasPermission(#quizUuid, 'com.meegoo.quizproject.server.data.entity.Quiz', 'SHARE')" +
            ")"
    )
    fun deleteQuizFromCourse(@PathVariable("course_id") courseUuid: UUID, @PathVariable("quiz_id") quizUuid: UUID): CourseDto {
        return courseService.deleteQuizFromCourse(quizUuid, courseUuid)
    }
}
