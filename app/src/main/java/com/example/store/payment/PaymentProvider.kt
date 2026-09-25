package com.example.store.payment

import com.example.data.model.PaymentStatus

data class PaymentInitRequest(
    val orderId: String,
    val orderNumber: String,
    val storeId: String,
    val amountMinorUnits: Long,
    val currency: String,
    val customerEmail: String,
    val customerName: String,
    val callbackUrl: String? = null,
    val metadata: Map<String, String> = emptyMap()
)

data class PaymentInitResponse(
    val reference: String,
    val authorizationUrl: String? = null,
    val accessCode: String? = null,
    val provider: String,
    val status: String
)

data class PaymentVerificationResult(
    val isVerified: Boolean,
    val reference: String,
    val amountMinorUnits: Long,
    val currency: String,
    val status: PaymentStatus,
    val rawStatus: String,
    val failureReason: String? = null
)

data class WebhookProcessingResult(
    val isHandled: Boolean,
    val reference: String,
    val eventType: String,
    val paymentStatus: PaymentStatus,
    val confirmedAmountMinorUnits: Long,
    val confirmedCurrency: String,
    val isDuplicate: Boolean = false,
    val failureReason: String? = null
)

/**
 * Payment Provider Abstraction (Phase 5 Mandate).
 *
 * CRITICAL SECURITY ARCHITECTURE:
 * - Secret credentials MUST remain server-side.
 * - Client devices never execute direct payment authorizations with secret keys.
 * - Isolates provider-specific logic (e.g. Paystack, Mobile Money) behind a clean contract.
 */
interface PaymentProvider {
    val providerId: String

    suspend fun initializePayment(request: PaymentInitRequest): Result<PaymentInitResponse>

    suspend fun verifyPayment(
        reference: String,
        expectedAmountMinorUnits: Long,
        expectedCurrency: String
    ): Result<PaymentVerificationResult>

    suspend fun handleWebhook(
        payload: String,
        signature: String?
    ): Result<WebhookProcessingResult>
}
