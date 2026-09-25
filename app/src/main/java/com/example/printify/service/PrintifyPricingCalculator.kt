package com.example.printify.service

import com.example.printify.model.PrintifyProfitBreakdown
import com.example.store.checkout.Money

/**
 * SNAPBRAND — PHASE 7 PRICING & PROFIT ENGINE
 *
 * CRITICAL FINANCIAL SAFETY (Phase 5 & 7 Mandate):
 * 1. Uses integer minor units (pesewas / cents) exclusively.
 * 2. Never uses floating-point arithmetic for financial totals.
 * 3. Never fabricates production costs, shipping, or margins.
 * 4. Labels all margins as ESTIMATE.
 */
object PrintifyPricingCalculator {

    // Default platform fee rate (5% of customer selling price)
    private const val PLATFORM_FEE_BPS = 500L // 5.00% = 500 basis points

    /**
     * Computes profit breakdown using exact integer minor units.
     * If productionCost or shippingCost is missing (null), margin is explicitly null (Unavailable).
     */
    fun calculateBreakdown(
        sellingPriceMinor: Long,
        productionCostMinor: Long?,
        shippingCostMinor: Long?,
        currency: String = "USD"
    ): PrintifyProfitBreakdown {
        require(sellingPriceMinor >= 0) { "Selling price cannot be negative: $sellingPriceMinor" }

        // Platform fee in minor units: (sellingPrice * 500) / 10000
        val platformFeeMinor = (sellingPriceMinor * PLATFORM_FEE_BPS) / 10_000L

        // Seller margin can only be calculated if production cost is known.
        // If production cost is null, we NEVER invent or fabricate a cost!
        val sellerMarginMinor: Long? = if (productionCostMinor != null) {
            val shippingDeduction = shippingCostMinor ?: 0L
            sellingPriceMinor - productionCostMinor - platformFeeMinor - shippingDeduction
        } else {
            null
        }

        return PrintifyProfitBreakdown(
            sellingPriceMinor = sellingPriceMinor,
            productionCostMinor = productionCostMinor,
            shippingCostMinor = shippingCostMinor,
            platformFeesMinor = platformFeeMinor,
            estimatedSellerMarginMinor = sellerMarginMinor,
            currency = currency,
            isProductionCostEstimated = false,
            isShippingEstimated = shippingCostMinor != null
        )
    }

    /**
     * Formats financial values for UI display, showing "Unavailable" when null.
     */
    fun formatOrUnavailable(amountMinor: Long?, currency: String): String {
        return if (amountMinor != null) {
            Money.format(amountMinor, currency)
        } else {
            "Unavailable"
        }
    }
}
