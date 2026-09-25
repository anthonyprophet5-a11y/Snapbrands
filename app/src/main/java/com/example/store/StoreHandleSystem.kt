package com.example.store

import java.util.Locale

/**
 * Record stored in persistence layer for atomic handle reservations.
 */
data class StoreHandleRecord(
    val handle: String,
    val storeId: String,
    val ownerUid: String,
    val createdAt: Long = System.currentTimeMillis()
)

data class HandleAvailabilityResult(
    val isAvailable: Boolean,
    val normalizedHandle: String,
    val isValidFormat: Boolean,
    val reason: String? = null,
    val suggestions: List<String> = emptyList()
)

/**
 * StoreHandleSystem handles normalization, validation, and collision-safe suggestions
 * for shop web handles (e.g. snapbrand.site/jakes-dog-shop).
 */
object StoreHandleSystem {

    private val VALID_HANDLE_REGEX = Regex("^[a-z0-9]+(-[a-z0-9]+)*$")

    /**
     * Normalizes a raw handle or store name:
     * - Lowercase
     * - Replaces spaces and underscores with hyphens
     * - Removes invalid characters (anything not [a-z0-9-])
     * - Collapses consecutive hyphens
     * - Trims leading and trailing hyphens
     */
    fun normalize(input: String): String {
        var clean = input.trim().lowercase(Locale.ROOT)
        // Remove '@' if user typed an Instagram-style handle
        clean = clean.removePrefix("@")
        // Replace spaces, underscores, and common delimiters with hyphens
        clean = clean.replace(Regex("[\\s_./\\\\]+"), "-")
        // Remove apostrophes, quotes, special symbols
        clean = clean.replace(Regex("[^a-z0-9\\-]"), "")
        // Collapse multiple hyphens
        clean = clean.replace(Regex("-+"), "-")
        // Trim hyphens from start and end
        clean = clean.trim('-')
        return clean
    }

    /**
     * Checks if a normalized handle satisfies format and length rules.
     */
    fun isValid(handle: String): Boolean {
        if (handle.length < 3 || handle.length > 40) return false
        return VALID_HANDLE_REGEX.matches(handle)
    }

    /**
     * Generates 3 clean, deterministic, collision-resistant alternatives when a handle is taken.
     */
    fun generateSuggestions(
        baseHandle: String,
        isHandleAvailable: (String) -> Boolean
    ): List<String> {
        val normalized = normalize(baseHandle)
        if (normalized.isBlank()) return emptyList()

        val candidates = listOf(
            "$normalized-1",
            "$normalized-shop",
            "$normalized-store",
            "${normalized}shop",
            "$normalized-official",
            "$normalized-co",
            "$normalized-app"
        )

        return candidates.filter { isHandleAvailable(it) }.take(3)
    }

    /**
     * Evaluates availability against the persistent handles registry.
     */
    fun checkAvailability(
        rawInput: String,
        currentStoreId: String?,
        isReservedByOther: (normalized: String, currentStoreId: String?) -> Boolean
    ): HandleAvailabilityResult {
        val normalized = normalize(rawInput)

        if (normalized.isBlank()) {
            return HandleAvailabilityResult(
                isAvailable = false,
                normalizedHandle = "",
                isValidFormat = false,
                reason = "Handle cannot be empty.",
                suggestions = emptyList()
            )
        }

        if (normalized.length < 3) {
            return HandleAvailabilityResult(
                isAvailable = false,
                normalizedHandle = normalized,
                isValidFormat = false,
                reason = "Handle must be at least 3 characters long.",
                suggestions = emptyList()
            )
        }

        if (normalized.length > 40) {
            return HandleAvailabilityResult(
                isAvailable = false,
                normalizedHandle = normalized.take(40),
                isValidFormat = false,
                reason = "Handle cannot exceed 40 characters.",
                suggestions = emptyList()
            )
        }

        if (!isValid(normalized)) {
            return HandleAvailabilityResult(
                isAvailable = false,
                normalizedHandle = normalized,
                isValidFormat = false,
                reason = "Handle can only contain lowercase letters, numbers, and single hyphens.",
                suggestions = emptyList()
            )
        }

        val taken = isReservedByOther(normalized, currentStoreId)
        return if (taken) {
            val suggestions = generateSuggestions(normalized) { candidate ->
                !isReservedByOther(candidate, currentStoreId)
            }
            HandleAvailabilityResult(
                isAvailable = false,
                normalizedHandle = normalized,
                isValidFormat = true,
                reason = "This handle is already taken.",
                suggestions = suggestions
            )
        } else {
            HandleAvailabilityResult(
                isAvailable = true,
                normalizedHandle = normalized,
                isValidFormat = true,
                reason = null,
                suggestions = emptyList()
            )
        }
    }
}
