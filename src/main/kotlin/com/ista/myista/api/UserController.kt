package com.ista.myista.api

import com.ista.myista.auth.AuthService
import com.ista.myista.auth.UserPrincipal
import com.ista.myista.auth.dto.TokenResponse
import com.ista.myista.tenantapi.TenantApiService
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api")
class UserController(
    private val tenantApi: TenantApiService,
    private val authService: AuthService,
) {

    @GetMapping("/user")
    fun getUser(@AuthenticationPrincipal principal: UserPrincipal): Any =
        tenantApi.getTenant(principal.tenantRefreshToken)

    @GetMapping("/accounts")
    fun getAccounts(@AuthenticationPrincipal principal: UserPrincipal): Any =
        tenantApi.getProperties(principal.tenantRefreshToken)

    @PostMapping("/accounts/switch")
    fun switchAccount(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestBody body: Map<String, Int>,
    ): TokenResponse {
        val custId = body["custId"]
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "custId required")
        return authService.switchAccount(principal.tenantRefreshToken, custId)
    }
}
