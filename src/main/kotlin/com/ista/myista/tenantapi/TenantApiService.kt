package com.ista.myista.tenantapi

import com.ista.myista.tenantapi.dto.*
import org.springframework.stereotype.Service

@Service
class TenantApiService(private val client: TenantApiClient) {

    fun getTenant(refreshToken: String): UserInfo = call(refreshToken) {
        client.get("/api/customer/tenant", it, UserInfo::class.java)
    }

    fun getProperties(refreshToken: String): List<*> = call(refreshToken) {
        client.get("/api/customer/properties", it, List::class.java)
    }

    fun getBalance(refreshToken: String): Balance = call(refreshToken) {
        client.get("/api/finance/balance", it, Balance::class.java)
    }

    fun getTransactions(refreshToken: String, page: Int = 1, limit: Int = 20): List<Transaction> = call(refreshToken) {
        client.getList("/api/finance/transactionhistory", it, Transaction::class.java,
            mapOf("page" to page, "limit" to limit))
    }

    fun getBillingAgent(refreshToken: String): List<BillingAgentDetails> = call(refreshToken) {
        client.getList("/api/customer/billingagent", it, BillingAgentDetails::class.java)
    }

    fun getAccountDetails(refreshToken: String): AccountDetails = call(refreshToken) { token ->
        val profile = client.get("/api/customer/tenant", token, UserInfo::class.java)
        val properties = client.getList("/api/customer/properties", token, Account::class.java)
        val agents = client.getList("/api/customer/billingagent", token, BillingAgentDetails::class.java)
        val propertyAddress = properties.firstOrNull { it.active == true }?.address
            ?: properties.firstOrNull()?.address
        AccountDetails(
            profile = profile,
            propertyAddress = propertyAddress,
            billingAgent = agents.firstOrNull(),
        )
    }

    fun getContact(refreshToken: String): Contact = call(refreshToken) {
        client.get("/api/customer/contact", it, Contact::class.java)
    }

    fun updateContact(refreshToken: String, body: Map<String, Any>): Contact = call(refreshToken) {
        client.postJson("/api/customer/contact", it, body, Contact::class.java)
    }

    fun getMeterInfo(refreshToken: String): List<*> = call(refreshToken) {
        client.get("/api/meter/info", it, List::class.java)
    }

    // meterId as query param + billableOnly=true to match old PHP behaviour
    fun getMeterReads(refreshToken: String, meterId: String): MeterReadsResponse = call(refreshToken) {
        client.get("/api/meter/reads", it, MeterReadsResponse::class.java,
            mapOf("meterId" to meterId, "billableOnly" to "true"))
    }

    fun getTariffs(refreshToken: String): List<*> = call(refreshToken) {
        client.get("/api/meter/tariffs", it, List::class.java)
    }

    fun getSupplier(refreshToken: String): Supplier = call(refreshToken) {
        client.get("/api/meter/supplier", it, Supplier::class.java)
    }

    fun getDocuments(refreshToken: String): List<*> = call(refreshToken) {
        client.get("/api/customer/documents", it, List::class.java)
    }

    fun requestSoa(refreshToken: String, variantId: Int): Any = call(refreshToken) {
        client.get("/api/finance/RequestAccountSummaryDocument", it, Any::class.java,
            mapOf("variantId" to variantId))
    }

    // Old PHP: api/finance/postpayment?amount=X&type=CC&chkMoNo=
    fun postPayment(refreshToken: String, amount: Double): PaymentResult = call(refreshToken) {
        client.get("/api/finance/postpayment", it, PaymentResult::class.java,
            mapOf("amount" to amount, "type" to "CC", "chkMoNo" to ""))
    }

    fun paymentStatusUpdate(refreshToken: String, paymentId: String, status: String): Any = call(refreshToken) {
        client.get("/api/finance/paymentstatusupdate", it, Any::class.java,
            mapOf("paymentId" to paymentId, "status" to status))
    }

    fun updateStripeCustomerId(refreshToken: String, stripeCustomerId: String): Any = call(refreshToken) {
        client.postJson("/api/customer/property/updatestripe", it,
            mapOf("stripeCustomerId" to stripeCustomerId), Any::class.java)
    }

    fun updateEmail(refreshToken: String, body: Map<String, Any>): Any = call(refreshToken) {
        client.postJson("/api/customer/updateemail", it, body, Any::class.java)
    }

    fun changePassword(refreshToken: String, body: Map<String, Any>): Any = call(refreshToken) {
        client.postJson("/api/customer/ChangePassword", it, body, Any::class.java)
    }

    // Auto Top-Up (Stripe / prepayment variant only)
    fun getAutoTopUp(refreshToken: String): Any = call(refreshToken) {
        client.get("/api/finance/autotopup", it, Any::class.java)
    }

    fun setupAutoTopUp(refreshToken: String, body: Map<String, Any>): Any = call(refreshToken) {
        client.postJson("/api/finance/autotopup", it, body, Any::class.java)
    }

    fun cancelAutoTopUp(refreshToken: String): Any = call(refreshToken) {
        client.delete("/api/finance/autotopup", it, Any::class.java)
    }

    // Registration (unauthenticated — forward full TenantAPI response body)
    fun verifyAccount(body: Map<String, Any?>): Any = client.postJsonNoAuth("api/Account/Verification", body)
    fun generateRegistrationCode(body: Map<String, Any?>): Any = client.postJsonNoAuth("api/Account/GenerateRegistrationCode", body)
    fun registerAccount(body: Map<String, Any?>): Any = client.postJsonNoAuth("api/Account/Registration", body)
    fun activateAccount(body: Map<String, Any?>): Any = client.postJsonNoAuth("api/Account/SetPassword", body)
    fun requestPasswordReset(body: Map<String, Any?>): Any = client.postJsonNoAuth("api/Account/PasswordResetRequest", body)

    // Kiosk (QuickPay guest) — uses separate TenantAPI credentials, no user session
    fun kioskAccountBillInfo(accountNo: String): KioskAccount {
        val tokens = client.authenticateKiosk()
        return client.get("/api/kiosk/AccountBillInfo", tokens.accessToken, KioskAccount::class.java,
            mapOf("AccountNo" to accountNo))
    }

    fun kioskMakePayment(accountNo: String, amount: Double, paymentRef: String): PaymentResult {
        val tokens = client.authenticateKiosk()
        return client.get("/api/kiosk/makepayment", tokens.accessToken, PaymentResult::class.java,
            mapOf("AccountNo" to accountNo, "Amount" to amount, "PaymentRef" to paymentRef))
    }

    fun kioskPaymentStatusUpdate(paymentId: String, status: String): Any {
        val tokens = client.authenticateKiosk()
        return client.get("/api/kiosk/paymentStatusUpdate", tokens.accessToken, Any::class.java,
            mapOf("PaymentId" to paymentId, "Status" to status))
    }

    private fun <T> call(refreshToken: String, block: (accessToken: String) -> T): T {
        val tokens = client.refresh(refreshToken)
        return block(tokens.accessToken)
    }
}
