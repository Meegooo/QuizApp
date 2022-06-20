package com.meegoo.quizproject.server.data.entity

import com.vladmihalcea.hibernate.type.json.JsonBinaryType
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import java.time.Instant
import java.util.*
import javax.persistence.*
import kotlin.collections.ArrayList
import kotlin.collections.LinkedHashMap

@Entity
@TypeDef(name = "jsonb", typeClass = JsonBinaryType::class)
@Table(name = "quizzes")
class Quiz(@Id var id: UUID, var name: String) {

    constructor(name: String) : this(UUID.randomUUID(), name)

    var timeLimit: Int = -1
    var score: Double? = null
    var publishedAt: Instant? = null
    var automaticScore: Boolean = true

    @Type(type = "jsonb")
    var options: MutableMap<String, String> = HashMap()

    @OneToMany(mappedBy = "quiz", fetch = FetchType.LAZY)
    @MapKey(name = "id")
    var questions: MutableList<Question> = ArrayList()

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "quiz_course", joinColumns = [JoinColumn(name = "quiz_id", referencedColumnName = "id")],
        inverseJoinColumns = [JoinColumn(name = "course_id", referencedColumnName = "id")]
    )
    var courses: MutableSet<Course> = HashSet()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Quiz

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}