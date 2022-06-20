package com.meegoo.quizproject.android.network.api

import com.meegoo.quizproject.android.data.dto.QuizAttemptDto
import retrofit2.Response
import retrofit2.http.*
import java.util.*

interface QuizAttemptService {

    @GET("api/v1/attempt/quiz/{uuid}")
    suspend fun getQuizAttempts(@Path("uuid") quizUuid: UUID): Response<List<QuizAttemptDto>>

    @GET("api/v1/attempt/{uuid}")
    suspend fun getAttempt(@Path("uuid") attemptUuid: UUID): Response<QuizAttemptDto>

    @POST("api/v1/attempt/quiz/{uuid}")
    suspend fun createAttempt(@Path("uuid") quizUuid: UUID): Response<QuizAttemptDto>

    @PUT("api/v1/attempt/{uuid}")
    suspend fun putAttempt(@Path("uuid") attemptUuid: UUID, @Body quizAttempt: QuizAttemptDto): Response<QuizAttemptDto>

    @POST("api/v1/attempt/{uuid}/end")
    suspend fun finishAttempt(@Path("uuid") attemptUuid: UUID): Response<QuizAttemptDto>
}