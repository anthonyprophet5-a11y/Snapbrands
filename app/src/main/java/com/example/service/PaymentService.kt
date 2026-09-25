package com.example.service

/**
 * Service Abstraction for Payments & Subscriptions (Paystack / Stripe Server-Side).
 *
 * CRITICAL SECURITY PRINCIPLE:
 * Client devices NEVER execute payment authorizations directly, and NEVER trust client-side
 * success flags. All transactions must be initialized via Cloud Functions, handled by
 * webhook signatures, and validated against Paystack before updating order/subscription states in Firestore.
 */
interface PaymentService {
    suspend fun initializeCheckout(orderId: String, amount: Double, currency: String): CheckoutSession
    suspend fun initializeSubscription(planId: String): SubscriptionSession
    suspend fun verifyTransactionOnServer(reference: String): TransactionVerificationResult
    suspend fun requestSellerPayout(sellerUid: String, amount: Double, currency: String): PayoutResult
}

data class CheckoutSession(
    val accessCode: String,
    val reference: String,
    val checkoutUrl: String
)

data class SubscriptionSession(
    val authorizationUrl: String,
    val reference: String,
    val planCode: String
)

data class TransactionVerificationResult(
    val verified: Boolean,
    val reference: String,
    val amountPaid: Double,
    val status: String,
    val failureReason: String? = null
)

data class PayoutResult(
    val transferCode: String,
    val status: String,
    val estimatedArrival: String
)

class SnapBrandPaymentService : PaymentService {
    override suspend fun initializeCheckout(orderId: String, amount: Double, currency: String): CheckoutSession {
        throw UnsupportedOperationException(
            "Direct payment processing is deferred to Phase 2. " +
            "All transactions will route through secure Cloud Functions (initializePayment) via Paystack."
        )
    }

    override suspend fun initializeSubscription(planId: String): SubscriptionSession {
        throw UnsupportedOperationException(
            "Subscription checkout will be handled server-side via Cloud Functions."
        )
    }

    override suspend fun verifyTransactionOnServer(reference: String): TransactionVerificationResult {
        throw UnsupportedOperationException(
            "Transaction verification strictly occurs on Firebase Cloud Functions via signed webhooks."
        )
    }

    override suspend fun requestSellerPayout(sellerUid: String, amount: Double, currency: String): PayoutResult {
        throw UnsupportedOperationException(
            "Seller payouts require automated KYC and Paystack Transfers API integration in Phase 2."
        )
    }
}
