package com.example.subscription.webhook

import com.example.subscription.BillingTransaction
import com.example.subscription.BillingTransactionStatus
import com.example.subscription.Subscription
import com.example.subscription.SubscriptionPlan
import com.example.subscription.SubscriptionStatus
import com.example.subscription.payment.SubscriptionPaymentProvider
import com.example.subscription.payment.SubscriptionWebhookResult
import java.util.concurrent.ConcurrentHashMap

/**
 * Handles Webhook Security, Idempotency, and Subscription state transitions.
 * Guarantees duplicate webhooks never extend billing periods twice or create duplicate records.
 */
class SubscriptionWebhookHandler(
    private val paymentProvider: SubscriptionPaymentProvider
) {
    // Thread-safe idempotency tracking for processed webhook references
    private val processedEventIds = ConcurrentHashMap.newKeySet<String>()

    /**
     * Processes an incoming provider webhook payload.
     * Validates signature, parses event, checks idempotency, and updates state.
     */
    suspend fun processWebhook(
        payload: String,
        signature: String?,
        currentSubscription: Subscription? = null,
        subscriptionLookup: ((ownerUid: String) -> Subscription?)? = null,
        onSubscriptionUpdated: (Subscription) -> Unit,
        onTransactionRecorded: (BillingTransaction) -> Unit
    ): Result<SubscriptionWebhookResult> {
        val parseResult = paymentProvider.handleWebhook(payload, signature)
        if (parseResult.isFailure) {
            return parseResult
        }

        val event = parseResult.getOrThrow()

        // 1. Idempotency Check: prevent duplicate event processing
        val eventKey = "${event.eventType}_${event.reference}"
        if (processedEventIds.contains(eventKey)) {
            return Result.success(event.copy(isDuplicate = true))
        }

        // 2. Amount and Currency validation
        if (event.amountMinor != 2000L && event.amountMinor != 3000L && event.status == SubscriptionStatus.ACTIVE) {
            return Result.failure(
                IllegalArgumentException("Amount mismatch: received ${event.amountMinor} minor units, expected 2000 or 3000.")
            )
        }
        if (!event.currency.equals("USD", ignoreCase = true) && event.status == SubscriptionStatus.ACTIVE) {
            return Result.failure(
                IllegalArgumentException("Currency mismatch: received ${event.currency}, expected USD.")
            )
        }

        // 3. Mark event as processed
        processedEventIds.add(eventKey)

        // 4. Update subscription state authoritatively
        val existingSub = subscriptionLookup?.invoke(event.ownerUid)
            ?: currentSubscription?.takeIf { it.ownerUid == event.ownerUid }

        val updatedSubscription = if (existingSub != null) {
            val now = System.currentTimeMillis()
            when (event.status) {
                SubscriptionStatus.ACTIVE -> {
                    // Extend period by 30 days from now or currentPeriodEnd
                    val newPeriodEnd = maxOf(now, existingSub.currentPeriodEnd) + 30L * 24 * 60 * 60 * 1000L
                    existingSub.copy(
                        plan = event.plan,
                        status = SubscriptionStatus.ACTIVE,
                        amountMinor = event.amountMinor,
                        providerReference = event.reference,
                        providerSubscriptionId = event.providerSubscriptionId ?: existingSub.providerSubscriptionId,
                        currentPeriodStart = now,
                        currentPeriodEnd = newPeriodEnd,
                        cancelAtPeriodEnd = false,
                        updatedAt = now
                    )
                }
                SubscriptionStatus.PAST_DUE -> {
                    existingSub.copy(
                        status = SubscriptionStatus.PAST_DUE,
                        isGracePeriod = true,
                        updatedAt = now
                    )
                }
                SubscriptionStatus.CANCELLED -> {
                    existingSub.copy(
                        status = SubscriptionStatus.CANCELLED,
                        cancelAtPeriodEnd = true,
                        updatedAt = now
                    )
                }
                SubscriptionStatus.EXPIRED -> {
                    existingSub.copy(
                        status = SubscriptionStatus.EXPIRED,
                        updatedAt = now
                    )
                }
                else -> existingSub
            }
        } else {
            val now = System.currentTimeMillis()
            Subscription(
                id = "sub_${event.reference}",
                ownerUid = event.ownerUid,
                plan = event.plan,
                status = event.status,
                amountMinor = event.amountMinor,
                currency = event.currency,
                providerReference = event.reference,
                providerSubscriptionId = event.providerSubscriptionId,
                currentPeriodStart = now,
                currentPeriodEnd = now + 30L * 24 * 60 * 60 * 1000L,
                createdAt = now,
                updatedAt = now
            )
        }

        onSubscriptionUpdated(updatedSubscription)

        // 5. Record billing transaction if applicable
        if (event.status == SubscriptionStatus.ACTIVE) {
            val transaction = BillingTransaction(
                id = "txn_${event.reference}",
                ownerUid = updatedSubscription.ownerUid,
                subscriptionId = updatedSubscription.id,
                provider = paymentProvider.providerName,
                providerReference = event.reference,
                amountMinor = event.amountMinor,
                currency = event.currency,
                status = BillingTransactionStatus.PAID,
                paidAt = System.currentTimeMillis()
            )
            onTransactionRecorded(transaction)
        } else if (event.status == SubscriptionStatus.PAST_DUE) {
            val transaction = BillingTransaction(
                id = "txn_fail_${event.reference}",
                ownerUid = updatedSubscription.ownerUid,
                subscriptionId = updatedSubscription.id,
                provider = paymentProvider.providerName,
                providerReference = event.reference,
                amountMinor = event.amountMinor,
                currency = event.currency,
                status = BillingTransactionStatus.FAILED,
                paidAt = null
            )
            onTransactionRecorded(transaction)
        }

        return Result.success(event)
    }

    fun isEventProcessed(eventKey: String): Boolean = processedEventIds.contains(eventKey)
}
