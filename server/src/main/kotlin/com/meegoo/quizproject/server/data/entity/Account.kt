package com.meegoo.quizproject.server.data.entity

import org.hibernate.annotations.Type
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "accounts")
class Account(
    @Id var id: UUID,
    private var username: String,
    private var password: String,
    @Type(type = "jsonb")
    var authorities: ArrayList<String>
) : UserDetails {

    constructor(
        username: String,
        password: String,
        authorities: ArrayList<String>
    ) : this(UUID.randomUUID(), username, password, authorities)

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "group_account", joinColumns = [JoinColumn(name="account_id", referencedColumnName = "id")],
        inverseJoinColumns = [JoinColumn(name="group_id", referencedColumnName = "id")])
    var groups: MutableSet<Group> = HashSet()

    override fun getUsername() = username

    override fun getPassword() = password

    override fun isAccountNonExpired(): Boolean {
        return true
    }

    override fun isAccountNonLocked(): Boolean {
        return true
    }

    override fun isCredentialsNonExpired(): Boolean {
        //TODO Logging out
        return true
    }

    override fun isEnabled(): Boolean {
        return true
    }

    override fun getAuthorities(): MutableCollection<out GrantedAuthority> {
        return authorities.map { SimpleGrantedAuthority(it) }.toMutableList()
    }

    fun addAuthority(authority: GrantedAuthority) {
        authorities.add(authority.authority)
    }

    fun addAuthority(authority: String) {
        authorities.add(authority)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Account

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }


    enum class Role {
        ROLE_ADMINISTRATOR,
        ROLE_USER
    }
}