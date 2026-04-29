package com.ista.myista.auth

import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.util.Date

data class JwtClaims(
    val userId: String,
    val custId: Int,
    val accountNumber: String,
    val tenantRefreshToken: String,
)

@Service
class JwtService(
    @Value("\${app.jwt.secret}") secret: String,
    @Value("\${app.jwt.expiration-ms}") private val expirationMs: Long,
) {
    private val key = Keys.hmacShaKeyFor(secret.toByteArray(StandardCharsets.UTF_8))

    fun generateToken(claims: JwtClaims): String =
        Jwts.builder()
            .subject(claims.userId)
            .claim("custId", claims.custId)
            .claim("accountNumber", claims.accountNumber)
            .claim("tenantRefreshToken", claims.tenantRefreshToken)
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + expirationMs))
            .signWith(key)
            .compact()

    fun parseClaims(token: String): JwtClaims {
        val payload = Jwts.parser().verifyWith(key).build()
            .parseSignedClaims(token).payload
        return JwtClaims(
            userId = payload.subject,
            custId = payload.get("custId", Integer::class.java).toInt(),
            accountNumber = payload.get("accountNumber", String::class.java),
            tenantRefreshToken = payload.get("tenantRefreshToken", String::class.java),
        )
    }

    fun isValid(token: String): Boolean =
        try {
            parseClaims(token)
            true
        } catch (_: JwtException) {
            false
        } catch (_: IllegalArgumentException) {
            false
        }
}
