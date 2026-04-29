package com.ista.myista.auth

import com.ista.myista.auth.dto.LoginRequest
import com.ista.myista.tenantapi.TenantApiClient
import com.ista.myista.tenantapi.TenantTokens
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class AuthServiceTest {

    private val jwtService = JwtService(
        secret = "test-secret-key-that-is-at-least-32-characters-long!",
        expirationMs = 3_600_000,
    )

    private val tenantResponse = mapOf(
        "StatusCode" to 200,
        "Data" to mapOf(
            "SecTenantId" to "user-001",
            "AccountNumber" to "ACC-DEMO-001",
        ),
    )

    private val tenantClient = mock<TenantApiClient> {
        on { authenticate(any(), any()) } doReturn TenantTokens("access-token", "refresh-token")
        on { refresh(any()) } doReturn TenantTokens("new-access-token", "new-refresh-token")
        on { get(any(), any(), any<Class<Map<*, *>>>()) } doReturn tenantResponse
    }

    private val service = AuthService(tenantClient, jwtService)

    @Test
    fun `login returns a valid JWT`() {
        val result = service.login(LoginRequest("user", "pass"))

        assertNotNull(result.accessToken)
        assertTrue(jwtService.isValid(result.accessToken))
    }

    @Test
    fun `JWT contains correct account number`() {
        val result = service.login(LoginRequest("user", "pass"))
        val claims = jwtService.parseClaims(result.accessToken)

        assert(claims.accountNumber == "ACC-DEMO-001")
        assert(claims.userId == "user-001")
        assert(claims.tenantRefreshToken == "refresh-token")
    }

    @Test
    fun `refresh returns a new valid JWT`() {
        val result = service.refresh("old-refresh-token")

        assertNotNull(result.accessToken)
        assertTrue(jwtService.isValid(result.accessToken))
        assert(result.refreshToken == "new-refresh-token")
    }
}
