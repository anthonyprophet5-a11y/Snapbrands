package com.example.printify.service

import com.example.data.model.Product
import com.example.printify.model.PrintifyDesignStatus

/**
 * SNAPBRAND — PHASE 7 DESIGN / ARTWORK VALIDATOR
 *
 * Strictly adheres to Phase 3 asset hierarchy:
 * 1. SOURCE IMAGE: Raw user-uploaded photograph or capture.
 * 2. PRODUCT DESIGN / CONCEPT: Semantic vector/design extracted by Brand Genius.
 * 3. FINAL PRODUCT IMAGE: High-resolution, print-ready asset suitable for POD printing.
 *
 * CRITICAL SAFETY:
 * Never assumes a raw camera snap is automatically print-ready.
 * Never silently replaces or alters seller designs with fake artwork.
 */
object PrintifyDesignValidator {

    data class ValidationResult(
        val isPrintReady: Boolean,
        val status: PrintifyDesignStatus,
        val artworkUrl: String?,
        val message: String
    )

    /**
     * Inspects product asset states and returns an honest design readiness status.
     */
    fun validateForPrintify(product: Product): ValidationResult {
        // Only MERCH products can have print assets for Printify
        if (!product.businessMode.equals("MERCH", ignoreCase = true)) {
            return ValidationResult(
                isPrintReady = false,
                status = PrintifyDesignStatus.ERROR,
                artworkUrl = null,
                message = "Only MERCH products can connect to Printify on-demand production."
            )
        }

        // 1. Check if final print-ready artwork exists
        val finalArtwork = product.finalProductImageUrl?.takeIf { it.isNotBlank() }
        if (finalArtwork != null) {
            return ValidationResult(
                isPrintReady = true,
                status = PrintifyDesignStatus.READY,
                artworkUrl = finalArtwork,
                message = "Print-ready artwork is verified and ready for production."
            )
        }

        // 2. Check if design concept exists (artwork required / preparation needed)
        val hasConcept = product.imageConcept.isNotBlank()
        val hasSourcePhoto = !product.sourcePhotoUrl.isNullOrBlank()

        if (hasConcept || hasSourcePhoto) {
            return ValidationResult(
                isPrintReady = false,
                status = PrintifyDesignStatus.ARTWORK_REQUIRED,
                artworkUrl = null,
                message = "This product has a design concept or source photo, but requires a print-ready asset before Printify creation."
            )
        }

        return ValidationResult(
            isPrintReady = false,
            status = PrintifyDesignStatus.CONFIGURATION_REQUIRED,
            artworkUrl = null,
            message = "This product is missing a print-ready design. Please upload or generate artwork."
        )
    }
}
