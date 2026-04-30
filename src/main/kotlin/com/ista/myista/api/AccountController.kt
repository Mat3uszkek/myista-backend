package com.ista.myista.api

import com.ista.myista.auth.UserPrincipal
import com.ista.myista.tenantapi.TenantApiService
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/account")
class AccountController(private val tenantApi: TenantApiService) {

    @GetMapping("/profile")
    fun getProfile(@AuthenticationPrincipal principal: UserPrincipal) =
        tenantApi.getUserProfile(principal.tenantRefreshToken)

    @PutMapping("/profile")
    fun updateEmail(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestBody body: Map<String, Any>,
    ): Any {
        body["email"] ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing email")
        return tenantApi.updateEmail(principal.tenantRefreshToken, body)
    }

    @PostMapping("/password")
    fun changePassword(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestBody body: Map<String, Any>,
    ): Any {
        body["currentPassword"] ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing currentPassword")
        body["newPassword"] ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing newPassword")
        return tenantApi.changePassword(principal.tenantRefreshToken, body)
    }
}
