package com.example.data.model

enum class ProductType {
    MERCH,
    PHYSICAL
}

enum class ProductStatus(val code: String, val displayName: String) {
    DRAFT("DRAFT", "Draft"),
    READY("READY", "Ready"),
    PUBLISHED("PUBLISHED", "Published"),
    OUT_OF_STOCK("OUT_OF_STOCK", "Out of Stock"),
    ARCHIVED("ARCHIVED", "Archived");

    companion object {
        fun fromString(value: String?): ProductStatus {
            return entries.find { it.code.equals(value, ignoreCase = true) || it.name.equals(value, ignoreCase = true) }
                ?: DRAFT
        }
    }
}

/**
 * Structured product variant for merchandise or sized goods.
 */
data class ProductVariant(
    val id: String,
    val size: String? = null,
    val color: String? = null,
    val sku: String? = null,
    val priceDelta: Double = 0.0,
    val inventory: Int = 10
)

/**
 * Cost and margin model for Print-on-Demand / Merchandise products.
 *
 * NOTE: As required by Phase 3, we prepare the data structure for future fulfillment,
 * but do NOT display fabricated Printify costs. Values remain null until real provider
 * connectivity is configured in Phase 5.
 */
data class MerchCostMarginModel(
    val customerPrice: Double,
    val currency: String = "USD",
    val estimatedProductionCost: Double? = null,
    val estimatedPlatformFees: Double? = null,
    val estimatedPaymentFees: Double? = null,
    val estimatedShippingCost: Double? = null,
    val estimatedSellerEarnings: Double? = null,
    val fulfillmentNote: String = "Fulfillment rates and real margins will unlock when Printify connects in Phase 5."
)

enum class ProductDisplayStatus(val label: String) {
    AVAILABLE("Available"),
    OUT_OF_STOCK("Out of Stock"),
    COMING_SOON("Coming Soon"),
    DRAFT_HIDDEN("Draft / Hidden")
}

data class Product(
    val id: String,
    val shopId: String,
    val ownerUid: String,
    val sourceSnapId: String? = null,
    val title: String,
    val shortDescription: String = "",
    val description: String,
    val price: Double,
    val currency: String = "USD",
    val priceType: String = "AI estimate",
    val category: String = "Merchandise",
    val type: ProductType = ProductType.MERCH,
    val businessMode: String = "MERCH", // "MERCH" or "REAL_SHOP"
    val targetCustomer: String = "General Audience",
    val sellingPoints: List<String> = emptyList(),
    val variants: List<ProductVariant> = emptyList(),
    val imageConcept: String = "",
    val sourcePhotoUrl: String? = null,
    val finalProductImageUrl: String? = null,
    val images: List<String> = emptyList(),
    val inventory: Int = 1,
    val condition: String? = null,
    val attributes: Map<String, String> = emptyMap(),
    val status: String = "DRAFT", // "DRAFT", "READY", "PUBLISHED", "OUT_OF_STOCK", "ARCHIVED"
    val isVisible: Boolean = false, // Seller decides when product appears on storefront
    val isFeatured: Boolean = false,
    val aiGenerated: Boolean = true,
    val costMargin: MerchCostMarginModel? = null,
    val printifyBlueprintId: Int? = null,
    val printifyProductId: String? = null,
    val printifyProviderId: Int? = null,
    val selectedPrintifyVariantIds: List<Int> = emptyList(),
    val printifyStatus: String? = null,
    val printifyLastSyncedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val displayStatus: ProductDisplayStatus
        get() = when {
            !isVisible || status == "DRAFT" || status == "ARCHIVED" -> ProductDisplayStatus.DRAFT_HIDDEN
            inventory <= 0 || status == "OUT_OF_STOCK" -> ProductDisplayStatus.OUT_OF_STOCK
            status == "COMING_SOON" -> ProductDisplayStatus.COMING_SOON
            else -> ProductDisplayStatus.AVAILABLE
        }
}
