package com.ista.myista.api

import com.ista.myista.payment.GuestPaymentRequest
import com.ista.myista.payment.PaymentGatewayRegistry
import com.ista.myista.tenantapi.TenantApiService
import com.ista.myista.variant.VariantService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/quickpay")
class QuickPayController(
    private val tenantApi: TenantApiService,
    private val gatewayRegistry: PaymentGatewayRegistry,
    private val variantService: VariantService,
) {
    @PostMapping("/lookup")
    fun lookupAccount(@RequestBody body: Map<String, String>): Any {
        val accountNo = body["accountNo"]
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing accountNo")
        return tenantApi.kioskAccountBillInfo(accountNo)
    }

    @PostMapping("/payment")
    fun createPayment(
        @RequestBody body: Map<String, Any>,
        httpRequest: HttpServletRequest,
    ): Any {
        val variant = variantService.detect(httpRequest)
        val gateway = gatewayRegistry.guestForVariant(variant)
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "QuickPay not available for variant: $variant")
        val accountNo = body["accountNo"] as? String
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing accountNo")
        val amount = (body["amount"] as? Number)?.toDouble()
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing amount")
        val email = body["email"] as? String
        return gateway.createGuestPaymentSession(GuestPaymentRequest(amount, accountNo, email))
    }

    @GetMapping("/status/{method}/{status}")
    fun handleStatus(
        @PathVariable method: String,
        @PathVariable status: String,
        @RequestParam(required = false) pid: String?,
        @RequestParam(required = false) err: String?,
        httpRequest: HttpServletRequest,
    ): Map<String, Any?> {
        if (!pid.isNullOrBlank()) {
            val variant = variantService.detect(httpRequest)
            gatewayRegistry.guestForVariant(variant)?.confirmGuestPayment(pid, status)
        }
        val decodedError = err?.let { runCatching { String(java.util.Base64.getDecoder().decode(it)) }.getOrNull() }
        return mapOf("method" to method, "status" to status, "paymentId" to pid, "error" to decodedError)
    }
}
