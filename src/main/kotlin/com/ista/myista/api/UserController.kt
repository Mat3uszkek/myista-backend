package com.ista.myista.api

import com.ista.myista.auth.UserPrincipal
import com.ista.myista.tenantapi.TenantApiService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class UserController(private val tenantApi: TenantApiService) {

    @GetMapping("/user")
    fun getUser(@AuthenticationPrincipal principal: UserPrincipal): Any =
        tenantApi.getTenant(principal.tenantRefreshToken)

    @GetMapping("/accounts")
    fun getAccounts(@AuthenticationPrincipal principal: UserPrincipal): Any =
        tenantApi.getProperties(principal.tenantRefreshToken)
}
