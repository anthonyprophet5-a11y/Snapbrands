package com.example.subscription

/**
 * SNAPBRAND SUBSCRIPTION & ENTITLEMENT MODELS — PHASE 6
 * Centralized, strongly-typed subscription configuration.
 *
 * Target Pricing:
 * SNAPBRAND: $20/month (2,000 minor units)
 * SNAPBRAND_PRO: $30/month (3,000 minor units)
 */

enum class SubscriptionPlan(
    val planId: String,
    val displayName: String,
    val priceUsdMonthlyMinor: Long, // e.g. 2000 = $20.00
    val targetAudience: String
) {
    NONE(
        planId = "none",
        displayName = "Free / Unsubscribed",
        priceUsdMonthlyMinor = 0L,
        targetAudience = "Explorers and new creators"
    ),
    SNAPBRAND(
        planId = "snapbrand",
        displayName = "SnapBrand",
        priceUsdMonthlyMinor = 2000L,
        targetAudience = "Independent creators & brand builders"
    ),
    SNAPBRAND_PRO(
        planId = "snapbrand_pro",
        displayName = "SnapBrand Pro",
        priceUsdMonthlyMinor = 3000L,
        targetAudience = "Growth-focused sellers & content creators"
    );

    val priceUsdMonthly: Double get() = priceUsdMonthlyMinor / 100.0

    companion object {
        fun fromId(id: String?): SubscriptionPlan {
            return entries.find { it.planId.equals(id, ignoreCase = true) } ?: NONE
        }
    }
}

enum class SubscriptionStatus(val label: String) {
    INACTIVE("Inactive"),
    PENDING("Pending Payment"),
    ACTIVE("Active"),
    PAST_DUE("Past Due"),
    CANCELLED("Cancelled"),
    EXPIRED("Expired")
}

enum class BillingInterval {
    MONTHLY
}

enum class BillingTransactionStatus {
    PAID,
    FAILED,
    REFUNDED,
    PENDING
}

/**
 * Entitlements for SnapBrand.
 * Core: Snap Engine, Brand Genius, Product Engine, Storefront, Orders, Commerce
 * Pro: Future AI marketing tools (Phase 10 prepared)
 */
enum class FeatureEntitlement(val isProOnly: Boolean, val description: String) {
    // Core SnapBrand features (included in SNAPBRAND and SNAPBRAND_PRO)
    SNAP_ENGINE(false, "Snap photo & AI catalog analysis"),
    AI_BRAND_GENIUS(false, "AI Brand identity, naming, and visual design"),
    PRODUCT_ENGINE(false, "AI product generation & editing"),
    STORE_ENGINE(false, "Digital storefront generation & customization"),
    STORE_PUBLISHING(false, "Storefront hosting & live link publishing"),
    CUSTOMER_CHECKOUT(false, "Customer cart & Paystack payment checkout"),
    SELLER_ORDERS(false, "Merchant order management & status processing"),
    STORE_MANAGEMENT(false, "Catalog, inventory & store settings"),

    // Pro-only features (SNAPBRAND_PRO) — Gates prepared for Phase 10
    AI_TIKTOK_REELS_IDEAS(true, "AI TikTok and Instagram Reels content strategy"),
    AI_SCRIPTS(true, "AI video scripts and voiceover prompts"),
    AI_CAPTIONS(true, "AI social captions and hashtag optimization"),
    AI_AD_COPY(true, "AI advertising copy for Instagram & Facebook ads"),
    WHATSAPP_PROMO_COPY(true, "WhatsApp broadcast and direct marketing copy"),
    CAMPAIGN_GENERATION(true, "Multi-channel automated marketing campaigns"),
    ADVANCED_MARKETING_TOOLS(true, "Advanced influencer outreach and templates"),
    ADVANCED_ANALYTICS(true, "Comprehensive traffic & customer engagement analytics")
}

/**
 * Authoritative subscription record.
 * Stored in backend / repository; never directly mutable by client.
 */
data class Subscription(
    val id: String,
    val ownerUid: String,
    val plan: SubscriptionPlan,
    val status: SubscriptionStatus,
    val billingInterval: BillingInterval = BillingInterval.MONTHLY,
    val currency: String = "USD",
    val amountMinor: Long = plan.priceUsdMonthlyMinor,
    val provider: String = "PAYSTACK",
    val providerCustomerId: String? = null,
    val providerSubscriptionId: String? = null,
    val providerReference: String? = null,
    val currentPeriodStart: Long = System.currentTimeMillis(),
    val currentPeriodEnd: Long = System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000L,
    val cancelAtPeriodEnd: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isGracePeriod: Boolean = false
) {
    val isEffectivelyActive: Boolean
        get() {
            val now = System.currentTimeMillis()
            return when (status) {
                SubscriptionStatus.ACTIVE -> now <= currentPeriodEnd
                SubscriptionStatus.CANCELLED -> cancelAtPeriodEnd && now <= currentPeriodEnd
                SubscriptionStatus.PAST_DUE -> isGracePeriod && now <= currentPeriodEnd
                else -> false
            }
        }
}

/**
 * Authoritative billing transaction record.
 */
data class BillingTransaction(
    val id: String,
    val ownerUid: String,
    val subscriptionId: String,
    val provider: String = "PAYSTACK",
    val providerReference: String,
    val amountMinor: Long,
    val currency: String = "USD",
    val status: BillingTransactionStatus,
    val paidAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Safe, read-only projection for client UI.
 */
data class UserEntitlementProjection(
    val ownerUid: String,
    val plan: SubscriptionPlan,
    val status: SubscriptionStatus,
    val hasCoreAccess: Boolean,
    val hasProAccess: Boolean,
    val currentPeriodEnd: Long,
    val cancelAtPeriodEnd: Boolean,
    val isPastDue: Boolean = false,
    val statusMessage: String = ""
)

/**
 * Centralized catalog helper for subscription pricing.
 * Ensures $20 and $30 pricing is not scattered or hardcoded.
 */
object SubscriptionProductCatalog {
    val corePlan: SubscriptionPlan get() = SubscriptionPlan.SNAPBRAND
    val proPlan: SubscriptionPlan get() = SubscriptionPlan.SNAPBRAND_PRO

    val allPaidPlans: List<SubscriptionPlan> = listOf(
        SubscriptionPlan.SNAPBRAND,
        SubscriptionPlan.SNAPBRAND_PRO
    )

    fun getPlan(planId: String): SubscriptionPlan = SubscriptionPlan.fromId(planId)

    fun formatPrice(amountMinor: Long, currency: String = "USD"): String {
        val amount = amountMinor / 100.0
        val symbol = when (currency.uppercase()) {
            "USD" -> "$"
            "GHS" -> "GH₵ "
            "EUR" -> "€"
            "GBP" -> "£"
            else -> "$currency "
        }
        return if (amount % 1.0 == 0.0) {
            "$symbol${amount.toLong()}"
        } else {
            "$symbol${String.format(java.util.Locale.US, "%.2f", amount)}"
        }
    }
}
