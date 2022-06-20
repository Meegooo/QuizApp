package com.meegoo.quizproject.server

import com.meegoo.quizproject.server.data.repositories.QuestionRepository
import com.meegoo.quizproject.server.data.repositories.QuizRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ImportResource
import org.springframework.context.event.EventListener
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.stereotype.Component


var logger = LoggerFactory.getLogger(QuizServerApplication::class.java)

@SpringBootApplication
@ImportResource("classpath:security.xml")
@EnableJpaRepositories(basePackages = ["com.meegoo.quizproject.server.data.repositories"])
@EntityScan("com.meegoo.quizproject.server.data")
class QuizServerApplication

@Component
class EventListener(val questionRepository: QuestionRepository, val quizRepository: QuizRepository) {

    @EventListener(ApplicationReadyEvent::class)
    fun doSomethingAfterStartup(event: ApplicationReadyEvent) {
//
//        val question1 = Question(
//            "Hello",
//            Question.QuestionType.SINGLE_ANSWER,
//            listOf(Question.Answer("Yes", true)).toMutableList(),
//            mapOf("One" to "Two", "Three" to "Four").toMutableMap()
//        )
//        val question2 = Question(
//            "Abc",
//            Question.QuestionType.SINGLE_ANSWER,
//            listOf(Question.Answer("Def", true)).toMutableList(),
//            mapOf("One" to "Two", "Three" to "Four").toMutableMap()
//        )
//
//        val quiz = Quiz(10, 10, HashMap())
//        quiz.questions.addAll(listOf(question1, question2))
//        questionRepository.save(question1)
//        questionRepository.save(question2)
//        quizRepository.save(quiz)
    }
}

fun main(args: Array<String>) {
    runApplication<QuizServerApplication>(*args)
}
