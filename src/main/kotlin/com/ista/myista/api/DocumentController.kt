package com.ista.myista.api

import com.ista.myista.auth.UserPrincipal
import com.ista.myista.tenantapi.TenantApiService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class DocumentController(private val tenantApi: TenantApiService) {

    @GetMapping("/documents")
    fun getDocuments(@AuthenticationPrincipal principal: UserPrincipal): Any =
        tenantApi.getDocuments(principal.tenantRefreshToken)
}
