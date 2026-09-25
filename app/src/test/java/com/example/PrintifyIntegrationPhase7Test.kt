package com.example

import com.example.data.model.*
import com.example.data.repository.SnapBrandRepository
import com.example.printify.backend.PrintifyBackendService
import com.example.printify.backend.PrintifyServerSecretProvider
import com.example.printify.client.PrintifyHttpClient
import com.example.printify.client.RealPrintifyHttpClient
import com.example.printify.model.*
import com.example.printify.service.PrintifyDesignValidator
import com.example.printify.service.PrintifyPricingCalculator
import com.example.printify.webhook.PrintifyWebhookService
import com.example.store.checkout.Money
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * SNAPBRAND — PHASE 7 PRINTIFY INTEGRATION COMPREHENSIVE TEST SUITE
 *
 * Covers all 16 user-mandated test requirements:
 * 1. Authentication header formation
 * 2. Missing token handling
 * 3. Invalid token handling
 * 4. Tenant isolation for shop connection
 * 5. Tenant isolation for product mapping
 * 6. Tenant isolation for order fulfillment
 * 7. MERCH vs REAL SHOP boundary (REAL SHOP strictly rejected)
 * 8. Product creation payload generation
 * 9. Product creation duplicate protection / idempotency
 * 10. Order submission duplicate protection / idempotency
 * 11. Webhook signature verification (HMAC SHA256)
 * 12. Webhook payload parsing
 * 13. Status mapping from Printify to SnapBrand
 * 14. Error handling on 429 rate limit
 * 15. Error handling on 5xx Printify outage
 * 16. Profit calculation math using integer minor units
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PrintifyIntegrationPhase7Test {

    private lateinit var testHttpClient: FakePrintifyHttpClient
    private lateinit var backendService: PrintifyBackendService
    private lateinit var webhookService: PrintifyWebhookService
    private lateinit var repository: SnapBrandRepository

    private val userAUid = "user_seller_a"
    private val userBUid = "user_seller_b"
    private val validTestToken = "test_token_secret_12345"
    private val webhookSharedSecret = "secret_webhook_key_xyz"

    @Before
    fun setUp() {
        testHttpClient = FakePrintifyHttpClient()
        webhookService = PrintifyWebhookService()
        backendService = PrintifyBackendService(testHttpClient, webhookService)
        backendService.setServerWebhookSecret(webhookSharedSecret)
        repository = SnapBrandRepository(printifyBackendService = backendService)
        repository.signInWithAccountA()
    }

    @After
    fun tearDown() {
        PrintifyServerSecretProvider.clearTestOverride()
    }

    // 1. Authentication header formation
    @Test
    fun testAuthenticationHeaderFormation() {
        val client = RealPrintifyHttpClient()
        val headers = client.buildAuthHeaders("sample_bearer_token")
        assertEquals("Bearer sample_bearer_token", headers["Authorization"])
        assertEquals("application/json", headers["Content-Type"])
        assertEquals("SnapBrand/1.0", headers["User-Agent"])
    }

    // 2. Missing Printify token handling
    @Test
    fun testMissingPrintifyTokenHandling() = runBlocking {
        val result = backendService.connectShop(
            ownerUid = userAUid,
            snapbrandShopId = "shop_a",
            printifyShopId = "pfy_shop_1",
            tokenToValidate = ""
        )
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SecurityException)
    }

    // 3. Invalid token handling
    @Test
    fun testInvalidTokenHandling() = runBlocking {
        testHttpClient.shouldFailWithUnauthorized = true
        val result = backendService.connectShop(
            ownerUid = userAUid,
            snapbrandShopId = "shop_a",
            printifyShopId = "pfy_shop_1",
            tokenToValidate = "bad_token_value"
        )
        assertTrue(result.isFailure)
        val conn = backendService.getConnection(userAUid, "shop_a")
        assertNotNull(conn)
        assertEquals(PrintifyConnectionStatus.ERROR, conn?.status)
    }

    // 4. Tenant isolation for Printify shop connection
    @Test
    fun testTenantIsolationForShopConnection() = runBlocking {
        // Seller A connects shop
        val connResult = backendService.connectShop(
            ownerUid = userAUid,
            snapbrandShopId = "shop_a",
            printifyShopId = "pfy_shop_1",
            tokenToValidate = validTestToken
        )
        assertTrue(connResult.isSuccess)

        // Seller A can view connection
        assertNotNull(backendService.getConnection(userAUid, "shop_a"))

        // Seller B CANNOT view Seller A's connection (tenant isolation)
        assertNull(backendService.getConnection(userBUid, "shop_a"))
    }

    // 5. Tenant isolation for Printify product mapping
    @Test
    fun testTenantIsolationForProductMapping() = runBlocking {
        backendService.connectShop(userAUid, "shop_a", "pfy_shop_1", validTestToken)

        val prodA = createSampleMerchProduct("prod_1", userAUid, "shop_a")
        val mapResult = backendService.createPrintifyProduct(
            callerUid = userAUid,
            product = prodA,
            blueprintId = 12,
            providerId = 1,
            variantIds = listOf(101, 102)
        )
        assertTrue(mapResult.isSuccess)

        // Seller A owns the mapping
        assertNotNull(backendService.getProductMapping(userAUid, "prod_1"))

        // Seller B CANNOT read or hijack Seller A's mapping
        assertNull(backendService.getProductMapping(userBUid, "prod_1"))

        // Seller B attempting to create Printify product for Seller A's item is rejected
        val unauthorizedAttempt = backendService.createPrintifyProduct(
            callerUid = userBUid,
            product = prodA,
            blueprintId = 12,
            providerId = 1,
            variantIds = listOf(101)
        )
        assertTrue(unauthorizedAttempt.isFailure)
        assertTrue(unauthorizedAttempt.exceptionOrNull() is SecurityException)
    }

    // 6. Tenant isolation for Printify order fulfillment
    @Test
    fun testTenantIsolationForOrderFulfillment() = runBlocking {
        backendService.connectShop(userAUid, "shop_a", "pfy_shop_1", validTestToken)
        val prodA = createSampleMerchProduct("prod_1", userAUid, "shop_a")
        backendService.createPrintifyProduct(userAUid, prodA, 12, 1, listOf(101))

        val orderA = Order(
            id = "order_123",
            orderNumber = "ORD-123",
            storeId = "shop_a",
            sellerUid = userAUid,
            customerId = "cust_1",
            customerEmail = "cust@example.com",
            customerName = "Jane Doe",
            items = listOf(OrderItem("prod_1", "Canvas Tote", 1, 25.0)),
            total = 2500L,
            paymentStatus = PaymentStatus.PAID,
            orderStatus = OrderStatus.PAID
        )

        // Seller B attempting to fulfill Seller A's order is rejected
        val unauthorizedOrderAttempt = backendService.syncPaidOrderToPrintify(
            callerUid = userBUid,
            order = orderA,
            productsById = mapOf("prod_1" to prodA)
        )
        assertTrue(unauthorizedOrderAttempt.isFailure)
        assertTrue(unauthorizedOrderAttempt.exceptionOrNull() is SecurityException)
    }

    // 7. MERCH vs REAL SHOP restriction (REAL SHOP products must be rejected)
    @Test
    fun testMerchVsRealShopBoundary() = runBlocking {
        backendService.connectShop(userAUid, "shop_a", "pfy_shop_1", validTestToken)

        val realShopProduct = Product(
            id = "prod_physical",
            shopId = "shop_a",
            ownerUid = userAUid,
            title = "Vintage Jacket",
            description = "Handmade real stock jacket",
            price = 50.0,
            businessMode = "REAL_SHOP", // Real physical product
            type = ProductType.PHYSICAL,
            inventory = 1,
            finalProductImageUrl = "https://example.com/artwork.png"
        )

        val result = backendService.createPrintifyProduct(
            callerUid = userAUid,
            product = realShopProduct,
            blueprintId = 12,
            providerId = 1,
            variantIds = listOf(101)
        )

        // MUST be rejected! REAL_SHOP inventory is managed directly by the seller.
        assertTrue(result.isFailure)
        val msg = result.exceptionOrNull()?.message ?: ""
        assertTrue(msg.contains("REAL SHOP / PHYSICAL", ignoreCase = true))
    }

    // 8. Product creation payload generation
    @Test
    fun testProductCreationPayloadGeneration() = runBlocking {
        backendService.connectShop(userAUid, "shop_a", "pfy_shop_1", validTestToken)
        val prodA = createSampleMerchProduct("prod_test_payload", userAUid, "shop_a")

        val result = backendService.createPrintifyProduct(
            callerUid = userAUid,
            product = prodA,
            blueprintId = 12,
            providerId = 3,
            variantIds = listOf(201, 202)
        )
        assertTrue(result.isSuccess)

        val lastPayload = testHttpClient.lastCreatedProductPayload
        assertNotNull(lastPayload)
        assertEquals(prodA.title, lastPayload?.getString("title"))
        assertEquals(12, lastPayload?.getInt("blueprint_id"))
        assertEquals(3, lastPayload?.getInt("print_provider_id"))
        val variants = lastPayload?.getJSONArray("variants")
        assertEquals(2, variants?.length())
    }

    // 9. Product creation duplicate protection / idempotency
    @Test
    fun testProductCreationIdempotency() = runBlocking {
        backendService.connectShop(userAUid, "shop_a", "pfy_shop_1", validTestToken)
        val prodA = createSampleMerchProduct("prod_idem", userAUid, "shop_a")

        // First call
        val firstCall = backendService.createPrintifyProduct(userAUid, prodA, 12, 1, listOf(101))
        assertTrue(firstCall.isSuccess)
        val initialCount = testHttpClient.createProductCalls

        // Second call with same product
        val secondCall = backendService.createPrintifyProduct(userAUid, prodA, 12, 1, listOf(101))
        assertTrue(secondCall.isSuccess)

        // Ensures Printify API was NOT called again! Reused existing mapping.
        assertEquals(initialCount, testHttpClient.createProductCalls)
        assertEquals(firstCall.getOrThrow().printifyProductId, secondCall.getOrThrow().printifyProductId)
    }

    // 10. Order submission duplicate protection / idempotency
    @Test
    fun testOrderSubmissionIdempotency() = runBlocking {
        backendService.connectShop(userAUid, "shop_a", "pfy_shop_1", validTestToken)
        val prodA = createSampleMerchProduct("prod_order_idem", userAUid, "shop_a")
        backendService.createPrintifyProduct(userAUid, prodA, 12, 1, listOf(101))

        val order = Order(
            id = "order_idem_1",
            orderNumber = "ORD-IDEM-1",
            storeId = "shop_a",
            sellerUid = userAUid,
            customerId = "cust_idem",
            customerEmail = "buyer@test.com",
            customerName = "Buyer One",
            items = listOf(OrderItem("prod_order_idem", "Merch Mug", 1, 20.0)),
            total = 2000L,
            paymentStatus = PaymentStatus.PAID,
            orderStatus = OrderStatus.PAID
        )

        val firstSubmit = backendService.syncPaidOrderToPrintify(
            callerUid = userAUid,
            order = order,
            productsById = mapOf("prod_order_idem" to prodA)
        )
        assertTrue(firstSubmit.isSuccess)
        val initialOrderCalls = testHttpClient.createOrderCalls

        // Second submit attempt
        val secondSubmit = backendService.syncPaidOrderToPrintify(
            callerUid = userAUid,
            order = order,
            productsById = mapOf("prod_order_idem" to prodA)
        )
        assertTrue(secondSubmit.isSuccess)

        // API call count must remain unchanged
        assertEquals(initialOrderCalls, testHttpClient.createOrderCalls)
        assertEquals(firstSubmit.getOrThrow().printifyOrderId, secondSubmit.getOrThrow().printifyOrderId)
    }

    // 11. Webhook signature verification
    @Test
    fun testWebhookSignatureVerification() {
        val payload = """{"type":"order:created","id":"evt_123","resource":{"id":"pfy_ord_999"}}"""
        // Compute valid signature
        val validMac = javax.crypto.Mac.getInstance("HmacSHA256").apply {
            init(javax.crypto.spec.SecretKeySpec(webhookSharedSecret.toByteArray(), "HmacSHA256"))
        }
        val expectedHash = validMac.doFinal(payload.toByteArray()).joinToString("") { "%02x".format(it) }

        // Valid signature passes
        assertTrue(webhookService.verifySignature(payload, expectedHash, webhookSharedSecret))
        assertTrue(webhookService.verifySignature(payload, "sha256=$expectedHash", webhookSharedSecret))

        // Tampered signature fails
        assertFalse(webhookService.verifySignature(payload, "bad_hash_value", webhookSharedSecret))

        // Missing secret fails
        assertFalse(webhookService.verifySignature(payload, expectedHash, null))
    }

    // 12. Webhook payload parsing
    @Test
    fun testWebhookPayloadParsing() {
        val payload = """
            {
                "type": "order:shipment:created",
                "id": "evt_ship_001",
                "resource": {
                    "id": "pfy_ord_100",
                    "shop_id": "pfy_shop_1",
                    "shipments": [
                        {
                            "carrier": "USPS",
                            "number": "9400111899562537628849",
                            "url": "https://tools.usps.com"
                        }
                    ]
                }
            }
        """.trimIndent()

        val parseResult = webhookService.processWebhook(
            rawPayload = payload,
            signatureHeader = null,
            sharedSecret = null,
            skipSignatureForTesting = true
        )
        assertTrue(parseResult.isSuccess)
        val event = parseResult.getOrThrow()
        assertEquals("order:shipment:created", event.eventType)
        assertEquals("evt_ship_001", event.eventId)
        assertEquals("pfy_ord_100", event.resourceId)
        assertEquals("9400111899562537628849", event.trackingNumber)
        assertEquals("USPS", event.carrier)
    }

    // 13. Status mapping from Printify to SnapBrand
    @Test
    fun testStatusMappingFromPrintifyToSnapBrand() {
        val fulfilledPayload = """
            {
                "type": "order:updated",
                "id": "evt_fulfilled",
                "resource": {
                    "id": "pfy_ord_200",
                    "status": "fulfilled"
                }
            }
        """.trimIndent()

        val result = webhookService.processWebhook(
            rawPayload = fulfilledPayload,
            signatureHeader = null,
            sharedSecret = null,
            skipSignatureForTesting = true
        )
        assertTrue(result.isSuccess)
        assertEquals(PrintifyOrderSyncStatus.FULFILLED, result.getOrThrow().mappedSyncStatus)
    }

    // 14. Error handling on 429 rate limit
    @Test
    fun testErrorHandlingOn429RateLimit() = runBlocking {
        testHttpClient.shouldFailWithRateLimit = true
        backendService.connectShop(userAUid, "shop_a", "pfy_shop_1", validTestToken)
        val prod = createSampleMerchProduct("prod_429", userAUid, "shop_a")

        val result = backendService.createPrintifyProduct(userAUid, prod, 12, 1, listOf(101))
        assertTrue(result.isFailure)
        val err = result.exceptionOrNull()
        assertTrue(err?.message?.contains("429") == true || err?.message?.contains("Rate limit") == true)
    }

    // 15. Error handling on 5xx Printify outage
    @Test
    fun testErrorHandlingOn5xxPrintifyOutage() = runBlocking {
        testHttpClient.shouldFailWith500 = true
        backendService.connectShop(userAUid, "shop_a", "pfy_shop_1", validTestToken)
        val prod = createSampleMerchProduct("prod_500", userAUid, "shop_a")

        val result = backendService.createPrintifyProduct(userAUid, prod, 12, 1, listOf(101))
        assertTrue(result.isFailure)
        val err = result.exceptionOrNull()
        assertTrue(err?.message?.contains("500") == true || err?.message?.contains("Service unavailable") == true)
    }

    // 16. Profit calculation math using integer minor units
    @Test
    fun testProfitCalculationMathMinorUnits() {
        // Selling price: $30.00 (3000 cents)
        // Production cost: $12.50 (1250 cents)
        // Shipping cost: $4.00 (400 cents)
        // Platform fee: 5% of $30.00 = $1.50 (150 cents)
        // Expected margin: 3000 - 1250 - 400 - 150 = 1200 cents ($12.00)
        val breakdown = PrintifyPricingCalculator.calculateBreakdown(
            sellingPriceMinor = 3000L,
            productionCostMinor = 1250L,
            shippingCostMinor = 400L,
            currency = "USD"
        )

        assertEquals(3000L, breakdown.sellingPriceMinor)
        assertEquals(1250L, breakdown.productionCostMinor)
        assertEquals(400L, breakdown.shippingCostMinor)
        assertEquals(150L, breakdown.platformFeesMinor)
        assertEquals(1200L, breakdown.estimatedSellerMarginMinor)
        assertFalse(breakdown.isProductionCostEstimated)

        // When production cost is unavailable (null), seller margin MUST be null (never fake profit)
        val unknownBreakdown = PrintifyPricingCalculator.calculateBreakdown(
            sellingPriceMinor = 3000L,
            productionCostMinor = null,
            shippingCostMinor = null,
            currency = "USD"
        )
        assertNull(unknownBreakdown.productionCostMinor)
        assertNull(unknownBreakdown.estimatedSellerMarginMinor)
        assertEquals("Unavailable", PrintifyPricingCalculator.formatOrUnavailable(unknownBreakdown.productionCostMinor, "USD"))
    }

    // 17. Server secret provider resolution and token sanitization
    @Test
    fun testServerSecretProviderResolutionAndSanitization() {
        PrintifyServerSecretProvider.setTestOverrideKey("live_super_secret_pfy_token_987654")
        assertTrue(PrintifyServerSecretProvider.isApiKeyConfigured())
        assertEquals("live_super_secret_pfy_token_987654", PrintifyServerSecretProvider.getPrintifyApiKey())

        // Ensure sanitization strips secret tokens and headers
        val leakedLog = "Request error with Authorization: Bearer live_super_secret_pfy_token_987654 and PRINTIFY_API_KEY=live_super_secret_pfy_token_987654"
        val sanitized = PrintifyServerSecretProvider.sanitize(leakedLog)
        assertFalse("Sanitized string must not contain raw token", sanitized.contains("live_super_secret_pfy_token_987654"))
        assertTrue(sanitized.contains("[REDACTED_API_KEY]") || sanitized.contains("[REDACTED_TOKEN]"))
    }

    // 18. Safe authentication verification: Success
    @Test
    fun testSafeAuthenticationVerificationSuccess() = runBlocking {
        PrintifyServerSecretProvider.setTestOverrideKey("valid_server_key_123")
        val result = backendService.verifyAuthentication()
        assertTrue(result.isSuccess)
        assertEquals("PRINTIFY_AUTHENTICATION: SUCCESS", result.statusHeader)
        val summary = result.formatSafeSummary()
        assertTrue(summary.contains("PRINTIFY_AUTHENTICATION: SUCCESS"))
        assertTrue(summary.contains("AUTHENTICATED"))
        assertFalse("Summary must never contain raw token", summary.contains("valid_server_key_123"))
    }

    // 19. Safe authentication verification: Failure when unconfigured
    @Test
    fun testSafeAuthenticationVerificationFailureWhenUnconfigured() = runBlocking {
        PrintifyServerSecretProvider.setTestOverrideKey(null)
        val unconfiguredService = PrintifyBackendService(testHttpClient, webhookService)
        val result = unconfiguredService.verifyAuthentication()
        assertFalse(result.isSuccess)
        assertEquals("PRINTIFY_AUTHENTICATION: FAILED", result.statusHeader)
        val summary = result.formatSafeSummary()
        assertTrue(summary.contains("PRINTIFY_AUTHENTICATION: FAILED"))
        assertTrue(summary.contains("AUTHENTICATION_FAILED"))
    }

    private fun createSampleMerchProduct(id: String, ownerUid: String, shopId: String): Product {
        return Product(
            id = id,
            shopId = shopId,
            ownerUid = ownerUid,
            title = "Aesthetic Minimalist Mug",
            description = "Ceramic 11oz on-demand mug",
            price = 24.0,
            businessMode = "MERCH",
            type = ProductType.MERCH,
            inventory = 99,
            finalProductImageUrl = "https://snapbrand.storage/final_artwork.png"
        )
    }
}

/**
 * Controlled Fake Printify HTTP Client for unit testing.
 */
class FakePrintifyHttpClient : PrintifyHttpClient {

    var shouldFailWithUnauthorized = false
    var shouldFailWithRateLimit = false
    var shouldFailWith500 = false

    var createProductCalls = 0
    var createOrderCalls = 0
    var lastCreatedProductPayload: JSONObject? = null

    override suspend fun getShops(token: String): Result<List<PrintifyShopSummary>> {
        if (shouldFailWithUnauthorized) {
            return Result.failure(SecurityException("401 Unauthorized: Invalid API token"))
        }
        return Result.success(listOf(PrintifyShopSummary("pfy_shop_1", "Test Merch Shop")))
    }

    override suspend fun getBlueprints(token: String): Result<List<PrintifyBlueprint>> {
        return Result.success(listOf(PrintifyBlueprint(12, "Ceramic Mug 11oz", "Mugs", "White ceramic mug")))
    }

    override suspend fun getPrintProviders(token: String, blueprintId: Int): Result<List<PrintifyPrintProvider>> {
        return Result.success(listOf(PrintifyPrintProvider(1, "District Photo", "US")))
    }

    override suspend fun getVariants(token: String, blueprintId: Int, providerId: Int): Result<List<PrintifyVariant>> {
        return Result.success(listOf(PrintifyVariant(101, "11oz / White", emptyMap(), 800L, true)))
    }

    override suspend fun getShippingInfo(token: String, blueprintId: Int, providerId: Int): Result<PrintifyShippingEstimate?> {
        return Result.success(PrintifyShippingEstimate(400L, "USD", 3, 5))
    }

    override suspend fun uploadArtwork(token: String, fileName: String, imageUrl: String): Result<String> {
        return Result.success("img_pfy_mock_artwork_123")
    }

    override suspend fun createProduct(token: String, shopId: String, payload: JSONObject): Result<JSONObject> {
        if (shouldFailWithRateLimit) {
            return Result.failure(IllegalStateException("429 Too Many Requests: Rate limit exceeded"))
        }
        if (shouldFailWith500) {
            return Result.failure(IllegalStateException("500 Internal Server Error: Printify service unavailable"))
        }
        createProductCalls++
        lastCreatedProductPayload = payload
        return Result.success(
            JSONObject().apply {
                put("id", "prod_pfy_9999")
                put("status", "draft")
            }
        )
    }

    override suspend fun createOrder(token: String, shopId: String, payload: JSONObject): Result<JSONObject> {
        createOrderCalls++
        return Result.success(
            JSONObject().apply {
                put("id", "order_pfy_8888")
                put("status", "pending")
            }
        )
    }

    override suspend fun getOrder(token: String, shopId: String, orderId: String): Result<JSONObject> {
        return Result.success(
            JSONObject().apply {
                put("id", orderId)
                put("status", "in_production")
            }
        )
    }
}
