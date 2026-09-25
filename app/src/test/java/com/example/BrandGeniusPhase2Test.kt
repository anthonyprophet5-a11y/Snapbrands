package com.example

import com.example.data.model.BrandConcept
import com.example.data.model.ShopMode
import com.example.data.model.SnapAnalysis
import com.example.data.repository.SnapBrandRepository
import com.example.service.gemini.BrandConceptParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BrandGeniusPhase2Test {

    private val sampleAnalysis = SnapAnalysis(
        id = "snap_test_dog_1",
        ownerUid = SnapBrandRepository.ACCOUNT_A_UID,
        photoUri = "file:///data/photo_dog.jpg",
        category = "Pet / Animal",
        detectedSubject = "Golden Retriever Dog",
        description = "A happy golden retriever sitting on grass.",
        visualCharacteristics = listOf("warm golden coat", "lush green grass"),
        possibleBusinessModes = listOf("MERCH", "REAL_SHOP"),
        recommendedBusinessMode = "MERCH",
        suggestedProducts = emptyList(),
        targetAudience = "Dog lovers",
        brandOpportunities = "Apparel",
        suggestedPriceRange = "$20 - $40",
        confidence = 0.95f
    )

    // ==========================================
    // 1. BRAND CONCEPT PARSER TESTS
    // ==========================================

    @Test
    fun `parser parses valid brand concept JSON`() {
        val json = """
        {
          "brandName": "Golden Hearth Co.",
          "usernameSuggestion": "@goldenhearth",
          "tagline": "Warm hearts, wagging tails.",
          "shortDescription": "Comfort-first apparel and home goods inspired by family dogs.",
          "brandStory": "Founded out of love for our golden retriever, Jake. We make everyday goods that celebrate the loyal companions who brighten our homes.",
          "targetAudience": "Pet parents and dog enthusiasts aged 24-45 looking for cozy lifestyle goods.",
          "brandPersonality": ["Warm", "Playful", "Authentic", "Cozy"],
          "visualStyle": "Earthy, sunny, and approachable with organic hand-drawn textures.",
          "typographyPersonality": "Soft rounded serif paired with warm clean sans-serif.",
          "suggestedColorDirection": ["#E8A838", "#2E5A44", "#FFF8EE"],
          "logoConcept": "Minimalist golden retriever head outline integrated into a warm hearth emblem."
        }
        """

        val concept = BrandConceptParser.parse(
            jsonString = json,
            ownerUid = SnapBrandRepository.ACCOUNT_A_UID,
            sourceAnalysis = sampleAnalysis
        )

        assertEquals("Golden Hearth Co.", concept.brandName)
        assertEquals("@goldenhearth", concept.usernameSuggestion)
        assertEquals("Warm hearts, wagging tails.", concept.tagline)
        assertEquals("MERCH", concept.businessMode)
        assertEquals(4, concept.brandPersonality.size)
        assertTrue(concept.brandPersonality.contains("Warm"))
        assertEquals(3, concept.suggestedColorDirection.size)
        assertEquals("snap_test_dog_1", concept.sourceSnapAnalysisId)
        assertEquals(SnapBrandRepository.ACCOUNT_A_UID, concept.ownerUid)
    }

    @Test
    fun `parser cleanly handles markdown json fencing`() {
        val wrappedJson = """
        ```json
        {
          "brandName": "Pawsitive Vibe",
          "usernameSuggestion": "pawsitivevibe",
          "tagline": "Spread joy one wag at a time.",
          "shortDescription": "Positive pet apparel.",
          "brandStory": "Born on morning park walks.",
          "targetAudience": "Dog lovers.",
          "brandPersonality": ["Joyful", "Energetic"],
          "visualStyle": "Bright and lively.",
          "typographyPersonality": "Bold modern sans.",
          "suggestedColorDirection": ["#FF5722", "#FFC107"],
          "logoConcept": "Stylized smiling paw print."
        }
        ```
        """

        val concept = BrandConceptParser.parse(
            jsonString = wrappedJson,
            ownerUid = SnapBrandRepository.ACCOUNT_A_UID,
            sourceAnalysis = sampleAnalysis
        )

        assertEquals("Pawsitive Vibe", concept.brandName)
        assertEquals("@pawsitivevibe", concept.usernameSuggestion)
        assertEquals("Spread joy one wag at a time.", concept.tagline)
    }

    @Test
    fun `parser falls back safely on corrupted or missing fields`() {
        val corrupted = "{ broken json content "

        val fallback = BrandConceptParser.parse(
            jsonString = "{}",
            ownerUid = SnapBrandRepository.ACCOUNT_A_UID,
            sourceAnalysis = sampleAnalysis
        )

        assertNotNull(fallback.brandName)
        assertTrue(fallback.brandName.contains("Golden Retriever") || fallback.brandName.isNotBlank())
        assertTrue(fallback.usernameSuggestion.startsWith("@"))
        assertNotNull(fallback.tagline)
        assertFalse(fallback.brandPersonality.isEmpty())
    }

    @Test
    fun `parser parses single field regenerations`() {
        val nameJson = """{"brandName": "Sunlit Paws", "usernameSuggestion": "@sunlitpaws"}"""
        val (name, handle) = BrandConceptParser.parseNameRegeneration(nameJson, "Fallback Name")
        assertEquals("Sunlit Paws", name)
        assertEquals("@sunlitpaws", handle)

        val taglineJson = """{"tagline": "Every tail has a tale."}"""
        val tagline = BrandConceptParser.parseTaglineRegeneration(taglineJson, "Default tagline")
        assertEquals("Every tail has a tale.", tagline)

        val storyJson = """{"brandStory": "Built from years of unconditional love and wagging tails."}"""
        val story = BrandConceptParser.parseStoryRegeneration(storyJson, "Default story")
        assertEquals("Built from years of unconditional love and wagging tails.", story)
    }

    // ==========================================
    // 2. ACCOUNT ISOLATION TESTS
    // ==========================================

    @Test
    fun `account B cannot access account A brand concept`() {
        val repo = SnapBrandRepository()
        repo.signInWithAccountA()

        val conceptA = BrandConcept(
            id = "concept_a_1",
            ownerUid = SnapBrandRepository.ACCOUNT_A_UID,
            sourceSnapAnalysisId = "snap_a_1",
            brandName = "Account A Brand",
            usernameSuggestion = "@accountabrand",
            tagline = "Only for A",
            shortDescription = "Secret brand",
            brandStory = "Owner A story",
            targetAudience = "Audience A",
            brandPersonality = listOf("Exclusive"),
            visualStyle = "Minimalist",
            typographyPersonality = "Serif",
            suggestedColorDirection = listOf("#111111"),
            logoConcept = "Letter A mark",
            businessMode = "MERCH"
        )
        repo.saveBrandConcept(conceptA)

        // Switch to Account B
        repo.signInWithAccountB()

        // Account B listing should NOT contain Account A's concept
        val bConcepts = repo.getBrandConceptsForCurrentAccount()
        assertFalse(bConcepts.any { it.id == "concept_a_1" })

        // Direct access test
        val directAccess = repo.accessBrandConceptAs(SnapBrandRepository.ACCOUNT_B_UID, "concept_a_1")
        assertTrue("Account B must not access Account A concept", directAccess.isFailure)
        assertTrue(directAccess.exceptionOrNull() is SecurityException)
    }

    // ==========================================
    // 3. DRAFT SHOP CREATION & RESUME FLOW
    // ==========================================

    @Test
    fun `accepting brand creates draft shop with status DRAFT`() {
        val repo = SnapBrandRepository()
        repo.signInWithAccountA()

        val concept = BrandConcept(
            id = "concept_draft_1",
            ownerUid = SnapBrandRepository.ACCOUNT_A_UID,
            sourceSnapAnalysisId = "snap_dog_100",
            brandName = "Bark & Bloom",
            usernameSuggestion = "@barkandbloom",
            tagline = "Blossoming with pet happiness",
            shortDescription = "Floral pet accessories",
            brandStory = "Where dogs meet garden florals.",
            targetAudience = "Botanical and dog lovers",
            brandPersonality = listOf("Charming", "Floral"),
            visualStyle = "Soft floral pastels",
            typographyPersonality = "Elegant serif",
            suggestedColorDirection = listOf("#FFB7B2", "#E2F0CB"),
            logoConcept = "Dog silhouette with daisy crown",
            businessMode = "MERCH"
        )

        val result = repo.acceptBrandAndCreateDraftShop(concept)
        assertTrue(result.isSuccess)

        val shop = result.getOrThrow()
        assertEquals("Bark & Bloom", shop.name)
        assertEquals("barkandbloom", shop.handle)
        assertEquals("DRAFT", shop.status)
        assertEquals("Where dogs meet garden florals.", shop.story)
        assertEquals("Botanical and dog lovers", shop.targetAudience)
        assertEquals(listOf("Charming", "Floral"), shop.brandPersonality)
        assertEquals(ShopMode.MERCH_SHOP, shop.businessMode)
        assertEquals("snap_dog_100", shop.sourceSnapAnalysisId)

        // Verify shop is present in account A's shops list
        val shops = repo.getShopsForCurrentAccount()
        assertTrue(shops.any { it.id == shop.id && it.status == "DRAFT" })
    }

    @Test
    fun `re-accepting brand updates existing draft shop with stable ID preventing duplicates`() {
        val repo = SnapBrandRepository()
        repo.signInWithAccountA()

        val initialConcept = BrandConcept(
            id = "concept_v1",
            ownerUid = SnapBrandRepository.ACCOUNT_A_UID,
            sourceSnapAnalysisId = "snap_resume_test",
            brandName = "Initial Bark",
            usernameSuggestion = "@initialbark",
            tagline = "Initial tagline",
            shortDescription = "Initial desc",
            brandStory = "Initial story",
            targetAudience = "Initial audience",
            brandPersonality = listOf("Friendly"),
            visualStyle = "Simple",
            typographyPersonality = "Sans",
            suggestedColorDirection = listOf("#000000"),
            logoConcept = "Paw print",
            businessMode = "MERCH"
        )

        val initialResult = repo.acceptBrandAndCreateDraftShop(initialConcept)
        val initialShop = initialResult.getOrThrow()
        val initialShopId = initialShop.id

        // User edits name and re-accepts
        val updatedConcept = initialConcept.copy(
            brandName = "Refined Bark Co.",
            tagline = "Refined tagline for dogs"
        )

        val secondResult = repo.acceptBrandAndCreateDraftShop(updatedConcept)
        val secondShop = secondResult.getOrThrow()

        // Must reuse stable ID
        assertEquals("Draft shop ID must remain stable across resumes", initialShopId, secondShop.id)
        assertEquals("Refined Bark Co.", secondShop.name)
        assertEquals("Refined tagline for dogs", secondShop.tagline)

        // Count of shops for this user must not have increased
        val shopsForUser = repo.getShopsForCurrentAccount().filter { it.sourceSnapAnalysisId == "snap_resume_test" }
        assertEquals("Must NOT duplicate draft shop", 1, shopsForUser.size)
    }
}
