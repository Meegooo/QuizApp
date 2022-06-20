package com.meegoo.quizproject.android.network.api

import com.meegoo.quizproject.android.data.dto.CourseDto
import com.meegoo.quizproject.android.data.dto.QuizAttemptDto
import com.meegoo.quizproject.android.data.dto.QuizDto
import retrofit2.Response
import retrofit2.http.*
import java.util.*

interface CourseService {
    @GET("api/v1/course")
    suspend fun getCourses(): Response<List<CourseDto>>

    @GET("api/v1/course/{uuid}")
    suspend fun getCourse(@Path("uuid") uuid: UUID): Response<CourseDto>

    @POST("api/v1/course")
    suspend fun createCourse(@Query("name") name: String): Response<CourseDto>

    @PUT("api/v1/course/{uuid}")
    suspend fun updateCourse(@Path("uuid") uuid: UUID, @Query("name") name: String): Response<CourseDto>

    @DELETE("api/v1/course/{uuid}")
    suspend fun deleteCourse(@Path("uuid") uuid: UUID): Response<Unit>

    @POST("api/v1/course/{course_uuid}/quiz/{quiz_uuid}")
    suspend fun addQuizToCourse(@Path("course_uuid") courseUuid: UUID, @Path("quiz_uuid") quizUuid: UUID): Response<CourseDto>

    @DELETE("api/v1/course/{course_uuid}/quiz/{quiz_uuid}")
    suspend fun deleteQuizFromCourse(@Path("course_uuid") courseUuid: UUID, @Path("quiz_uuid") quizUuid: UUID): Response<CourseDto>
}