package com.ista.myista.tenantapi

import org.springframework.stereotype.Service

@Service
class TenantApiService(private val client: TenantApiClient) {

    // Each method: refreshes the TenantAPI access token, then makes the API call.
    // This ensures we always use a valid token even if the stored one is stale.

    fun getTenant(refreshToken: String): Map<*, *> = call(refreshToken) {
        client.get("/api/customer/Tenant", it, Map::class.java)
    }

    fun getProperties(refreshToken: String): Map<*, *> = call(refreshToken) {
        client.get("/api/customer/Properties", it, Map::class.java)
    }

    fun getBalance(refreshToken: String): Map<*, *> = call(refreshToken) {
        client.get("/api/finance/Balance", it, Map::class.java)
    }

    fun getTransactions(refreshToken: String): Map<*, *> = call(refreshToken) {
        client.get("/api/finance/TransactionHistory", it, Map::class.java)
    }

    fun getBillingAgent(refreshToken: String): Map<*, *> = call(refreshToken) {
        client.get("/api/customer/BillingAgent", it, Map::class.java)
    }

    fun getContact(refreshToken: String): Map<*, *> = call(refreshToken) {
        client.get("/api/customer/Contact", it, Map::class.java)
    }

    fun updateContact(refreshToken: String, body: Map<String, Any>): Map<*, *> = call(refreshToken) {
        client.postJson("/api/customer/Contact", it, body, Map::class.java)
    }

    fun getMeterInfo(refreshToken: String): Map<*, *> = call(refreshToken) {
        client.get("/api/meter/Info", it, Map::class.java)
    }

    fun getMeterReads(refreshToken: String, meterId: String): Map<*, *> = call(refreshToken) {
        client.get("/api/meter/Reads/$meterId", it, Map::class.java)
    }

    fun getTariffs(refreshToken: String): Map<*, *> = call(refreshToken) {
        client.get("/api/meter/Tariffs", it, Map::class.java)
    }

    fun getSupplier(refreshToken: String): Map<*, *> = call(refreshToken) {
        client.get("/api/meter/Supplier", it, Map::class.java)
    }

    fun getDocuments(refreshToken: String): Map<*, *> = call(refreshToken) {
        client.get("/api/document/Documents", it, Map::class.java)
    }

    fun postPayment(refreshToken: String, body: Map<String, Any>): Map<*, *> = call(refreshToken) {
        client.postJson("/api/finance/postpayment", it, body, Map::class.java)
    }

    private fun <T> call(refreshToken: String, block: (accessToken: String) -> T): T {
        val tokens = client.refresh(refreshToken)
        return block(tokens.accessToken)
    }
}
