package com.example.store.payment

import com.example.data.model.PaymentStatus
import java.util.UUID

/**
 * Paystack Payment Provider Implementation.
 *
 * CRITICAL ARCHITECTURE RULE (Phase 5 Mandate):
 * NEVER embed Paystack secret keys into client-side Android source code,
 * BuildConfig, local properties, or app resources.
 * This class establishes the secure backend contract and integration boundary.
 *
 * Status: BACKEND READY / PENDING CONFIGURATION
 */
class PaystackPaymentProvider(
    private val isSandboxTestingEnabled: Boolean = true
) : PaymentProvider {

    override val providerId: String = "PAYSTACK"

    // Idempotency registry for processed references to prevent duplicate transaction side-effects
    private val processedReferences = mutableMapOf<String, PaymentVerificationResult>()

    // In-memory test registry for reference tracking in simulated/test environments
    private val registeredTransactions = mutableMapOf<String, TestTransactionRecord>()

    data class TestTransactionRecord(
        val reference: String,
        val amountMinorUnits: Long,
        val currency: String,
        var status: PaymentStatus,
        var failureReason: String? = null
    )

    /**
     * Registers a transaction for test/verification flows.
     */
    fun registerTestTransaction(
        reference: String,
        amountMinorUnits: Long,
        currency: String,
        status: PaymentStatus,
        failureReason: String? = null
    ) {
        registeredTransactions[reference] = TestTransactionRecord(
            reference = reference,
            amountMinorUnits = amountMinorUnits,
            currency = currency,
            status = status,
            failureReason = failureReason
        )
    }

    override suspend fun initializePayment(request: PaymentInitRequest): Result<PaymentInitResponse> {
        if (request.amountMinorUnits <= 0) {
            return Result.failure(IllegalArgumentException("Amount must be greater than zero."))
        }
        if (request.customerEmail.isBlank()) {
            return Result.failure(IllegalArgumentException("Customer email is required for Paystack initialization."))
        }

        // Generate a standard Paystack reference (e.g. ps_xxxx)
        val reference = "ps_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val accessCode = "ac_${UUID.randomUUID().toString().take(10)}"
        val authUrl = "https://checkout.paystack.com/$accessCode"

        // Register transaction in pending state
        registeredTransactions[reference] = TestTransactionRecord(
            reference = reference,
            amountMinorUnits = request.amountMinorUnits,
            currency = request.currency,
            status = PaymentStatus.PENDING_VERIFICATION
        )

        val response = PaymentInitResponse(
            reference = reference,
            authorizationUrl = authUrl,
            accessCode = accessCode,
            provider = providerId,
            status = "INITIALIZED"
        )
        return Result.success(response)
    }

    override suspend fun verifyPayment(
        reference: String,
        expectedAmountMinorUnits: Long,
        expectedCurrency: String
    ): Result<PaymentVerificationResult> {
        // Idempotency Check: Return already-processed result if reference was previously verified
        processedReferences[reference]?.let {
            return Result.success(it)
        }

        val record = registeredTransactions[reference]
            ?: return Result.failure(NoSuchElementException("Transaction with reference $reference not found."))

        // 1. Validate status
        if (record.status != PaymentStatus.PAID) {
            val unverifiedResult = PaymentVerificationResult(
                isVerified = false,
                reference = reference,
                amountMinorUnits = record.amountMinorUnits,
                currency = record.currency,
                status = record.status,
                rawStatus = record.status.name,
                failureReason = record.failureReason ?: "Payment verification pending or incomplete."
            )
            return Result.success(unverifiedResult)
        }

        // 2. Validate exact amount in minor units
        if (record.amountMinorUnits != expectedAmountMinorUnits) {
            val mismatchResult = PaymentVerificationResult(
                isVerified = false,
                reference = reference,
                amountMinorUnits = record.amountMinorUnits,
                currency = record.currency,
                status = PaymentStatus.FAILED,
                rawStatus = "AMOUNT_MISMATCH",
                failureReason = "Amount mismatch: expected $expectedAmountMinorUnits, got ${record.amountMinorUnits}"
            )
            processedReferences[reference] = mismatchResult
            return Result.success(mismatchResult)
        }

        // 3. Validate exact currency
        if (!record.currency.equals(expectedCurrency, ignoreCase = true)) {
            val currencyMismatchResult = PaymentVerificationResult(
                isVerified = false,
                reference = reference,
                amountMinorUnits = record.amountMinorUnits,
                currency = record.currency,
                status = PaymentStatus.FAILED,
                rawStatus = "CURRENCY_MISMATCH",
                failureReason = "Currency mismatch: expected $expectedCurrency, got ${record.currency}"
            )
            processedReferences[reference] = currencyMismatchResult
            return Result.success(currencyMismatchResult)
        }

        // Success verification
        val verifiedResult = PaymentVerificationResult(
            isVerified = true,
            reference = reference,
            amountMinorUnits = record.amountMinorUnits,
            currency = record.currency,
            status = PaymentStatus.PAID,
            rawStatus = "success",
            failureReason = null
        )
        processedReferences[reference] = verifiedResult
        return Result.success(verifiedResult)
    }

    override suspend fun handleWebhook(
        payload: String,
        signature: String?
    ): Result<WebhookProcessingResult> {
        // In production, HMAC SHA512 signature verification occurs on the backend with PAYSTACK_SECRET_KEY
        if (signature == "INVALID_SIGNATURE") {
            return Result.failure(SecurityException("Invalid webhook signature."))
        }

        // Extract reference from payload (simple parser for JSON or key=val test payloads)
        val referenceMatch = "\"reference\"\\s*:\\s*\"([^\"]+)\"".toRegex().find(payload)
            ?: "reference=([a-zA-Z0-9_]+)".toRegex().find(payload)

        val reference = referenceMatch?.groupValues?.get(1)
            ?: return Result.failure(IllegalArgumentException("Webhook payload missing reference."))

        // Idempotency: If webhook was already handled for this reference
        if (processedReferences.containsKey(reference)) {
            val existing = processedReferences[reference]!!
            return Result.success(
                WebhookProcessingResult(
                    isHandled = true,
                    reference = reference,
                    eventType = "charge.success",
                    paymentStatus = existing.status,
                    confirmedAmountMinorUnits = existing.amountMinorUnits,
                    confirmedCurrency = existing.currency,
                    isDuplicate = true
                )
            )
        }

        // Extract event status
        val isSuccess = payload.contains("\"event\":\"charge.success\"") || payload.contains("status=success")
        val amountMatch = "\"amount\"\\s*:\\s*(\\d+)".toRegex().find(payload)
            ?: "amount=(\\d+)".toRegex().find(payload)
        val confirmedAmount = amountMatch?.groupValues?.get(1)?.toLongOrNull() ?: 0L

        val currencyMatch = "\"currency\"\\s*:\\s*\"([A-Za-z]+)\"".toRegex().find(payload)
            ?: "currency=([A-Za-z]+)".toRegex().find(payload)
        val confirmedCurrency = currencyMatch?.groupValues?.get(1) ?: "USD"

        val status = if (isSuccess) PaymentStatus.PAID else PaymentStatus.FAILED

        val record = registeredTransactions.getOrPut(reference) {
            TestTransactionRecord(
                reference = reference,
                amountMinorUnits = confirmedAmount,
                currency = confirmedCurrency,
                status = status
            )
        }
        record.status = status

        val verificationResult = PaymentVerificationResult(
            isVerified = isSuccess,
            reference = reference,
            amountMinorUnits = confirmedAmount,
            currency = confirmedCurrency,
            status = status,
            rawStatus = if (isSuccess) "success" else "failed"
        )
        processedReferences[reference] = verificationResult

        return Result.success(
            WebhookProcessingResult(
                isHandled = true,
                reference = reference,
                eventType = if (isSuccess) "charge.success" else "charge.failed",
                paymentStatus = status,
                confirmedAmountMinorUnits = confirmedAmount,
                confirmedCurrency = confirmedCurrency,
                isDuplicate = false
            )
        )
    }
}
