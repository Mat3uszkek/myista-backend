package com.ista.myista.auth

import com.ista.myista.auth.dto.LoginRequest
import com.ista.myista.auth.dto.RefreshRequest
import com.ista.myista.auth.dto.TokenResponse
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
class AuthController(private val authService: AuthService) {

    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): TokenResponse =
        authService.login(request)

    @PostMapping("/refresh")
    fun refresh(@RequestBody request: RefreshRequest): TokenResponse =
        authService.refresh(request.refreshToken)
}
