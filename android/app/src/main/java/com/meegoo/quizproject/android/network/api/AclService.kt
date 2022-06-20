package com.meegoo.quizproject.android.network.api

import com.meegoo.quizproject.android.data.dto.AclDto
import retrofit2.Response
import retrofit2.http.*
import java.util.*

interface AclService {
    @PUT("/api/v1/acl/quiz/{id}")
    suspend fun grantQuiz(@Path("id") quizId: UUID, @Body aclDto: AclDto): Response<AclDto>

    @HTTP(method = "DELETE", path = "/api/v1/acl/quiz/{id}", hasBody = true)
    suspend fun revokeQuiz(@Path("id") quizId: UUID, @Body aclDto: AclDto): Response<AclDto>

    @PUT("/api/v1/acl/course/{id}")
    suspend fun grantCourse(@Path("id") courseId: UUID, @Body aclDto: AclDto): Response<AclDto>

    @HTTP(method = "DELETE", path = "/api/v1/acl/course/{id}", hasBody = true)
    suspend fun revokeCourse(@Path("id") courseId: UUID, @Body aclDto: AclDto): Response<AclDto>

    @GET("/api/v1/acl/quiz/{id}")
    suspend fun findAllForQuiz(@Path("id") quizId: UUID): Response<List<AclDto>>

    @GET("/api/v1/acl/course/{id}")
    suspend fun findAllForCourse(@Path("id") courseId: UUID): Response<List<AclDto>>
}