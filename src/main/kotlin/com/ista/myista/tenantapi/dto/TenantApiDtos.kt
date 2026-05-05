package com.ista.myista.tenantapi.dto

import com.fasterxml.jackson.annotation.JsonAlias
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
    @JsonAlias("AccountNo") val accountNo: String? = null,
    @JsonAlias("Address") val address: String? = null,
    @JsonAlias("PropertyType") val propertyType: String? = null,
    @JsonAlias("Active") val active: Boolean? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class BillingAgentDetails(
    @JsonAlias("ContactId") val contactId: Int? = null,
    @JsonAlias("AttentionOf") val attentionOf: String? = null,
    @JsonAlias("Name") val name: String? = null,
    @JsonAlias("Phone") val phone: String? = null,
    @JsonAlias("Email") val email: String? = null,
    @JsonAlias("Address") val address: String? = null,
    @JsonAlias("Web") val web: String? = null,
)

data class AccountDetails(
    val profile: UserInfo,
    val propertyAddress: String?,
    val billingAgent: BillingAgentDetails?,
)

// TenantAPI returns PascalCase — @JsonAlias maps deserialization, camelCase field name
// controls serialization back to the frontend.
@JsonIgnoreProperties(ignoreUnknown = true)
data class Balance(
    @JsonAlias("BalanceDue") val balanceDue: Double? = null,
    @JsonAlias("LastInvoiceAmount") val lastInvoiceAmount: Double? = null,
    @JsonAlias("LastInvoiceStatus") val lastInvoiceStatus: String? = null,
    @JsonAlias("LastInvoiceDate") val lastInvoiceDate: String? = null,
    @JsonAlias("LastInvoicePeriodFrom") val lastInvoicePeriodFrom: String? = null,
    @JsonAlias("LastInvoicePeriodTo") val lastInvoicePeriodTo: String? = null,
    @JsonAlias("PaymentMethod") val paymentMethod: String? = null,
    @JsonAlias("InvoiceTerms") val invoiceTerms: Int? = null,
    // Prepayment / real-time billing only
    @JsonAlias("EmergencyCredit") val emergencyCredit: Double? = null,
    @JsonAlias("AlarmCredit") val alarmCredit: Double? = null,
    @JsonAlias("SupplyStatus") val supplyStatus: Boolean? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Transaction(
    @JsonAlias("Type") val type: String? = null,
    @JsonAlias("Datetime") val datetime: String? = null,
    @JsonAlias("Value") val value: Double? = null,
    @JsonAlias("SubType") val subType: String? = null,
    @JsonAlias("DocumentId") val documentId: String? = null,
    @JsonAlias("Code") val code: String? = null,
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
