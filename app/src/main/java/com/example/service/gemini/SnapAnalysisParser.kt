package com.example.service.gemini

import com.example.data.model.SnapAnalysis
import com.example.data.model.SuggestedProduct
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object SnapAnalysisParser {

    /**
     * Parses raw JSON string returned by Gemini into a strongly typed SnapAnalysis domain model.
     */
    fun parse(
        jsonString: String,
        ownerUid: String,
        photoUri: String
    ): SnapAnalysis {
        try {
            // Strip any markdown fences if present
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

            val category = root.optString("category", "General Commerce")
            val detectedSubject = root.optString("detectedSubject", "Commercial Object")
            val description = root.optString("description", "Identified visual subject for commerce creation.")

            val visualCharacteristics = parseStringList(root.optJSONArray("visualCharacteristics"))
            val possibleModes = parseStringList(root.optJSONArray("possibleBusinessModes")).ifEmpty {
                listOf("MERCH", "REAL_SHOP")
            }

            var recommendedMode = root.optString("recommendedBusinessMode", "MERCH").uppercase()
            if (recommendedMode != "MERCH" && recommendedMode != "REAL_SHOP") {
                recommendedMode = if (category.contains("Electronics", ignoreCase = true) ||
                    category.contains("Hardware", ignoreCase = true) ||
                    category.contains("Furniture", ignoreCase = true) ||
                    category.contains("Device", ignoreCase = true)
                ) {
                    "REAL_SHOP"
                } else {
                    "MERCH"
                }
            }

            val products = parseSuggestedProducts(root.optJSONArray("suggestedProducts"), recommendedMode)

            val targetAudience = root.optString("targetAudience", "Enthusiasts and early adopters.")
            val brandOpportunities = root.optString("brandOpportunities", "Direct-to-consumer digital commerce brand.")
            val suggestedPriceRange = root.optString("suggestedPriceRange", "$18 - $48 (AI estimate)")

            val confidence = root.optDouble("confidence", 0.90).toFloat().coerceIn(0.1f, 1.0f)

            val rawRightsWarning = root.optString("rightsWarning", "")
            val rightsWarning = if (rawRightsWarning.isNotBlank() && !rawRightsWarning.equals("null", ignoreCase = true)) {
                rawRightsWarning
            } else {
                null
            }

            val safetyFlags = parseStringList(root.optJSONArray("safetyFlags"))

            val brandInspiration = root.optString(
                "brandInspiration",
                "Inspired by $detectedSubject, capturing modern aesthetic appeal for digital commerce."
            )

            val rawThemes = parseStringList(root.optJSONArray("productThemes"))
            val productThemes = if (rawThemes.isNotEmpty()) rawThemes else listOf(
                "Modern Minimalist",
                "Streetwear & Lifestyle",
                "Desk & Workspace Aesthetic"
            )

            val rawPrintifyOptions = parseStringList(root.optJSONArray("printifyMerchOptions"))
            val printifyMerchOptions = if (rawPrintifyOptions.isNotEmpty()) rawPrintifyOptions else listOf(
                "Classic Heavyweight Tee (Printify Blueprint #12)",
                "Ceramic Accent Mug 11oz (Printify Blueprint #19)",
                "Unisex Heavy Blend Hoodie (Printify Blueprint #77)",
                "Die-Cut Vinyl Stickers (Printify Blueprint #44)",
                "Stretched Canvas Gallery Wrap (Printify Blueprint #2)"
            )

            val rawViralAngles = root.optJSONArray("viralAngles")
            val viralAngles = mutableListOf<com.example.data.model.ViralProductIdea>()
            if (rawViralAngles != null && rawViralAngles.length() > 0) {
                for (i in 0 until rawViralAngles.length()) {
                    val vo = rawViralAngles.optJSONObject(i) ?: continue
                    viralAngles.add(
                        com.example.data.model.ViralProductIdea(
                            title = vo.optString("title", "Signature $detectedSubject Drop"),
                            hook = vo.optString("hook", "Unique visual statement piece."),
                            format = vo.optString("format", "Social Drop"),
                            viralPotentialScore = vo.optInt("viralPotentialScore", 88).coerceIn(60, 99),
                            audienceAppeal = vo.optString("audienceAppeal", "High novelty and cultural appeal")
                        )
                    )
                }
            }
            if (viralAngles.isEmpty()) {
                viralAngles.add(
                    com.example.data.model.ViralProductIdea(
                        title = "The $detectedSubject Essential Drop",
                        hook = "The viral aesthetic statement that your followers will instantly recognize.",
                        format = "Limited Batch Drop",
                        viralPotentialScore = 94,
                        audienceAppeal = "Super high social proof & gifting appeal"
                    )
                )
                viralAngles.add(
                    com.example.data.model.ViralProductIdea(
                        title = "Morning Coffee & Desk Companion",
                        hook = "Aesthetic desk setup unboxing piece built for Instagram & TikTok reels.",
                        format = "TikTok Unboxing",
                        viralPotentialScore = 89,
                        audienceAppeal = "Everyday aesthetic utility"
                    )
                )
                viralAngles.add(
                    com.example.data.model.ViralProductIdea(
                        title = "Collector Vinyl & Canvas Pack",
                        hook = "Collectible tactile art piece celebrating $detectedSubject.",
                        format = "Collector Drop",
                        viralPotentialScore = 82,
                        audienceAppeal = "Enthusiasts and niche collectors"
                    )
                )
            }

            return SnapAnalysis(
                id = "snap_" + UUID.randomUUID().toString().take(12),
                ownerUid = ownerUid,
                photoUri = photoUri,
                category = category,
                detectedSubject = detectedSubject,
                description = description,
                visualCharacteristics = visualCharacteristics,
                possibleBusinessModes = possibleModes,
                recommendedBusinessMode = recommendedMode,
                suggestedProducts = products,
                targetAudience = targetAudience,
                brandOpportunities = brandOpportunities,
                suggestedPriceRange = suggestedPriceRange,
                confidence = confidence,
                rightsWarning = rightsWarning,
                safetyFlags = safetyFlags,
                brandInspiration = brandInspiration,
                printifyMerchOptions = printifyMerchOptions,
                productThemes = productThemes,
                viralAngles = viralAngles,
                timestamp = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            throw GeminiParseException("Failed to parse Gemini structured commerce response: ${e.message}", e)
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

    private fun parseSuggestedProducts(array: JSONArray?, mode: String): List<SuggestedProduct> {
        val products = mutableListOf<SuggestedProduct>()
        if (array != null && array.length() > 0) {
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val name = obj.optString("name", "Custom Merch Item")
                val category = obj.optString("category", if (mode == "MERCH") "Apparel" else "Goods")
                val price = obj.optString("estimatedPriceRange", "$25 - $35")
                val desc = obj.optString("description", "Quality item inspired by image.")
                val reason = obj.optString("reason", "Commercial match for detected subject.")
                val iconType = obj.optString("iconType", inferIconType(name, category))

                products.add(
                    SuggestedProduct(
                        name = name,
                        category = category,
                        estimatedPriceRange = price,
                        description = desc,
                        reason = reason,
                        iconType = iconType
                    )
                )
            }
        }

        if (products.isEmpty()) {
            // Default baseline products based on mode
            if (mode == "MERCH") {
                products.add(SuggestedProduct("Classic Graphic Tee", "Apparel", "$24 - $32", "High-density soft cotton print.", "High demand merch staple.", "tshirt"))
                products.add(SuggestedProduct("Ceramic Accent Mug", "Drinkware", "$16 - $22", "11oz glossy ceramic with vibrant transfer.", "Accessible entry-price item.", "mug"))
                products.add(SuggestedProduct("Archival Matte Print", "Wall Art", "$20 - $38", "Museum-quality heavy stock poster.", "Ideal for visual art presentation.", "poster"))
                products.add(SuggestedProduct("Everyday Canvas Tote", "Accessories", "$18 - $26", "Reinforced organic cotton everyday carry.", "High repeat utility lifestyle item.", "bag"))
            } else {
                products.add(SuggestedProduct("Featured Hardware / Item", "Physical Good", "$45 - $95", "Direct item listing with specifications.", "Direct physical inventory.", "electronics"))
                products.add(SuggestedProduct("Protective Case / Bundle", "Accessories", "$20 - $35", "Complementary accessory pack.", "Increases average order value.", "general"))
            }
        }

        return products
    }

    fun inferIconType(name: String, category: String): String {
        val combined = (name + " " + category).lowercase()
        return when {
            combined.contains("tee") || combined.contains("t-shirt") || combined.contains("shirt") || combined.contains("hoodie") || combined.contains("apparel") -> "tshirt"
            combined.contains("mug") || combined.contains("cup") || combined.contains("drinkware") || combined.contains("bottle") -> "mug"
            combined.contains("poster") || combined.contains("print") || combined.contains("art") || combined.contains("canvas") -> "poster"
            combined.contains("tote") || combined.contains("bag") || combined.contains("backpack") -> "bag"
            combined.contains("laptop") || combined.contains("phone") || combined.contains("tech") || combined.contains("electronic") -> "electronics"
            else -> "general"
        }
    }
}
