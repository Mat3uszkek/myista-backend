package com.ista.myista.payment.ngenius

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.Base64

@RestController
@RequestMapping("/payments/webhook/ngenius")
class NGeniusWebhookController(
    @Value("\${ngenius.webhook.username:}") private val webhookUsername: String,
    @Value("\${ngenius.webhook.password:}") private val webhookPassword: String,
    private val repository: NGeniusWebhookRepository,
) {
    @PostMapping
    fun handleWebhook(
        @RequestHeader("Authorization") authHeader: String?,
        @RequestBody payload: Map<String, Any>,
        @RequestParam(defaultValue = "false") sandbox: Boolean,
    ): ResponseEntity<Map<String, Any>> {
        if (!isAuthorized(authHeader)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(mapOf("code" to 401, "message" to "Unauthorized"))
        }

        val eventName = payload["eventName"] as? String ?: return ResponseEntity.badRequest()
            .body(mapOf("code" to 400, "message" to "Missing eventName"))

        @Suppress("UNCHECKED_CAST")
        val order = payload["order"] as? Map<String, Any> ?: return ResponseEntity.badRequest()
            .body(mapOf("code" to 400, "message" to "Missing order"))

        repository.logEvent(eventName, order, sandbox)
        repository.reconcilePayment(eventName, order)

        return ResponseEntity.ok(mapOf(
            "code" to 200,
            "api" to "myista",
            "message" to "Request successful",
            "data" to payload,
        ))
    }

    private fun isAuthorized(authHeader: String?): Boolean {
        if (authHeader == null || !authHeader.startsWith("Basic ")) return false
        val decoded = String(Base64.getDecoder().decode(authHeader.removePrefix("Basic ")))
        val (user, pass) = decoded.split(":", limit = 2).let {
            if (it.size == 2) it[0] to it[1] else return false
        }
        return user == webhookUsername && pass == webhookPassword
    }
}
