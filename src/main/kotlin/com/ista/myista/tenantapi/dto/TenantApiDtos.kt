package com.ista.myista.tenantapi.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class UserInfo(
    val userId: String? = null,
    val custId: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val email: String? = null,
    val accountNo: String? = null,
    val stripeCustomerId: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Account(
    val accountNo: String? = null,
    val address: String? = null,
    val propertyType: String? = null,
    val active: Boolean? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Balance(
    val balance: Double? = null,
    val currency: String? = null,
    val outletRef: String? = null,
    val accountNo: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Transaction(
    val transactionDate: String? = null,
    val amount: Double? = null,
    val description: String? = null,
    val reference: String? = null,
    val type: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class BillingAgent(
    val name: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val address: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Contact(
    val email: String? = null,
    val phone: String? = null,
    val mobilePhone: String? = null,
    val address1: String? = null,
    val address2: String? = null,
    val city: String? = null,
    val postCode: String? = null,
    val country: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Supplier(
    val name: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val address: String? = null,
    val website: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class MeterInfo(
    val meterId: String? = null,
    val meterSerial: String? = null,
    val fuel: String? = null,
    val units: String? = null,
    val status: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class MeterRead(
    val readDate: String? = null,
    val reading: Double? = null,
    val units: String? = null,
    val type: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class MeterReadsResponse(
    val meterId: String? = null,
    val reads: List<MeterRead>? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Tariff(
    val fuel: String? = null,
    val unitRate: Double? = null,
    val standingCharge: Double? = null,
    val currency: String? = null,
    val effectiveDate: String? = null,
    val units: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Document(
    val documentId: String? = null,
    val name: String? = null,
    val type: String? = null,
    val date: String? = null,
    val url: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class KioskAccount(
    val custId: String? = null,
    val accountNo: String? = null,
    val name: String? = null,
    val balance: Double? = null,
    val currency: String? = null,
    val outletRef: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PaymentResult(
    val paymentId: String? = null,
    val status: String? = null,
)

// Wrapper matching TenantAPI envelope: { StatusCode, Data }
@JsonIgnoreProperties(ignoreUnknown = true)
data class TenantApiResponse<T>(
    val statusCode: Int? = null,
    val data: T? = null,
    val message: String? = null,
)
