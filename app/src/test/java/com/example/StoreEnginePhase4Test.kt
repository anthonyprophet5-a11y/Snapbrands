package com.example

import com.example.data.model.BrandConcept
import com.example.data.model.Product
import com.example.data.model.ProductType
import com.example.data.model.Shop
import com.example.data.model.ShopMode
import com.example.data.model.SnapAnalysis
import com.example.data.model.StoreStatus
import com.example.data.model.StorefrontTheme
import com.example.data.repository.SnapBrandRepository
import com.example.store.PublicStorefrontMapper
import com.example.store.PublishValidator
import com.example.store.StoreHandleSystem
import com.example.store.StorefrontThemeResolver
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class StoreEnginePhase4Test {

    private lateinit var repository: SnapBrandRepository

    private val sampleShop = Shop(
        id = "shop_test_123",
        ownerUid = SnapBrandRepository.ACCOUNT_A_UID,
        name = "Komorebi Artisan Goods",
        handle = "komorebi",
        businessMode = ShopMode.REAL_SHOP,
        currency = "USD",
        status = StoreStatus.DRAFT,
        theme = StorefrontTheme.MINIMAL,
        tagline = "Sunlight filtered through leaves.",
        description = "Mindfully curated artisan objects for slow living and calm daily rituals.",
        story = "Started in a quiet garden studio in 2024.",
        returnPolicy = "Returns accepted within 14 days of delivery.",
        shippingPolicy = "Dispatches within 2 business days via carbon-neutral shipping."
    )

    private val sampleProduct1 = Product(
        id = "prod_1",
        shopId = "shop_test_123",
        ownerUid = SnapBrandRepository.ACCOUNT_A_UID,
        title = "Ceramic Pour-Over Dripper",
        price = 38.00,
        currency = "USD",
        category = "Coffee Ware",
        type = ProductType.PHYSICAL,
        isVisible = true,
        isFeatured = true,
        shortDescription = "Matte speckled stoneware dripper",
        description = "Designed for conical filters to produce a clean, balanced cup.",
        sellingPoints = listOf("Hand-thrown speckled stoneware", "Heat-retaining thick walls")
    )

    private val sampleProduct2 = Product(
        id = "prod_2",
        shopId = "shop_test_123",
        ownerUid = SnapBrandRepository.ACCOUNT_A_UID,
        title = "Linen Coffee Filters (Pack of 3)",
        price = 16.00,
        currency = "USD",
        category = "Coffee Ware",
        type = ProductType.PHYSICAL,
        isVisible = false, // Hidden!
        isFeatured = false,
        shortDescription = "Reusable unbleached organic linen filters",
        description = "Handmade from 100% organic European flax."
    )

    private val sampleProduct3 = Product(
        id = "prod_3",
        shopId = "shop_test_123",
        ownerUid = SnapBrandRepository.ACCOUNT_A_UID,
        title = "Stoneware Serving Pitcher",
        price = 48.00,
        currency = "USD",
        category = "Tableware",
        type = ProductType.PHYSICAL,
        isVisible = true,
        isFeatured = false,
        shortDescription = "16oz serving carafe with dripless spout",
        description = "Wheel-thrown and high-fired for lasting durability."
    )

    @Before
    fun setUp() {
        repository = SnapBrandRepository()
        // Sign in as Account A
        repository.signInWithAccountA()
    }

    // ==========================================
    // 1. STORE HANDLE SYSTEM TESTS
    // ==========================================

    @Test
    fun testHandleNormalization() {
        assertEquals("sol-craft", StoreHandleSystem.normalize("Sol Craft"))
        assertEquals("komorebi-studio", StoreHandleSystem.normalize("Komorebi_Studio!"))
        assertEquals("mystore", StoreHandleSystem.normalize("@MyStore"))
        assertEquals("coffee-cup", StoreHandleSystem.normalize("  coffee---cup  "))
        assertEquals("a", StoreHandleSystem.normalize("a"))
    }

    @Test
    fun testHandleValidationRules() {
        // Valid (3-40 chars, lowercase, numbers, hyphens)
        assertTrue(StoreHandleSystem.isValid("komorebi"))
        assertTrue(StoreHandleSystem.isValid("artisan-goods-24"))

        // Invalid: too short (< 3)
        assertFalse(StoreHandleSystem.isValid("ab"))

        // Invalid: too long (> 40)
        assertFalse(StoreHandleSystem.isValid("this-is-a-handle-that-is-way-too-long-and-exceeds-the-maximum-length"))

        // Invalid: starts or ends with hyphen
        assertFalse(StoreHandleSystem.isValid("-my-store"))
        assertFalse(StoreHandleSystem.isValid("my-store-"))
    }

    @Test
    fun testHandleAvailabilityCheck() {
        val existingHandles = setOf("komorebi", "artisan-goods")

        val result1 = StoreHandleSystem.checkAvailability("komorebi", currentStoreId = null) { normalized, _ ->
            existingHandles.contains(normalized)
        }
        assertFalse(result1.isAvailable)
        assertTrue(result1.suggestions.isNotEmpty())

        val result2 = StoreHandleSystem.checkAvailability("new-handle", currentStoreId = null) { normalized, _ ->
            existingHandles.contains(normalized)
        }
        assertTrue(result2.isAvailable)
    }

    // ==========================================
    // 2. STOREFRONT THEMES & DESIGN TOKENS
    // ==========================================

    @Test
    fun testThemeTokensResolution() {
        val minimalTokens = StorefrontThemeResolver.resolve(sampleShop.copy(theme = StorefrontTheme.MINIMAL))
        assertEquals(StorefrontTheme.MINIMAL, minimalTokens.theme)
        assertFalse(minimalTokens.isDarkBackground)

        val luxuryTokens = StorefrontThemeResolver.resolve(sampleShop.copy(theme = StorefrontTheme.LUXURY))
        assertEquals(StorefrontTheme.LUXURY, luxuryTokens.theme)
        assertTrue(luxuryTokens.isDarkBackground)

        val boldTokens = StorefrontThemeResolver.resolve(sampleShop.copy(theme = StorefrontTheme.BOLD))
        assertEquals(StorefrontTheme.BOLD, boldTokens.theme)

        val creativeTokens = StorefrontThemeResolver.resolve(sampleShop.copy(theme = StorefrontTheme.CREATIVE))
        assertEquals(StorefrontTheme.CREATIVE, creativeTokens.theme)
    }

    // ==========================================
    // 3. PUBLIC STOREFRONT MAPPER & SECURITY PROJECTION
    // ==========================================

    @Test
    fun testPublicStorefrontProjectionSecurity() {
        val allProducts = listOf(sampleProduct1, sampleProduct2, sampleProduct3)
        val publicStore = PublicStorefrontMapper.toPublicStorefront(sampleShop, allProducts)

        // Verifies public identity fields
        assertEquals("shop_test_123", publicStore.storeId)
        assertEquals("Komorebi Artisan Goods", publicStore.name)
        assertEquals("komorebi", publicStore.handle)
        assertEquals(StorefrontTheme.MINIMAL, publicStore.theme)
        assertEquals(StoreStatus.DRAFT, publicStore.status)

        // CRITICAL PRIVACY & VISIBILITY:
        // sampleProduct2 has isVisible = false -> MUST NOT be in publicStore.products!
        assertEquals(2, publicStore.products.size)
        val productIds = publicStore.products.map { it.id }
        assertTrue(productIds.contains("prod_1"))
        assertTrue(productIds.contains("prod_3"))
        assertFalse(productIds.contains("prod_2"))

        // Featured products verification
        assertEquals(1, publicStore.featuredProducts.size)
        assertEquals("prod_1", publicStore.featuredProducts.first().id)

        // Verify PublicProduct contains only public fields
        val pubProd = publicStore.products.first { it.id == "prod_1" }
        assertEquals("Ceramic Pour-Over Dripper", pubProd.title)
        assertEquals(38.00, pubProd.price, 0.001)
        assertEquals("USD", pubProd.currency)
    }

    // ==========================================
    // 4. PUBLISH VALIDATOR TESTS
    // ==========================================

    @Test
    fun testPublishValidation() {
        // 1. Ready to publish shop
        val validShop = sampleShop.copy(
            name = "Valid Shop",
            handle = "valid-shop",
            tagline = "Quality goods",
            description = "Handmade ceramics."
        )
        val resultValid = PublishValidator.validate(validShop, listOf(sampleProduct1), isHandleTakenByOther = { false })
        assertTrue(resultValid.canPublish)
        assertTrue(resultValid.missingRequirements.isEmpty())

        // 2. Shop with no products
        val resultNoProducts = PublishValidator.validate(validShop, emptyList(), isHandleTakenByOther = { false })
        assertFalse(resultNoProducts.canPublish)
        assertTrue(resultNoProducts.missingRequirements.any { it.contains("product") })

        // 3. Shop with only hidden products
        val resultOnlyHidden = PublishValidator.validate(validShop, listOf(sampleProduct2), isHandleTakenByOther = { false })
        assertFalse(resultOnlyHidden.canPublish)
        assertTrue(resultOnlyHidden.missingRequirements.any { it.contains("visible product") })

        // 4. Shop missing handle
        val resultNoHandle = PublishValidator.validate(validShop.copy(handle = ""), listOf(sampleProduct1), isHandleTakenByOther = { false })
        assertFalse(resultNoHandle.canPublish)
        assertTrue(resultNoHandle.missingRequirements.any { it.contains("handle") })

        // 5. Rights warning advisory
        val analysisWithWarning = SnapAnalysis(
            id = "snap_ip_test",
            ownerUid = SnapBrandRepository.ACCOUNT_A_UID,
            photoUri = "",
            category = "",
            detectedSubject = "",
            description = "",
            visualCharacteristics = emptyList(),
            possibleBusinessModes = listOf("MERCH"),
            recommendedBusinessMode = "MERCH",
            suggestedProducts = emptyList(),
            rightsWarning = "Verify you own reproduction rights to this character art."
        )
        val resultWarning = PublishValidator.validate(validShop, listOf(sampleProduct1), isHandleTakenByOther = { false }, sourceAnalysis = analysisWithWarning)
        assertTrue(resultWarning.canPublish) // Warnings do not block
        assertTrue(resultWarning.warnings.isNotEmpty())
    }

    // ==========================================
    // 5. REPOSITORY STORE ENGINE MUTATION & LIFECYCLE
    // ==========================================

    @Test
    fun testStoreEngineRepositoryOperations() = runBlocking {
        // 1. Create a shop
        val createResult = repository.createShop(
            name = "Studio Kanso",
            handle = "studio-kanso",
            tagline = "Japanese modernism",
            description = "Minimalist objects for the contemporary desk.",
            mode = ShopMode.REAL_SHOP
        )
        assertTrue(createResult.isSuccess)
        val createdShop = createResult.getOrThrow()
        assertEquals(StoreStatus.DRAFT, createdShop.status)
        assertEquals("studio-kanso", createdShop.handle)

        // 2. Add products
        val prod1 = Product(
            id = "p_kanso_1",
            shopId = createdShop.id,
            ownerUid = createdShop.ownerUid,
            title = "Brass Desk Tray",
            price = 45.0,
            currency = "USD",
            isVisible = true,
            isFeatured = true,
            shortDescription = "Solid brushed brass valet tray",
            description = "Machined from solid brass with natural patina."
        )
        val prod2 = Product(
            id = "p_kanso_2",
            shopId = createdShop.id,
            ownerUid = createdShop.ownerUid,
            title = "Cedar Pen Rest",
            price = 22.0,
            currency = "USD",
            isVisible = true,
            isFeatured = false,
            shortDescription = "Aromatic hinoki cedar",
            description = "Sustainably harvested Japanese cypress."
        )
        repository.saveProduct(prod1)
        repository.saveProduct(prod2)

        // 3. Update store theme to LUXURY
        val updateThemeResult = repository.updateStore(createdShop.copy(theme = StorefrontTheme.LUXURY))
        assertTrue(updateThemeResult.isSuccess)
        assertEquals(StorefrontTheme.LUXURY, updateThemeResult.getOrThrow().theme)

        // 4. Update store policies
        val updatePoliciesResult = repository.updateStore(
            createdShop.copy(
                theme = StorefrontTheme.LUXURY,
                returnPolicy = "30-day hassle free returns.",
                shippingPolicy = "Dispatches within 24 hours."
            )
        )
        assertTrue(updatePoliciesResult.isSuccess)
        assertEquals("30-day hassle free returns.", updatePoliciesResult.getOrThrow().returnPolicy)

        // 5. Set featured products
        val setFeaturedResult = repository.setFeaturedProducts(createdShop.id, listOf(prod2.id))
        assertTrue(setFeaturedResult.isSuccess)
        assertEquals(listOf(prod2.id), setFeaturedResult.getOrThrow().featuredProductIds)

        // 6. Toggle product visibility
        val hideProdResult = repository.setProductVisibility(prod1.id, isVisible = false)
        assertTrue(hideProdResult.isSuccess)
        assertFalse(hideProdResult.getOrThrow().isVisible)

        // 7. Publish store
        val publishResult = repository.publishStore(createdShop.id)
        assertTrue(publishResult.isSuccess)
        val publishedShop = publishResult.getOrThrow()
        assertEquals(StoreStatus.PUBLISHED, publishedShop.status)
        assertNotNull(publishedShop.publishedAt)

        // 8. Unpublish store
        val unpublishResult = repository.unpublishStore(createdShop.id)
        assertTrue(unpublishResult.isSuccess)
        assertEquals(StoreStatus.UNPUBLISHED, unpublishResult.getOrThrow().status)

        // 9. Re-publish store
        val republishResult = repository.publishStore(createdShop.id)
        assertTrue(republishResult.isSuccess)
        assertEquals(StoreStatus.PUBLISHED, republishResult.getOrThrow().status)

        // 10. Public projection reflects published status
        val publicResult = repository.resolvePublicStorefront("studio-kanso")
        assertTrue(publicResult.isSuccess)
        val publicStore = publicResult.getOrThrow()
        assertEquals(StoreStatus.PUBLISHED, publicStore.status)
        assertEquals("studio-kanso", publicStore.handle)
    }

    @Test
    fun testTenantSecurityEnforcement() = runBlocking {
        // Account A creates a shop
        val shopResult = repository.createShop(
            name = "Account A Store",
            handle = "account-a-store",
            tagline = "Original items",
            description = "Items by Account A",
            mode = ShopMode.REAL_SHOP
        )
        assertTrue(shopResult.isSuccess)
        val shopA = shopResult.getOrThrow()

        // Switch to Account B
        repository.signInWithAccountB()

        // Account B attempts to update Account A's store -> MUST FAIL with SecurityException
        val illegalUpdate = repository.updateStore(shopA.copy(name = "Hijacked Name"))
        assertTrue(illegalUpdate.isFailure)
        assertTrue(illegalUpdate.exceptionOrNull() is SecurityException)

        // Account B attempts to publish Account A's store -> MUST FAIL
        val illegalPublish = repository.publishStore(shopA.id)
        assertTrue(illegalPublish.isFailure)
        assertTrue(illegalPublish.exceptionOrNull() is SecurityException)
    }
}
