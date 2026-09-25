package com.example.subscription

/**
 * Authoritative Resolver separating PAYMENT/SUBSCRIPTION records
 * from FEATURE ENTITLEMENTS.
 *
 * SNAPBRAND_PRO -> hasCoreAccess = true, hasProAccess = true
 * SNAPBRAND     -> hasCoreAccess = true, hasProAccess = false
 * NONE / EXPIRED -> hasCoreAccess = false, hasProAccess = false
 */
object SubscriptionEntitlementResolver {

    /**
     * Determines whether the creator has active Core SnapBrand platform access.
     * Core includes: Snap Engine, Brand Genius, Product Engine, Store Engine,
     * publishing, checkout, and order management.
     */
    fun hasCoreAccess(subscription: Subscription?): Boolean {
        if (subscription == null) return false
        if (!isEntitlementActive(subscription)) return false
        return subscription.plan == SubscriptionPlan.SNAPBRAND ||
                subscription.plan == SubscriptionPlan.SNAPBRAND_PRO
    }

    /**
     * Determines whether the creator has active Pro access.
     * Pro includes all core features + future AI Marketing tools.
     */
    fun hasProAccess(subscription: Subscription?): Boolean {
        if (subscription == null) return false
        if (!isEntitlementActive(subscription)) return false
        return subscription.plan == SubscriptionPlan.SNAPBRAND_PRO
    }

    /**
     * Checks permission for a specific feature gate.
     */
    fun canUseFeature(subscription: Subscription?, feature: FeatureEntitlement): Boolean {
        return if (feature.isProOnly) {
            hasProAccess(subscription)
        } else {
            hasCoreAccess(subscription)
        }
    }

    /**
     * Evaluates if a subscription currently grants entitled access.
     * Handles active periods, cancellation with remaining time, and grace periods.
     */
    fun isEntitlementActive(subscription: Subscription): Boolean {
        val now = System.currentTimeMillis()

        return when (subscription.status) {
            SubscriptionStatus.ACTIVE -> {
                now <= subscription.currentPeriodEnd
            }
            SubscriptionStatus.CANCELLED -> {
                // Cancelled creators retain entitled access until the current paid period ends
                subscription.cancelAtPeriodEnd && now <= subscription.currentPeriodEnd
            }
            SubscriptionStatus.PAST_DUE -> {
                // If in grace period, access is temporarily maintained
                subscription.isGracePeriod && now <= subscription.currentPeriodEnd
            }
            SubscriptionStatus.PENDING,
            SubscriptionStatus.INACTIVE,
            SubscriptionStatus.EXPIRED -> false
        }
    }

    /**
     * Generates a safe projection of entitlements for UI consumption.
     */
    fun resolveProjection(ownerUid: String, subscription: Subscription?): UserEntitlementProjection {
        if (subscription == null || subscription.plan == SubscriptionPlan.NONE) {
            return UserEntitlementProjection(
                ownerUid = ownerUid,
                plan = SubscriptionPlan.NONE,
                status = SubscriptionStatus.INACTIVE,
                hasCoreAccess = false,
                hasProAccess = false,
                currentPeriodEnd = 0L,
                cancelAtPeriodEnd = false,
                isPastDue = false,
                statusMessage = "No active subscription"
            )
        }

        val hasCore = hasCoreAccess(subscription)
        val hasPro = hasProAccess(subscription)
        val isPastDue = subscription.status == SubscriptionStatus.PAST_DUE

        val statusMessage = when {
            subscription.status == SubscriptionStatus.EXPIRED -> "Subscription expired"
            isPastDue -> "Payment past due — please update payment method"
            subscription.status == SubscriptionStatus.CANCELLED && subscription.cancelAtPeriodEnd ->
                "Subscription cancelled — access active until period end"
            subscription.status == SubscriptionStatus.ACTIVE ->
                "Active ${subscription.plan.displayName} subscription"
            subscription.status == SubscriptionStatus.PENDING ->
                "Subscription payment pending verification"
            else -> "Subscription inactive"
        }

        return UserEntitlementProjection(
            ownerUid = ownerUid,
            plan = subscription.plan,
            status = subscription.status,
            hasCoreAccess = hasCore,
            hasProAccess = hasPro,
            currentPeriodEnd = subscription.currentPeriodEnd,
            cancelAtPeriodEnd = subscription.cancelAtPeriodEnd,
            isPastDue = isPastDue,
            statusMessage = statusMessage
        )
    }
}
