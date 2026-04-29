package com.ista.myista.auth.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class RefreshRequest(
    @JsonProperty("refresh_token") val refreshToken: String,
)
