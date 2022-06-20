package com.meegoo.quizproject.server.data.entity

import java.util.*
import javax.persistence.*

@Entity
@Table(name = "refresh_sessions")
class Session(
    @Id var id: UUID,
    @ManyToOne(targetEntity = Account::class, optional = false)
    @JoinColumn(name="account", nullable = false)
    var account: Account,
    var token: String,
    var issuedAt: Date,
    val deviceId: String
) {

    constructor(
        account: Account,
        token: String,
        issuedAt: Date,
        deviceId: String
    ) : this(UUID.randomUUID(), account, token, issuedAt, deviceId)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Session

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }


}