package com.ista.myista.variant

import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Service

@Service
class VariantService {

    private val domainMap = mapOf(
        "myista.co.uk" to "uk",
        "myista.ae" to "uae",
        "myenergy-thameswey.co.uk" to "thameswey",
        "prepay.myista.co.uk" to "prepayment",
        "myista.qa" to "qatar",
        "myista.be" to "be",
    )

    // TenantAPI identifiers per variant — match values in MinuteView.mi site_variants table
    private val schemeIds = mapOf("uk" to 1, "uae" to 2, "thameswey" to 3, "prepayment" to 5, "qatar" to 6, "be" to 7)
    private val apiVariantIds = mapOf("uk" to 1, "uae" to 2, "thameswey" to 3, "prepayment" to 5, "qatar" to 6, "be" to 7)

    fun detect(request: HttpServletRequest): String {
        val host = request.serverName.lowercase()
        return domainMap.entries.firstOrNull { (domain, _) -> host.endsWith(domain) }?.value
            ?: request.getParameter("variant")
            ?: "uk"
    }

    fun getSchemeId(variant: String): Int = schemeIds[variant] ?: 1
    fun getApiVariantId(variant: String): Int = apiVariantIds[variant] ?: 1
}
