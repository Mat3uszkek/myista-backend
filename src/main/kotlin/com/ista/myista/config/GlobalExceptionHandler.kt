package com.ista.myista.config

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.server.ResponseStatusException

data class ApiError(val status: Int, val message: String)

@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    // Explicit HTTP errors thrown by controllers (e.g. 400 Missing amount)
    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatus(ex: ResponseStatusException): ResponseEntity<ApiError> =
        ResponseEntity
            .status(ex.statusCode)
            .body(ApiError(ex.statusCode.value(), ex.reason ?: ex.message))

    // TenantAPI returned a 4xx — surface as-is to the frontend
    @ExceptionHandler(HttpClientErrorException::class)
    fun handleTenantApi4xx(ex: HttpClientErrorException): ResponseEntity<ApiError> {
        log.warn("TenantAPI client error: {} {}", ex.statusCode, ex.message)
        return ResponseEntity
            .status(ex.statusCode)
            .body(ApiError(ex.statusCode.value(), ex.message ?: "Upstream error"))
    }

    // TenantAPI returned a 5xx — hide details, log for ops
    @ExceptionHandler(HttpServerErrorException::class)
    fun handleTenantApi5xx(ex: HttpServerErrorException): ResponseEntity<ApiError> {
        log.error("TenantAPI server error: {} {}", ex.statusCode, ex.message)
        return ResponseEntity
            .status(HttpStatus.BAD_GATEWAY)
            .body(ApiError(HttpStatus.BAD_GATEWAY.value(), "Service temporarily unavailable"))
    }

    // Anything else — log full stack trace, never expose it
    @ExceptionHandler(Exception::class)
    fun handleUnexpected(ex: Exception): ResponseEntity<ApiError> {
        log.error("Unexpected error", ex)
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiError(500, "An unexpected error occurred"))
    }
}
