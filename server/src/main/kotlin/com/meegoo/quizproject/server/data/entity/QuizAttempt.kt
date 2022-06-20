package com.meegoo.quizproject.server.data.entity

import com.vladmihalcea.hibernate.type.json.JsonBinaryType
import com.vladmihalcea.hibernate.type.json.JsonStringType
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import java.time.Instant
import java.util.*
import javax.persistence.*

@Entity
@TypeDef(name = "jsonb", typeClass = JsonBinaryType::class)
@Table(name = "quiz_attempts")
class QuizAttempt(
    @Id var id: UUID,

    @ManyToOne(optional = false)
    @JoinColumn(name = "quiz_id")
    var quiz: Quiz,

    var startedAt: Instant,

    @ManyToOne(optional = false)
    @JoinColumn(name = "account_id")
    var account: Account,

    var closed: Boolean
) {

    constructor(quiz: Quiz, startedAt: Instant, account: Account, closed: Boolean = false) :
            this(UUID.randomUUID(), quiz, startedAt, account, closed)

    var score: Double? = null
    var timeTaken: Int? = null

    @Type(type = "jsonb")
    @Column(name = "answers")
    var userAnswers: LinkedHashMap<UUID, Any> = linkedMapOf()

    fun isExpired(): Boolean {
        //+5 seconds for network delays. We don't want people to end attempt at the last second and not save their answer
        //because it took too long to reach server
        //(5 because default timeout is 5 seconds)
        return closed || if(quiz.timeLimit <= 0) false
        else startedAt.plusSeconds(quiz.timeLimit.toLong() + 5)?.isAfter(Instant.now()) == false
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as QuizAttempt

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

}