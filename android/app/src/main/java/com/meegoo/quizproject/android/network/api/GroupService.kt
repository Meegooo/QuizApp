package com.meegoo.quizproject.android.network.api

import com.meegoo.quizproject.android.data.dto.CourseDto
import com.meegoo.quizproject.android.data.dto.QuizAttemptDto
import com.meegoo.quizproject.android.data.dto.QuizDto
import com.meegoo.quizproject.server.data.dto.GroupDto
import retrofit2.Response
import retrofit2.http.*
import java.util.*

interface GroupService {
    @GET("/api/v1/group/{group_id}")
    suspend fun getGroup(@Path("group_id") groupId: UUID): Response<GroupDto>

    @GET("/api/v1/group")
    suspend fun getAllGroups(): Response<List<GroupDto>>

    @POST("/api/v1/group")
    suspend fun createGroup(@Query("name") name: String): Response<GroupDto>

    @PUT("/api/v1/group/{group_id}")
    suspend fun changeGroupName(@Path("group_id") groupId: UUID, @Query("name") name: String): Response<GroupDto>

    @DELETE("/api/v1/group/{group_id}")
    suspend fun deleteGroup(@Path("group_id") groupId: UUID): Response<Unit>

    @DELETE("/api/v1/group/{group_id}/user/{username}")
    suspend fun deleteUserFromGroup(@Path("group_id") groupId: UUID,@Path("username") username: String): Response<GroupDto>

    @POST("/api/v1/group/{group_id}/user/{username}")
    suspend fun addUserToGroup(@Path("group_id") groupId: UUID, @Path("username") username: String): Response<GroupDto>

    @POST("/api/v1/group/{group_id}/user/{username}/writer")
    suspend fun addUserToGroupWriter(@Path("group_id") groupId: UUID, @Path("username") username: String): Response<GroupDto>

    @DELETE("/api/v1/group/{group_id}/user/{username}/writer")
    suspend fun removeUserFromGroupWriter(@Path("group_id") groupId: UUID, @Path("username") username: String): Response<GroupDto>

    @GET("api/v1/account/check_available")
    suspend fun checkUsernameExists(@Query("username") username: String): Response<Boolean>
}