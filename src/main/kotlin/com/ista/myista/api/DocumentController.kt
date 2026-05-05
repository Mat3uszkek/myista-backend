package com.ista.myista.api

import com.ista.myista.auth.UserPrincipal
import com.ista.myista.tenantapi.TenantApiService
import com.ista.myista.variant.VariantService
import jakarta.servlet.http.HttpServletRequest
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
class DocumentController(
    private val tenantApi: TenantApiService,
    private val variantService: VariantService,
) {

    @GetMapping("/documents")
    fun getDocuments(@AuthenticationPrincipal principal: UserPrincipal): Any =
        tenantApi.getDocuments(principal.tenantRefreshToken)

    @PostMapping("/documents/request")
    fun requestDocument(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestBody body: Map<String, String>,
        request: HttpServletRequest,
    ): Any {
        val docType = body["docType"]
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "docType required")
        return when (docType) {
            "soa" -> {
                val variantId = variantService.getApiVariantId(variantService.detect(request))
                tenantApi.requestSoa(principal.tenantRefreshToken, variantId)
            }
            else -> throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown docType: $docType")
        }
    }
}
