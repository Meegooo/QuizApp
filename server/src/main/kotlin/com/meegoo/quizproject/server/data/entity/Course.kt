package com.meegoo.quizproject.server.data.entity

import java.util.*
import javax.persistence.*

@Entity
@Table(name = "courses")
class Course(
    @Id var id: UUID,
    var name: String,
) {

    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "courses")
    var quizzes: MutableSet<Quiz> = HashSet()

    constructor(
        name: String,
    ) : this(UUID.randomUUID(), name)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Course

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }


}