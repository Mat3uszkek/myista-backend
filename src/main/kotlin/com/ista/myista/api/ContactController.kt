package com.ista.myista.api

import com.ista.myista.auth.UserPrincipal
import com.ista.myista.tenantapi.TenantApiService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class ContactController(private val tenantApi: TenantApiService) {

    @GetMapping("/contact")
    fun getContact(@AuthenticationPrincipal principal: UserPrincipal): Any =
        tenantApi.getContact(principal.tenantRefreshToken)

    @PutMapping("/contact")
    fun updateContact(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestBody body: Map<String, Any>,
    ): Any = tenantApi.updateContact(principal.tenantRefreshToken, body)

    @GetMapping("/supplier")
    fun getSupplier(@AuthenticationPrincipal principal: UserPrincipal): Any =
        tenantApi.getSupplier(principal.tenantRefreshToken)
}
