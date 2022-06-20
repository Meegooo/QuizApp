package com.meegoo.quizproject.android.network.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.meegoo.quizproject.android.QuizApplication
import com.meegoo.quizproject.android.data.dto.QuestionDto
import com.meegoo.quizproject.android.network.interceptors.AccessTokenInterceptor
import com.meegoo.quizproject.android.network.interceptors.RefreshTokenInterceptor
import com.meegoo.quizproject.android.network.interceptors.ErrorInterceptor
import com.meegoo.quizproject.server.data.dto.SystemAnswerDto
import com.meegoo.quizproject.server.data.dto.UserAnswerDto
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import java.util.concurrent.TimeUnit

object RequestController {
    private val BASE_URL = "https://meegoo.ddns.net:28167"
    private const val TIMEOUT_MILLIS = 5000
    private val TIMEOUT_UNIT = TimeUnit.MILLISECONDS
    val objectMapper = let {
        val objectMapper = ObjectMapper().findAndRegisterModules()
        val module = SimpleModule()
        module.addSerializer(UserAnswerDto::class.java, UserAnswerDto.DataSerializer())
        module.addDeserializer(UserAnswerDto::class.java, UserAnswerDto.DataDeserializer())
        module.addSerializer(SystemAnswerDto::class.java, SystemAnswerDto.DataSerializer())
        module.addDeserializer(SystemAnswerDto::class.java, SystemAnswerDto.DataDeserializer())
        module.addSerializer(QuestionDto.AnswerDto::class.java, QuestionDto.AnswerDto.DataSerializer())
        module.addDeserializer(QuestionDto.AnswerDto::class.java, QuestionDto.AnswerDto.DataDeserializer())
        objectMapper.registerModule(module)
        objectMapper
    }
    private val converterFactory: JacksonConverterFactory = JacksonConverterFactory.create(objectMapper)


    val accountService: AccountService
        get() {
            val retrofitAsync = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(createLoginOkHttpClient())
                .addConverterFactory(converterFactory)
                .build()
            return retrofitAsync.create(AccountService::class.java)
        }

    private val generalRetrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(createGeneralOkHttpClient())
        .addConverterFactory(converterFactory)
        .build()

    val quizService: QuizService
        get() = generalRetrofit.create(QuizService::class.java)

    val quizAttemptService: QuizAttemptService
        get() = generalRetrofit.create(QuizAttemptService::class.java)

    val courseService: CourseService
        get() = generalRetrofit.create(CourseService::class.java)

    val aclService: AclService
        get() = generalRetrofit.create(AclService::class.java)

    val groupService: GroupService
        get() = generalRetrofit.create(GroupService::class.java)

    private fun createLoginOkHttpClient(): OkHttpClient {
        return httpClientBoilerplate().build()
    }

    private fun createGeneralOkHttpClient(): OkHttpClient {
        val httpLoggingInterceptor = HttpLoggingInterceptor();
        httpLoggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BASIC).redactHeader("Authorization");
        return httpClientBoilerplate()
            .addInterceptor(AccessTokenInterceptor)
            .addInterceptor(RefreshTokenInterceptor)
            .addInterceptor(httpLoggingInterceptor)
            .build()
    }

    fun httpClientBoilerplate(): OkHttpClient.Builder {
        return OkHttpClient().newBuilder()
            .cache(Cache(QuizApplication.appContext.cacheDir, 10 * 1024 * 1024)) // 10 MB
            .addInterceptor(ErrorInterceptor)
            .connectTimeout(TIMEOUT_MILLIS.toLong(), TIMEOUT_UNIT)
            .readTimeout(TIMEOUT_MILLIS.toLong(), TIMEOUT_UNIT)
            .writeTimeout(TIMEOUT_MILLIS.toLong(), TIMEOUT_UNIT)
    }
}