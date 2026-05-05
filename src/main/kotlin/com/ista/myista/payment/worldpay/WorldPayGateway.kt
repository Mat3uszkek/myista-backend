package com.ista.myista.payment.worldpay

import com.ista.myista.auth.UserPrincipal
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
class WorldPayGateway(
    @Value("\${worldpay.url:}") private val url: String,
    @Value("\${worldpay.merchant-code:}") private val merchantCode: String,
    @Value("\${worldpay.xml-password:}") private val xmlPassword: String,
    @Value("\${worldpay.js-lib-url:https://secure.worldpay.com/wcc/wrappedMessages}") private val jsLibUrl: String,
    private val tenantApi: TenantApiService,
) : PaymentGateway {

    private val client by lazy { RestClient.builder().baseUrl(url).build() }

    override fun createPaymentSession(request: PaymentRequest, principal: UserPrincipal): PaymentSession {
        val paymentResult = tenantApi.postPayment(principal.tenantRefreshToken, request.amount)
        val paymentId = requireNotNull(paymentResult.paymentId) { "TenantAPI did not return paymentId" }
        val orderCode = paymentId.padStart(9, '0')
        val amountPence = (request.amount * 100).toInt()

        val responseXml = client.post()
            .uri("/payments/merchants/$merchantCode")
            .header("Authorization", basicAuth())
            .contentType(MediaType.APPLICATION_XML)
            .accept(MediaType.APPLICATION_XML)
            .body(buildXml(orderCode, amountPence, request.currency))
            .retrieve()
            .body(String::class.java) ?: error("WorldPay returned empty response")

        val hppUrl = parseReferenceUrl(responseXml)
        return PaymentSession.WorldPaySession(paymentId, hppUrl, jsLibUrl)
    }

    override fun voidPayment(paymentId: String, principal: UserPrincipal) {
        tenantApi.paymentStatusUpdate(principal.tenantRefreshToken, paymentId, "R")
    }

    private fun buildXml(orderCode: String, amountPence: Int, currency: String) = """
        <?xml version="1.0" encoding="UTF-8"?>
        <!DOCTYPE paymentService PUBLIC "-//WorldPay//DTD WorldPay PaymentService v1//EN" "http://dtd.worldpay.com/paymentService_v1.dtd">
        <paymentService version="1.4" merchantCode="$merchantCode">
          <submit>
            <order orderCode="$orderCode">
              <description>Payment</description>
              <amount value="$amountPence" currencyCode="$currency" exponent="2"/>
              <paymentMethodMask>
                <include code="ALL"/>
              </paymentMethodMask>
            </order>
          </submit>
        </paymentService>
    """.trimIndent()

    private fun parseReferenceUrl(xml: String): String =
        Regex("""<reference[^>]*>([^<]+)</reference>""").find(xml)
            ?.groupValues?.get(1)?.trim()
            ?: error("WorldPay response missing <reference> URL")

    private fun basicAuth(): String {
        val encoded = Base64.getEncoder().encodeToString("$merchantCode:$xmlPassword".toByteArray())
        return "Basic $encoded"
    }
}
