package com.ista.myista.auth.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class TokenResponse(
    @JsonProperty("access_token") val accessToken: String,
    @JsonProperty("refresh_token") val refreshToken: String,
    @JsonProperty("token_type") val tokenType: String = "Bearer",
    @JsonProperty("expires_in") val expiresIn: Long = 3600,
)
