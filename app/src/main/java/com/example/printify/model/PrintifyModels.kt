package com.example.printify.model

/**
 * SNAPBRAND — PHASE 7: PRINTIFY INTEGRATION MODELS
 *
 * Core domain and backend data models for Print-on-Demand (POD) merchandise integration.
 * Strictly separates SnapBrand identity from external Printify identity.
 * Strictly enforces that no Printify API tokens or credentials appear in client models.
 */

enum class PrintifyConnectionStatus(val label: String) {
    DISCONNECTED("Disconnected"),
    CONNECTING("Connecting"),
    CONNECTED("Connected"),
    ERROR("Error"),
    REVOKED("Revoked")
}

data class PrintifyShopSummary(
    val id: String,
    val title: String,
    val salesChannel: String? = null
)

/**
 * Metadata record for a seller's connected Printify shop.
 * Stored securely per-seller and per-shop.
 * CRITICAL: NEVER contains Printify API tokens or secrets.
 */
data class PrintifyShopConnection(
    val id: String,
    val ownerUid: String,
    val snapbrandShopId: String,
    val printifyShopId: String,
    val printifyShopTitle: String? = null,
    val status: PrintifyConnectionStatus,
    val errorMessage: String? = null,
    val connectedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class PrintifyBlueprint(
    val id: Int,
    val title: String,
    val description: String = "",
    val brand: String = "",
    val model: String = "",
    val images: List<String> = emptyList()
)

data class PrintifyPrintProvider(
    val id: Int,
    val title: String,
    val location: String? = null
)

data class PrintifyVariant(
    val id: Int,
    val title: String,
    val options: Map<String, String> = emptyMap(), // e.g. "size" to "L", "color" to "Black"
    val costMinorUnits: Long? = null, // Production cost returned officially by Printify (minor units)
    val isAvailable: Boolean = true
)

data class PrintifyShippingEstimate(
    val standardCostMinorUnits: Long? = null,
    val currency: String = "USD",
    val handlingDaysMin: Int? = null,
    val handlingDaysMax: Int? = null
)

/**
 * Mapping between SnapBrand's internal MERCH product and Printify's product record.
 */
data class PrintifyProductMapping(
    val id: String,
    val ownerUid: String,
    val shopId: String,
    val snapbrandProductId: String,
    val printifyProductId: String,
    val printifyBlueprintId: Int,
    val printifyProviderId: Int,
    val selectedVariantIds: List<Int>,
    val printifyStatus: String = "draft", // "draft", "locked", "published"
    val lastSyncedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Explicit lifecycle states for Printify order synchronization (Phase 7 Mandate).
 */
enum class PrintifyOrderSyncStatus(val label: String) {
    NOT_REQUIRED("Not Required"),
    PENDING("Pending Synchronization"),
    SUBMITTED("Submitted to Printify"),
    ACCEPTED("Accepted by Printify"),
    PROCESSING("In Production"),
    FULFILLED("Fulfilled"),
    FAILED("Synchronization Failed"),
    CANCELLED("Cancelled")
}

data class PrintifyOrderMapping(
    val id: String,
    val ownerUid: String,
    val snapbrandOrderId: String,
    val printifyOrderId: String? = null,
    val printifyStatus: String? = null,
    val syncStatus: PrintifyOrderSyncStatus = PrintifyOrderSyncStatus.NOT_REQUIRED,
    val trackingNumber: String? = null,
    val trackingUrl: String? = null,
    val carrier: String? = null,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Persistent idempotency and operation record to prevent duplicate operations.
 */
data class PrintifyOperationRecord(
    val operationId: String,
    val ownerUid: String,
    val shopId: String,
    val snapbrandProductId: String,
    val operationType: String, // "CREATE_PRODUCT", "SUBMIT_ORDER", "PUBLISH_PRODUCT"
    val status: String, // "PENDING", "SUCCESS", "FAILED"
    val printifyResourceId: String? = null,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class PrintifyDesignStatus(val label: String) {
    ARTWORK_REQUIRED("Artwork Required"),
    CONFIGURATION_REQUIRED("Configuration Required"),
    READY("Ready"),
    CREATING("Creating"),
    CONNECTED("Connected to Printify"),
    SYNCING("Syncing"),
    ERROR("Error")
}

/**
 * Honest financial cost & margin breakdown using Phase 5 minor-unit Money architecture.
 * Any values that are estimated are explicitly flagged as estimates.
 */
data class PrintifyProfitBreakdown(
    val sellingPriceMinor: Long,
    val productionCostMinor: Long?,
    val shippingCostMinor: Long?,
    val platformFeesMinor: Long?,
    val estimatedSellerMarginMinor: Long?,
    val currency: String = "USD",
    val isProductionCostEstimated: Boolean = false,
    val isShippingEstimated: Boolean = false
) {
    val isProductionCostAvailable: Boolean get() = productionCostMinor != null
    val isShippingAvailable: Boolean get() = shippingCostMinor != null
    val isSellerMarginAvailable: Boolean get() = estimatedSellerMarginMinor != null
}
