package com.ista.myista.api

import com.ista.myista.variant.VariantService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class ConfigController(private val variantService: VariantService) {

    @GetMapping("/config")
    fun getConfig(request: HttpServletRequest): Map<String, String> =
        mapOf("variant" to variantService.detect(request))
}
