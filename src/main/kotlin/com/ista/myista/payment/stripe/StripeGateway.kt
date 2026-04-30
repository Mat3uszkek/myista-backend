package com.ista.myista.payment.stripe

import com.ista.myista.auth.UserPrincipal
import com.ista.myista.payment.PaymentGateway
import com.ista.myista.payment.PaymentRequest
import com.ista.myista.payment.PaymentSession
import com.ista.myista.tenantapi.TenantApiService
import com.stripe.Stripe
import com.stripe.model.Customer
import com.stripe.model.PaymentIntent
import com.stripe.model.PaymentMethod
import com.stripe.model.SetupIntent
import com.stripe.model.checkout.Session
import com.stripe.param.CustomerCreateParams
import com.stripe.param.PaymentIntentConfirmParams
import com.stripe.param.PaymentIntentCreateParams
import com.stripe.param.PaymentMethodListParams
import com.stripe.param.SetupIntentCreateParams
import com.stripe.param.checkout.SessionCreateParams
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class StripeGateway(
    @Value("\${stripe.secret-key:}") secretKey: String,
    @Value("\${stripe.publishable-key:}") val publishableKey: String,
    private val tenantApi: TenantApiService,
) : PaymentGateway {

    init {
        if (secretKey.isNotBlank()) Stripe.apiKey = secretKey
    }

    override fun createPaymentSession(request: PaymentRequest, principal: UserPrincipal): PaymentSession {
        val paymentResult = tenantApi.postPayment(principal.tenantRefreshToken, request.amount)
        val paymentId = requireNotNull(paymentResult.paymentId) { "TenantAPI did not return paymentId" }

        val customerId = ensureStripeCustomer(principal)
        val amountInPence = (request.amount * 100).toLong()
        check(amountInPence >= 30) { "Minimum Stripe payment is £0.30" }

        val intent = PaymentIntent.create(
            PaymentIntentCreateParams.builder()
                .setAmount(amountInPence)
                .setCurrency("gbp")
                .setCustomer(customerId)
                .setSetupFutureUsage(PaymentIntentCreateParams.SetupFutureUsage.OFF_SESSION)
                .putMetadata("paymentId", paymentId)
                .build()
        )
        return PaymentSession.StripeSession(paymentId, intent.clientSecret, publishableKey)
    }

    override fun voidPayment(paymentId: String, principal: UserPrincipal) {
        PaymentIntent.retrieve(paymentId).cancel()
        tenantApi.paymentStatusUpdate(principal.tenantRefreshToken, paymentId, "R")
    }

    fun createSetupIntent(principal: UserPrincipal): SetupIntent {
        val customerId = ensureStripeCustomer(principal)
        return SetupIntent.create(
            SetupIntentCreateParams.builder()
                .setCustomer(customerId)
                .addPaymentMethodType("card")
                .build()
        )
    }

    // Confirm a PaymentIntent using a saved card (off-session)
    fun completeOffSession(paymentIntentId: String, paymentMethodId: String, principal: UserPrincipal): PaymentSession {
        val confirmed = PaymentIntent.retrieve(paymentIntentId).confirm(
            PaymentIntentConfirmParams.builder()
                .setPaymentMethod(paymentMethodId)
                .build()
        )
        val paymentId = confirmed.metadata["paymentId"] ?: paymentIntentId
        return PaymentSession.StripeSession(paymentId, confirmed.clientSecret, publishableKey)
    }

    // Create a Stripe Checkout session for BACS Direct Debit mandate setup
    fun createDirectDebitMandate(principal: UserPrincipal, successUrl: String, cancelUrl: String): Session {
        val customerId = ensureStripeCustomer(principal)
        return Session.create(
            SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SETUP)
                .setCustomer(customerId)
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.BACS_DEBIT)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .build()
        )
    }

    fun listDirectDebitMandates(principal: UserPrincipal): List<PaymentMethod> {
        val customerId = ensureStripeCustomer(principal)
        return PaymentMethod.list(
            PaymentMethodListParams.builder()
                .setCustomer(customerId)
                .setType(PaymentMethodListParams.Type.BACS_DEBIT)
                .build()
        ).data
    }

    fun cancelDirectDebitMandate(paymentMethodId: String) {
        PaymentMethod.retrieve(paymentMethodId).detach()
    }

    fun listPaymentMethods(principal: UserPrincipal, type: String): List<PaymentMethod> {
        val customerId = ensureStripeCustomer(principal)
        val pmType = when (type) {
            "bacs_debit" -> PaymentMethodListParams.Type.BACS_DEBIT
            else -> PaymentMethodListParams.Type.CARD
        }
        val methods = PaymentMethod.list(
            PaymentMethodListParams.builder()
                .setCustomer(customerId)
                .setType(pmType)
                .build()
        ).data
        return deduplicatePaymentMethods(methods)
    }

    fun deletePaymentMethod(paymentMethodId: String) {
        PaymentMethod.retrieve(paymentMethodId).detach()
    }

    // Remove duplicate physical cards (same fingerprint)
    private fun deduplicatePaymentMethods(methods: List<PaymentMethod>): List<PaymentMethod> {
        val seen = mutableSetOf<String>()
        return methods.filter { pm ->
            val fingerprint = pm.card?.fingerprint ?: pm.bacsDebit?.fingerprint ?: pm.id
            seen.add(fingerprint)
        }
    }

    fun ensureStripeCustomer(principal: UserPrincipal): String {
        val user = tenantApi.getTenant(principal.tenantRefreshToken)
        if (!user.stripeCustomerId.isNullOrBlank()) return user.stripeCustomerId

        val customer = Customer.create(
            CustomerCreateParams.builder()
                .setEmail(user.email)
                .setName("${user.firstName} ${user.lastName}".trim().ifBlank { null })
                .putMetadata("accountNo", user.accountNo ?: "")
                .build()
        )
        tenantApi.updateStripeCustomerId(principal.tenantRefreshToken, customer.id)
        return customer.id
    }
}
