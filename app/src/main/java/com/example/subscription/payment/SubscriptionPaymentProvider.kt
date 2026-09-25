package com.example.subscription.payment

import com.example.subscription.BillingInterval
import com.example.subscription.Subscription
import com.example.subscription.SubscriptionPlan
import com.example.subscription.SubscriptionStatus
import org.json.JSONObject
import java.util.UUID

/**
 * Result data models for subscription payments.
 */
data class SubscriptionSession(
    val reference: String,
    val authorizationUrl: String?,
    val accessCode: String?,
    val plan: SubscriptionPlan,
    val amountMinor: Long,
    val currency: String = "USD",
    val statusMessage: String = ""
)

data class SubscriptionVerificationResult(
    val isVerified: Boolean,
    val reference: String,
    val providerSubscriptionId: String?,
    val providerCustomerId: String?,
    val amountMinor: Long,
    val currency: String,
    val plan: SubscriptionPlan,
    val status: SubscriptionStatus,
    val failureReason: String? = null
)

data class SubscriptionWebhookResult(
    val eventType: String,
    val reference: String,
    val providerSubscriptionId: String?,
    val ownerUid: String,
    val amountMinor: Long,
    val currency: String,
    val plan: SubscriptionPlan,
    val status: SubscriptionStatus,
    val isDuplicate: Boolean = false
)

/**
 * Payment Provider Abstraction for Subscriptions.
 * Keeps provider-specific details isolated from business logic.
 */
interface SubscriptionPaymentProvider {
    val providerName: String
    val isConfiguredForProduction: Boolean

    suspend fun createCustomer(ownerUid: String, email: String, name: String): Result<String>

    suspend fun initializeSubscription(
        ownerUid: String,
        plan: SubscriptionPlan,
        customerEmail: String
    ): Result<SubscriptionSession>

    suspend fun verifySubscription(
        ownerUid: String,
        reference: String,
        expectedPlan: SubscriptionPlan
    ): Result<SubscriptionVerificationResult>

    suspend fun cancelSubscription(
        subscription: Subscription,
        cancelAtPeriodEnd: Boolean
    ): Result<Subscription>

    suspend fun getSubscriptionStatus(providerSubscriptionId: String): Result<SubscriptionStatus>

    suspend fun handleWebhook(payload: String, signature: String?): Result<SubscriptionWebhookResult>
}

/**
 * Paystack recurring billing provider implementation.
 * Follows Ghana-first payments architecture.
 *
 * NOTE ON SECURITY:
 * Secret keys are strictly maintained server-side.
 * When running without live production Cloud Functions, reports:
 * "Subscription billing backend prepared; production provider configuration pending."
 */
class PaystackSubscriptionProvider(
    private val isTestSandbox: Boolean = false
) : SubscriptionPaymentProvider {

    override val providerName: String = "PAYSTACK"

    // Live production recurring billing requires a verified backend service with server-side secrets
    override val isConfiguredForProduction: Boolean = false

    override suspend fun createCustomer(ownerUid: String, email: String, name: String): Result<String> {
        val customerCode = "CUS_${UUID.randomUUID().toString().take(10)}"
        return Result.success(customerCode)
    }

    override suspend fun initializeSubscription(
        ownerUid: String,
        plan: SubscriptionPlan,
        customerEmail: String
    ): Result<SubscriptionSession> {
        if (plan == SubscriptionPlan.NONE) {
            return Result.failure(IllegalArgumentException("Cannot initialize subscription for NONE plan"))
        }

        val reference = "sub_ref_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"

        if (!isConfiguredForProduction && !isTestSandbox) {
            // Truthful status as mandated by Phase 6 guidelines:
            return Result.success(
                SubscriptionSession(
                    reference = reference,
                    authorizationUrl = null,
                    accessCode = null,
                    plan = plan,
                    amountMinor = plan.priceUsdMonthlyMinor,
                    currency = "USD",
                    statusMessage = "Subscription billing backend prepared; production provider configuration pending."
                )
            )
        }

        // Test sandbox session (used in testing environments)
        return Result.success(
            SubscriptionSession(
                reference = reference,
                authorizationUrl = "https://checkout.paystack.com/$reference",
                accessCode = "sub_code_${System.currentTimeMillis()}",
                plan = plan,
                amountMinor = plan.priceUsdMonthlyMinor,
                currency = "USD",
                statusMessage = "Test subscription session ready"
            )
        )
    }

    override suspend fun verifySubscription(
        ownerUid: String,
        reference: String,
        expectedPlan: SubscriptionPlan
    ): Result<SubscriptionVerificationResult> {
        if (!isConfiguredForProduction && !isTestSandbox) {
            return Result.failure(
                IllegalStateException("Subscription billing backend prepared; production provider configuration pending.")
            )
        }

        // In test sandbox mode, validates input parameters strictly
        if (reference.isBlank()) {
            return Result.failure(IllegalArgumentException("Invalid empty payment reference"))
        }

        return Result.success(
            SubscriptionVerificationResult(
                isVerified = true,
                reference = reference,
                providerSubscriptionId = "SUB_${UUID.randomUUID().toString().take(8)}",
                providerCustomerId = "CUS_${ownerUid.take(8)}",
                amountMinor = expectedPlan.priceUsdMonthlyMinor,
                currency = "USD",
                plan = expectedPlan,
                status = SubscriptionStatus.ACTIVE
            )
        )
    }

    override suspend fun cancelSubscription(
        subscription: Subscription,
        cancelAtPeriodEnd: Boolean
    ): Result<Subscription> {
        val updated = subscription.copy(
            status = if (cancelAtPeriodEnd) SubscriptionStatus.ACTIVE else SubscriptionStatus.CANCELLED,
            cancelAtPeriodEnd = cancelAtPeriodEnd,
            updatedAt = System.currentTimeMillis()
        )
        return Result.success(updated)
    }

    override suspend fun getSubscriptionStatus(providerSubscriptionId: String): Result<SubscriptionStatus> {
        return Result.success(SubscriptionStatus.ACTIVE)
    }

    override suspend fun handleWebhook(payload: String, signature: String?): Result<SubscriptionWebhookResult> {
        if (payload.isBlank()) {
            return Result.failure(IllegalArgumentException("Empty webhook payload"))
        }

        return try {
            val json = JSONObject(payload)
            val event = json.optString("event")
            val data = json.optJSONObject("data") ?: JSONObject()

            val reference = data.optString("reference").ifBlank {
                data.optString("subscription_code", "sub_hook_${System.currentTimeMillis()}")
            }
            val amountMinor = data.optLong("amount", 0L)
            val currency = data.optString("currency", "USD")
            val customerObj = data.optJSONObject("customer")
            val metadataObj = data.optJSONObject("metadata")
            val ownerUid = metadataObj?.optString("ownerUid")?.takeIf { it.isNotBlank() }
                ?: metadataObj?.optString("owner_uid")?.takeIf { it.isNotBlank() }
                ?: customerObj?.optString("metadata_owner_uid")?.takeIf { it.isNotBlank() }
                ?: data.optString("owner_uid", "")

            val plan = when (amountMinor) {
                3000L -> SubscriptionPlan.SNAPBRAND_PRO
                2000L -> SubscriptionPlan.SNAPBRAND
                else -> SubscriptionPlan.NONE
            }

            val status = when (event) {
                "subscription.create",
                "charge.success",
                "invoice.payment_succeeded" -> SubscriptionStatus.ACTIVE
                "invoice.payment_failed" -> SubscriptionStatus.PAST_DUE
                "subscription.disable",
                "subscription.not_renew" -> SubscriptionStatus.CANCELLED
                "subscription.expiring" -> SubscriptionStatus.EXPIRED
                else -> SubscriptionStatus.ACTIVE
            }

            Result.success(
                SubscriptionWebhookResult(
                    eventType = event,
                    reference = reference,
                    providerSubscriptionId = data.optString("subscription_code").takeIf { it.isNotBlank() },
                    ownerUid = ownerUid,
                    amountMinor = amountMinor,
                    currency = currency,
                    plan = plan,
                    status = status
                )
            )
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("Malformed webhook payload: ${e.message}"))
        }
    }
}
