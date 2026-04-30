package com.ista.myista.api

import com.ista.myista.auth.UserPrincipal
import com.ista.myista.payment.PaymentGatewayRegistry
import com.ista.myista.payment.cbq.CBQGateway
import com.ista.myista.payment.stripe.StripeGateway
import com.ista.myista.tenantapi.TenantApiService
import com.ista.myista.variant.VariantService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/payments/status")
class PaymentStatusController(
    private val tenantApi: TenantApiService,
    private val stripeGateway: StripeGateway,
    private val cbqGateway: CBQGateway,
    private val variantService: VariantService,
) {
    /**
     * Called after payment processor redirects the user back.
     * method: stripe | ngenius | cbq | worldpay
     * status: success | failure | pending | cancel | error | ddsuccess
     */
    @GetMapping("/{method}/{status}")
    fun handleCallback(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable method: String,
        @PathVariable status: String,
        @RequestParam(required = false) pid: String?,
        @RequestParam(required = false) err: String?,
    ): Map<String, Any?> {
        when (method) {
            "stripe" -> if (status == "success") stripeGateway.deduplicatePaymentMethods(principal)
            "cbq" -> if (!pid.isNullOrBlank()) {
                cbqGateway.confirmPayment(pid, principal)
            }
        }
        val decodedError = err?.let { runCatching { String(java.util.Base64.getDecoder().decode(it)) }.getOrNull() }
        return mapOf("method" to method, "status" to status, "paymentId" to pid, "error" to decodedError)
    }
}
