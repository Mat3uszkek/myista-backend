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

    fun detect(request: HttpServletRequest): String {
        val host = request.serverName.lowercase()
        return domainMap.entries.firstOrNull { (domain, _) -> host.endsWith(domain) }?.value
            ?: request.getParameter("variant")
            ?: "uk"
    }
}
