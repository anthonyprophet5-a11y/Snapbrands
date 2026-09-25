package com.example

import android.graphics.Bitmap
import com.example.data.model.SnapAnalysis
import com.example.data.repository.SnapBrandRepository
import com.example.service.SnapBrandAIService
import com.example.service.gemini.GeminiClient
import com.example.service.gemini.GeminiConfigurationException
import com.example.service.gemini.GeminiInvalidRequestException
import com.example.service.gemini.GeminiNetworkException
import com.example.service.gemini.GeminiParseException
import com.example.service.gemini.GeminiRateLimitException
import com.example.service.gemini.GeminiUnavailableException
import com.example.service.gemini.SnapAnalysisParser
import com.example.util.ImageUtils
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SnapEnginePhase1Test {

    // ==========================================
    // 1. SNAP ANALYSIS PARSER TESTS
    // ==========================================

    @Test
    fun `parser correctly handles pet photo and maps to MERCH mode`() {
        val dogJson = """
        {
          "category": "Pet / Animal",
          "detectedSubject": "Golden Retriever Dog",
          "description": "A happy golden retriever sitting on grass in bright daylight.",
          "visualCharacteristics": ["warm golden coat", "lush green grass", "cheerful expression"],
          "possibleBusinessModes": ["MERCH", "REAL_SHOP"],
          "recommendedBusinessMode": "MERCH",
          "suggestedProducts": [
            {
              "name": "Classic Golden Retriever Tee",
              "category": "Apparel",
              "estimatedPriceRange": "$24 - $30 (AI estimate)",
              "description": "Vibrant soft cotton t-shirt with dog portrait.",
              "reason": "Popular pet lover staple.",
              "iconType": "tshirt"
            },
            {
              "name": "Ceramic Pet Lover Mug",
              "category": "Drinkware",
              "estimatedPriceRange": "$16 - $22 (AI estimate)",
              "description": "Dishwasher-safe morning coffee mug.",
              "reason": "High-volume gift item.",
              "iconType": "mug"
            }
          ],
          "targetAudience": "Dog owners, Golden Retriever enthusiasts, and pet lovers.",
          "brandOpportunities": "Lifestyle brand celebrating faithful canine companions.",
          "suggestedPriceRange": "$16 - $45 (AI estimate)",
          "confidence": 0.96,
          "rightsWarning": null,
          "safetyFlags": []
        }
        """.trimIndent()

        val analysis = SnapAnalysisParser.parse(
            jsonString = dogJson,
            ownerUid = "user_test_123",
            photoUri = "content://media/photos/dog.jpg"
        )

        assertEquals("Pet / Animal", analysis.category)
        assertEquals("Golden Retriever Dog", analysis.detectedSubject)
        assertEquals("MERCH", analysis.recommendedBusinessMode)
        assertEquals(2, analysis.suggestedProducts.size)
        assertEquals("tshirt", analysis.suggestedProducts[0].iconType)
        assertEquals("mug", analysis.suggestedProducts[1].iconType)
        assertNull("Rights warning should be null for common pet photo", analysis.rightsWarning)
        assertEquals(0.96f, analysis.confidence, 0.01f)
    }

    @Test
    fun `parser correctly handles hardware photo and maps to REAL_SHOP mode with rights warning`() {
        val laptopJson = """
        {
          "category": "Electronics & Hardware",
          "detectedSubject": "Apple MacBook Pro",
          "description": "Space gray aluminum laptop on a minimalist walnut desk.",
          "visualCharacteristics": ["metallic gray", "sleek aluminum", "modern workstation"],
          "possibleBusinessModes": ["REAL_SHOP", "MERCH"],
          "recommendedBusinessMode": "REAL_SHOP",
          "suggestedProducts": [
            {
              "name": "Refurbished Laptop Unit",
              "category": "Hardware",
              "estimatedPriceRange": "$850 - $1200 (AI estimate)",
              "description": "Certified inspected workstation.",
              "reason": "Direct physical commerce item.",
              "iconType": "electronics"
            },
            {
              "name": "Leather Workstation Sleeve",
              "category": "Accessories",
              "estimatedPriceRange": "$35 - $60 (AI estimate)",
              "description": "Padded protective sleeve.",
              "reason": "High-margin accessory bundle.",
              "iconType": "bag"
            }
          ],
          "targetAudience": "Remote tech workers, designers, and software engineers.",
          "brandOpportunities": "Premium curated hardware and workspace accessories store.",
          "suggestedPriceRange": "$35 - $1200 (AI estimate)",
          "confidence": 0.94,
          "rightsWarning": "Make sure you own this image or have permission to use it commercially. Trademarked brand name detected.",
          "safetyFlags": []
        }
        """.trimIndent()

        val analysis = SnapAnalysisParser.parse(
            jsonString = laptopJson,
            ownerUid = "user_test_456",
            photoUri = "content://media/photos/macbook.jpg"
        )

        assertEquals("Electronics & Hardware", analysis.category)
        assertEquals("Apple MacBook Pro", analysis.detectedSubject)
        assertEquals("REAL_SHOP", analysis.recommendedBusinessMode)
        assertNotNull("Should contain rights warning for trademarked item", analysis.rightsWarning)
        assertTrue(analysis.rightsWarning!!.contains("Make sure you own this image"))
    }

    @Test
    fun `parser gracefully strips markdown code fences if returned by model`() {
        val wrappedJson = """
        ```json
        {
          "category": "Handmade Craft",
          "detectedSubject": "Pottery Vase",
          "description": "Hand-spun clay vase with blue glaze.",
          "visualCharacteristics": ["cobalt glaze", "earthy clay"],
          "possibleBusinessModes": ["MERCH", "REAL_SHOP"],
          "recommendedBusinessMode": "REAL_SHOP",
          "suggestedProducts": [],
          "targetAudience": "Home decor collectors.",
          "brandOpportunities": "Artisan homewares.",
          "suggestedPriceRange": "$40 - $90 (AI estimate)",
          "confidence": 0.91,
          "rightsWarning": null,
          "safetyFlags": []
        }
        ```
        """.trimIndent()

        val analysis = SnapAnalysisParser.parse(
            jsonString = wrappedJson,
            ownerUid = "user_artisan",
            photoUri = "photo_vase.jpg"
        )

        assertEquals("Pottery Vase", analysis.detectedSubject)
        assertEquals("REAL_SHOP", analysis.recommendedBusinessMode)
    }

    @Test
    fun `parser throws GeminiParseException on malformed JSON`() {
        val brokenJson = "This is not JSON content at all. 500 error."
        try {
            SnapAnalysisParser.parse(brokenJson, "user_1", "uri_1")
            fail("Expected GeminiParseException")
        } catch (e: GeminiParseException) {
            assertTrue(e.message!!.contains("Failed to parse Gemini"))
        }
    }

    // ==========================================
    // 2. SNAP BRAND AI SERVICE & ERROR HANDLING
    // ==========================================

    @Test
    fun `AIService executes analyzeSnap with mock client successfully`() = runBlocking {
        val mockResponse = """
        {
          "category": "Illustration",
          "detectedSubject": "Cyberpunk Neon Cat",
          "description": "Digital illustration of a futuristic cat with neon colors.",
          "visualCharacteristics": ["neon magenta", "cyan glow", "futuristic"],
          "possibleBusinessModes": ["MERCH"],
          "recommendedBusinessMode": "MERCH",
          "suggestedProducts": [
            {
              "name": "Cyberpunk Cat Canvas Print",
              "category": "Wall Art",
              "estimatedPriceRange": "$28 - $50 (AI estimate)",
              "description": "Museum-grade canvas art.",
              "reason": "Visual art wall decor.",
              "iconType": "poster"
            }
          ],
          "targetAudience": "Gamers and sci-fi art enthusiasts.",
          "brandOpportunities": "Streetwear & digital art prints brand.",
          "suggestedPriceRange": "$28 - $50 (AI estimate)",
          "confidence": 0.98,
          "rightsWarning": null,
          "safetyFlags": []
        }
        """.trimIndent()

        val mockClient = object : GeminiClient {
            override suspend fun generateContent(prompt: String, base64Image: String, mimeType: String): String {
                return mockResponse
            }
        }

        val aiService = SnapBrandAIService(geminiClient = mockClient)
        val result = aiService.analyzeSnap(
            ownerUid = "alex_123",
            photoUri = "cyberpunk_cat.jpg",
            base64Image = "base64data"
        )

        assertEquals("Cyberpunk Neon Cat", result.detectedSubject)
        assertEquals("MERCH", result.recommendedBusinessMode)
        assertEquals(1, result.suggestedProducts.size)
        assertEquals("poster", result.suggestedProducts[0].iconType)
    }

    @Test
    fun `AIService executes analyzeSnap with headset photo commerce analysis successfully`() = runBlocking {
        val headsetResponse = """
        {
          "category": "Electronics",
          "detectedSubject": "Over-Ear Wireless Headphones",
          "description": "A pair of sleek, matte black over-ear wireless headphones with metallic accents.",
          "visualCharacteristics": ["matte black", "metallic accents", "minimalist modern"],
          "possibleBusinessModes": ["REAL_SHOP", "MERCH"],
          "recommendedBusinessMode": "REAL_SHOP",
          "suggestedProducts": [
            {
              "name": "Premium Wireless Over-Ear Headphones",
              "category": "Consumer Electronics",
              "estimatedPriceRange": "${'$'}120 - ${'$'}199",
              "description": "High-fidelity audio with active noise cancellation.",
              "reason": "Direct retail hardware product.",
              "iconType": "electronics"
            },
            {
              "name": "Hard Shell Protective Headphone Case",
              "category": "Audio Accessories",
              "estimatedPriceRange": "${'$'}15 - ${'$'}29",
              "description": "Custom molded EVA case for travel.",
              "reason": "High-margin companion accessory.",
              "iconType": "bag"
            },
            {
              "name": "Minimalist Aluminum Headphone Stand",
              "category": "Desk Accessories",
              "estimatedPriceRange": "${'$'}20 - ${'$'}45",
              "description": "Architectural desk stand for headphones display.",
              "reason": "Aesthetic workspace upsell.",
              "iconType": "general"
            }
          ],
          "targetAudience": "Audiophiles, remote workers, and gamers.",
          "brandOpportunities": "Direct-to-consumer premium audio hardware brand.",
          "suggestedPriceRange": "${'$'}120 - ${'$'}199 (AI estimate)",
          "confidence": 0.96,
          "rightsWarning": null,
          "safetyFlags": []
        }
        """.trimIndent()

        val mockClient = object : GeminiClient {
            override suspend fun generateContent(prompt: String, base64Image: String, mimeType: String): String {
                return headsetResponse
            }
        }

        val aiService = SnapBrandAIService(geminiClient = mockClient)
        val result = aiService.analyzeSnap(
            ownerUid = "audio_user_01",
            photoUri = "headset_photo.jpg",
            base64Image = "base64headset"
        )

        assertEquals("Electronics", result.category)
        assertEquals("Over-Ear Wireless Headphones", result.detectedSubject)
        assertEquals("REAL_SHOP", result.recommendedBusinessMode)
        assertEquals(3, result.suggestedProducts.size)
        assertEquals("electronics", result.suggestedProducts[0].iconType)
        assertNull(result.rightsWarning)
    }

    @Test
    fun `AIService propagates GeminiConfigurationException when key missing`() = runBlocking {
        val mockClient = object : GeminiClient {
            override suspend fun generateContent(prompt: String, base64Image: String, mimeType: String): String {
                throw GeminiConfigurationException("Gemini API key is not configured.")
            }
        }

        val aiService = SnapBrandAIService(geminiClient = mockClient)
        try {
            aiService.analyzeSnap("uid", "uri", "base64")
            fail("Should throw GeminiConfigurationException")
        } catch (e: GeminiConfigurationException) {
            assertTrue(e.message!!.contains("Gemini API key is not configured"))
        }
    }

    @Test
    fun `AIService propagates GeminiRateLimitException on 429`() = runBlocking {
        val mockClient = object : GeminiClient {
            override suspend fun generateContent(prompt: String, base64Image: String, mimeType: String): String {
                throw GeminiRateLimitException("Gemini API rate limit reached.")
            }
        }

        val aiService = SnapBrandAIService(geminiClient = mockClient)
        try {
            aiService.analyzeSnap("uid", "uri", "base64")
            fail("Should throw GeminiRateLimitException")
        } catch (e: GeminiRateLimitException) {
            assertTrue(e.message!!.contains("rate limit reached"))
        }
    }

    @Test
    fun `AIService propagates GeminiUnavailableException on server error`() = runBlocking {
        val mockClient = object : GeminiClient {
            override suspend fun generateContent(prompt: String, base64Image: String, mimeType: String): String {
                throw GeminiUnavailableException("Gemini service is temporarily unavailable.")
            }
        }

        val aiService = SnapBrandAIService(geminiClient = mockClient)
        try {
            aiService.analyzeSnap("uid", "uri", "base64")
            fail("Should throw GeminiUnavailableException")
        } catch (e: GeminiUnavailableException) {
            assertTrue(e.message!!.contains("temporarily unavailable"))
        }
    }

    @Test
    fun `AIService propagates GeminiInvalidRequestException on HTTP 400`() = runBlocking {
        val mockClient = object : GeminiClient {
            override suspend fun generateContent(prompt: String, base64Image: String, mimeType: String): String {
                throw GeminiInvalidRequestException("SnapBrand couldn't process this image.")
            }
        }

        val aiService = SnapBrandAIService(geminiClient = mockClient)
        try {
            aiService.analyzeSnap("uid", "uri", "base64")
            fail("Should throw GeminiInvalidRequestException")
        } catch (e: GeminiInvalidRequestException) {
            assertEquals("SnapBrand couldn't process this image.", e.message)
        }
    }

    @Test
    fun `AIService propagates GeminiNetworkException on network failure`() = runBlocking {
        val mockClient = object : GeminiClient {
            override suspend fun generateContent(prompt: String, base64Image: String, mimeType: String): String {
                throw GeminiNetworkException("Check your internet connection and try again.")
            }
        }

        val aiService = SnapBrandAIService(geminiClient = mockClient)
        try {
            aiService.analyzeSnap("uid", "uri", "base64")
            fail("Should throw GeminiNetworkException")
        } catch (e: GeminiNetworkException) {
            assertEquals("Check your internet connection and try again.", e.message)
        }
    }

    // ==========================================
    // 3. STRICT ACCOUNT ISOLATION FOR SNAP ANALYSIS
    // ==========================================

    @Test
    fun `strict account isolation prevents Account B from accessing Account A Snap Analysis`() {
        val repository = SnapBrandRepository()

        // Create Snap Analysis owned by Account A
        val snapAnalysisA = SnapAnalysis(
            id = "snap_alpha_001",
            ownerUid = SnapBrandRepository.ACCOUNT_A_UID,
            photoUri = "photo_alpha.jpg",
            category = "Artwork",
            detectedSubject = "Abstract Oil Painting",
            description = "Original oil painting",
            visualCharacteristics = listOf("blue", "gold"),
            possibleBusinessModes = listOf("MERCH"),
            recommendedBusinessMode = "MERCH",
            suggestedProducts = emptyList(),
            targetAudience = "Art lovers",
            brandOpportunities = "Galleries",
            suggestedPriceRange = "$30 - $75",
            confidence = 0.95f,
            rightsWarning = null,
            safetyFlags = emptyList(),
            timestamp = System.currentTimeMillis()
        )

        repository.saveSnapAnalysis(snapAnalysisA)

        // 1. Account A can access its own Snap Analysis
        val accessByA = repository.accessSnapAnalysisAs(
            requesterUid = SnapBrandRepository.ACCOUNT_A_UID,
            analysisId = "snap_alpha_001"
        )
        assertTrue("Account A must be able to access its own analysis", accessByA.isSuccess)
        assertEquals("snap_alpha_001", accessByA.getOrThrow().id)

        // 2. Account B cannot access Account A's Snap Analysis (Account Isolation Violation)
        val accessByB = repository.accessSnapAnalysisAs(
            requesterUid = SnapBrandRepository.ACCOUNT_B_UID,
            analysisId = "snap_alpha_001"
        )
        assertTrue("Account B must be forbidden from accessing Account A's analysis", accessByB.isFailure)
        val exception = accessByB.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception is SecurityException)
        assertTrue(exception!!.message!!.contains("Account Isolation Violation"))
    }

    // ==========================================
    // 4. IMAGE UTILS PROCESSING & SAFETY
    // ==========================================

    @Test
    fun `ImageUtils downscales oversized bitmap to safe Gemini dimensions`() {
        // Create large 2000x2000 bitmap
        val largeBitmap = Bitmap.createBitmap(2000, 2000, Bitmap.Config.ARGB_8888)
        val processed = ImageUtils.processBitmap(largeBitmap)

        assertNotNull(processed.base64Data)
        assertTrue("Base64 string should not be empty", processed.base64Data.isNotBlank())
        assertEquals("image/jpeg", processed.mimeType)
        assertTrue("Processed width must be <= 1280", processed.width <= 1280)
        assertTrue("Processed height must be <= 1280", processed.height <= 1280)
    }
}
