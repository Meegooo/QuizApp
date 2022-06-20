package com.meegoo.quizproject.android.network.api

import com.meegoo.quizproject.android.data.dto.QuizDto
import retrofit2.Response
import retrofit2.http.*
import java.util.*

interface QuizService {
    @GET("api/v1/quiz")
    suspend fun getOverview(): Response<List<QuizDto>>

    @GET("api/v1/quiz/{uuid}")
    suspend fun getQuiz(@Path("uuid") uuid: UUID): Response<QuizDto>

    @GET("api/v1/quiz/{uuid}?editable=true")
    suspend fun getQuizEditable(@Path("uuid") uuid: UUID): Response<QuizDto>

    @POST("api/v1/quiz/")
    suspend fun createQuiz(@Query("name") name: String): Response<QuizDto>

    @PUT("api/v1/quiz/{uuid}")
    suspend fun updateQuiz(@Path("uuid") attemptUuid: UUID, @Body quizAttempt: QuizDto): Response<QuizDto>

    @POST("api/v1/quiz/{uuid}")
    suspend fun publishQuiz(@Path("uuid") attemptUuid: UUID): Response<QuizDto>

    @DELETE("api/v1/quiz/{uuid}")
    suspend fun deleteQuiz(@Path("uuid") attemptUuid: UUID): Response<Unit>

    @GET("api/v1/quiz/{uuid}/export")
    suspend fun exportQuiz(@Path("uuid") attemptUuid: UUID): Response<QuizDto>

    @POST("api/v1/quiz/import")
    suspend fun importQuiz(@Body quiz: QuizDto): Response<QuizDto>
}