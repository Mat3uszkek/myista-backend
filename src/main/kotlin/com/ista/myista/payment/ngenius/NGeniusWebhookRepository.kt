package com.ista.myista.payment.ngenius

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class NGeniusWebhookRepository(@Qualifier("uksql01Jdbc") private val jdbc: JdbcTemplate) {

    fun logEvent(eventName: String, order: Map<String, Any>, sandbox: Boolean) {
        @Suppress("UNCHECKED_CAST")
        val amount = order["amount"] as? Map<String, Any>
        val amountValue = (amount?.get("value") as? Number)?.toDouble()?.div(100) ?: 0.0
        val currencyCode = amount?.get("currencyCode") as? String ?: ""
        val merchantOrderRef = order["merchantOrderReference"] as? String ?: ""
        val reference = order["reference"] as? String ?: ""
        val createDateTime = order["createDateTime"] as? String ?: ""
        val orderType = order["type"] as? String ?: ""
        val orderAction = order["action"] as? String ?: ""

        val db = if (sandbox) "MinuteView_Dev" else "MinuteView"
        jdbc.update(
            "EXEC $db.mi.NGeniusEventLogInsert ?, ?, ?, ?, ?, ?, ?, ?, ?",
            eventName, orderType, orderAction, merchantOrderRef,
            currencyCode, amountValue, reference, createDateTime,
            com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(order),
        )
    }

    fun reconcilePayment(eventName: String, order: Map<String, Any>) {
        @Suppress("UNCHECKED_CAST")
        val amount = order["amount"] as? Map<String, Any>
        val amountValue = (amount?.get("value") as? Number)?.toDouble()?.div(100) ?: 0.0
        val merchantOrderRef = order["merchantOrderReference"] as? String ?: ""

        @Suppress("UNCHECKED_CAST")
        val embedded = order["_embedded"] as? Map<String, Any>
        @Suppress("UNCHECKED_CAST")
        val payments = embedded?.get("payment") as? List<Map<String, Any>>
        @Suppress("UNCHECKED_CAST")
        val authResponse = payments?.firstOrNull()?.get("authResponse") as? Map<String, Any>
        val rrn = authResponse?.get("rrn") as? String ?: ""

        jdbc.update(
            "EXEC MinuteView.mi.NGeniusEventPaymentReconciliation ?, ?, ?, ?",
            eventName, merchantOrderRef, amountValue, rrn,
        )
    }
}
