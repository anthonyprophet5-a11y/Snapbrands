package com.example.service.gemini

import com.example.data.model.BrandConcept
import com.example.data.model.SnapAnalysis
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object BrandConceptParser {

    fun parse(
        jsonString: String,
        ownerUid: String,
        sourceAnalysis: SnapAnalysis,
        directive: String? = null
    ): BrandConcept {
        try {
            var cleaned = jsonString.trim()
            if (cleaned.startsWith("```json")) {
                cleaned = cleaned.removePrefix("```json")
            } else if (cleaned.startsWith("```")) {
                cleaned = cleaned.removePrefix("```")
            }
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.substring(0, cleaned.length - 3)
            }
            cleaned = cleaned.trim()

            val root = JSONObject(cleaned)

            val subject = sourceAnalysis.detectedSubject
            val isMerch = sourceAnalysis.recommendedBusinessMode == "MERCH"

            val brandName = root.optString("brandName").takeIf { it.isNotBlank() }
                ?: (if (isMerch) "$subject Studio" else "$subject Supply Co.")

            val fallbackHandle = brandName.lowercase().replace("[^a-z0-9]".toRegex(), "")
            val usernameSuggestion = root.optString("usernameSuggestion").takeIf { it.isNotBlank() }
                ?: "@$fallbackHandle"

            val tagline = root.optString("tagline").takeIf { it.isNotBlank() }
                ?: "Crafted for everyday life."

            val shortDescription = root.optString("shortDescription").takeIf { it.isNotBlank() }
                ?: sourceAnalysis.description

            val brandStory = root.optString("brandStory").takeIf { it.isNotBlank() }
                ?: "Founded on a passion for $subject, we turn authentic moments into distinctive everyday essentials."

            val targetAudience = root.optString("targetAudience").takeIf { it.isNotBlank() }
                ?: sourceAnalysis.targetAudience

            val brandPersonality = parseStringList(root.optJSONArray("brandPersonality")).ifEmpty {
                if (isMerch) listOf("Playful", "Creative", "Authentic", "Community-led")
                else listOf("Reliable", "Sleek", "Professional", "Curated")
            }

            val brandKeywords = parseStringList(root.optJSONArray("brandKeywords")).ifEmpty {
                listOf(subject.lowercase(), "lifestyle", "commerce")
            }

            val visualStyle = root.optString("visualStyle").takeIf { it.isNotBlank() }
                ?: "Clean, modern, and warm."

            val suggestedColorDirection = parseStringList(root.optJSONArray("suggestedColorDirection")).ifEmpty {
                listOf("#6750A4", "#EADDFF", "#1C1B1F")
            }

            val typographyPersonality = root.optString("typographyPersonality").takeIf { it.isNotBlank() }
                ?: "Bold geometric display with clean sans-serif body."

            val logoConcept = root.optString("logoConcept").takeIf { it.isNotBlank() }
                ?: "Minimalist emblem of $subject with modern wordmark typography."

            val productNamingStyle = root.optString("productNamingStyle").takeIf { it.isNotBlank() }
                ?: "Simple, descriptive, and premium."

            val marketingAngle = root.optString("marketingAngle").takeIf { it.isNotBlank() }
                ?: sourceAnalysis.brandOpportunities

            return BrandConcept(
                id = "brand_" + UUID.randomUUID().toString().take(12),
                ownerUid = ownerUid,
                sourceSnapAnalysisId = sourceAnalysis.id,
                photoUri = sourceAnalysis.photoUri,
                businessMode = sourceAnalysis.recommendedBusinessMode,
                brandName = brandName,
                usernameSuggestion = if (usernameSuggestion.startsWith("@")) usernameSuggestion else "@$usernameSuggestion",
                tagline = tagline,
                shortDescription = shortDescription,
                brandStory = brandStory,
                targetAudience = targetAudience,
                brandPersonality = brandPersonality,
                brandKeywords = brandKeywords,
                visualStyle = visualStyle,
                suggestedColorDirection = suggestedColorDirection,
                typographyPersonality = typographyPersonality,
                logoConcept = logoConcept,
                productNamingStyle = productNamingStyle,
                marketingAngle = marketingAngle,
                customizationDirective = directive,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            throw GeminiParseException("Failed to parse Gemini structured brand concept: ${e.message}", e)
        }
    }

    fun parseNameRegeneration(jsonString: String, defaultName: String): Pair<String, String> {
        try {
            var cleaned = jsonString.trim()
            if (cleaned.startsWith("```json")) cleaned = cleaned.removePrefix("```json")
            if (cleaned.startsWith("```")) cleaned = cleaned.removePrefix("```")
            if (cleaned.endsWith("```")) cleaned = cleaned.substring(0, cleaned.length - 3)
            cleaned = cleaned.trim()

            val root = JSONObject(cleaned)
            val name = root.optString("brandName").takeIf { it.isNotBlank() } ?: defaultName
            var handle = root.optString("usernameSuggestion").takeIf { it.isNotBlank() }
                ?: ("@" + name.lowercase().replace("[^a-z0-9]".toRegex(), ""))
            if (!handle.startsWith("@")) handle = "@$handle"
            return Pair(name, handle)
        } catch (e: Exception) {
            val fallbackHandle = "@" + defaultName.lowercase().replace("[^a-z0-9]".toRegex(), "")
            return Pair(defaultName, fallbackHandle)
        }
    }

    fun parseTaglineRegeneration(jsonString: String, defaultTagline: String): String {
        try {
            var cleaned = jsonString.trim()
            if (cleaned.startsWith("```json")) cleaned = cleaned.removePrefix("```json")
            if (cleaned.startsWith("```")) cleaned = cleaned.removePrefix("```")
            if (cleaned.endsWith("```")) cleaned = cleaned.substring(0, cleaned.length - 3)
            cleaned = cleaned.trim()

            val root = JSONObject(cleaned)
            return root.optString("tagline").takeIf { it.isNotBlank() } ?: defaultTagline
        } catch (e: Exception) {
            return defaultTagline
        }
    }

    fun parseStoryRegeneration(jsonString: String, defaultStory: String): String {
        try {
            var cleaned = jsonString.trim()
            if (cleaned.startsWith("```json")) cleaned = cleaned.removePrefix("```json")
            if (cleaned.startsWith("```")) cleaned = cleaned.removePrefix("```")
            if (cleaned.endsWith("```")) cleaned = cleaned.substring(0, cleaned.length - 3)
            cleaned = cleaned.trim()

            val root = JSONObject(cleaned)
            return root.optString("brandStory").takeIf { it.isNotBlank() } ?: defaultStory
        } catch (e: Exception) {
            return defaultStory
        }
    }

    private fun parseStringList(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        val list = mutableListOf<String>()
        for (i in 0 until array.length()) {
            val item = array.optString(i)
            if (!item.isNullOrBlank()) {
                list.add(item)
            }
        }
        return list
    }
}
