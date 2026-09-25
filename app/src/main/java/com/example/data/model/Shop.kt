package com.example.data.model

enum class ShopMode(val displayName: String, val description: String) {
    MERCH_SHOP("Merch Shop", "AI creates merchandise using your image, fulfilled on-demand via Printify"),
    REAL_SHOP("Real Shop", "Sell your actual physical items, inventory, and manage your own fulfillment")
}

enum class StorefrontTheme(
    val code: String,
    val displayName: String,
    val description: String,
    val primaryHex: String,
    val secondaryHex: String,
    val backgroundHex: String
) {
    MINIMAL(
        code = "MINIMAL",
        displayName = "Minimal",
        description = "Clean typography, generous negative space, refined neutrals, and crisp lines",
        primaryHex = "#18181B",
        secondaryHex = "#71717A",
        backgroundHex = "#FAFAFA"
    ),
    BOLD(
        code = "BOLD",
        displayName = "Bold",
        description = "High-contrast heavy typography, punchy accents, and prominent card geometry",
        primaryHex = "#4338CA",
        secondaryHex = "#06B6D4",
        backgroundHex = "#FFFFFF"
    ),
    LUXURY(
        code = "LUXURY",
        displayName = "Luxury",
        description = "Deep dark tones, elegant serif accents, warm champagne gold, and sleek borders",
        primaryHex = "#121214",
        secondaryHex = "#D4AF37",
        backgroundHex = "#18181B"
    ),
    CREATIVE(
        code = "CREATIVE",
        displayName = "Creative",
        description = "Playful rounded geometry, expressive gradients, and vibrant creator energy",
        primaryHex = "#F43F5E",
        secondaryHex = "#F59E0B",
        backgroundHex = "#FFFBEB"
    );

    companion object {
        fun fromCode(code: String?): StorefrontTheme {
            return entries.find { it.code.equals(code, ignoreCase = true) || it.name.equals(code, ignoreCase = true) }
                ?: MINIMAL
        }
    }
}

object StoreStatus {
    const val DRAFT = "DRAFT"
    const val PUBLISHED = "PUBLISHED"
    const val UNPUBLISHED = "UNPUBLISHED"
}

data class Shop(
    val id: String,
    val ownerUid: String,
    val name: String,
    val handle: String,
    val tagline: String? = null,
    val description: String? = null,
    val story: String? = null,
    val targetAudience: String? = null,
    val brandPersonality: List<String> = emptyList(),
    val visualDirection: String? = null,
    val brandConceptId: String? = null,
    val sourceSnapAnalysisId: String? = null,
    val logoUrl: String? = null,
    val bannerUrl: String? = null,
    val coverImageUrl: String? = null,
    val businessMode: ShopMode = ShopMode.MERCH_SHOP,
    val currency: String = "USD",
    val country: String = "US",
    val location: String? = null,

    // Storefront Theme & Styling
    val theme: StorefrontTheme = StorefrontTheme.MINIMAL,
    val primaryColor: String? = null,
    val secondaryColor: String? = null,
    val backgroundColor: String? = null,
    val textColor: String? = null,
    val visualStyle: String? = null,

    // Contact & Social
    val contactEmail: String? = null,
    val contactPhone: String? = null,
    val whatsappNumber: String? = null,
    val socialLinks: Map<String, String> = emptyMap(),

    // Commerce & Informational Policies (Configurable delivery fee or confirmed by seller)
    val deliveryInformation: String? = null,
    val fixedDeliveryFee: Double? = null,
    val returnPolicy: String? = null,
    val shippingPolicy: String? = null,
    val privacyPolicy: String? = null,

    // Storefront Product Organization
    val featuredProductIds: List<String> = emptyList(),
    val productOrderIds: List<String> = emptyList(),
    val productCount: Int = 0,

    // Storefront State Machine (DRAFT -> PUBLISHED <-> UNPUBLISHED)
    val status: String = StoreStatus.DRAFT,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val publishedAt: Long? = null
)
