package com.ista.myista.payment

import com.ista.myista.auth.UserPrincipal

interface PaymentGateway {
    fun createPaymentSession(request: PaymentRequest, principal: UserPrincipal): PaymentSession
    fun voidPayment(paymentId: String, principal: UserPrincipal)
}

interface GuestPaymentGateway {
    fun createGuestPaymentSession(request: GuestPaymentRequest): PaymentSession
    fun confirmGuestPayment(paymentId: String, status: String)
}

data class PaymentRequest(
    val amount: Double,
    val accountNo: String,
)

data class GuestPaymentRequest(
    val amount: Double,
    val accountNo: String,
    val email: String? = null,
)

sealed class PaymentSession {
    abstract val paymentId: String
    abstract val method: String

    data class StripeSession(
        override val paymentId: String,
        val clientSecret: String,
        val publishableKey: String,
    ) : PaymentSession() {
        override val method = "stripe"
    }

    data class NGeniusSession(
        override val paymentId: String,
        val redirectUrl: String,
    ) : PaymentSession() {
        override val method = "ngenius"
    }

    data class CBQSession(
        override val paymentId: String,
        val sessionId: String,
        val successIndicator: String,
        val jsLibUrl: String,
    ) : PaymentSession() {
        override val method = "cbq"
    }
}
