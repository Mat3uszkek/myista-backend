package com.ista.myista.api

import com.ista.myista.tenantapi.TenantApiService
import com.ista.myista.variant.VariantService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/auth/register")
class RegisterController(
    private val tenantApiService: TenantApiService,
    private val variantService: VariantService,
) {
    // Step 1: verify account exists before allowing registration
    @PostMapping("/verify")
    fun verify(@RequestBody body: Map<String, Any?>, request: HttpServletRequest): Any {
        val variant = variantService.detect(request)
        val tenantBody = buildMap<String, Any?> {
            put("AccountNumber", body["accountNumber"])
            put("SchemeId", variantService.getSchemeId(variant))
            body["lastName"]?.let { put("Surname", it) }
            body["postcode"]?.let { put("Postcode", it) }
        }
        return tenantApiService.verifyAccount(tenantBody)
    }

    // Step 2 (optional): request a registration code via email/SMS
    @PostMapping("/generate-code")
    fun generateCode(@RequestBody body: Map<String, Any?>, request: HttpServletRequest): Any {
        val variant = variantService.detect(request)
        val tenantBody = buildMap<String, Any?> {
            put("AccountNumber", body["accountNumber"])
            put("RegistrationCodeToken", body["registrationCodeToken"])
            put("SchemeId", variantService.getSchemeId(variant))
            put("DeliveryMethodId", body["deliveryMethodId"])
            put("VariantId", variantService.getApiVariantId(variant))
        }
        return tenantApiService.generateRegistrationCode(tenantBody)
    }

    // Step 3: submit personal details + optional registration code
    @PostMapping("/complete")
    fun complete(@RequestBody body: Map<String, Any?>, request: HttpServletRequest): Any {
        val variant = variantService.detect(request)
        val tenantBody = buildMap<String, Any?> {
            put("AccountNumber", body["accountNumber"])
            put("Title", body["title"])
            put("FirstName", body["firstName"])
            put("PhoneNumber", body["phone"])
            put("IgnoreDuplicate", body["ignoreDuplicate"] ?: false)
            put("SchemeId", variantService.getSchemeId(variant))
            put("VariantId", variantService.getApiVariantId(variant))
            body["lastName"]?.let { put("Surname", it) }
            body["postcode"]?.let { put("Postcode", it) }
            body["registrationCode"]?.let { put("RegistrationCode", it) }
        }
        return tenantApiService.registerAccount(tenantBody)
    }

    // Activate: set password from email activation link (token = JWT from TenantAPI email)
    @PostMapping("/set-password")
    fun setPassword(@RequestBody body: Map<String, Any?>, request: HttpServletRequest): Any {
        val variant = variantService.detect(request)
        return tenantApiService.activateAccount(
            mapOf(
                "Code" to body["token"],
                "NewPassword" to body["password"],
                "VariantId" to variantService.getApiVariantId(variant),
            )
        )
    }

    // Forgot password: send reset email
    @PostMapping("/password-reset")
    fun passwordReset(@RequestBody body: Map<String, Any?>, request: HttpServletRequest): Any {
        val variant = variantService.detect(request)
        return tenantApiService.requestPasswordReset(
            mapOf(
                "EmailAddress" to body["email"],
                "VariantId" to variantService.getApiVariantId(variant),
            )
        )
    }
}
