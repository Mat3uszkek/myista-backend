package com.ista.myista.payment

import com.ista.myista.payment.aps.APSGateway
import com.ista.myista.payment.stripe.StripeGateway
import com.ista.myista.payment.ngenius.NGeniusGateway
import com.ista.myista.payment.cbq.CBQGateway
import com.ista.myista.payment.worldpay.WorldPayGateway
import org.springframework.stereotype.Component

@Component
class PaymentGatewayRegistry(
    private val stripe: StripeGateway,
    private val ngenius: NGeniusGateway,
    private val cbq: CBQGateway,
    private val worldPay: WorldPayGateway,
    private val aps: APSGateway,
) {
    fun forVariant(variant: String): PaymentGateway? = when (variant) {
        "uk", "thameswey", "prepayment" -> stripe
        "uae", "qatar" -> ngenius
        "be" -> null
        else -> null
    }

    fun guestForVariant(variant: String): GuestPaymentGateway? = when (variant) {
        "uae" -> ngenius
        "qatar" -> cbq
        else -> null
    }
}
