package com.ista.myista.api

import com.ista.myista.auth.UserPrincipal
import com.ista.myista.payment.PaymentGatewayRegistry
import com.ista.myista.payment.PaymentRequest
import com.ista.myista.payment.aps.APSGateway
import com.ista.myista.payment.stripe.StripeGateway
import com.ista.myista.tenantapi.TenantApiService
import com.ista.myista.variant.VariantService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api")
class BillingController(
    private val tenantApi: TenantApiService,
    private val gatewayRegistry: PaymentGatewayRegistry,
    private val variantService: VariantService,
    private val stripeGateway: StripeGateway,
    private val apsGateway: APSGateway,
) {
    @GetMapping("/balance")
    fun getBalance(@AuthenticationPrincipal principal: UserPrincipal) =
        tenantApi.getBalance(principal.tenantRefreshToken)

    @GetMapping("/transactions")
    fun getTransactions(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "20") limit: Int,
    ) = tenantApi.getTransactions(principal.tenantRefreshToken, page, limit)

    @GetMapping("/billing-agent")
    fun getBillingAgent(@AuthenticationPrincipal principal: UserPrincipal) =
        tenantApi.getBillingAgent(principal.tenantRefreshToken)

    @PostMapping("/payments")
    fun createPayment(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestBody body: Map<String, Any>,
        httpRequest: HttpServletRequest,
    ): Any {
        val variant = variantService.detect(httpRequest)
        val gateway = gatewayRegistry.forVariant(variant)
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Payments not available for variant: $variant")
        val amount = (body["amount"] as? Number)?.toDouble()
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing amount")
        val accountNo = body["accountNo"] as? String ?: principal.accountNumber
        val currency = variantService.getCurrency(variant)
        return gateway.createPaymentSession(PaymentRequest(amount, accountNo, currency), principal)
    }

    @PostMapping("/payments/void")
    fun voidPayment(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestBody body: Map<String, Any>,
        httpRequest: HttpServletRequest,
    ) {
        val variant = variantService.detect(httpRequest)
        val gateway = gatewayRegistry.forVariant(variant)
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Payments not available for variant: $variant")
        val paymentId = body["paymentId"] as? String
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing paymentId")
        gateway.voidPayment(paymentId, principal)
    }

    // APS (Amazon Payment Services / PayFort): confirm payment after redirect
    @PostMapping("/payments/aps/confirm")
    fun confirmAps(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestBody body: Map<String, String>,
    ): Map<String, Any?> {
        val paymentId = body["paymentId"]
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing paymentId")
        val responseCode = body["responseCode"] ?: ""
        apsGateway.confirmPayment(paymentId, responseCode, principal)
        val success = responseCode.startsWith("0")
        return mapOf("success" to success, "paymentId" to paymentId)
    }

    // Stripe-specific: setup intent for saving cards
    @PostMapping("/payments/stripe/setup-intent")
    fun createSetupIntent(@AuthenticationPrincipal principal: UserPrincipal) =
        mapOf("clientSecret" to stripeGateway.createSetupIntent(principal).clientSecret,
              "publishableKey" to stripeGateway.publishableKey)

    // Stripe-specific: complete off-session payment with saved card
    @PostMapping("/payments/stripe/complete-off-session")
    fun completeOffSession(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestBody body: Map<String, String>,
    ): Any {
        val piid = body["paymentIntentId"] ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing paymentIntentId")
        val pmid = body["paymentMethodId"] ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing paymentMethodId")
        return stripeGateway.completeOffSession(piid, pmid, principal)
    }

    // Stripe-specific: Direct Debit (BACS) mandate
    @PostMapping("/payments/stripe/direct-debit/mandate")
    fun createDirectDebitMandate(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestBody body: Map<String, String>,
    ): Any {
        val successUrl = body["successUrl"] ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing successUrl")
        val cancelUrl = body["cancelUrl"] ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing cancelUrl")
        val session = stripeGateway.createDirectDebitMandate(principal, successUrl, cancelUrl)
        return mapOf("checkoutUrl" to session.url)
    }

    @GetMapping("/payments/stripe/direct-debit/mandates")
    fun listDirectDebitMandates(@AuthenticationPrincipal principal: UserPrincipal) =
        stripeGateway.listDirectDebitMandates(principal)

    @DeleteMapping("/payments/stripe/direct-debit/mandate/{paymentMethodId}")
    fun cancelDirectDebitMandate(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable paymentMethodId: String,
    ) = stripeGateway.cancelDirectDebitMandate(paymentMethodId)

    // Stripe-specific: saved payment methods
    @GetMapping("/payments/stripe/payment-methods")
    fun listPaymentMethods(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam(defaultValue = "card") type: String,
    ) = stripeGateway.listPaymentMethods(principal, type)

    @DeleteMapping("/payments/stripe/payment-methods/{paymentMethodId}")
    fun deletePaymentMethod(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable paymentMethodId: String,
    ) = stripeGateway.deletePaymentMethod(paymentMethodId)

    // Auto Top-Up (prepayment variant, Stripe only)
    @GetMapping("/payments/autotopup")
    fun getAutoTopUp(@AuthenticationPrincipal principal: UserPrincipal) =
        tenantApi.getAutoTopUp(principal.tenantRefreshToken)

    @PostMapping("/payments/autotopup")
    fun setupAutoTopUp(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestBody body: Map<String, Any>,
    ): Any {
        val mapped = mapOf(
            "TopUpAmount" to (body["amount"] ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing amount")),
            "BalanceThreshold" to (body["threshold"] ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing threshold")),
            "StripePaymentMethod" to (body["stripe_pm"] ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing stripe_pm")),
            "CardVendor" to (body["card_brand"] ?: ""),
            "CardLastFour" to (body["card_last4"] ?: ""),
        )
        return tenantApi.setupAutoTopUp(principal.tenantRefreshToken, mapped)
    }

    @DeleteMapping("/payments/autotopup")
    fun cancelAutoTopUp(@AuthenticationPrincipal principal: UserPrincipal) =
        tenantApi.cancelAutoTopUp(principal.tenantRefreshToken)
}
