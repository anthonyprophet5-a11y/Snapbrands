package com.example.store

import com.example.data.model.Product
import com.example.data.model.ProductDisplayStatus
import com.example.data.model.ProductType
import com.example.data.model.ProductVariant
import com.example.data.model.Shop
import com.example.data.model.StoreStatus
import com.example.data.model.StorefrontTheme

/**
 * Publicly renderable customer-facing product projection.
 *
 * CRITICAL PRIVACY & SECURITY:
 * Excludes: ownerUid, aiPrompts, geminiResponses, costMargin, supplier IDs, private seller notes.
 */
data class PublicProduct(
    val id: String,
    val title: String,
    val shortDescription: String,
    val description: String,
    val price: Double,
    val currency: String,
    val priceType: String,
    val category: String,
    val type: ProductType,
    val displayStatus: ProductDisplayStatus,
    val variants: List<ProductVariant>,
    val imageUrl: String?,
    val isFeatured: Boolean,
    val inventory: Int,
    val sellingPoints: List<String>
)

/**
 * Publicly renderable storefront model.
 *
 * CRITICAL PRIVACY & SECURITY:
 * Excludes: ownerUid, private credentials, internal diagnostic fields, unapproved draft products.
 */
data class PublicStorefront(
    val storeId: String,
    val name: String,
    val handle: String,
    val tagline: String?,
    val description: String?,
    val story: String?,
    val logoUrl: String?,
    val coverImageUrl: String?,
    val bannerUrl: String?,
    val theme: StorefrontTheme,
    val primaryColor: String?,
    val secondaryColor: String?,
    val backgroundColor: String?,
    val textColor: String?,
    val visualStyle: String?,
    val currency: String,
    val country: String,
    val location: String?,
    val contactEmail: String?,
    val contactPhone: String?,
    val whatsappNumber: String?,
    val socialLinks: Map<String, String>,
    val deliveryInformation: String?,
    val fixedDeliveryFee: Double? = null,
    val returnPolicy: String?,
    val shippingPolicy: String?,
    val privacyPolicy: String?,
    val featuredProducts: List<PublicProduct>,
    val products: List<PublicProduct>,
    val status: String = StoreStatus.PUBLISHED,
    val publishedAt: Long?
) {
    val id: String get() = storeId
}

object PublicStorefrontMapper {

    /**
     * Converts a Shop and its products into a public storefront.
     * Only visible products (isVisible == true and not archived) are included.
     */
    fun toPublicStorefront(
        shop: Shop,
        allProducts: List<Product>
    ): PublicStorefront {
        val visibleProducts = allProducts.filter { it.isVisible && it.status != "ARCHIVED" }

        val publicProducts = visibleProducts.map { p ->
            val isFeatured = shop.featuredProductIds.contains(p.id) || p.isFeatured
            val displayStatus = when {
                p.inventory <= 0 || p.status == "OUT_OF_STOCK" -> ProductDisplayStatus.OUT_OF_STOCK
                p.status == "COMING_SOON" -> ProductDisplayStatus.COMING_SOON
                else -> ProductDisplayStatus.AVAILABLE
            }

            PublicProduct(
                id = p.id,
                title = p.title,
                shortDescription = p.shortDescription,
                description = p.description,
                price = p.price,
                currency = p.currency,
                priceType = p.priceType,
                category = p.category,
                type = p.type,
                displayStatus = displayStatus,
                variants = p.variants,
                imageUrl = p.finalProductImageUrl ?: p.sourcePhotoUrl,
                isFeatured = isFeatured,
                inventory = p.inventory,
                sellingPoints = p.sellingPoints
            )
        }

        // Order featured products according to shop.featuredProductIds
        val featuredOrdered = if (shop.featuredProductIds.isNotEmpty()) {
            val featuredMap = publicProducts.filter { it.isFeatured }.associateBy { it.id }
            val orderedList = mutableListOf<PublicProduct>()
            for (id in shop.featuredProductIds) {
                featuredMap[id]?.let { orderedList.add(it) }
            }
            // Add any other featured products that were marked featured directly on the product
            publicProducts.filter { it.isFeatured && !shop.featuredProductIds.contains(it.id) }.forEach {
                orderedList.add(it)
            }
            orderedList
        } else {
            publicProducts.filter { it.isFeatured }
        }

        return PublicStorefront(
            storeId = shop.id,
            name = shop.name,
            handle = shop.handle,
            tagline = shop.tagline,
            description = shop.description,
            story = shop.story,
            logoUrl = shop.logoUrl,
            coverImageUrl = shop.coverImageUrl ?: shop.bannerUrl,
            bannerUrl = shop.bannerUrl,
            theme = shop.theme,
            primaryColor = shop.primaryColor,
            secondaryColor = shop.secondaryColor,
            backgroundColor = shop.backgroundColor,
            textColor = shop.textColor,
            visualStyle = shop.visualStyle,
            currency = shop.currency,
            country = shop.country,
            location = shop.location,
            contactEmail = shop.contactEmail,
            contactPhone = shop.contactPhone,
            whatsappNumber = shop.whatsappNumber,
            socialLinks = shop.socialLinks,
            deliveryInformation = shop.deliveryInformation,
            fixedDeliveryFee = shop.fixedDeliveryFee,
            returnPolicy = shop.returnPolicy,
            shippingPolicy = shop.shippingPolicy,
            privacyPolicy = shop.privacyPolicy,
            featuredProducts = featuredOrdered,
            products = publicProducts,
            status = shop.status,
            publishedAt = shop.publishedAt
        )
    }
}
