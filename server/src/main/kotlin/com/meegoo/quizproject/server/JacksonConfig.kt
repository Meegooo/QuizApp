package com.meegoo.quizproject.server

import com.fasterxml.jackson.databind.module.SimpleModule
import com.meegoo.quizproject.server.data.dto.UserAnswerDto
import com.meegoo.quizproject.server.data.dto.SystemAnswerDto
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class JacksonConfig {
    @Bean
    fun customJacksonModule(): SimpleModule {
        val module = SimpleModule()
        module.addSerializer(UserAnswerDto::class.java, UserAnswerDto.DataSerializer())
        module.addDeserializer(UserAnswerDto::class.java, UserAnswerDto.DataDeserializer())
        module.addSerializer(SystemAnswerDto::class.java, SystemAnswerDto.DataSerializer())
        module.addDeserializer(SystemAnswerDto::class.java, SystemAnswerDto.DataDeserializer())
        return module
    }
}