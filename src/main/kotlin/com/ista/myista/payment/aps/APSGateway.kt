package com.ista.myista.payment.aps

import com.ista.myista.auth.UserPrincipal
import com.ista.myista.payment.PaymentGateway
import com.ista.myista.payment.PaymentRequest
import com.ista.myista.payment.PaymentSession
import com.ista.myista.tenantapi.TenantApiService
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class APSGateway(
    @Value("\${aps.merchant-identifier:}") val merchantIdentifier: String,
    @Value("\${aps.access-code:}") val accessCode: String,
    @Value("\${aps.sha-request-phrase:}") val shaRequestPhrase: String,
    @Value("\${aps.hpp-url:https://checkout.payfort.com/FortAPI/paymentPage}") private val hppUrl: String,
    private val tenantApi: TenantApiService,
) : PaymentGateway {

    override fun createPaymentSession(request: PaymentRequest, principal: UserPrincipal): PaymentSession {
        val paymentResult = tenantApi.postPayment(principal.tenantRefreshToken, request.amount)
        val paymentId = requireNotNull(paymentResult.paymentId) { "TenantAPI did not return paymentId" }

        val email = runCatching { tenantApi.getTenant(principal.tenantRefreshToken).email }
            .getOrNull() ?: "noreply@ista.com"
        val merchantReference = "${paymentId.padStart(9, '0')}-${principal.userId}-np"
        val amountLowest = (request.amount * 100).toInt()

        return PaymentSession.APSSession(
            paymentId = paymentId,
            merchantReference = merchantReference,
            accessCode = accessCode,
            merchantIdentifier = merchantIdentifier,
            shaRequestPhrase = shaRequestPhrase,
            amount = amountLowest,
            currency = request.currency,
            customerEmail = email,
            hppUrl = hppUrl,
        )
    }

    override fun voidPayment(paymentId: String, principal: UserPrincipal) {
        tenantApi.paymentStatusUpdate(principal.tenantRefreshToken, paymentId, "R")
    }

    fun confirmPayment(paymentId: String, responseCode: String, principal: UserPrincipal) {
        val tenantStatus = if (responseCode.startsWith("0")) "A" else "R"
        tenantApi.paymentStatusUpdate(principal.tenantRefreshToken, paymentId, tenantStatus)
    }
}
