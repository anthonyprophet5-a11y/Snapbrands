package com.example.service

import com.example.data.model.BrandConcept
import com.example.data.model.Product
import com.example.data.model.SnapAnalysis
import com.example.service.gemini.BrandConceptParser
import com.example.service.gemini.GeminiClient
import com.example.service.gemini.ProductConceptParser
import com.example.service.gemini.RealGeminiClient
import com.example.service.gemini.SnapAnalysisParser
import org.json.JSONObject

/**
 * Service Abstraction for AI Operations (Google Gemini Multimodal Vision API & Brand Genius).
 *
 * PHASE 2 ARCHITECTURE:
 * Connects to Google's gemini-3.5-flash endpoint to transform Phase 1 SnapAnalysis
 * into structured, editable brand identities with targeted regeneration.
 *
 * NOTE ON SECURITY:
 * During development, client-side requests utilize BuildConfig.GEMINI_API_KEY via AI Studio Secrets.
 * In a production deployment, this interface must be backed by a secure backend proxy or Firebase Cloud Functions.
 */
interface AIService {
    val isPhaseActive: Boolean get() = true

    suspend fun analyzeSnap(
        ownerUid: String,
        photoUri: String,
        base64Image: String,
        mimeType: String = "image/jpeg"
    ): SnapAnalysis

    suspend fun generateBrandConcept(
        analysis: SnapAnalysis,
        directive: String? = null,
        userCountry: String? = null
    ): BrandConcept

    suspend fun regenerateBrandName(
        current: BrandConcept,
        analysis: SnapAnalysis,
        directive: String? = null
    ): Pair<String, String> // Pair(brandName, usernameSuggestion)

    suspend fun regenerateTagline(
        current: BrandConcept,
        analysis: SnapAnalysis,
        directive: String? = null
    ): String

    suspend fun regenerateBrandStory(
        current: BrandConcept,
        analysis: SnapAnalysis,
        directive: String? = null
    ): String

    suspend fun generateProductsForShop(
        shopId: String,
        ownerUid: String,
        analysis: SnapAnalysis,
        brandConcept: BrandConcept?,
        businessMode: String,
        currency: String = "USD",
        directive: String? = null
    ): List<Product>

    suspend fun regenerateSingleProduct(
        product: Product,
        brandConcept: BrandConcept?,
        analysis: SnapAnalysis?,
        action: String,
        directive: String? = null
    ): Product

    suspend fun generateProducts(request: ProductGenerationRequest): List<ProductDraft>
    suspend fun generateMarketingCopy(request: MarketingCopyRequest): MarketingCopyDraft
}

data class BrandGenerationRequest(
    val photoUri: String,
    val userPreferences: String? = null,
    val businessMode: String
)

data class BrandDraft(
    val brandName: String,
    val tagline: String,
    val colorPalette: List<String>,
    val typographyStyle: String,
    val brandStory: String
)

data class ProductGenerationRequest(
    val brandName: String,
    val photoUri: String,
    val targetCategories: List<String>
)

data class ProductDraft(
    val title: String,
    val description: String,
    val suggestedPrice: Double,
    val category: String,
    val mockupTemplateId: String
)

data class MarketingCopyRequest(
    val brandName: String,
    val productTitle: String,
    val platform: String
)

data class MarketingCopyDraft(
    val headline: String,
    val caption: String,
    val hashtags: List<String>
)

class SnapBrandAIService(
    private val geminiClient: GeminiClient = RealGeminiClient(),
    private val analysisParser: SnapAnalysisParser = SnapAnalysisParser,
    private val brandParser: BrandConceptParser = BrandConceptParser,
    private val productParser: ProductConceptParser = ProductConceptParser
) : AIService {

    companion object {
        val SNAP_ANALYSIS_PROMPT = """
You are the commerce engine for SnapBrand ("SNAP ANYTHING. GET A SHOP.").
Your mission is to instantly transform any photo into an actionable commerce analysis.
Carefully examine the image to identify what it is and whether it is best suited for:
1. MERCH: Print-on-demand merchandise inspired by the image (e.g. pets, dogs, cats, artwork, drawings, sketches, personal photography, original patterns, crafts).
2. REAL_SHOP: Direct selling of physical items, electronics, gadgets, shoes, furniture, vehicles, manufactured goods, or inventory.

You MUST respond ONLY with a valid JSON object matching this schema:
{
  "category": "High-level category (e.g. Pet, Electronics, Apparel, Art & Illustration, Home Decor, Handmade, Vehicles, Sports)",
  "detectedSubject": "Precise name of the primary subject (e.g. Golden Retriever, MacBook Pro, Watercolor Flower, Vintage Watch)",
  "description": "Short 1-2 sentence description of the visual scene and subject",
  "visualCharacteristics": ["dominant color palette", "style/mood", "key visual elements"],
  "possibleBusinessModes": ["MERCH", "REAL_SHOP"],
  "recommendedBusinessMode": "MERCH or REAL_SHOP",
  "suggestedProducts": [
    {
      "name": "Product idea name (e.g. Classic Organic T-Shirt, Ceramic Mug, Hardcover Journal, Canvas Tote Bag)",
      "category": "Product category",
      "estimatedPriceRange": "${'$'}XX - ${'$'}YY",
      "description": "Why this product fits the image",
      "reason": "Commercial justification",
      "iconType": "tshirt|mug|poster|bag|electronics|general"
    }
  ],
  "targetAudience": "Ideal target audience description (e.g. Dog lovers, pet owners, and animal rescue supporters)",
  "brandOpportunities": "Compelling brand concept and market positioning narrative",
  "suggestedPriceRange": "${'$'}XX - ${'$'}YY (AI estimate)",
  "confidence": 0.95,
  "rightsWarning": "Make sure you own this image or have permission to use it commercially. This is a warning, not legal advice." (or null if purely generic/safe),
  "safetyFlags": []
}

CRITICAL RULES:
1. Business Mode:
   - Use 'MERCH' if the image is an animal/pet, drawing, sketch, artwork, painting, personal photo, craft, or decorative visual suitable for merchandise.
   - Use 'REAL_SHOP' if the image is a physical item (e.g. laptop, phone, gadget, shoes, furniture, vehicle, hardware, manufactured goods) that could be inventoried and sold directly.
2. Rights Protection:
   - If the image contains a recognizable copyrighted character, corporate trademark/logo (e.g. Apple, Nike), famous celebrity, or unverified artwork, set rightsWarning to "Make sure you own this image or have permission to use it commercially. This is a warning, not legal advice."
   - If it is an ordinary photo of an animal, sketch, laptop, or generic object, set rightsWarning to null.
3. Suggested products: Provide 3 to 5 realistic product ideas with appropriate price ranges clearly labeled as AI estimates.
""".trimIndent()
    }

    override suspend fun analyzeSnap(
        ownerUid: String,
        photoUri: String,
        base64Image: String,
        mimeType: String
    ): SnapAnalysis {
        val jsonResponse = geminiClient.generateContent(
            prompt = SNAP_ANALYSIS_PROMPT,
            base64Image = base64Image,
            mimeType = mimeType
        )

        return analysisParser.parse(
            jsonString = jsonResponse,
            ownerUid = ownerUid,
            photoUri = photoUri
        )
    }

    override suspend fun generateBrandConcept(
        analysis: SnapAnalysis,
        directive: String?,
        userCountry: String?
    ): BrandConcept {
        val isGhana = userCountry.equals("Ghana", ignoreCase = true) || userCountry.equals("GH", ignoreCase = true)
        val mode = analysis.recommendedBusinessMode

        val prompt = """
You are the AI Brand Genius engine for SnapBrand ("SNAP ANYTHING. GET A SHOP.").
Your mission is to transform a visual analysis into a distinctive, commercially viable brand identity.

IMAGE CONTEXT & COMMERCE ANALYSIS:
- Detected Subject: "${analysis.detectedSubject}"
- Category: "${analysis.category}"
- Visual Description: "${analysis.description}"
- Visual Characteristics: ${analysis.visualCharacteristics.joinToString(", ")}
- Business Mode: $mode (${if (mode == "MERCH") "Merchandise / Lifestyle Print-on-Demand Brand" else "Physical Products / Inventory Retail Store"})
- Suggested Products: ${analysis.suggestedProducts.joinToString(", ") { it.name }}
- Initial Audience: "${analysis.targetAudience}"
- Market Opportunities: "${analysis.brandOpportunities}"
${if (!directive.isNullOrBlank()) "- USER CUSTOMIZATION DIRECTIVE: \"$directive\" (Honor this specific creative direction)" else ""}
${if (isGhana) "- REGIONAL CONTEXT: The user is in Ghana. Reflect contemporary Ghanaian/African digital commerce vitality, social commerce, and local customer aspirations without stereotyping or forcing clichés." else ""}

BRANDING GUIDELINES:
1. Avoid generic names like "Awesome Store", "Cool Brand", or "Super Goods".
2. Create names that feel authentic, memorable, and rooted in the detected subject:
   - For pets (e.g. Golden Retriever): warm, characterful names (e.g. "Golden Hearth", "Rover & Oak", "Wag Republic").
   - For art/drawings: expressive, creative studio names (e.g. "Ink & Chroma", "Prism Studio", "Canvas Drift").
   - For hardware/electronics (e.g. Laptop): sleek, credible brand names (e.g. "Apex Workspace Co.", "Krona Tech", "Circuit & Stone").
3. Tagline must be short (under 10 words), memorable, and punchy.
4. Brand story must be 2-3 sentences explaining why the brand exists, what it represents, and who it serves.
5. Provide 3-4 distinct brand personality adjectives.
6. Provide a textual logo concept (no image generation required).

You MUST respond ONLY with valid JSON matching this schema:
{
  "brandName": "Brand Name",
  "usernameSuggestion": "@brandname",
  "tagline": "Memorable tagline under 10 words",
  "shortDescription": "1-2 sentence brand summary",
  "brandStory": "2-3 sentences explaining why it exists, what it represents, and who it is for.",
  "targetAudience": "Primary audience profile, customer interests, and motivation.",
  "brandPersonality": ["Trait1", "Trait2", "Trait3", "Trait4"],
  "brandKeywords": ["keyword1", "keyword2", "keyword3"],
  "visualStyle": "Aesthetic style description",
  "suggestedColorDirection": ["#Hex1", "#Hex2", "#Hex3"],
  "typographyPersonality": "Typography description",
  "logoConcept": "Textual description of logo icon and wordmark",
  "productNamingStyle": "Item naming approach",
  "marketingAngle": "Primary marketing hook"
}
""".trimIndent()

        val jsonResponse = geminiClient.generateText(prompt, asJson = true)
        return brandParser.parse(
            jsonString = jsonResponse,
            ownerUid = analysis.ownerUid,
            sourceAnalysis = analysis,
            directive = directive
        )
    }

    override suspend fun regenerateBrandName(
        current: BrandConcept,
        analysis: SnapAnalysis,
        directive: String?
    ): Pair<String, String> {
        val prompt = """
You are the AI Brand Genius engine for SnapBrand.
Generate an alternative, highly authentic brand name and matching social username for this business:
- Visual Subject: "${analysis.detectedSubject}"
- Business Mode: ${current.businessMode}
- Current Name: "${current.brandName}"
${if (!directive.isNullOrBlank()) "- Direction: \"$directive\"" else ""}

Respond ONLY with valid JSON:
{
  "brandName": "New Brand Name",
  "usernameSuggestion": "@newbrandhandle"
}
""".trimIndent()

        val json = geminiClient.generateText(prompt, asJson = true)
        val cleaned = json.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val obj = JSONObject(cleaned)
        val name = obj.optString("brandName", current.brandName)
        var handle = obj.optString("usernameSuggestion", current.usernameSuggestion)
        if (!handle.startsWith("@")) handle = "@$handle"
        return Pair(name, handle)
    }

    override suspend fun regenerateTagline(
        current: BrandConcept,
        analysis: SnapAnalysis,
        directive: String?
    ): String {
        val prompt = """
You are the AI Brand Genius engine for SnapBrand.
Generate a fresh, short, memorable tagline (under 10 words) for the brand "${current.brandName}":
- Visual Subject: "${analysis.detectedSubject}"
- Business Mode: ${current.businessMode}
- Current Tagline: "${current.tagline}"
${if (!directive.isNullOrBlank()) "- Direction: \"$directive\"" else ""}

Respond ONLY with valid JSON:
{
  "tagline": "Punchy new tagline"
}
""".trimIndent()

        val json = geminiClient.generateText(prompt, asJson = true)
        val cleaned = json.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val obj = JSONObject(cleaned)
        return obj.optString("tagline", current.tagline)
    }

    override suspend fun regenerateBrandStory(
        current: BrandConcept,
        analysis: SnapAnalysis,
        directive: String?
    ): String {
        val prompt = """
You are the AI Brand Genius engine for SnapBrand.
Write a fresh 2-3 sentence brand story for "${current.brandName}":
- Visual Subject: "${analysis.detectedSubject}"
- Business Mode: ${current.businessMode}
- What it represents: why it exists, what it makes, and who it serves.
${if (!directive.isNullOrBlank()) "- Direction: \"$directive\"" else ""}

Respond ONLY with valid JSON:
{
  "brandStory": "Compelling brand story"
}
""".trimIndent()

        val json = geminiClient.generateText(prompt, asJson = true)
        val cleaned = json.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val obj = JSONObject(cleaned)
        return obj.optString("brandStory", current.brandStory)
    }

    override suspend fun generateProductsForShop(
        shopId: String,
        ownerUid: String,
        analysis: SnapAnalysis,
        brandConcept: BrandConcept?,
        businessMode: String,
        currency: String,
        directive: String?
    ): List<Product> {
        val isMerch = businessMode.equals("MERCH", ignoreCase = true)
        val prompt = if (isMerch) {
            """
You are the AI Product Engine for SnapBrand ("SNAP ANYTHING. GET A SHOP.").
Your mission is to generate 3 to 5 distinct, commercially compelling merchandise product listings inspired by or featuring the user's photo.
- Detected Subject: "${analysis.detectedSubject}"
- Brand Name: "${brandConcept?.brandName ?: analysis.detectedSubject}"
- Tagline: "${brandConcept?.tagline ?: ""}"
- Brand Personality: ${brandConcept?.brandPersonality?.joinToString() ?: "Creative, Authentic"}
- Target Audience: "${brandConcept?.targetAudience ?: analysis.targetAudience}"
- Visual Style: "${brandConcept?.visualStyle ?: "Clean & Modern"}"
- Business Mode: MERCH
${if (!directive.isNullOrBlank()) "- Custom Directive: \"$directive\"" else ""}

Recommend 3 to 5 appropriate merchandise products suited specifically to this subject (e.g. T-shirts, Hoodies, Ceramic Mugs, Gallery Posters, Canvas Totes, Phone Cases, Stickers). Do not output generic duplicates.
Every product price must be a reasonable retail price estimate in $currency.
Every AI price will be visibly presented with an "AI estimate" badge.

Respond ONLY with valid JSON matching this schema:
{
  "products": [
    {
      "productName": "Signature Classic Dog Tee",
      "productType": "MERCH",
      "category": "Apparel",
      "shortDescription": "Premium graphic tee featuring original artwork",
      "description": "Full 2-3 sentence product description...",
      "sellingPrice": 28.0,
      "currency": "$currency",
      "priceType": "AI estimate",
      "targetCustomer": "Pet lovers and casual wear enthusiasts",
      "sellingPoints": ["Point 1", "Point 2", "Point 3"],
      "imageConcept": "Centered high-definition graphic on front chest"
    }
  ]
}
""".trimIndent()
        } else {
            """
You are the AI Product Engine for SnapBrand ("SNAP ANYTHING. GET A SHOP.").
The user wants to sell the ACTUAL physical item shown in their photograph.
- Detected Subject: "${analysis.detectedSubject}"
- Category: "${analysis.category}"
- Brand/Store: "${brandConcept?.brandName ?: analysis.detectedSubject}"
- Description from Photo: "${analysis.description}"
- Business Mode: REAL_SHOP
${if (!directive.isNullOrBlank()) "- Custom Directive: \"$directive\"" else ""}

CRITICAL RULES FOR REAL SHOP:
1. Suggest a realistic listing for the photographed item.
2. AI suggestions must be clearly labeled as suggestions.
3. DO NOT invent facts about the physical product that cannot reliably be determined from the image (e.g. DO NOT claim exact storage capacity, exact model number, internal specs, warranty, or authenticity). State that buyer/seller should verify specs.
4. Suggest a realistic resale/listing price in $currency labeled as an AI estimate.
5. Suggest an initial inventory count of 1.

Respond ONLY with valid JSON matching this schema:
{
  "products": [
    {
      "productName": "${analysis.detectedSubject}",
      "productType": "PHYSICAL",
      "category": "${analysis.category}",
      "shortDescription": "Authentic photographed item",
      "description": "Physical item listed directly by owner...",
      "sellingPrice": 120.0,
      "currency": "$currency",
      "priceType": "AI estimate",
      "targetCustomer": "Buyers looking for authentic ${analysis.detectedSubject}",
      "sellingPoints": ["Item shown in photo", "Listed by verified seller", "Ready for local pickup or dispatch"],
      "imageConcept": "Original seller photograph of the physical item",
      "condition": "Pre-owned - Good (AI Suggestion)",
      "inventory": 1
    }
  ]
}
""".trimIndent()
        }

        return try {
            val json = geminiClient.generateText(prompt, asJson = true)
            productParser.parse(
                jsonString = json,
                shopId = shopId,
                ownerUid = ownerUid,
                sourceAnalysis = analysis,
                brandConcept = brandConcept,
                businessMode = businessMode,
                currency = currency
            )
        } catch (e: Exception) {
            productParser.generateFallbackProducts(
                shopId = shopId,
                ownerUid = ownerUid,
                sourceAnalysis = analysis,
                brandConcept = brandConcept,
                businessMode = businessMode,
                currency = currency
            )
        }
    }

    override suspend fun regenerateSingleProduct(
        product: Product,
        brandConcept: BrandConcept?,
        analysis: SnapAnalysis?,
        action: String,
        directive: String?
    ): Product {
        val prompt = """
You are the AI Product Engine for SnapBrand.
The user wants to update the following product:
- Current Title: "${product.title}"
- Current Category: "${product.category}"
- Current Description: "${product.description}"
- Business Mode: ${product.businessMode}
- Requested Action: $action
${if (!directive.isNullOrBlank()) "- User Directive: \"$directive\"" else ""}

Generate an improved, refined product specification.
Respond ONLY with valid JSON:
{
  "productName": "Refined Product Title",
  "category": "${product.category}",
  "shortDescription": "Concise 1-sentence summary",
  "description": "Engaging, high-converting product description",
  "sellingPrice": ${product.price},
  "imageConcept": "${product.imageConcept}",
  "sellingPoints": ["Point 1", "Point 2", "Point 3"]
}
""".trimIndent()

        return try {
            val json = geminiClient.generateText(prompt, asJson = true)
            productParser.parseSingleProduct(json, product, analysis, brandConcept)
        } catch (e: Exception) {
            product
        }
    }

    override suspend fun generateProducts(request: ProductGenerationRequest): List<ProductDraft> {
        throw UnsupportedOperationException(
            "Legacy ProductDraft generation deprecated in favor of Phase 3 generateProductsForShop."
        )
    }

    override suspend fun generateMarketingCopy(request: MarketingCopyRequest): MarketingCopyDraft {
        throw UnsupportedOperationException(
            "AI Marketing Generation is scheduled for Phase 5."
        )
    }
}
