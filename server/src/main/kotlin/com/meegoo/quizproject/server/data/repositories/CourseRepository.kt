package com.meegoo.quizproject.server.data.repositories

import com.meegoo.quizproject.server.data.entity.Course
import com.meegoo.quizproject.server.data.entity.Group
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface CourseRepository : JpaRepository<Course, UUID> {

    fun findByName(name: String): Optional<Course>
}