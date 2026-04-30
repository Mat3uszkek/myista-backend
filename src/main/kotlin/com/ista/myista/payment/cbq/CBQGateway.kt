package com.ista.myista.payment.cbq

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
class CBQGateway(
    @Value("\${cbq.url:}") private val url: String,
    @Value("\${cbq.merchant-id:}") private val merchantId: String,
    @Value("\${cbq.password:}") private val cbqPassword: String,
    @Value("\${cbq.js-lib-url:}") val jsLibUrl: String,
    @Value("\${cbq.notification-url:}") private val notificationUrl: String,
    private val tenantApi: TenantApiService,
) : PaymentGateway, GuestPaymentGateway {

    private val client by lazy { RestClient.builder().baseUrl(url).build() }

    // CBQ is only used for Qatar QuickPay — logged-in Qatar users use N-Genius
    override fun createPaymentSession(request: PaymentRequest, principal: UserPrincipal): PaymentSession {
        val paymentResult = tenantApi.postPayment(
            principal.tenantRefreshToken, request.amount
        )
        val paymentId = requireNotNull(paymentResult.paymentId) { "TenantAPI did not return paymentId" }
        return createCBQSession(request.amount, "QAR", paymentId)
    }

    override fun voidPayment(paymentId: String, principal: UserPrincipal) {
        tenantApi.paymentStatusUpdate(principal.tenantRefreshToken, paymentId, "R")
    }

    override fun createGuestPaymentSession(request: GuestPaymentRequest): PaymentSession {
        val kioskAccount = tenantApi.kioskAccountBillInfo(request.accountNo)
        val paymentResult = tenantApi.kioskMakePayment(request.accountNo, request.amount, "QUICKPAY")
        val paymentId = requireNotNull(paymentResult.paymentId) { "TenantAPI did not return paymentId" }
        val currency = kioskAccount.currency ?: "QAR"
        return createCBQSession(request.amount, currency, paymentId)
    }

    override fun confirmGuestPayment(paymentId: String, status: String) {
        val cbqStatus = queryCBQOrderStatus(paymentId)
        val tenantStatus = if (cbqStatus == "CAPTURED") "A" else "R"
        tenantApi.kioskPaymentStatusUpdate(paymentId, tenantStatus)
    }

    fun confirmPayment(paymentId: String, principal: UserPrincipal) {
        val cbqStatus = queryCBQOrderStatus(paymentId)
        val tenantStatus = if (cbqStatus == "CAPTURED") "A" else "R"
        tenantApi.paymentStatusUpdate(principal.tenantRefreshToken, paymentId, tenantStatus)
    }

    private fun createCBQSession(amount: Double, currency: String, paymentId: String): PaymentSession.CBQSession {
        val body = mapOf(
            "apiOperation" to "INITIATE_CHECKOUT",
            "interaction" to mapOf("operation" to "PURCHASE"),
            "order" to mapOf(
                "id" to paymentId,
                "amount" to amount,
                "currency" to currency,
                "notificationUrl" to notificationUrl,
            ),
        )
        @Suppress("UNCHECKED_CAST")
        val response = client.post()
            .uri("/session")
            .header("Authorization", basicAuth())
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .body(Map::class.java) as Map<String, Any>

        @Suppress("UNCHECKED_CAST")
        val session = response["session"] as? Map<String, Any>
            ?: error("CBQ did not return a session")
        val sessionId = session["id"] as? String ?: error("CBQ session missing id")
        val successIndicator = response["successIndicator"] as? String ?: ""

        return PaymentSession.CBQSession(paymentId, sessionId, successIndicator, jsLibUrl)
    }

    private fun queryCBQOrderStatus(paymentId: String): String {
        @Suppress("UNCHECKED_CAST")
        val response = client.get()
            .uri("/order/$paymentId")
            .header("Authorization", basicAuth())
            .retrieve()
            .body(Map::class.java) as Map<String, Any>
        return response["status"] as? String ?: "UNKNOWN"
    }

    private fun basicAuth(): String {
        val credentials = Base64.getEncoder().encodeToString("merchant.$merchantId:$cbqPassword".toByteArray())
        return "Basic $credentials"
    }
}
