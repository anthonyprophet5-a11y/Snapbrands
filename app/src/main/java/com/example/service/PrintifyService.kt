package com.example.service

/**
 * Service Abstraction for Fulfillment & Merch Production (Printify REST API).
 *
 * CRITICAL SECURITY PRINCIPLE:
 * Printify API tokens and production keys are stored strictly in Google Cloud Secret Manager
 * and accessed via Firebase Cloud Functions. Client apps never receive or bundle Printify secrets.
 */
interface PrintifyService {
    suspend fun getAvailableBlueprints(): List<PrintifyBlueprint>
    suspend fun createMerchProduct(request: CreateMerchRequest): String // returns printifyProductId
    suspend fun submitFulfillmentOrder(orderId: String): FulfillmentOrderResult
    suspend fun getTrackingInfo(fulfillmentId: String): TrackingInfo
}

data class PrintifyBlueprint(
    val id: Int,
    val title: String,
    val description: String,
    val brand: String,
    val category: String,
    val defaultPrintArea: String
)

data class CreateMerchRequest(
    val blueprintId: Int,
    val printProviderId: Int,
    val title: String,
    val description: String,
    val artworkUrl: String,
    val variants: List<String>
)

data class FulfillmentOrderResult(
    val printifyOrderId: String,
    val status: String,
    val estimatedProductionDays: Int
)

data class TrackingInfo(
    val carrier: String,
    val trackingNumber: String,
    val trackingUrl: String,
    val status: String
)

class SnapBrandPrintifyService : PrintifyService {
    override suspend fun getAvailableBlueprints(): List<PrintifyBlueprint> {
        throw UnsupportedOperationException(
            "Printify fulfillment catalog is an integration boundary reserved for Phase 2. " +
            "Requests will route through Cloud Functions to protect Printify API secret keys."
        )
    }

    override suspend fun createMerchProduct(request: CreateMerchRequest): String {
        throw UnsupportedOperationException(
            "Printify product publishing occurs server-side in Phase 2."
        )
    }

    override suspend fun submitFulfillmentOrder(orderId: String): FulfillmentOrderResult {
        throw UnsupportedOperationException(
            "Fulfillment orders are dispatched automatically upon Paystack payment webhook confirmation."
        )
    }

    override suspend fun getTrackingInfo(fulfillmentId: String): TrackingInfo {
        throw UnsupportedOperationException(
            "Tracking synchronization will consume Printify delivery webhooks."
        )
    }
}
