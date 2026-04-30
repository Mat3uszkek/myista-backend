package com.ista.myista.tenantapi

import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestClient

data class TenantTokens(
    @JsonProperty("access_token") val accessToken: String,
    @JsonProperty("refresh_token") val refreshToken: String,
)

@Component
class TenantApiClient(
    @Value("\${tenantapi.host}") host: String,
    @Value("\${tenantapi.app-id}") private val appId: String,
    @Value("\${tenantapi.kiosk-username:}") private val kioskUsername: String,
    @Value("\${tenantapi.kiosk-password:}") private val kioskPassword: String,
) {
    private val client = RestClient.builder().baseUrl(host).build()

    fun authenticate(username: String, password: String): TenantTokens =
        postTokenForm(
            "grant_type" to "password",
            "username" to username,
            "password" to password,
        )

    fun refresh(refreshToken: String): TenantTokens =
        postTokenForm(
            "grant_type" to "refresh_token",
            "refresh_token" to refreshToken,
        )

    fun authenticateKiosk(): TenantTokens =
        postTokenForm(
            "grant_type" to "password",
            "username" to kioskUsername,
            "password" to kioskPassword,
        )

    fun <T : Any> get(path: String, accessToken: String, type: Class<T>): T =
        client.get().uri(path)
            .header("Authorization", "Bearer $accessToken")
            .retrieve()
            .body(type)!!

    fun <T : Any> get(path: String, accessToken: String, type: Class<T>, params: Map<String, Any>): T {
        val uri = if (params.isEmpty()) path else {
            val query = params.entries.joinToString("&") { "${it.key}=${it.value}" }
            "$path?$query"
        }
        return client.get().uri(uri)
            .header("Authorization", "Bearer $accessToken")
            .retrieve()
            .body(type)!!
    }

    fun <T : Any, B : Any> postJson(path: String, accessToken: String, body: B, type: Class<T>): T =
        client.post().uri(path)
            .header("Authorization", "Bearer $accessToken")
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .body(type)!!

    fun <T : Any> delete(path: String, accessToken: String, type: Class<T>): T =
        client.delete().uri(path)
            .header("Authorization", "Bearer $accessToken")
            .retrieve()
            .body(type)!!

    // Public (no auth) — used for registration flow. Adds AppId to body per TenantAPI convention.
    fun postJsonNoAuth(path: String, body: Map<String, Any?>): Any {
        val bodyWithAppId = body.toMutableMap().apply { put("AppId", appId) }
        return client.post().uri(path)
            .contentType(MediaType.APPLICATION_JSON)
            .body(bodyWithAppId)
            .retrieve()
            .body(Any::class.java)!!
    }

    private fun postTokenForm(vararg pairs: Pair<String, String>): TenantTokens {
        val form: MultiValueMap<String, String> = LinkedMultiValueMap<String, String>().apply {
            pairs.forEach { (k, v) -> add(k, v) }
            add("appId", appId)
        }
        return client.post().uri("/token")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(form)
            .retrieve()
            .body(TenantTokens::class.java)!!
    }
}
