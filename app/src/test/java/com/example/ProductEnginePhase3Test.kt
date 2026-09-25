package com.example

import com.example.data.model.BrandConcept
import com.example.data.model.Product
import com.example.data.model.ProductType
import com.example.data.model.ShopMode
import com.example.data.model.SnapAnalysis
import com.example.data.repository.SnapBrandRepository
import com.example.service.gemini.ProductConceptParser
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
class ProductEnginePhase3Test {

    private val sampleAnalysis = SnapAnalysis(
        id = "snap_test_ceramics_1",
        ownerUid = SnapBrandRepository.ACCOUNT_A_UID,
        photoUri = "file:///data/photo_mug.jpg",
        category = "Handmade Ceramic Pottery",
        detectedSubject = "Handcrafted Ceramic Mug",
        description = "A speckled clay coffee mug with a blue glaze rim.",
        visualCharacteristics = listOf("earthy speckled clay", "cobalt blue glazed rim", "rustic curved handle"),
        possibleBusinessModes = listOf("MERCH", "REAL_SHOP"),
        recommendedBusinessMode = "REAL_SHOP",
        suggestedProducts = emptyList(),
        targetAudience = "Coffee enthusiasts & artisanal home lovers",
        brandOpportunities = "Kitchenware, artisanal decor",
        suggestedPriceRange = "$28 - $45",
        confidence = 0.94f
    )

    private val sampleBrandConcept = BrandConcept(
        id = "concept_ceramics_1",
        ownerUid = SnapBrandRepository.ACCOUNT_A_UID,
        sourceSnapAnalysisId = "snap_test_ceramics_1",
        brandName = "Cobalt Clay Studio",
        usernameSuggestion = "@cobaltclay",
        tagline = "Earth-spun, fire-cured ceramics.",
        shortDescription = "Handcrafted functional ceramics built for mindful morning rituals.",
        brandStory = "Every mug starts on the wheel in our small seaside pottery workshop.",
        targetAudience = "Slow living and specialty coffee fans.",
        brandPersonality = listOf("Artisanal", "Tactile", "Earthy", "Timeless"),
        visualStyle = "Rustic stoneware with clean coastal color accents.",
        suggestedColorDirection = listOf("#2B4C7E", "#D8C7B5", "#F4EFEA")
    )

    // ==========================================
    // 1. PRODUCT CONCEPT PARSER TESTS
    // ==========================================

    @Test
    fun `parser parses valid MERCH product concept JSON`() {
        val json = """
        {
          "products": [
            {
              "title": "Cobalt Clay Heavyweight Graphic Tee",
              "description": "Premium 100% ring-spun cotton tee featuring our studio emblem and mug sketch.",
              "price": 32.00,
              "category": "Apparel",
              "type": "MERCH",
              "suggestedInventory": 100,
              "features": ["100% organic cotton", "Screenprinted emblem", "Pre-shrunk"],
              "variants": [
                {"name": "Size", "options": ["S", "M", "L", "XL"]},
                {"name": "Color", "options": ["Natural Cream", "Slate Blue"]}
              ],
              "aiEstimateNote": "Based on standard DTG merchandise retail pricing"
            },
            {
              "title": "Studio Ceramic Ceramic Mug Replica",
              "description": "High-gloss 11oz ceramic mug decorated with the Cobalt Clay visual illustration.",
              "price": 18.50,
              "category": "Drinkware",
              "type": "MERCH",
              "suggestedInventory": 50,
              "features": ["Dishwasher safe", "Microwave safe", "Durable ceramic"],
              "aiEstimateNote": "On-demand drinkware standard rate"
            }
          ]
        }
        """

        val products: List<Product> = ProductConceptParser.parse(
            jsonString = json,
            shopId = "shop_test_1",
            ownerUid = SnapBrandRepository.ACCOUNT_A_UID,
            sourceAnalysis = sampleAnalysis,
            brandConcept = sampleBrandConcept,
            businessMode = "MERCH",
            currency = "USD"
        )

        assertEquals(2, products.size)
        val tee = products[0]
        assertEquals("Cobalt Clay Heavyweight Graphic Tee", tee.title)
        assertEquals(32.00, tee.price, 0.01)
        assertEquals(ProductType.MERCH, tee.type)
        assertEquals("DRAFT", tee.status)
        assertTrue(tee.aiGenerated)
        assertEquals("AI estimate", tee.priceType)
        assertTrue(tee.variants.isNotEmpty())
        assertTrue(tee.sellingPoints.isNotEmpty())
    }

    @Test
    fun `parser parses valid REAL_SHOP physical product JSON`() {
        val json = """
        {
          "products": [
            {
              "title": "Original Speckled Stoneware Mug",
              "description": "Authentic handmade wheel-thrown stoneware mug with cobalt drip glaze.",
              "price": 36.00,
              "category": "Ceramics",
              "type": "PHYSICAL",
              "suggestedInventory": 8,
              "features": ["Hand-thrown", "Food-safe glaze", "Holds 12oz"],
              "aiEstimateNote": "Estimated based on artisan ceramic market prices"
            }
          ]
        }
        """

        val products: List<Product> = ProductConceptParser.parse(
            jsonString = json,
            shopId = "shop_test_1",
            ownerUid = SnapBrandRepository.ACCOUNT_A_UID,
            sourceAnalysis = sampleAnalysis,
            brandConcept = sampleBrandConcept,
            businessMode = "REAL_SHOP",
            currency = "USD"
        )

        assertEquals(1, products.size)
        val mug = products[0]
        assertEquals("Original Speckled Stoneware Mug", mug.title)
        assertEquals(36.00, mug.price, 0.01)
        assertEquals(ProductType.PHYSICAL, mug.type)
        assertEquals(8, mug.inventory)
        assertEquals("DRAFT", mug.status)
    }

    @Test
    fun `parser handles malformed JSON with contextual fallbacks`() {
        val malformedJson = "This is not valid JSON content"

        val products: List<Product> = ProductConceptParser.parse(
            jsonString = malformedJson,
            shopId = "shop_fallback_1",
            ownerUid = SnapBrandRepository.ACCOUNT_A_UID,
            sourceAnalysis = sampleAnalysis,
            brandConcept = sampleBrandConcept,
            businessMode = "MERCH",
            currency = "USD"
        )

        assertTrue("Fallback products should be generated", products.isNotEmpty())
        assertTrue(products.size in 3..5)
        for (prod in products) {
            assertEquals("DRAFT", prod.status)
            assertTrue(prod.price > 0)
            assertNotNull(prod.title)
            assertEquals("shop_fallback_1", prod.shopId)
            assertEquals(SnapBrandRepository.ACCOUNT_A_UID, prod.ownerUid)
        }
    }

    // ==========================================
    // 2. REPOSITORY PERSISTENCE & MULTI-TENANT ISOLATION
    // ==========================================

    @Test
    fun `repository saves and isolates products between accounts`() {
        val repository = SnapBrandRepository()

        // Switch to Account A
        repository.signInWithAccountA()
        val accountA = repository.currentUser
        assertNotNull(accountA)

        val productA = Product(
            id = "prod_account_a_mug",
            shopId = "shop_account_a",
            ownerUid = SnapBrandRepository.ACCOUNT_A_UID,
            title = "Account A Artisan Mug",
            description = "Exclusive to Account A",
            price = 34.00,
            currency = "USD",
            inventory = 12,
            type = ProductType.PHYSICAL,
            status = "DRAFT"
        )
        val saveResultA = repository.saveProduct(productA)
        assertTrue(saveResultA.isSuccess)

        // Verify Account A can read the product
        val productsA = repository.getProductsForShop("shop_account_a")
        assertTrue(productsA.any { it.id == "prod_account_a_mug" })

        // Switch to Account B
        repository.signInWithAccountB()
        val accountB = repository.currentUser
        assertNotNull(accountB)

        // Verify Account B cannot see Account A's product
        val productsB = repository.getProductsForShop("shop_account_a")
        assertTrue("Account B must not see Account A's products", productsB.isEmpty())

        // Verify strict accessProductAs denies Account B access to Account A's product
        val accessResult = repository.accessProductAs(SnapBrandRepository.ACCOUNT_B_UID, "prod_account_a_mug")
        assertTrue("Access across accounts must fail", accessResult.isFailure)
        assertTrue(accessResult.exceptionOrNull() is SecurityException)

        // Attempting to save a product with mismatched ownerUid must fail
        val forbiddenSave = repository.saveProduct(productA) // Account B attempting to save Account A's product
        assertTrue("Saving product for another user must fail", forbiddenSave.isFailure)
    }

    @Test
    fun `product inventory update marks out of stock when inventory reaches zero`() {
        val repository = SnapBrandRepository()
        repository.signInWithAccountA()

        val product = Product(
            id = "prod_inventory_test",
            shopId = "shop_inv_test",
            ownerUid = SnapBrandRepository.ACCOUNT_A_UID,
            title = "Limited Edition Print",
            description = "Only a few made",
            price = 50.00,
            currency = "USD",
            inventory = 0,
            type = ProductType.MERCH,
            status = "ACTIVE"
        )

        val saveResult = repository.saveProduct(product)
        assertTrue(saveResult.isSuccess)
        val saved = saveResult.getOrThrow()
        assertEquals("OUT_OF_STOCK", saved.status)
        assertEquals(0, saved.inventory)
    }

    // ==========================================
    // 3. PHASE BOUNDARIES VERIFICATION
    // ==========================================

    @Test
    fun `phase boundaries remain strictly decoupled`() {
        // Products must start with status DRAFT
        val product = Product(
            id = "prod_boundary_test",
            shopId = "shop_test",
            ownerUid = SnapBrandRepository.ACCOUNT_A_UID,
            title = "Test Product",
            description = "Test description",
            price = 25.00
        )
        assertEquals("DRAFT", product.status)

        // Verify that default product type is MERCH or PHYSICAL
        assertTrue(product.type == ProductType.MERCH || product.type == ProductType.PHYSICAL)
    }
}
