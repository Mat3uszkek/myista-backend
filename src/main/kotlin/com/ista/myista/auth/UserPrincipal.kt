package com.ista.myista.auth

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

data class UserPrincipal(
    val userId: String,
    val custId: Int,
    val accountNumber: String,
    val tenantRefreshToken: String,
) : UserDetails {
    override fun getAuthorities(): Collection<GrantedAuthority> = emptyList()
    override fun getPassword(): String? = null
    override fun getUsername(): String = userId
}
