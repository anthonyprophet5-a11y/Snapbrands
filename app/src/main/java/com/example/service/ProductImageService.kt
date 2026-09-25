package com.example.service

import com.example.data.model.BrandConcept
import com.example.data.model.Product

/**
 * Phase 3 Image Architecture Boundary.
 *
 * Distinguishes clearly between:
 * 1. Source Image (original user photo from Phase 1)
 * 2. Product Design / Concept (how the product utilizes the graphic/photo)
 * 3. Final Product Image / Mockup (future provider or AI-generated mockup in Phase 5)
 *
 * This abstraction protects against claiming mockups exist before they are generated.
 */
interface ProductImageService {
    /**
     * Future capability: Generate a realistic product mockup (e.g. T-shirt or mug mockup with graphic applied).
     */
    suspend fun generateProductMockup(product: Product, brandConcept: BrandConcept?): Result<String>

    /**
     * Future capability: Generate lifestyle promotional imagery for marketing campaigns.
     */
    suspend fun generateLifestyleImage(product: Product): Result<String>
}

/**
 * Honest default implementation for Phase 3 before external rendering/Printify is wired.
 */
class DefaultProductImageService : ProductImageService {
    override suspend fun generateProductMockup(product: Product, brandConcept: BrandConcept?): Result<String> {
        // Honest boundary: do not fake generation. Return informative failure until Phase 5.
        return Result.failure(
            UnsupportedOperationException("Product mockup rendering connects in Phase 5 with print-on-demand fulfillment.")
        )
    }

    override suspend fun generateLifestyleImage(product: Product): Result<String> {
        return Result.failure(
            UnsupportedOperationException("Lifestyle image rendering connects in future marketing phases.")
        )
    }
}
