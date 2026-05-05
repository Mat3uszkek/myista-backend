package com.ista.myista.payment.ngenius

import com.ista.myista.auth.UserPrincipal
import com.ista.myista.payment.GuestPaymentGateway
import com.ista.myista.payment.GuestPaymentRequest
import com.ista.myista.payment.PaymentGateway
import com.ista.myista.payment.PaymentRequest
import com.ista.myista.payment.PaymentSession
import com.ista.myista.tenantapi.TenantApiService
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.util.Base64

@Component
class NGeniusGateway(
    @Value("\${ngenius.url:}") private val url: String,
    @Value("\${ngenius.username:}") private val username: String,
    @Value("\${ngenius.password:}") private val password: String,
    @Value("\${ngenius.outlet:}") private val outletId: String,
    private val tenantApi: TenantApiService,
) : PaymentGateway, GuestPaymentGateway {

    private val client by lazy { RestClient.builder().baseUrl(url).build() }

    override fun createPaymentSession(request: PaymentRequest, principal: UserPrincipal): PaymentSession {
        val paymentResult = tenantApi.postPayment(
            principal.tenantRefreshToken, request.amount
        )
        val paymentId = requireNotNull(paymentResult.paymentId) { "TenantAPI did not return paymentId" }
        val redirectUrl = createOrderAndGetPaymentUrl(request.amount, request.currency, paymentId, null)
        return PaymentSession.NGeniusSession(paymentId, redirectUrl)
    }

    override fun voidPayment(paymentId: String, principal: UserPrincipal) {
        val token = getAccessToken()
        client.put().uri("/transactions/outlets/$outletId/orders/$paymentId/cancel")
            .header("Authorization", "Bearer $token")
            .retrieve()
            .toBodilessEntity()
        tenantApi.paymentStatusUpdate(principal.tenantRefreshToken, paymentId, "R")
    }

    override fun createGuestPaymentSession(request: GuestPaymentRequest): PaymentSession {
        val kioskAccount = tenantApi.kioskAccountBillInfo(request.accountNo)
        val paymentResult = tenantApi.kioskMakePayment(request.accountNo, request.amount, "QUICKPAY")
        val paymentId = requireNotNull(paymentResult.paymentId) { "TenantAPI did not return paymentId for kiosk" }
        val currency = kioskAccount.currency ?: "AED"

        val redirectUrl = createOrderAndGetPaymentUrl(request.amount, currency, paymentId, request.email)
        return PaymentSession.NGeniusSession(paymentId, redirectUrl)
    }

    override fun confirmGuestPayment(paymentId: String, status: String) {
        val tenantStatus = if (status == "success") "A" else "R"
        tenantApi.kioskPaymentStatusUpdate(paymentId, tenantStatus)
    }

    private fun createOrderAndGetPaymentUrl(
        amount: Double,
        currency: String,
        merchantOrderReference: String,
        email: String?,
    ): String {
        val token = getAccessToken()
        val amountInFils = (amount * 100).toInt()
        val body = buildMap<String, Any> {
            put("action", "SALE")
            put("amount", mapOf("currencyCode" to currency, "value" to amountInFils))
            put("merchantOrderReference", merchantOrderReference)
            if (email != null) put("emailAddress", email)
        }

        @Suppress("UNCHECKED_CAST")
        val response = client.post()
            .uri("/transactions/outlets/$outletId/orders")
            .header("Authorization", "Bearer $token")
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .body(Map::class.java) as Map<String, Any>

        @Suppress("UNCHECKED_CAST")
        val links = response["_links"] as? Map<String, Any>
        @Suppress("UNCHECKED_CAST")
        val payment = links?.get("payment") as? Map<String, Any>
        return payment?.get("href") as? String
            ?: error("N-Genius did not return a payment URL")
    }

    private fun getAccessToken(): String {
        val credentials = Base64.getEncoder().encodeToString("$username:$password".toByteArray())

        @Suppress("UNCHECKED_CAST")
        val response = client.post()
            .uri("/identity/auth/access-token")
            .header("Authorization", "Basic $credentials")
            .contentType(MediaType.APPLICATION_JSON)
            .retrieve()
            .body(Map::class.java) as Map<String, Any>

        return response["access_token"] as? String
            ?: error("N-Genius did not return an access token")
    }
}
