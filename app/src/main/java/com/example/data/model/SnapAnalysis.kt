package com.example.data.model

/**
 * Structured product idea proposed by Gemini Vision analysis.
 */
data class SuggestedProduct(
    val name: String,
    val category: String,
    val estimatedPriceRange: String,
    val description: String,
    val reason: String,
    val iconType: String = "general" // "tshirt", "mug", "poster", "bag", "electronics", "general"
)

/**
 * Viral merchandise or product concept designed for social discovery.
 */
data class ViralProductIdea(
    val title: String,
    val hook: String,
    val format: String = "Social Drop", // "TikTok Unboxing", "Meme / Gift Drop", "Collector Limited Drop"
    val viralPotentialScore: Int = 85, // 1 - 100
    val audienceAppeal: String = "High shareability and cultural appeal"
)

/**
 * Structured commercial intelligence extracted from a user's photo by Gemini.
 */
data class SnapAnalysis(
    val id: String,
    val ownerUid: String,
    val photoUri: String,
    val category: String,
    val detectedSubject: String,
    val description: String,
    val visualCharacteristics: List<String>,
    val possibleBusinessModes: List<String>,
    val recommendedBusinessMode: String, // "MERCH" or "REAL_SHOP"
    val suggestedProducts: List<SuggestedProduct>,
    val targetAudience: String = "General Audience",
    val brandOpportunities: String = "Merchandise & Lifestyle goods",
    val suggestedPriceRange: String = "$20 - $40 (AI estimate)",
    val confidence: Float = 0.95f,
    val rightsWarning: String? = null,
    val safetyFlags: List<String> = emptyList(),
    val brandInspiration: String = "",
    val printifyMerchOptions: List<String> = emptyList(),
    val productThemes: List<String> = emptyList(),
    val viralAngles: List<ViralProductIdea> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)
