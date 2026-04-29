package com.ista.myista.auth

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class JwtServiceTest {

    private val jwtService = JwtService(
        secret = "test-secret-key-that-is-at-least-32-characters-long!",
        expirationMs = 3_600_000,
    )

    private val sampleClaims = JwtClaims(
        userId = "user-001",
        custId = 12345,
        accountNumber = "ACC-001",
        tenantRefreshToken = "refresh-token",
    )

    @Test
    fun `generated token is valid`() {
        val token = jwtService.generateToken(sampleClaims)
        assertTrue(jwtService.isValid(token))
    }

    @Test
    fun `parsed claims match original`() {
        val token = jwtService.generateToken(sampleClaims)
        val parsed = jwtService.parseClaims(token)

        assertEquals(sampleClaims.userId, parsed.userId)
        assertEquals(sampleClaims.custId, parsed.custId)
        assertEquals(sampleClaims.accountNumber, parsed.accountNumber)
        assertEquals(sampleClaims.tenantRefreshToken, parsed.tenantRefreshToken)
    }

    @Test
    fun `tampered token is invalid`() {
        val token = jwtService.generateToken(sampleClaims)
        val tampered = token.dropLast(5) + "XXXXX"
        assertFalse(jwtService.isValid(tampered))
    }
}
