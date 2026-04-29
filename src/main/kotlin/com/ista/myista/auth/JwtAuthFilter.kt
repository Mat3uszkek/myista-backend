package com.ista.myista.auth

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthFilter(private val jwtService: JwtService) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain,
    ) {
        val token = request.getHeader("Authorization")
            ?.takeIf { it.startsWith("Bearer ") }
            ?.substring(7)

        if (token != null && jwtService.isValid(token)) {
            val claims = jwtService.parseClaims(token)
            val principal = UserPrincipal(
                userId = claims.userId,
                custId = claims.custId,
                accountNumber = claims.accountNumber,
                tenantRefreshToken = claims.tenantRefreshToken,
            )
            SecurityContextHolder.getContext().authentication =
                UsernamePasswordAuthenticationToken(principal, null, principal.authorities)
        }
        chain.doFilter(request, response)
    }
}
