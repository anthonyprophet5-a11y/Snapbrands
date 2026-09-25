package com.example.subscription.analytics

import android.util.Log

/**
 * Analytics events for Phase 6 Seller Subscriptions.
 * Deterministic and clean — does not fabricate revenue or conversion metrics.
 */
enum class SubscriptionAnalyticsEvent {
    PRICING_VIEWED,
    SUBSCRIPTION_STARTED,
    PAYMENT_INITIALIZED,
    SUBSCRIPTION_PAYMENT_SUCCESS,
    SUBSCRIPTION_PAYMENT_FAILED,
    SUBSCRIPTION_ACTIVATED,
    PLAN_UPGRADE_STARTED,
    PLAN_UPGRADED,
    PLAN_DOWNGRADE_STARTED,
    SUBSCRIPTION_CANCEL_STARTED,
    SUBSCRIPTION_CANCELLED,
    SUBSCRIPTION_EXPIRED,
    BILLING_SCREEN_VIEWED
}

object SubscriptionAnalytics {
    private const val TAG = "SubscriptionAnalytics"

    fun logEvent(
        event: SubscriptionAnalyticsEvent,
        ownerUid: String? = null,
        plan: String? = null,
        amountMinor: Long? = null,
        currency: String? = null,
        properties: Map<String, Any> = emptyMap()
    ) {
        val payload = buildMap {
            put("event", event.name)
            ownerUid?.let { put("ownerUid", it) }
            plan?.let { put("plan", it) }
            amountMinor?.let { put("amountMinor", it) }
            currency?.let { put("currency", it) }
            putAll(properties)
            put("timestamp", System.currentTimeMillis())
        }
        Log.d(TAG, "Event logged: $payload")
    }
}
