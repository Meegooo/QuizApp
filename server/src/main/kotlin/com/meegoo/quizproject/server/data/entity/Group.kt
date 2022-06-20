package com.meegoo.quizproject.server.data.entity

import java.util.*
import javax.persistence.*

@Entity
@Table(name = "groups")
class Group(
    @Id var id: UUID,
    var name: String,
) {
    @ManyToMany(mappedBy = "groups")
    var accounts: MutableSet<Account> = HashSet()

    constructor(
        name: String,
    ) : this(UUID.randomUUID(), name)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Group

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }


}