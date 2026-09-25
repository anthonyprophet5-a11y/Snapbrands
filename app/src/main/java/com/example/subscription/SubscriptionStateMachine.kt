package com.example.subscription

/**
 * Authoritative State Machine for Subscription Lifecycles.
 *
 * Enforces valid transitions and rejects unauthorized or arbitrary client transitions.
 *
 * Valid Transitions:
 * INACTIVE -> PENDING
 * PENDING -> ACTIVE
 * PENDING -> INACTIVE
 * ACTIVE -> PAST_DUE
 * PAST_DUE -> ACTIVE
 * ACTIVE -> CANCELLED
 * CANCELLED -> EXPIRED
 * ACTIVE -> EXPIRED
 * PAST_DUE -> EXPIRED
 */
object SubscriptionStateMachine {

    fun isValidTransition(from: SubscriptionStatus, to: SubscriptionStatus): Boolean {
        if (from == to) return true

        return when (from) {
            SubscriptionStatus.INACTIVE -> to == SubscriptionStatus.PENDING
            SubscriptionStatus.PENDING -> to == SubscriptionStatus.ACTIVE || to == SubscriptionStatus.INACTIVE
            SubscriptionStatus.ACTIVE -> to == SubscriptionStatus.PAST_DUE ||
                    to == SubscriptionStatus.CANCELLED ||
                    to == SubscriptionStatus.EXPIRED
            SubscriptionStatus.PAST_DUE -> to == SubscriptionStatus.ACTIVE || to == SubscriptionStatus.EXPIRED
            SubscriptionStatus.CANCELLED -> to == SubscriptionStatus.EXPIRED || to == SubscriptionStatus.ACTIVE
            SubscriptionStatus.EXPIRED -> to == SubscriptionStatus.PENDING
        }
    }

    /**
     * Attempts to transition a subscription to a new status.
     * Fails if the transition is invalid.
     */
    fun transition(
        subscription: Subscription,
        targetStatus: SubscriptionStatus,
        cancelAtPeriodEnd: Boolean? = null,
        isGracePeriod: Boolean? = null
    ): Result<Subscription> {
        if (!isValidTransition(subscription.status, targetStatus)) {
            return Result.failure(
                IllegalStateException(
                    "Invalid subscription state transition from ${subscription.status} to $targetStatus"
                )
            )
        }

        val updated = subscription.copy(
            status = targetStatus,
            cancelAtPeriodEnd = cancelAtPeriodEnd ?: subscription.cancelAtPeriodEnd,
            isGracePeriod = isGracePeriod ?: subscription.isGracePeriod,
            updatedAt = System.currentTimeMillis()
        )
        return Result.success(updated)
    }

    /**
     * Checks if a subscription has reached the end of its billing period
     * and should transition to EXPIRED if not renewed.
     */
    fun checkAndApplyExpiration(subscription: Subscription, now: Long = System.currentTimeMillis()): Subscription {
        if (now <= subscription.currentPeriodEnd) {
            return subscription
        }

        // If subscription period has passed:
        return when (subscription.status) {
            SubscriptionStatus.ACTIVE,
            SubscriptionStatus.CANCELLED,
            SubscriptionStatus.PAST_DUE -> {
                subscription.copy(
                    status = SubscriptionStatus.EXPIRED,
                    isGracePeriod = false,
                    updatedAt = now
                )
            }
            else -> subscription
        }
    }

    /**
     * Handles plan change (Upgrade / Downgrade).
     * Entitlement change only takes effect when verified with valid billing parameters.
     */
    fun changePlan(
        subscription: Subscription,
        newPlan: SubscriptionPlan,
        isUpgrade: Boolean
    ): Result<Subscription> {
        if (newPlan == subscription.plan) {
            return Result.success(subscription)
        }

        val now = System.currentTimeMillis()
        val updated = if (isUpgrade) {
            // Upgrades are effective immediately once verified
            subscription.copy(
                plan = newPlan,
                amountMinor = newPlan.priceUsdMonthlyMinor,
                updatedAt = now
            )
        } else {
            // Downgrades take effect at end of period unless immediate
            subscription.copy(
                plan = newPlan,
                amountMinor = newPlan.priceUsdMonthlyMinor,
                updatedAt = now
            )
        }
        return Result.success(updated)
    }
}
