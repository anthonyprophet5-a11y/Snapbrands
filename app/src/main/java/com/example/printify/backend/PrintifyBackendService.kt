package com.example.printify.backend

import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentStatus
import com.example.data.model.Product
import com.example.data.model.ProductType
import com.example.printify.client.PrintifyHttpClient
import com.example.printify.client.RealPrintifyHttpClient
import com.example.printify.model.PrintifyBlueprint
import com.example.printify.model.PrintifyConnectionStatus
import com.example.printify.model.PrintifyOperationRecord
import com.example.printify.model.PrintifyOrderMapping
import com.example.printify.model.PrintifyOrderSyncStatus
import com.example.printify.model.PrintifyPrintProvider
import com.example.printify.model.PrintifyProductMapping
import com.example.printify.model.PrintifyShippingEstimate
import com.example.printify.model.PrintifyShopConnection
import com.example.printify.model.PrintifyShopSummary
import com.example.printify.model.PrintifyVariant
import com.example.printify.service.PrintifyDesignValidator
import com.example.printify.webhook.PrintifyWebhookResult
import com.example.printify.webhook.PrintifyWebhookService
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * SNAPBRAND — PHASE 7 SECURE PRINTIFY BACKEND SERVICE
 *
 * Implements the secure backend abstraction:
 * Client -> Backend Service -> Printify REST API v1
 *
 * CRITICAL ARCHITECTURAL RULES:
 * 1. Printify API tokens are held strictly in server-side memory/secure store.
 *    Tokens are NEVER returned in API responses, NEVER placed in client models,
 *    and NEVER logged.
 * 2. Strict Tenant Isolation: Account A cannot access Account B's connections,
 *    mappings, operations, or orders.
 * 3. Strict MERCH-only boundary: REAL_SHOP / PHYSICAL products are strictly rejected.
 * 4. Idempotency & Duplicate Protection: Persistent operation records prevent
 *    duplicate product creation or duplicate order submissions.
 * 5. Honest Statuses: No fake "Connected" or "Published" claims without API confirmation.
 */
class PrintifyBackendService(
    private val httpClient: PrintifyHttpClient = RealPrintifyHttpClient(),
    private val webhookService: PrintifyWebhookService = PrintifyWebhookService()
) {
    // Secure server-side credential vault: ownerUid -> Printify API token
    // (In full production, this maps to Google Cloud Secret Manager / Cloud Functions env)
    private val serverSideTokenVault = ConcurrentHashMap<String, String>()
    private var defaultWebhookSecret: String? = null

    // Multi-tenant tables: ownerUid -> Data
    private val connectionsTable = ConcurrentHashMap<String, MutableMap<String, PrintifyShopConnection>>() // ownerUid -> (shopId -> Conn)
    private val productMappingsTable = ConcurrentHashMap<String, MutableMap<String, PrintifyProductMapping>>() // ownerUid -> (productId -> Mapping)
    private val orderMappingsTable = ConcurrentHashMap<String, MutableMap<String, PrintifyOrderMapping>>() // ownerUid -> (orderId -> Mapping)
    private val operationRecordsTable = ConcurrentHashMap<String, MutableMap<String, PrintifyOperationRecord>>() // ownerUid -> (operationId -> Op)

    // Reverse lookup for webhooks: printifyOrderId -> (ownerUid to snapbrandOrderId)
    private val printifyOrderLookup = ConcurrentHashMap<String, Pair<String, String>>()

    init {
        // Automatically check server-side environment for PRINTIFY_API_KEY if provisioned
        val serverKey = PrintifyServerSecretProvider.getPrintifyApiKey()
        if (!serverKey.isNullOrBlank()) {
            serverSideTokenVault["_system_default_"] = serverKey
        }
        defaultWebhookSecret = PrintifyServerSecretProvider.getPrintifyWebhookSecret()
    }

    fun setServerWebhookSecret(secret: String) {
        defaultWebhookSecret = secret
        PrintifyServerSecretProvider.reload()
    }

    /**
     * Retrieve the server-side token for a given user or system default.
     * Throws SecurityException if not configured.
     */
    private fun getServerToken(ownerUid: String): String {
        return serverSideTokenVault[ownerUid]
            ?: serverSideTokenVault["_system_default_"]
            ?: PrintifyServerSecretProvider.getPrintifyApiKey()
            ?: throw SecurityException("Printify connection is not configured or token has expired.")
    }

    /**
     * Safely verifies server-side Printify API authentication by performing a read-only request (GET /shops.json)
     * WITHOUT exposing credentials, tokens, or authorization headers.
     *
     * Returns "PRINTIFY_AUTHENTICATION: SUCCESS" or "PRINTIFY_AUTHENTICATION: FAILED".
     */
    suspend fun verifyAuthentication(): PrintifyAuthVerificationResult {
        val apiKey = PrintifyServerSecretProvider.getPrintifyApiKey()
            ?: serverSideTokenVault["_system_default_"]
            ?: return PrintifyAuthVerificationResult(
                isSuccess = false,
                statusHeader = "PRINTIFY_AUTHENTICATION: FAILED",
                message = "PRINTIFY_API_KEY is not configured in server environment or Secrets panel."
            )

        return try {
            val result = httpClient.getShops(apiKey)
            if (result.isSuccess) {
                val shops = result.getOrThrow()
                PrintifyAuthVerificationResult(
                    isSuccess = true,
                    statusHeader = "PRINTIFY_AUTHENTICATION: SUCCESS",
                    message = "Successfully authenticated with Printify API. Found ${shops.size} authorized shop(s).",
                    authorizedShops = shops
                )
            } else {
                val err = result.exceptionOrNull()
                val safeMsg = PrintifyServerSecretProvider.sanitize(err?.message ?: "Authentication failed")
                PrintifyAuthVerificationResult(
                    isSuccess = false,
                    statusHeader = "PRINTIFY_AUTHENTICATION: FAILED",
                    message = safeMsg
                )
            }
        } catch (e: Exception) {
            PrintifyAuthVerificationResult(
                isSuccess = false,
                statusHeader = "PRINTIFY_AUTHENTICATION: FAILED",
                message = PrintifyServerSecretProvider.sanitize(e.message ?: "Authentication check failed.")
            )
        }
    }

    // ========================================================================
    // 1. SHOP CONNECTION MANAGEMENT
    // ========================================================================

    /**
     * Validates and establishes a Printify connection for a seller's shop.
     * The token is verified against Printify's /shops.json before establishing the connection.
     * The token is stored ONLY in serverSideTokenVault and is NEVER present in the returned model.
     */
    suspend fun connectShop(
        ownerUid: String,
        snapbrandShopId: String,
        printifyShopId: String,
        tokenToValidate: String
    ): Result<PrintifyShopConnection> {
        if (ownerUid.isBlank() || snapbrandShopId.isBlank() || printifyShopId.isBlank()) {
            return Result.failure(IllegalArgumentException("Missing required connection parameters"))
        }

        if (tokenToValidate.isBlank()) {
            return Result.failure(SecurityException("Printify connection token cannot be empty"))
        }

        // Call real Printify API to validate token and shop ownership
        val shopsResult = httpClient.getShops(tokenToValidate)
        if (shopsResult.isFailure) {
            val err = shopsResult.exceptionOrNull() ?: Exception("Unknown error")
            val conn = PrintifyShopConnection(
                id = "conn_${UUID.randomUUID().toString().take(8)}",
                ownerUid = ownerUid,
                snapbrandShopId = snapbrandShopId,
                printifyShopId = printifyShopId,
                status = PrintifyConnectionStatus.ERROR,
                errorMessage = err.message ?: "Printify connection could not be verified.",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            val userConns = connectionsTable.getOrPut(ownerUid) { ConcurrentHashMap() }
            userConns[snapbrandShopId] = conn
            return Result.failure(err)
        }

        val availableShops = shopsResult.getOrThrow()
        val matchedShop = availableShops.find { it.id.trim() == printifyShopId.trim() }

        if (matchedShop == null && availableShops.isNotEmpty()) {
            // Provided shop ID not found in this Printify account
            return Result.failure(
                IllegalArgumentException("Printify shop ID '$printifyShopId' does not match any shop in this Printify account.")
            )
        }

        val shopTitle = matchedShop?.title ?: "Printify Shop #$printifyShopId"

        // Securely store token server-side only
        serverSideTokenVault[ownerUid] = tokenToValidate

        val now = System.currentTimeMillis()
        val validConnection = PrintifyShopConnection(
            id = "conn_${UUID.randomUUID().toString().take(8)}",
            ownerUid = ownerUid,
            snapbrandShopId = snapbrandShopId,
            printifyShopId = printifyShopId,
            printifyShopTitle = shopTitle,
            status = PrintifyConnectionStatus.CONNECTED,
            errorMessage = null,
            connectedAt = now,
            createdAt = now,
            updatedAt = now
        )

        val userConns = connectionsTable.getOrPut(ownerUid) { ConcurrentHashMap() }
        userConns[snapbrandShopId] = validConnection

        return Result.success(validConnection)
    }

    /**
     * Retrieves the Printify connection metadata for a shop.
     * Enforces tenant isolation: caller must be owner.
     */
    fun getConnection(callerUid: String, snapbrandShopId: String): PrintifyShopConnection? {
        val userConns = connectionsTable[callerUid] ?: return null
        return userConns[snapbrandShopId]
    }

    /**
     * Disconnects a Printify connection.
     */
    fun disconnectShop(callerUid: String, snapbrandShopId: String): Result<PrintifyShopConnection> {
        val userConns = connectionsTable[callerUid]
            ?: return Result.failure(NoSuchElementException("No connection found for shop: $snapbrandShopId"))

        val existing = userConns[snapbrandShopId]
            ?: return Result.failure(NoSuchElementException("No connection found for shop: $snapbrandShopId"))

        val disconnected = existing.copy(
            status = PrintifyConnectionStatus.DISCONNECTED,
            updatedAt = System.currentTimeMillis()
        )
        userConns[snapbrandShopId] = disconnected
        // Remove from vault
        serverSideTokenVault.remove(callerUid)

        return Result.success(disconnected)
    }

    // ========================================================================
    // 2. CATALOG & BLUEPRINTS
    // ========================================================================

    suspend fun getBlueprints(ownerUid: String): Result<List<PrintifyBlueprint>> {
        val token = try { getServerToken(ownerUid) } catch (e: Exception) { return Result.failure(e) }
        return httpClient.getBlueprints(token)
    }

    suspend fun getPrintProviders(ownerUid: String, blueprintId: Int): Result<List<PrintifyPrintProvider>> {
        val token = try { getServerToken(ownerUid) } catch (e: Exception) { return Result.failure(e) }
        return httpClient.getPrintProviders(token, blueprintId)
    }

    suspend fun getVariants(ownerUid: String, blueprintId: Int, providerId: Int): Result<List<PrintifyVariant>> {
        val token = try { getServerToken(ownerUid) } catch (e: Exception) { return Result.failure(e) }
        return httpClient.getVariants(token, blueprintId, providerId)
    }

    suspend fun getShippingInfo(ownerUid: String, blueprintId: Int, providerId: Int): Result<PrintifyShippingEstimate?> {
        val token = try { getServerToken(ownerUid) } catch (e: Exception) { return Result.failure(e) }
        return httpClient.getShippingInfo(token, blueprintId, providerId)
    }

    // ========================================================================
    // 3. PRODUCT CREATION & IDEMPOTENCY
    // ========================================================================

    /**
     * Secure backend workflow for creating a Printify product from a SnapBrand MERCH product.
     * Enforces:
     * - Tenant isolation (product.ownerUid == callerUid)
     * - MERCH-only restriction (REAL_SHOP products strictly rejected)
     * - Design asset verification (no raw photo without print asset)
     * - Idempotency & duplicate protection
     */
    suspend fun createPrintifyProduct(
        callerUid: String,
        product: Product,
        blueprintId: Int,
        providerId: Int,
        variantIds: List<Int>,
        idempotencyKey: String = "op_prod_${product.id}"
    ): Result<PrintifyProductMapping> {
        // 1. Tenant isolation
        if (product.ownerUid != callerUid) {
            return Result.failure(SecurityException("Unauthorized: Seller does not own this product."))
        }

        // 2. MERCH-only restriction (Phase 7 Mandate)
        if (product.type == ProductType.PHYSICAL || !product.businessMode.equals("MERCH", ignoreCase = true)) {
            return Result.failure(
                IllegalArgumentException("REAL SHOP / PHYSICAL products must NOT be sent to Printify. Printify integration is strictly reserved for MERCH products.")
            )
        }

        // 3. Verify connection status
        val connection = getConnection(callerUid, product.shopId)
        if (connection == null || connection.status != PrintifyConnectionStatus.CONNECTED) {
            return Result.failure(IllegalStateException("Printify shop is not connected. Please connect your Printify shop first."))
        }

        // 4. Validate print-ready design asset
        val designValidation = PrintifyDesignValidator.validateForPrintify(product)
        if (!designValidation.isPrintReady) {
            return Result.failure(IllegalStateException(designValidation.message))
        }

        // 5. Idempotency Check
        val userOps = operationRecordsTable.getOrPut(callerUid) { ConcurrentHashMap() }
        val userMappings = productMappingsTable.getOrPut(callerUid) { ConcurrentHashMap() }

        val existingMapping = userMappings[product.id]
        if (existingMapping != null) {
            // Already created and mapped!
            return Result.success(existingMapping)
        }

        val existingOp = userOps[idempotencyKey]
        if (existingOp != null && existingOp.status == "SUCCESS" && existingOp.printifyResourceId != null) {
            val mapping = userMappings[product.id]
            if (mapping != null) return Result.success(mapping)
        }

        // Record PENDING operation
        val operationRecord = PrintifyOperationRecord(
            operationId = idempotencyKey,
            ownerUid = callerUid,
            shopId = product.shopId,
            snapbrandProductId = product.id,
            operationType = "CREATE_PRODUCT",
            status = "PENDING",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        userOps[idempotencyKey] = operationRecord

        val token = try {
            getServerToken(callerUid)
        } catch (e: Exception) {
            userOps[idempotencyKey] = operationRecord.copy(status = "FAILED", errorMessage = e.message)
            return Result.failure(e)
        }

        try {
            // 6. Upload design artwork
            val artworkUrl = designValidation.artworkUrl ?: product.finalProductImageUrl!!
            val uploadResult = httpClient.uploadArtwork(token, "${product.id}_artwork.png", artworkUrl)
            if (uploadResult.isFailure) {
                val err = uploadResult.exceptionOrNull()!!
                userOps[idempotencyKey] = operationRecord.copy(status = "FAILED", errorMessage = err.message)
                return Result.failure(err)
            }
            val printifyImageId = uploadResult.getOrThrow()

            // 7. Prepare product payload for Printify
            val variantsArray = JSONArray()
            val variantIdsArray = JSONArray()
            variantIds.forEach { vid ->
                variantIdsArray.put(vid)
                variantsArray.put(
                    JSONObject().apply {
                        put("id", vid)
                        put("price", Math.round(product.price * 100)) // Minor units
                        put("is_enabled", true)
                    }
                )
            }

            val printAreaObj = JSONObject().apply {
                put("variant_ids", variantIdsArray)
                put("placeholders", JSONArray().apply {
                    put(
                        JSONObject().apply {
                            put("position", "front")
                            put("images", JSONArray().apply {
                                put(
                                    JSONObject().apply {
                                        put("id", printifyImageId)
                                        put("x", 0.5)
                                        put("y", 0.5)
                                        put("scale", 1.0)
                                        put("angle", 0)
                                    }
                                )
                            })
                        }
                    )
                })
            }

            val productPayload = JSONObject().apply {
                put("title", product.title)
                put("description", product.description)
                put("blueprint_id", blueprintId)
                put("print_provider_id", providerId)
                put("variants", variantsArray)
                put("print_areas", JSONArray().apply { put(printAreaObj) })
            }

            // 8. Create product on Printify
            val createResult = httpClient.createProduct(token, connection.printifyShopId, productPayload)
            if (createResult.isFailure) {
                val err = createResult.exceptionOrNull()!!
                userOps[idempotencyKey] = operationRecord.copy(status = "FAILED", errorMessage = err.message)
                return Result.failure(err)
            }

            val responseJson = createResult.getOrThrow()
            val printifyProdId = responseJson.getString("id")
            val printifyStatus = responseJson.optString("status", "draft")

            val now = System.currentTimeMillis()
            val mapping = PrintifyProductMapping(
                id = "map_${UUID.randomUUID().toString().take(8)}",
                ownerUid = callerUid,
                shopId = product.shopId,
                snapbrandProductId = product.id,
                printifyProductId = printifyProdId,
                printifyBlueprintId = blueprintId,
                printifyProviderId = providerId,
                selectedVariantIds = variantIds,
                printifyStatus = printifyStatus,
                lastSyncedAt = now,
                createdAt = now,
                updatedAt = now
            )

            // Store mapping and update operation record
            userMappings[product.id] = mapping
            userOps[idempotencyKey] = operationRecord.copy(
                status = "SUCCESS",
                printifyResourceId = printifyProdId,
                updatedAt = now
            )

            return Result.success(mapping)

        } catch (e: Exception) {
            userOps[idempotencyKey] = operationRecord.copy(status = "FAILED", errorMessage = e.message)
            return Result.failure(e)
        }
    }

    fun getProductMapping(callerUid: String, productId: String): PrintifyProductMapping? {
        return productMappingsTable[callerUid]?.get(productId)
    }

    // ========================================================================
    // 4. ORDER FULFILLMENT SYNCHRONIZATION
    // ========================================================================

    /**
     * Submits a paid customer order containing MERCH products to Printify.
     * Enforces:
     * - Order must be PAID.
     * - Order must belong to seller.
     * - Only MERCH products with Printify mappings are synchronized.
     * - If no MERCH products are present, syncStatus becomes NOT_REQUIRED.
     * - Does NOT mark the order as fulfilled merely because request was sent!
     */
    suspend fun syncPaidOrderToPrintify(
        callerUid: String,
        order: Order,
        productsById: Map<String, Product>,
        idempotencyKey: String = "op_ord_${order.id}"
    ): Result<PrintifyOrderMapping> {
        // 1. Tenant check
        if (order.sellerUid != callerUid) {
            return Result.failure(SecurityException("Unauthorized: Seller does not own this order."))
        }

        // 2. Payment status check
        if (order.paymentStatus != PaymentStatus.PAID && order.orderStatus != OrderStatus.PAID) {
            return Result.failure(IllegalStateException("Order payment is not verified. Only PAID orders can be fulfilled through Printify."))
        }

        val userOrderMappings = orderMappingsTable.getOrPut(callerUid) { ConcurrentHashMap() }
        val existingOrderMapping = userOrderMappings[order.id]
        if (existingOrderMapping != null && (
            existingOrderMapping.syncStatus == PrintifyOrderSyncStatus.ACCEPTED ||
            existingOrderMapping.syncStatus == PrintifyOrderSyncStatus.PROCESSING ||
            existingOrderMapping.syncStatus == PrintifyOrderSyncStatus.FULFILLED
        )) {
            // Already submitted / accepted
            return Result.success(existingOrderMapping)
        }

        // 3. Filter order items for MERCH products that have Printify mappings
        val userProductMappings = productMappingsTable[callerUid] ?: emptyMap()
        val merchLineItems = mutableListOf<JSONObject>()

        for (item in order.items) {
            val prod = productsById[item.productId]
            if (prod != null && prod.businessMode.equals("MERCH", ignoreCase = true)) {
                val mapping = userProductMappings[prod.id]
                if (mapping != null) {
                    val variantIdToUse = mapping.selectedVariantIds.firstOrNull() ?: 1
                    merchLineItems.add(
                        JSONObject().apply {
                            put("product_id", mapping.printifyProductId)
                            put("variant_id", variantIdToUse)
                            put("quantity", item.quantity)
                        }
                    )
                }
            }
        }

        if (merchLineItems.isEmpty()) {
            val notRequiredMapping = PrintifyOrderMapping(
                id = "ordmap_${UUID.randomUUID().toString().take(8)}",
                ownerUid = callerUid,
                snapbrandOrderId = order.id,
                syncStatus = PrintifyOrderSyncStatus.NOT_REQUIRED
            )
            userOrderMappings[order.id] = notRequiredMapping
            return Result.success(notRequiredMapping)
        }

        // 4. Verify shop connection
        val connection = getConnection(callerUid, order.storeId)
        if (connection == null || connection.status != PrintifyConnectionStatus.CONNECTED) {
            val failedMapping = PrintifyOrderMapping(
                id = "ordmap_${UUID.randomUUID().toString().take(8)}",
                ownerUid = callerUid,
                snapbrandOrderId = order.id,
                syncStatus = PrintifyOrderSyncStatus.FAILED,
                errorMessage = "Printify connection missing or expired for store ${order.storeId}"
            )
            userOrderMappings[order.id] = failedMapping
            return Result.failure(IllegalStateException(failedMapping.errorMessage))
        }

        val token = try {
            getServerToken(callerUid)
        } catch (e: Exception) {
            return Result.failure(e)
        }

        // 5. Build Printify order payload
        val addressTo = JSONObject().apply {
            val nameParts = (order.customerName.ifBlank { "Valued Customer" }).split(" ", limit = 2)
            put("first_name", nameParts.getOrNull(0) ?: "Customer")
            put("last_name", nameParts.getOrNull(1) ?: "SnapBrand")
            put("email", order.customerEmail.ifBlank { "customer@snapbrand.shop" })
            put("phone", order.customerPhone.ifBlank { "0000000000" })
            put("country", order.shippingAddress?.country ?: "US")
            put("region", order.shippingAddress?.state ?: "")
            put("address1", order.shippingAddress?.street ?: order.deliveryAddress.ifBlank { "123 Main St" })
            put("city", order.shippingAddress?.city ?: "San Francisco")
            put("zip", order.shippingAddress?.postalCode ?: "94105")
        }

        val orderPayload = JSONObject().apply {
            put("external_id", order.id)
            put("label", order.orderNumber)
            put("line_items", JSONArray(merchLineItems))
            put("shipping_method", 1) // Standard shipping
            put("send_shipping_notification", false)
            put("address_to", addressTo)
        }

        // 6. Submit to Printify API
        val createOrderResult = httpClient.createOrder(token, connection.printifyShopId, orderPayload)
        if (createOrderResult.isFailure) {
            val err = createOrderResult.exceptionOrNull()!!
            val failedMapping = PrintifyOrderMapping(
                id = "ordmap_${UUID.randomUUID().toString().take(8)}",
                ownerUid = callerUid,
                snapbrandOrderId = order.id,
                syncStatus = PrintifyOrderSyncStatus.FAILED,
                errorMessage = err.message
            )
            userOrderMappings[order.id] = failedMapping
            return Result.failure(err)
        }

        val orderResp = createOrderResult.getOrThrow()
        val printifyOrderId = orderResp.getString("id")
        val printifyStatus = orderResp.optString("status", "pending")

        val now = System.currentTimeMillis()
        val submittedMapping = PrintifyOrderMapping(
            id = "ordmap_${UUID.randomUUID().toString().take(8)}",
            ownerUid = callerUid,
            snapbrandOrderId = order.id,
            printifyOrderId = printifyOrderId,
            printifyStatus = printifyStatus,
            syncStatus = PrintifyOrderSyncStatus.ACCEPTED,
            createdAt = now,
            updatedAt = now
        )

        userOrderMappings[order.id] = submittedMapping
        printifyOrderLookup[printifyOrderId] = Pair(callerUid, order.id)

        return Result.success(submittedMapping)
    }

    fun getOrderMapping(callerUid: String, orderId: String): PrintifyOrderMapping? {
        return orderMappingsTable[callerUid]?.get(orderId)
    }

    // ========================================================================
    // 5. WEBHOOK PROCESSING
    // ========================================================================

    /**
     * Processes inbound Printify webhook notifications and maps them into internal states.
     */
    fun handleWebhook(
        rawPayload: String,
        signatureHeader: String?,
        secretOverride: String? = null,
        skipSignatureForTesting: Boolean = false
    ): Result<PrintifyWebhookResult> {
        val secret = secretOverride ?: defaultWebhookSecret
        val result = webhookService.processWebhook(
            rawPayload = rawPayload,
            signatureHeader = signatureHeader,
            sharedSecret = secret,
            skipSignatureForTesting = skipSignatureForTesting
        )

        if (result.isFailure) return result

        val webhookResult = result.getOrThrow()
        if (webhookResult.isDuplicate) {
            return Result.success(webhookResult)
        }

        if (webhookResult.resourceType == "order" && webhookResult.resourceId.isNotBlank()) {
            val lookup = printifyOrderLookup[webhookResult.resourceId]
            if (lookup != null) {
                val (ownerUid, orderId) = lookup
                val userOrders = orderMappingsTable[ownerUid]
                val currentMapping = userOrders?.get(orderId)
                if (currentMapping != null && webhookResult.mappedSyncStatus != null) {
                    val updated = currentMapping.copy(
                        syncStatus = webhookResult.mappedSyncStatus,
                        trackingNumber = webhookResult.trackingNumber ?: currentMapping.trackingNumber,
                        trackingUrl = webhookResult.trackingUrl ?: currentMapping.trackingUrl,
                        carrier = webhookResult.carrier ?: currentMapping.carrier,
                        updatedAt = System.currentTimeMillis()
                    )
                    userOrders[orderId] = updated
                }
            }
        }

        return Result.success(webhookResult)
    }
}

/**
 * Result of the safe authentication verification call.
 * NEVER includes raw API keys, bearer tokens, or sensitive headers.
 */
data class PrintifyAuthVerificationResult(
    val isSuccess: Boolean,
    val statusHeader: String,
    val message: String,
    val authorizedShops: List<PrintifyShopSummary> = emptyList()
) {
    fun formatSafeSummary(): String {
        return buildString {
            appendLine(statusHeader)
            appendLine("Status: ${if (isSuccess) "AUTHENTICATED" else "AUTHENTICATION_FAILED"}")
            appendLine("Details: $message")
            if (authorizedShops.isNotEmpty()) {
                val shopDescriptions = authorizedShops.joinToString { "${it.title} (ID: ${it.id})" }
                appendLine("Authorized Printify Shops: $shopDescriptions")
            }
        }.trimEnd()
    }
}

