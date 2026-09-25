package com.example.store

import com.example.data.model.Product
import com.example.data.model.Shop
import com.example.data.model.SnapAnalysis

data class PublishValidationResult(
    val canPublish: Boolean,
    val missingRequirements: List<String>,
    val warnings: List<String>
)

object PublishValidator {

    /**
     * Validates if a shop has met all minimum requirements to be published.
     */
    fun validate(
        shop: Shop,
        products: List<Product>,
        isHandleTakenByOther: (handle: String) -> Boolean,
        sourceAnalysis: SnapAnalysis? = null
    ): PublishValidationResult {
        val missing = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        // 1. Valid store name
        if (shop.name.trim().isBlank()) {
            missing.add("Add a valid store name.")
        }

        // 2. Valid unique handle
        val normalizedHandle = StoreHandleSystem.normalize(shop.handle)
        if (normalizedHandle.isBlank()) {
            missing.add("Confirm your store web handle.")
        } else if (!StoreHandleSystem.isValid(normalizedHandle)) {
            missing.add("Store handle must be 3-40 lowercase characters, numbers, and hyphens.")
        } else if (isHandleTakenByOther(normalizedHandle)) {
            missing.add("Store handle '$normalizedHandle' is already taken by another creator.")
        }

        // 3. Brand identity (tagline or description)
        val hasBrandIdentity = !shop.tagline.isNullOrBlank() || !shop.description.isNullOrBlank()
        if (!hasBrandIdentity) {
            missing.add("Add a store description or tagline to introduce your brand.")
        }

        // 4. At least one product that is explicitly visible
        val visibleCount = products.count { it.isVisible && it.status != "ARCHIVED" }
        if (visibleCount == 0) {
            missing.add("Choose at least one visible product to showcase on your storefront.")
        }

        // Commercial rights / IP warning advisory
        if (!sourceAnalysis?.rightsWarning.isNullOrBlank()) {
            warnings.add("Review commercial rights: ${sourceAnalysis.rightsWarning}")
        }

        return PublishValidationResult(
            canPublish = missing.isEmpty(),
            missingRequirements = missing,
            warnings = warnings
        )
    }
}
