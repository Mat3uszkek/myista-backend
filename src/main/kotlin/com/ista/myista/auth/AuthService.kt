package com.ista.myista.auth

import com.ista.myista.auth.dto.LoginRequest
import com.ista.myista.auth.dto.TokenResponse
import com.ista.myista.tenantapi.TenantApiClient
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val tenantApiClient: TenantApiClient,
    private val jwtService: JwtService,
) {
    @Suppress("UNCHECKED_CAST")
    fun login(request: LoginRequest): TokenResponse {
        val tenantTokens = tenantApiClient.authenticate(request.username, request.password)
        val jwt = buildJwt(tenantTokens.accessToken, tenantTokens.refreshToken)
        return TokenResponse(accessToken = jwt, refreshToken = tenantTokens.refreshToken)
    }

    @Suppress("UNCHECKED_CAST")
    fun refresh(refreshToken: String): TokenResponse {
        val tenantTokens = tenantApiClient.refresh(refreshToken)
        val jwt = buildJwt(tenantTokens.accessToken, tenantTokens.refreshToken)
        return TokenResponse(accessToken = jwt, refreshToken = tenantTokens.refreshToken)
    }

    @Suppress("UNCHECKED_CAST")
    private fun buildJwt(tenantAccessToken: String, tenantRefreshToken: String): String {
        val response = tenantApiClient.get("/api/customer/Tenant", tenantAccessToken, Map::class.java)
            as Map<String, Any>
        val data = response["Data"] as Map<String, Any>

        return jwtService.generateToken(
            JwtClaims(
                userId = data["SecTenantId"] as String,
                custId = (data["AccountNumber"] as String).hashCode() and Int.MAX_VALUE,
                accountNumber = data["AccountNumber"] as String,
                tenantRefreshToken = tenantRefreshToken,
            ),
        )
    }
}
