package com.ista.myista.api

import com.ista.myista.auth.UserPrincipal
import com.ista.myista.tenantapi.TenantApiService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class BillingController(private val tenantApi: TenantApiService) {

    @GetMapping("/balance")
    fun getBalance(@AuthenticationPrincipal principal: UserPrincipal): Any =
        tenantApi.getBalance(principal.tenantRefreshToken)

    @GetMapping("/transactions")
    fun getTransactions(@AuthenticationPrincipal principal: UserPrincipal): Any =
        tenantApi.getTransactions(principal.tenantRefreshToken)

    @GetMapping("/billing-agent")
    fun getBillingAgent(@AuthenticationPrincipal principal: UserPrincipal): Any =
        tenantApi.getBillingAgent(principal.tenantRefreshToken)

    @PostMapping("/payments")
    fun createPayment(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestBody body: Map<String, Any>,
    ): Any = tenantApi.postPayment(principal.tenantRefreshToken, body)
}
