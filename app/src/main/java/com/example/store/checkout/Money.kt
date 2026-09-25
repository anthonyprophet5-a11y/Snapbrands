package com.example.store.checkout

import java.util.Locale
import kotlin.math.roundToLong

/**
 * High-precision monetary utility for financial calculations.
 *
 * CRITICAL FINANCIAL SAFETY (Phase 5 Mandate):
 * Never rely on raw floating-point arithmetic for financial transactions.
 * All monetary amounts are internally held and computed in integer minor units
 * (e.g. pesewas / cents where 1 GHS / USD = 100 minor units).
 */
object Money {

    /**
     * Converts major decimal units (e.g. 25.50) into integer minor units (e.g. 2550).
     */
    fun toMinorUnits(amount: Double): Long {
        if (amount.isNaN() || amount.isInfinite()) return 0L
        return (amount * 100.0).roundToLong()
    }

    /**
     * Converts integer minor units (e.g. 2550) into major decimal units (e.g. 25.50).
     */
    fun toMajorUnits(minorUnits: Long): Double {
        return minorUnits / 100.0
    }

    /**
     * Safely computes item subtotal in minor units without floating-point drift.
     */
    fun calculateItemSubtotal(unitPriceMinorUnits: Long, quantity: Int): Long {
        require(quantity >= 0) { "Quantity cannot be negative: $quantity" }
        require(unitPriceMinorUnits >= 0) { "Unit price cannot be negative: $unitPriceMinorUnits" }
        return unitPriceMinorUnits * quantity
    }

    /**
     * Safely computes order total from subtotal and delivery fee.
     */
    fun calculateTotal(subtotalMinorUnits: Long, deliveryFeeMinorUnits: Long): Long {
        require(subtotalMinorUnits >= 0) { "Subtotal cannot be negative: $subtotalMinorUnits" }
        require(deliveryFeeMinorUnits >= 0) { "Delivery fee cannot be negative: $deliveryFeeMinorUnits" }
        return subtotalMinorUnits + deliveryFeeMinorUnits
    }

    /**
     * Formats minor units for customer display with currency symbol or code.
     * Preserves exact minor units (e.g. 2550 pesewas -> GH₵ 25.50).
     */
    fun format(minorUnits: Long, currency: String): String {
        val isNegative = minorUnits < 0
        val absUnits = kotlin.math.abs(minorUnits)
        val majorPart = absUnits / 100
        val minorPart = (absUnits % 100).toString().padStart(2, '0')
        val formattedNumber = "$majorPart.$minorPart"
        val sign = if (isNegative) "-" else ""

        val symbolOrCode = when (currency.uppercase(Locale.ROOT)) {
            "GHS" -> "GH₵"
            "USD" -> "$"
            "EUR" -> "€"
            "GBP" -> "£"
            "NGN" -> "₦"
            "KES" -> "KSh"
            else -> currency.uppercase(Locale.ROOT)
        }

        return if (symbolOrCode == "GH₵" || symbolOrCode == "KSh" || symbolOrCode == currency.uppercase(Locale.ROOT)) {
            "$sign$symbolOrCode $formattedNumber"
        } else {
            "$sign$symbolOrCode$formattedNumber"
        }
    }
}
