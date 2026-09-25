package com.example.store.checkout

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Random

/**
 * Human-readable order number generator (e.g. SB-20260911-8F42).
 *
 * NOTE: As per Phase 5 security specifications, orderNumber is strictly
 * for human customer communication and paper slips; the internal immutable
 * UUID is always used for security and access authorization.
 */
object OrderNumberGenerator {

    private val random = Random()
    private val HEX_CHARS = "0123456789ABCDEF".toCharArray()

    fun generate(timestamp: Long = System.currentTimeMillis()): String {
        val datePart = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(timestamp))
        val randomSuffix = buildString(4) {
            repeat(4) {
                append(HEX_CHARS[random.nextInt(HEX_CHARS.size)])
            }
        }
        return "SB-$datePart-$randomSuffix"
    }
}
