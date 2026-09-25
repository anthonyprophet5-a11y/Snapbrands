package com.example.printify.webhook

import android.util.Log
import com.example.printify.model.PrintifyOrderSyncStatus
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

data class PrintifyWebhookResult(
    val eventType: String,
    val eventId: String,
    val shopId: String,
    val resourceId: String,
    val resourceType: String, // "order" or "product"
    val mappedSyncStatus: PrintifyOrderSyncStatus?,
    val trackingNumber: String? = null,
    val trackingUrl: String? = null,
    val carrier: String? = null,
    val isDuplicate: Boolean = false,
    val errorMessage: String? = null
)

/**
 * SNAPBRAND — PHASE 7 PRINTIFY WEBHOOK HANDLER
 *
 * Requirements (Phase 7 Mandate):
 * 1. HMAC SHA256 signature verification.
 * 2. Strict event structure validation.
 * 3. Idempotent processing with processed event ID tracking.
 * 4. Safe status mapping to internal Order / Merch statuses.
 * 5. Replay attack rejection.
 */
class PrintifyWebhookService(
    private val processedEventIds: MutableSet<String> = mutableSetOf()
) {
    private val TAG = "PrintifyWebhookService"

    /**
     * Verifies the authenticity of incoming Printify webhook request using HMAC SHA256.
     * Compares against the secret configured server-side.
     */
    fun verifySignature(payload: String, signatureHeader: String?, sharedSecret: String?): Boolean {
        if (sharedSecret.isNullOrBlank()) {
            // If webhook secret is not configured, reject webhook
            return false
        }
        if (signatureHeader.isNullOrBlank()) {
            return false
        }

        return try {
            val cleanSignature = signatureHeader.trim().removePrefix("sha256=")
            val mac = Mac.getInstance("HmacSHA256")
            val secretKeySpec = SecretKeySpec(sharedSecret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256")
            mac.init(secretKeySpec)
            val hashBytes = mac.doFinal(payload.toByteArray(StandardCharsets.UTF_8))
            val computedHex = hashBytes.joinToString("") { "%02x".format(it) }

            // Constant-time comparison to prevent timing attacks
            MessageDigest.isEqual(
                computedHex.toByteArray(StandardCharsets.UTF_8),
                cleanSignature.toByteArray(StandardCharsets.UTF_8)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating webhook signature", e)
            false
        }
    }

    /**
     * Parses and safely processes incoming Printify webhook event.
     */
    fun processWebhook(
        rawPayload: String,
        signatureHeader: String?,
        sharedSecret: String?,
        skipSignatureForTesting: Boolean = false
    ): Result<PrintifyWebhookResult> {
        if (rawPayload.isBlank()) {
            return Result.failure(IllegalArgumentException("Empty webhook payload received"))
        }

        // Authenticity check
        if (!skipSignatureForTesting) {
            if (!verifySignature(rawPayload, signatureHeader, sharedSecret)) {
                return Result.failure(SecurityException("Invalid or missing Printify webhook signature"))
            }
        }

        return try {
            val json = JSONObject(rawPayload)
            val eventType = json.optString("type", json.optString("topic", "")).ifBlank {
                return Result.failure(IllegalArgumentException("Missing event type or topic in webhook"))
            }

            // Derive or extract a unique event ID for replay prevention
            val eventId = json.optString("id", json.optString("event_id", "")).ifBlank {
                // Fallback deterministic digest based on type + timestamp + resource
                val resourceId = json.optJSONObject("resource")?.optString("id", "") ?: ""
                "evt_${eventType}_${resourceId}_${json.optLong("created_at", System.currentTimeMillis())}"
            }

            // Replay protection & Idempotency check
            synchronized(processedEventIds) {
                if (processedEventIds.contains(eventId)) {
                    val resource = json.optJSONObject("resource")
                    val shopId = json.optString("shop_id", resource?.optString("shop_id", ""))
                    val resId = resource?.optString("id", "") ?: ""
                    return Result.success(
                        PrintifyWebhookResult(
                            eventType = eventType,
                            eventId = eventId,
                            shopId = shopId,
                            resourceId = resId,
                            resourceType = if (eventType.startsWith("order:")) "order" else "product",
                            mappedSyncStatus = null,
                            isDuplicate = true
                        )
                    )
                }
                processedEventIds.add(eventId)
            }

            val resourceObj = json.optJSONObject("resource") ?: json
            val shopId = json.optString("shop_id", resourceObj.optString("shop_id", ""))
            val resourceId = resourceObj.optString("id", "")

            var mappedStatus: PrintifyOrderSyncStatus? = null
            var trackingNum: String? = null
            var trackingUrl: String? = null
            var carrier: String? = null

            when (eventType) {
                "order:created" -> {
                    mappedStatus = PrintifyOrderSyncStatus.ACCEPTED
                }
                "order:sent-to-production", "order:updated" -> {
                    val statusStr = resourceObj.optString("status", "")
                    mappedStatus = when (statusStr.lowercase()) {
                        "fulfilled" -> PrintifyOrderSyncStatus.FULFILLED
                        "canceled", "cancelled" -> PrintifyOrderSyncStatus.CANCELLED
                        "failed" -> PrintifyOrderSyncStatus.FAILED
                        "in_production", "sent-to-production" -> PrintifyOrderSyncStatus.PROCESSING
                        else -> PrintifyOrderSyncStatus.PROCESSING
                    }
                }
                "order:shipment:created", "order:shipment:delivered" -> {
                    mappedStatus = if (eventType == "order:shipment:delivered") {
                        PrintifyOrderSyncStatus.FULFILLED
                    } else {
                        PrintifyOrderSyncStatus.PROCESSING
                    }
                    val shipments = resourceObj.optJSONArray("shipments")
                    if (shipments != null && shipments.length() > 0) {
                        val firstShipment = shipments.getJSONObject(0)
                        trackingNum = firstShipment.optString("number", null)
                        trackingUrl = firstShipment.optString("url", null)
                        carrier = firstShipment.optString("carrier", null)
                    }
                }
                "product:publish:succeeded", "product:publish:started", "product:publish:failed" -> {
                    // Handled as product sync
                }
            }

            Result.success(
                PrintifyWebhookResult(
                    eventType = eventType,
                    eventId = eventId,
                    shopId = shopId,
                    resourceId = resourceId,
                    resourceType = if (eventType.startsWith("order:")) "order" else "product",
                    mappedSyncStatus = mappedStatus,
                    trackingNumber = trackingNum,
                    trackingUrl = trackingUrl,
                    carrier = carrier,
                    isDuplicate = false
                )
            )
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("Malformed Printify webhook JSON: ${e.message}"))
        }
    }
}
