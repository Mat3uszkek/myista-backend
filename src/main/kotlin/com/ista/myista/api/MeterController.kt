package com.ista.myista.api

import com.ista.myista.auth.UserPrincipal
import com.ista.myista.tenantapi.TenantApiService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class MeterController(private val tenantApi: TenantApiService) {

    @GetMapping("/meters")
    fun getMeters(@AuthenticationPrincipal principal: UserPrincipal): Any =
        tenantApi.getMeterInfo(principal.tenantRefreshToken)

    @GetMapping("/meters/{id}/reads")
    fun getMeterReads(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: String,
    ): Any = tenantApi.getMeterReads(principal.tenantRefreshToken, id)
}
