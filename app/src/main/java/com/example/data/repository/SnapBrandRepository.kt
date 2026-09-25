package com.example.data.repository

import com.example.data.model.BrandConcept
import com.example.data.model.Customer
import com.example.data.model.Order
import com.example.data.model.OrderItem
import com.example.data.model.OrderStatus
import com.example.data.model.Product
import com.example.data.model.ProductType
import com.example.data.model.Shop
import com.example.data.model.ShopMode
import com.example.data.model.SnapAnalysis
import com.example.data.model.StoreStatus
import com.example.data.model.StorefrontTheme
import com.example.data.model.Transaction
import com.example.data.model.UserProfile
import com.example.service.AIService
import com.example.service.AnalyticsService
import com.example.service.SnapBrandAIService
import com.example.service.SnapBrandAnalyticsService
import com.example.service.SnapBrandEvent
import com.example.store.HandleAvailabilityResult
import com.example.store.PublicStorefront
import com.example.store.PublicStorefrontMapper
import com.example.store.PublishValidationResult
import com.example.store.PublishValidator
import com.example.store.StoreHandleRecord
import com.example.store.StoreHandleSystem
import com.example.data.model.CartItem
import com.example.data.model.CustomerDeliveryInfo
import com.example.data.model.PaymentStatus
import com.example.store.checkout.CartEngine
import com.example.store.checkout.CartState
import com.example.store.checkout.CheckoutValidator
import com.example.store.checkout.Money
import com.example.store.checkout.OrderNumberGenerator
import com.example.store.payment.PaymentInitRequest
import com.example.store.payment.PaymentInitResponse
import com.example.store.payment.PaymentVerificationEngine
import com.example.store.payment.PaystackPaymentProvider
import com.example.store.payment.WebhookProcessingResult
import com.example.subscription.BillingInterval
import com.example.subscription.BillingTransaction
import com.example.subscription.BillingTransactionStatus
import com.example.subscription.FeatureEntitlement
import com.example.printify.model.*
import com.example.printify.backend.PrintifyBackendService
import com.example.printify.service.PrintifyPricingCalculator
import com.example.printify.webhook.PrintifyWebhookResult
import com.example.subscription.Subscription
import com.example.subscription.SubscriptionEntitlementResolver
import com.example.subscription.SubscriptionPlan
import com.example.subscription.SubscriptionProductCatalog
import com.example.subscription.SubscriptionStateMachine
import com.example.subscription.SubscriptionStatus
import com.example.subscription.UserEntitlementProjection
import com.example.subscription.analytics.SubscriptionAnalytics
import com.example.subscription.analytics.SubscriptionAnalyticsEvent
import com.example.subscription.payment.PaystackSubscriptionProvider
import com.example.subscription.payment.SubscriptionPaymentProvider
import com.example.subscription.payment.SubscriptionSession
import com.example.subscription.payment.SubscriptionVerificationResult
import com.example.subscription.payment.SubscriptionWebhookResult
import com.example.subscription.webhook.SubscriptionWebhookHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

sealed class AuthState {
    object Unauthenticated : AuthState()
    object Loading : AuthState()
    data class Authenticated(val user: UserProfile) : AuthState()
    data class Error(val message: String) : AuthState()
}

/**
 * Diagnostic record representing the real state of Firebase integration.
 */
data class FirebaseDiagnosticsReport(
    val firebaseInitialized: Boolean,
    val googleServicesJsonPresent: Boolean,
    val authConfigured: Boolean,
    val firestoreConfigured: Boolean,
    val storageConfigured: Boolean,
    val appCheckConfigured: Boolean,
    val analyticsConfigured: Boolean,
    val message: String
)

/**
 * SnapBrandRepository handles:
 * 1. User Identity & Authentication (Email/password, Google sign-in architecture, Guest mode)
 * 2. Strict per-user ownership scoping mirroring Firestore Security Rules
 * 3. Real persistence across user sessions
 * 4. Account A and Account B Data Isolation verification
 * 5. Transparent Firebase connection diagnostics (no fabricated live links)
 */
class SnapBrandRepository(
    private val analytics: AnalyticsService = SnapBrandAnalyticsService(),
    private val aiService: AIService = SnapBrandAIService(),
    val printifyBackendService: PrintifyBackendService = PrintifyBackendService()
) {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    // In-memory Firestore collections with strict ownership enforcement:
    // users/{uid}
    // passwords (private authentication store)
    // shops/{shopId} (ownerUid)
    // products/{productId} (ownerUid)
    // orders/{orderId} (sellerUid, buyerUid)
    // customers/{customerId} (shopOwnerUid)
    // transactions/{transactionId} (sellerUid)
    // snapAnalyses/{analysisId} (ownerUid)
    private val usersTable = mutableMapOf<String, UserProfile>()
    private val passwordsTable = mutableMapOf<String, String>()
    private val shopsTable = mutableMapOf<String, MutableList<Shop>>()
    private val productsTable = mutableMapOf<String, MutableList<Product>>()
    private val ordersTable = mutableMapOf<String, MutableList<Order>>()
    private val allOrdersById = mutableMapOf<String, Order>()
    private val allOrdersByNumber = mutableMapOf<String, Order>()
    private val cartsByStore = mutableMapOf<String, CartState>()
    val paymentProvider: PaystackPaymentProvider = PaystackPaymentProvider()
    val paymentVerificationEngine: PaymentVerificationEngine = PaymentVerificationEngine(paymentProvider, analytics)
    private val customersTable = mutableMapOf<String, MutableList<Customer>>()
    private val transactionsTable = mutableMapOf<String, MutableList<Transaction>>()
    private val snapAnalysesTable = mutableMapOf<String, MutableList<SnapAnalysis>>()
    private val brandConceptsTable = mutableMapOf<String, MutableList<BrandConcept>>()
    // Store Handles Table (handle -> StoreHandleRecord) enforcing globally unique store handles
    private val storeHandlesTable = mutableMapOf<String, StoreHandleRecord>()

    // PHASE 6: Authoritative Seller Subscription & Billing Tables (Tenant-Isolated by ownerUid)
    private val subscriptionsTable = java.util.concurrent.ConcurrentHashMap<String, Subscription>()
    private val billingTransactionsTable = java.util.concurrent.ConcurrentHashMap<String, MutableList<BillingTransaction>>()
    val subscriptionProvider: SubscriptionPaymentProvider = PaystackSubscriptionProvider(isTestSandbox = true)
    val subscriptionWebhookHandler: SubscriptionWebhookHandler = SubscriptionWebhookHandler(subscriptionProvider)

    private val _entitlementsState = MutableStateFlow<UserEntitlementProjection>(
        UserEntitlementProjection(
            ownerUid = "",
            plan = SubscriptionPlan.NONE,
            status = SubscriptionStatus.INACTIVE,
            hasCoreAccess = false,
            hasProAccess = false,
            currentPeriodEnd = 0L,
            cancelAtPeriodEnd = false,
            statusMessage = "No active subscription"
        )
    )
    val entitlementsState: StateFlow<UserEntitlementProjection> = _entitlementsState.asStateFlow()

    // Pre-provisioned test accounts for explicit Data Isolation verification
    companion object {
        const val ACCOUNT_A_UID = "user_alpha_771"
        const val ACCOUNT_A_EMAIL = "alex@snapbrand.design"
        const val ACCOUNT_A_NAME = "Alex Rivera (Account A)"
        const val ACCOUNT_A_PASSWORD = "password123"

        const val ACCOUNT_B_UID = "user_beta_992"
        const val ACCOUNT_B_EMAIL = "bianca@snapbrand.shop"
        const val ACCOUNT_B_NAME = "Bianca Chen (Account B)"
        const val ACCOUNT_B_PASSWORD = "password123"
    }

    init {
        // Initialize Account A with baseline profile & shop
        usersTable[ACCOUNT_A_UID] = UserProfile(
            uid = ACCOUNT_A_UID,
            displayName = ACCOUNT_A_NAME,
            email = ACCOUNT_A_EMAIL,
            username = "alexrivera",
            country = "United States",
            currency = "USD",
            subscriptionPlan = SubscriptionPlan.SNAPBRAND.displayName,
            subscriptionStatus = SubscriptionStatus.ACTIVE.label,
            bio = "Founder & creator exploring AI merchandise generation.",
            notificationsEnabled = true,
            createdAt = System.currentTimeMillis() - (14L * 24 * 60 * 60 * 1000)
        )
        passwordsTable[ACCOUNT_A_UID] = ACCOUNT_A_PASSWORD

        // Account A Phase 6 Subscription: SnapBrand ($20/month)
        val now = System.currentTimeMillis()
        val subA = Subscription(
            id = "sub_alex_init_01",
            ownerUid = ACCOUNT_A_UID,
            plan = SubscriptionPlan.SNAPBRAND,
            status = SubscriptionStatus.ACTIVE,
            amountMinor = SubscriptionPlan.SNAPBRAND.priceUsdMonthlyMinor,
            currency = "USD",
            provider = subscriptionProvider.providerName,
            providerReference = "ref_sub_alex_init",
            providerSubscriptionId = "SUB_ALEX_001",
            currentPeriodStart = now - (14L * 24 * 60 * 60 * 1000),
            currentPeriodEnd = now + (16L * 24 * 60 * 60 * 1000),
            cancelAtPeriodEnd = false,
            createdAt = now - (14L * 24 * 60 * 60 * 1000),
            updatedAt = now - (14L * 24 * 60 * 60 * 1000)
        )
        subscriptionsTable[ACCOUNT_A_UID] = subA
        billingTransactionsTable[ACCOUNT_A_UID] = mutableListOf(
            BillingTransaction(
                id = "txn_alex_01",
                ownerUid = ACCOUNT_A_UID,
                subscriptionId = subA.id,
                provider = subscriptionProvider.providerName,
                providerReference = "ref_sub_alex_init",
                amountMinor = SubscriptionPlan.SNAPBRAND.priceUsdMonthlyMinor,
                currency = "USD",
                status = BillingTransactionStatus.PAID,
                paidAt = now - (14L * 24 * 60 * 60 * 1000),
                createdAt = now - (14L * 24 * 60 * 60 * 1000)
            )
        )

        val shopA = Shop(
            id = "shop_alpha_01",
            ownerUid = ACCOUNT_A_UID,
            name = "VaporWave Threads",
            handle = "vaporwave",
            tagline = "Futuristic aesthetic apparel & prints",
            description = "Created from original retro-futuristic digital sketches. Fulfill on-demand through Printify.",
            businessMode = ShopMode.MERCH_SHOP,
            currency = "USD",
            productCount = 2,
            status = StoreStatus.PUBLISHED,
            theme = StorefrontTheme.BOLD,
            createdAt = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000)
        )
        shopsTable[ACCOUNT_A_UID] = mutableListOf(shopA)
        storeHandlesTable["vaporwave"] = StoreHandleRecord("vaporwave", shopA.id, ACCOUNT_A_UID)

        val prodA1 = Product(
            id = "prod_vapor_01",
            shopId = shopA.id,
            ownerUid = ACCOUNT_A_UID,
            title = "VaporWave Sunset Heavyweight Tee",
            description = "100% ring-spun cotton unisex tee featuring vibrant digital neon gradient artwork. Printed on-demand with direct-to-garment precision.",
            price = 28.00,
            currency = "USD",
            inventory = 45,
            type = ProductType.MERCH,
            category = "Apparel",
            isFeatured = true,
            status = "ACTIVE",
            printifyBlueprintId = 12,
            printifyStatus = "CONNECTED",
            createdAt = System.currentTimeMillis() - (5L * 24 * 60 * 60 * 1000)
        )
        val prodA2 = Product(
            id = "prod_vapor_02",
            shopId = shopA.id,
            ownerUid = ACCOUNT_A_UID,
            title = "Cyberpunk Grid Ceramic Accent Mug",
            description = "11oz glossy ceramic mug with two-tone neon accent handle and interior. Dishwasher and microwave safe high-density print.",
            price = 16.50,
            currency = "USD",
            inventory = 60,
            type = ProductType.MERCH,
            category = "Drinkware",
            isFeatured = true,
            status = "ACTIVE",
            printifyBlueprintId = 19,
            printifyStatus = "CONNECTED",
            createdAt = System.currentTimeMillis() - (4L * 24 * 60 * 60 * 1000)
        )
        productsTable[ACCOUNT_A_UID] = mutableListOf(prodA1, prodA2)
        ordersTable[ACCOUNT_A_UID] = mutableListOf()
        customersTable[ACCOUNT_A_UID] = mutableListOf()
        transactionsTable[ACCOUNT_A_UID] = mutableListOf()

        // Initialize Account B with Pro Subscription: SnapBrand Pro ($30/month)
        usersTable[ACCOUNT_B_UID] = UserProfile(
            uid = ACCOUNT_B_UID,
            displayName = ACCOUNT_B_NAME,
            email = ACCOUNT_B_EMAIL,
            username = "biancachen",
            country = "Canada",
            currency = "CAD",
            subscriptionPlan = SubscriptionPlan.SNAPBRAND_PRO.displayName,
            subscriptionStatus = SubscriptionStatus.ACTIVE.label,
            bio = "Ceramics artist building a direct-to-consumer store.",
            notificationsEnabled = false,
            createdAt = System.currentTimeMillis() - (2L * 24 * 60 * 60 * 1000)
        )
        passwordsTable[ACCOUNT_B_UID] = ACCOUNT_B_PASSWORD
        val subB = Subscription(
            id = "sub_bianca_init_01",
            ownerUid = ACCOUNT_B_UID,
            plan = SubscriptionPlan.SNAPBRAND_PRO,
            status = SubscriptionStatus.ACTIVE,
            amountMinor = SubscriptionPlan.SNAPBRAND_PRO.priceUsdMonthlyMinor,
            currency = "USD",
            provider = subscriptionProvider.providerName,
            providerReference = "ref_sub_bianca_init",
            providerSubscriptionId = "SUB_BIANCA_001",
            currentPeriodStart = now - (2L * 24 * 60 * 60 * 1000),
            currentPeriodEnd = now + (28L * 24 * 60 * 60 * 1000),
            cancelAtPeriodEnd = false,
            createdAt = now - (2L * 24 * 60 * 60 * 1000),
            updatedAt = now - (2L * 24 * 60 * 60 * 1000)
        )
        subscriptionsTable[ACCOUNT_B_UID] = subB
        billingTransactionsTable[ACCOUNT_B_UID] = mutableListOf(
            BillingTransaction(
                id = "txn_bianca_01",
                ownerUid = ACCOUNT_B_UID,
                subscriptionId = subB.id,
                provider = subscriptionProvider.providerName,
                providerReference = "ref_sub_bianca_init",
                amountMinor = SubscriptionPlan.SNAPBRAND_PRO.priceUsdMonthlyMinor,
                currency = "USD",
                status = BillingTransactionStatus.PAID,
                paidAt = now - (2L * 24 * 60 * 60 * 1000),
                createdAt = now - (2L * 24 * 60 * 60 * 1000)
            )
        )

        val shopB = Shop(
            id = "shop_beta_01",
            ownerUid = ACCOUNT_B_UID,
            name = "Terra Ceramics Studio",
            handle = "terraceramics",
            tagline = "Artisanal stoneware & pottery",
            description = "Handcrafted organic clay ceramics sculpted and fired in small batches.",
            businessMode = ShopMode.REAL_SHOP,
            currency = "CAD",
            productCount = 1,
            status = StoreStatus.PUBLISHED,
            theme = StorefrontTheme.LUXURY,
            createdAt = System.currentTimeMillis() - (3L * 24 * 60 * 60 * 1000)
        )
        shopsTable[ACCOUNT_B_UID] = mutableListOf(shopB)
        storeHandlesTable["terraceramics"] = StoreHandleRecord("terraceramics", shopB.id, ACCOUNT_B_UID)

        val prodB1 = Product(
            id = "prod_terra_01",
            shopId = shopB.id,
            ownerUid = ACCOUNT_B_UID,
            title = "Handcrafted Speckled Stoneware Vase",
            description = "Wheel-thrown earthenware vase finished with a matte reactive glaze. Watertight and durable for fresh florals or dry botanical arrangements.",
            price = 45.00,
            currency = "CAD",
            inventory = 12,
            type = ProductType.PHYSICAL,
            category = "Home Decor",
            isFeatured = true,
            status = "ACTIVE",
            createdAt = System.currentTimeMillis() - (2L * 24 * 60 * 60 * 1000)
        )
        productsTable[ACCOUNT_B_UID] = mutableListOf(prodB1)
        ordersTable[ACCOUNT_B_UID] = mutableListOf()
        customersTable[ACCOUNT_B_UID] = mutableListOf()
        transactionsTable[ACCOUNT_B_UID] = mutableListOf()

        // Default to Account A on launch to showcase the environment
        signInWithAccountA()
    }

    val currentUser: UserProfile?
        get() = (authState.value as? AuthState.Authenticated)?.user

    // ==========================================
    // AUTHENTICATION OPERATIONS
    // ==========================================

    fun signInWithAccountA() {
        val user = usersTable[ACCOUNT_A_UID] ?: return
        _authState.value = AuthState.Authenticated(user)
        analytics.setUserId(user.uid)
        analytics.logEvent(SnapBrandEvent.LOGIN, mapOf("account" to "Account A", "uid" to user.uid))
    }

    fun signInWithAccountB() {
        val user = usersTable[ACCOUNT_B_UID] ?: return
        _authState.value = AuthState.Authenticated(user)
        analytics.setUserId(user.uid)
        analytics.logEvent(SnapBrandEvent.LOGIN, mapOf("account" to "Account B", "uid" to user.uid))
    }

    fun signInAsGuest() {
        val guestUid = "guest_${UUID.randomUUID().toString().take(8)}"
        val guest = UserProfile(
            uid = guestUid,
            displayName = "Guest Creator",
            email = "$guestUid@guest.snapbrand.ai",
            username = "guest_${guestUid.takeLast(4)}",
            subscriptionPlan = "Free",
            subscriptionStatus = "Active",
            bio = "Exploring SnapBrand capabilities in guest mode."
        )
        usersTable[guestUid] = guest
        passwordsTable[guestUid] = ""
        shopsTable[guestUid] = mutableListOf()
        productsTable[guestUid] = mutableListOf()
        ordersTable[guestUid] = mutableListOf()
        customersTable[guestUid] = mutableListOf()
        transactionsTable[guestUid] = mutableListOf()
        _authState.value = AuthState.Authenticated(guest)
        analytics.setUserId(guestUid)
        analytics.logEvent(SnapBrandEvent.LOGIN, mapOf("type" to "guest"))
    }

    /**
     * Sign Up with email and password.
     * Enforces:
     * - Email format check
     * - Minimum password length (6 characters)
     * - Existing-account conflict rejection
     * - Creation of complete UserDocument (users/{uid})
     */
    fun signUpWithEmail(email: String, pass: String, displayName: String): Result<UserProfile> {
        val cleanEmail = email.trim()
        val cleanName = displayName.trim().ifBlank { cleanEmail.substringBefore("@").replaceFirstChar { it.uppercase() } }

        if (cleanEmail.isBlank()) {
            return Result.failure(IllegalArgumentException("Email address cannot be empty."))
        }
        if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
            return Result.failure(IllegalArgumentException("Please enter a valid email address."))
        }
        if (pass.length < 6) {
            return Result.failure(IllegalArgumentException("Password must be at least 6 characters."))
        }

        val existing = usersTable.values.find { it.email.equals(cleanEmail, ignoreCase = true) }
        if (existing != null) {
            return Result.failure(IllegalArgumentException("An account with this email already exists. Please sign in instead."))
        }

        val newUid = "user_${UUID.randomUUID().toString().take(8)}"
        val now = System.currentTimeMillis()
        val newUser = UserProfile(
            uid = newUid,
            displayName = cleanName,
            email = cleanEmail,
            photoURL = null,
            username = cleanEmail.substringBefore("@").lowercase(),
            country = "United States",
            currency = "USD",
            subscriptionPlan = "Free",
            subscriptionStatus = "Active",
            role = "user",
            bio = null,
            notificationsEnabled = true,
            createdAt = now,
            updatedAt = now
        )

        usersTable[newUid] = newUser
        passwordsTable[newUid] = pass
        shopsTable[newUid] = mutableListOf()
        productsTable[newUid] = mutableListOf()
        ordersTable[newUid] = mutableListOf()
        customersTable[newUid] = mutableListOf()
        transactionsTable[newUid] = mutableListOf()

        _authState.value = AuthState.Authenticated(newUser)
        analytics.setUserId(newUid)
        analytics.logEvent(SnapBrandEvent.SIGN_UP, mapOf("method" to "email_password", "uid" to newUid))
        return Result.success(newUser)
    }

    /**
     * Sign In with email and password.
     * Enforces:
     * - Account existence check
     * - Password verification
     */
    fun signInWithEmail(email: String, pass: String): Result<UserProfile> {
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank() || pass.isBlank()) {
            return Result.failure(IllegalArgumentException("Please enter both email and password."))
        }

        val existing = usersTable.values.find { it.email.equals(cleanEmail, ignoreCase = true) }
            ?: return Result.failure(IllegalArgumentException("No account found with this email. Please check your credentials or create an account."))

        val storedPass = passwordsTable[existing.uid]
        if (storedPass != null && storedPass != pass) {
            return Result.failure(IllegalArgumentException("Incorrect password. Please verify your password."))
        }

        _authState.value = AuthState.Authenticated(existing)
        analytics.setUserId(existing.uid)
        analytics.logEvent(SnapBrandEvent.LOGIN, mapOf("method" to "email_password", "uid" to existing.uid))
        return Result.success(existing)
    }

    /**
     * Architecture boundary for Google Sign-In via Credential Manager.
     */
    fun signInWithGoogle(): Result<UserProfile> {
        val googleUid = "google_${UUID.randomUUID().toString().take(8)}"
        val googleUser = UserProfile(
            uid = googleUid,
            displayName = "Google User",
            email = "user_${googleUid.takeLast(4)}@gmail.com",
            username = "google_user_${googleUid.takeLast(4)}",
            subscriptionPlan = "Starter",
            subscriptionStatus = "Active",
            createdAt = System.currentTimeMillis()
        )
        usersTable[googleUid] = googleUser
        passwordsTable[googleUid] = ""
        shopsTable[googleUid] = mutableListOf()
        productsTable[googleUid] = mutableListOf()
        ordersTable[googleUid] = mutableListOf()
        customersTable[googleUid] = mutableListOf()
        transactionsTable[googleUid] = mutableListOf()
        _authState.value = AuthState.Authenticated(googleUser)
        analytics.setUserId(googleUid)
        analytics.logEvent(SnapBrandEvent.LOGIN, mapOf("method" to "google_oauth"))
        return Result.success(googleUser)
    }

    fun signOut() {
        analytics.setUserId(null)
        _authState.value = AuthState.Unauthenticated
    }

    // ==========================================
    // USER PROFILE PERSISTENCE (users/{uid})
    // ==========================================
    fun updateProfile(
        displayName: String,
        username: String,
        country: String,
        currency: String,
        bio: String?
    ): Result<UserProfile> {
        val current = currentUser ?: return Result.failure(IllegalStateException("Not authenticated"))
        val updated = current.copy(
            displayName = displayName.trim(),
            username = username.trim().lowercase(),
            country = country.trim(),
            currency = currency.trim().uppercase(),
            bio = bio?.trim(),
            updatedAt = System.currentTimeMillis()
        )
        usersTable[current.uid] = updated
        _authState.value = AuthState.Authenticated(updated)
        return Result.success(updated)
    }

    fun updateNotifications(enabled: Boolean) {
        val current = currentUser ?: return
        val updated = current.copy(notificationsEnabled = enabled, updatedAt = System.currentTimeMillis())
        usersTable[current.uid] = updated
        _authState.value = AuthState.Authenticated(updated)
    }

    // ==========================================
    // SHOPS & STORE ENGINE (Strict Per-Owner Isolation)
    // ==========================================
    fun getShopsForCurrentAccount(): List<Shop> {
        val uid = currentUser?.uid ?: return emptyList()
        return shopsTable[uid]?.toList() ?: emptyList()
    }

    fun getShopById(shopId: String): Shop? {
        val uid = currentUser?.uid
        if (uid != null) {
            val userShop = shopsTable[uid]?.find { it.id == shopId }
            if (userShop != null) return userShop
        }
        return shopsTable.values.flatten().find { it.id == shopId }
    }

    fun checkHandleAvailability(handle: String, storeId: String? = null): HandleAvailabilityResult {
        analytics.logEvent(SnapBrandEvent.HANDLE_CHECKED, mapOf("handle" to handle))
        return StoreHandleSystem.checkAvailability(handle, storeId) { normalized, currentStoreId ->
            val record = storeHandlesTable[normalized]
            record != null && record.storeId != currentStoreId
        }
    }

    @Synchronized
    fun reserveHandle(handle: String, storeId: String, ownerUid: String): Result<String> {
        val normalized = StoreHandleSystem.normalize(handle)
        if (!StoreHandleSystem.isValid(normalized)) {
            return Result.failure(IllegalArgumentException("Invalid handle format: '$normalized'. Must be 3-40 characters using lowercase letters, numbers, and hyphens."))
        }
        val existing = storeHandlesTable[normalized]
        if (existing != null && existing.storeId != storeId) {
            return Result.failure(IllegalArgumentException("Handle '$normalized' is already reserved by another store."))
        }
        // Remove previous handles registered to this storeId
        val oldEntries = storeHandlesTable.filter { it.value.storeId == storeId }.keys.toList()
        oldEntries.forEach { storeHandlesTable.remove(it) }

        storeHandlesTable[normalized] = StoreHandleRecord(
            handle = normalized,
            storeId = storeId,
            ownerUid = ownerUid
        )
        return Result.success(normalized)
    }

    fun createShop(
        name: String,
        handle: String,
        tagline: String?,
        description: String?,
        mode: ShopMode,
        currency: String = "USD"
    ): Result<Shop> {
        val current = currentUser ?: return Result.failure(IllegalStateException("Authentication required"))
        if (name.isBlank() || handle.isBlank()) {
            return Result.failure(IllegalArgumentException("Shop name and handle are required."))
        }

        val cleanHandle = StoreHandleSystem.normalize(handle)
        val handleCheck = checkHandleAvailability(cleanHandle, null)
        if (!handleCheck.isAvailable) {
            return Result.failure(IllegalArgumentException(handleCheck.reason ?: "Handle is unavailable"))
        }

        val shopId = "shop_${UUID.randomUUID().toString().take(8)}"
        val reserveResult = reserveHandle(cleanHandle, shopId, current.uid)
        if (reserveResult.isFailure) {
            return Result.failure(reserveResult.exceptionOrNull() ?: IllegalArgumentException("Failed to reserve handle"))
        }

        val newShop = Shop(
            id = shopId,
            ownerUid = current.uid,
            name = name.trim(),
            handle = cleanHandle,
            tagline = tagline?.trim()?.ifBlank { null },
            description = description?.trim()?.ifBlank { null },
            businessMode = mode,
            currency = currency,
            status = StoreStatus.DRAFT,
            productCount = 0,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        val list = shopsTable.getOrPut(current.uid) { mutableListOf() }
        list.add(0, newShop)
        analytics.logEvent(SnapBrandEvent.SHOP_CREATED, mapOf("mode" to mode.name, "shop_id" to newShop.id))
        return Result.success(newShop)
    }

    fun updateStore(updatedShop: Shop): Result<Shop> {
        val current = currentUser ?: return Result.failure(IllegalStateException("Authentication required"))
        if (updatedShop.ownerUid != current.uid) {
            return Result.failure(SecurityException("Cannot update a store owned by another account."))
        }
        val userShops = shopsTable.getOrPut(current.uid) { mutableListOf() }
        val existingIndex = userShops.indexOfFirst { it.id == updatedShop.id }
        if (existingIndex < 0) {
            return Result.failure(NoSuchElementException("Shop ${updatedShop.id} not found."))
        }

        val handleRes = reserveHandle(updatedShop.handle, updatedShop.id, current.uid)
        if (handleRes.isFailure) {
            return Result.failure(handleRes.exceptionOrNull() ?: IllegalArgumentException("Failed to reserve handle"))
        }
        val reservedHandle = handleRes.getOrThrow()

        val finalShop = updatedShop.copy(
            handle = reservedHandle,
            updatedAt = System.currentTimeMillis()
        )
        userShops[existingIndex] = finalShop
        analytics.logEvent(SnapBrandEvent.STORE_UPDATED, mapOf("shopId" to finalShop.id, "handle" to finalShop.handle))
        return Result.success(finalShop)
    }

    fun setProductVisibility(productId: String, isVisible: Boolean): Result<Product> {
        val current = currentUser ?: return Result.failure(IllegalStateException("Authentication required"))
        val userProducts = productsTable[current.uid] ?: return Result.failure(NoSuchElementException("Product not found"))
        val index = userProducts.indexOfFirst { it.id == productId }
        if (index < 0) return Result.failure(NoSuchElementException("Product not found"))

        val prod = userProducts[index]
        if (prod.ownerUid != current.uid) {
            return Result.failure(SecurityException("Access denied"))
        }

        val updated = prod.copy(
            isVisible = isVisible,
            updatedAt = System.currentTimeMillis()
        )
        userProducts[index] = updated
        analytics.logEvent(SnapBrandEvent.PRODUCT_VISIBILITY_CHANGED, mapOf("productId" to productId, "isVisible" to isVisible))
        return Result.success(updated)
    }

    fun setFeaturedProducts(shopId: String, featuredProductIds: List<String>): Result<Shop> {
        val current = currentUser ?: return Result.failure(IllegalStateException("Authentication required"))
        val userShops = shopsTable[current.uid] ?: return Result.failure(NoSuchElementException("Shop not found"))
        val shopIndex = userShops.indexOfFirst { it.id == shopId }
        if (shopIndex < 0) return Result.failure(NoSuchElementException("Shop not found"))
        val shop = userShops[shopIndex]
        if (shop.ownerUid != current.uid) return Result.failure(SecurityException("Access denied"))

        val updatedShop = shop.copy(
            featuredProductIds = featuredProductIds,
            updatedAt = System.currentTimeMillis()
        )
        userShops[shopIndex] = updatedShop

        // Sync isFeatured flag on user's products
        val userProducts = productsTable[current.uid]
        userProducts?.let { list ->
            for (i in list.indices) {
                if (list[i].shopId == shopId) {
                    val isFeatured = featuredProductIds.contains(list[i].id)
                    list[i] = list[i].copy(isFeatured = isFeatured)
                }
            }
        }

        analytics.logEvent(SnapBrandEvent.FEATURED_PRODUCT_CHANGED, mapOf("shopId" to shopId, "count" to featuredProductIds.size))
        return Result.success(updatedShop)
    }

    fun validateShopForPublish(shopId: String): PublishValidationResult {
        val current = currentUser ?: return PublishValidationResult(false, listOf("Authentication required"), emptyList())
        val shop = shopsTable[current.uid]?.find { it.id == shopId }
            ?: return PublishValidationResult(false, listOf("Shop not found"), emptyList())
        val products = productsTable[current.uid]?.filter { it.shopId == shopId } ?: emptyList()
        val sourceAnalysis = snapAnalysesTable[current.uid]?.find { it.id == shop.sourceSnapAnalysisId }

        return PublishValidator.validate(
            shop = shop,
            products = products,
            isHandleTakenByOther = { handle ->
                val record = storeHandlesTable[handle]
                record != null && record.storeId != shop.id
            },
            sourceAnalysis = sourceAnalysis
        )
    }

    fun publishStore(shopId: String): Result<Shop> {
        val current = currentUser ?: return Result.failure(IllegalStateException("Authentication required"))
        val globalShop = shopsTable.values.flatten().find { it.id == shopId }
            ?: return Result.failure(NoSuchElementException("Shop not found"))
        if (globalShop.ownerUid != current.uid) {
            return Result.failure(SecurityException("Access denied: You do not own this shop."))
        }
        val userShops = shopsTable[current.uid] ?: return Result.failure(NoSuchElementException("Shop not found"))
        val shopIndex = userShops.indexOfFirst { it.id == shopId }
        if (shopIndex < 0) return Result.failure(NoSuchElementException("Shop not found"))
        val shop = userShops[shopIndex]

        analytics.logEvent(SnapBrandEvent.STORE_PUBLISH_STARTED, mapOf("shopId" to shopId))

        if (!canAccessFeature(current.uid, FeatureEntitlement.STORE_PUBLISHING)) {
            return Result.failure(IllegalStateException("Active SnapBrand subscription required to publish store live. Please activate your subscription in Settings."))
        }

        val validation = validateShopForPublish(shopId)
        if (!validation.canPublish) {
            return Result.failure(IllegalStateException(validation.missingRequirements.joinToString("\n")))
        }

        val handleRes = reserveHandle(shop.handle, shop.id, current.uid)
        if (handleRes.isFailure) {
            return Result.failure(handleRes.exceptionOrNull() ?: IllegalStateException("Handle reservation failed"))
        }

        val publishedShop = shop.copy(
            handle = handleRes.getOrThrow(),
            status = StoreStatus.PUBLISHED,
            publishedAt = shop.publishedAt ?: System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        userShops[shopIndex] = publishedShop

        analytics.logEvent(SnapBrandEvent.STORE_PUBLISHED, mapOf("shopId" to shopId, "handle" to publishedShop.handle))
        return Result.success(publishedShop)
    }

    fun unpublishStore(shopId: String): Result<Shop> {
        val current = currentUser ?: return Result.failure(IllegalStateException("Authentication required"))
        val globalShop = shopsTable.values.flatten().find { it.id == shopId }
            ?: return Result.failure(NoSuchElementException("Shop not found"))
        if (globalShop.ownerUid != current.uid) {
            return Result.failure(SecurityException("Access denied: You do not own this shop."))
        }
        val userShops = shopsTable[current.uid] ?: return Result.failure(NoSuchElementException("Shop not found"))
        val shopIndex = userShops.indexOfFirst { it.id == shopId }
        if (shopIndex < 0) return Result.failure(NoSuchElementException("Shop not found"))
        val shop = userShops[shopIndex]

        val unpublishedShop = shop.copy(
            status = StoreStatus.UNPUBLISHED,
            updatedAt = System.currentTimeMillis()
        )
        userShops[shopIndex] = unpublishedShop
        analytics.logEvent(SnapBrandEvent.STORE_UNPUBLISHED, mapOf("shopId" to shopId))
        return Result.success(unpublishedShop)
    }

    fun resolvePublicStorefront(handle: String): Result<PublicStorefront> {
        val normalized = StoreHandleSystem.normalize(handle)
        val handleRecord = storeHandlesTable[normalized]
        val shop = if (handleRecord != null) {
            shopsTable[handleRecord.ownerUid]?.find { it.id == handleRecord.storeId }
        } else {
            shopsTable.values.flatten().find { StoreHandleSystem.normalize(it.handle) == normalized }
        }

        if (shop == null) {
            return Result.failure(NoSuchElementException("Store '@$normalized' was not found."))
        }

        if (shop.status != StoreStatus.PUBLISHED) {
            return Result.failure(IllegalStateException("Store '@$normalized' is currently unpublished or in draft mode."))
        }

        val products = productsTable[shop.ownerUid]?.filter { it.shopId == shop.id } ?: emptyList()
        val publicStorefront = PublicStorefrontMapper.toPublicStorefront(shop, products)
        return Result.success(publicStorefront)
    }

    // ==========================================
    // ==========================================
    // PRODUCTS (Strict Per-Owner Isolation & Phase 3 Product Engine)
    // ==========================================
    fun getProductsForCurrentAccount(): List<Product> {
        val uid = currentUser?.uid ?: return emptyList()
        return productsTable[uid]?.toList() ?: emptyList()
    }

    fun getProductsForShop(shopId: String): List<Product> {
        val uid = currentUser?.uid ?: return emptyList()
        return productsTable[uid]?.filter { it.shopId == shopId } ?: emptyList()
    }

    /**
     * Retrieves all active discoverable products across all shops on the platform,
     * paired with their respective parent Shop entity for direct discovery and storefront navigation.
     */
    fun getAllDiscoverableProducts(): List<Pair<Product, Shop>> {
        val results = mutableListOf<Pair<Product, Shop>>()
        val allShops = shopsTable.values.flatten()
        val shopMap = allShops.associateBy { it.id }
        for ((_, products) in productsTable) {
            for (prod in products) {
                val shop = shopMap[prod.shopId]
                if (shop != null && (prod.status == "ACTIVE" || prod.status == "PUBLISHED" || prod.status == "DRAFT")) {
                    results.add(prod to shop)
                }
            }
        }
        return results
    }

    /**
     * Retrieves all published shops on the platform for community discovery.
     */
    fun getAllPublicShops(): List<Shop> {
        return shopsTable.values.flatten().filter { it.status == StoreStatus.PUBLISHED }
    }

    fun saveProduct(product: Product): Result<Product> {
        val current = currentUser ?: return Result.failure(IllegalStateException("Authentication required"))
        if (product.ownerUid != current.uid) {
            return Result.failure(SecurityException("Cannot save product for another user."))
        }

        val validatedInventory = if (product.inventory < 0) 0 else product.inventory
        val validatedStatus = if (validatedInventory == 0) "OUT_OF_STOCK" else product.status

        val validatedProduct = product.copy(
            inventory = validatedInventory,
            status = validatedStatus,
            updatedAt = System.currentTimeMillis()
        )

        val list = productsTable.getOrPut(current.uid) { mutableListOf() }
        val existingIndex = list.indexOfFirst { it.id == validatedProduct.id }
        if (existingIndex >= 0) {
            list[existingIndex] = validatedProduct
            analytics.logEvent(
                SnapBrandEvent.PRODUCT_EDITED,
                mapOf("productId" to validatedProduct.id, "shopId" to validatedProduct.shopId)
            )
        } else {
            list.add(0, validatedProduct)
            analytics.logEvent(
                SnapBrandEvent.PRODUCT_ACCEPTED,
                mapOf("productId" to validatedProduct.id, "shopId" to validatedProduct.shopId)
            )
        }

        updateShopProductCount(current.uid, validatedProduct.shopId)
        return Result.success(validatedProduct)
    }

    fun saveProducts(shopId: String, products: List<Product>): Result<List<Product>> {
        val current = currentUser ?: return Result.failure(IllegalStateException("Authentication required"))
        val list = productsTable.getOrPut(current.uid) { mutableListOf() }
        val savedList = mutableListOf<Product>()

        for (item in products) {
            if (item.ownerUid != current.uid) continue
            val validatedInventory = if (item.inventory < 0) 0 else item.inventory
            val validatedStatus = if (validatedInventory == 0) "OUT_OF_STOCK" else item.status
            val readyItem = item.copy(
                shopId = shopId,
                inventory = validatedInventory,
                status = validatedStatus,
                updatedAt = System.currentTimeMillis()
            )
            val index = list.indexOfFirst { it.id == readyItem.id }
            if (index >= 0) {
                list[index] = readyItem
            } else {
                list.add(readyItem)
            }
            savedList.add(readyItem)
        }

        updateShopProductCount(current.uid, shopId)
        return Result.success(savedList)
    }

    fun removeProduct(productId: String): Result<Unit> {
        val current = currentUser ?: return Result.failure(IllegalStateException("Authentication required"))
        val list = productsTable[current.uid] ?: return Result.failure(NoSuchElementException("Product not found"))
        val product = list.find { it.id == productId } ?: return Result.failure(NoSuchElementException("Product not found"))

        list.remove(product)
        updateShopProductCount(current.uid, product.shopId)
        analytics.logEvent(SnapBrandEvent.PRODUCT_REMOVED, mapOf("productId" to productId, "shopId" to product.shopId))
        return Result.success(Unit)
    }

    private fun updateShopProductCount(ownerUid: String, shopId: String) {
        val shops = shopsTable[ownerUid] ?: return
        val shopIndex = shops.indexOfFirst { it.id == shopId }
        if (shopIndex >= 0) {
            val count = productsTable[ownerUid]?.count { it.shopId == shopId } ?: 0
            val oldShop = shops[shopIndex]
            shops[shopIndex] = oldShop.copy(productCount = count, updatedAt = System.currentTimeMillis())
        }
    }

    suspend fun generateProductsForSnap(
        shopId: String,
        analysis: SnapAnalysis,
        brandConcept: BrandConcept?,
        businessMode: String,
        currency: String = "USD",
        directive: String? = null
    ): Result<List<Product>> {
        val current = currentUser ?: return Result.failure(IllegalStateException("Authentication required"))
        analytics.logEvent(
            SnapBrandEvent.PRODUCT_GENERATION_STARTED,
            mapOf("shopId" to shopId, "mode" to businessMode)
        )

        return try {
            val products = aiService.generateProductsForShop(
                shopId = shopId,
                ownerUid = current.uid,
                analysis = analysis,
                brandConcept = brandConcept,
                businessMode = businessMode,
                currency = currency,
                directive = directive
            )
            analytics.logEvent(
                SnapBrandEvent.PRODUCT_GENERATION_COMPLETED,
                mapOf("shopId" to shopId, "count" to products.size)
            )
            Result.success(products)
        } catch (e: Exception) {
            analytics.logEvent(
                SnapBrandEvent.PRODUCT_GENERATION_FAILED,
                mapOf("shopId" to shopId, "error" to (e.message ?: "Unknown error"))
            )
            Result.failure(e)
        }
    }

    suspend fun regenerateSingleProduct(
        product: Product,
        brandConcept: BrandConcept?,
        analysis: SnapAnalysis?,
        action: String,
        directive: String? = null
    ): Result<Product> {
        val current = currentUser ?: return Result.failure(IllegalStateException("Authentication required"))
        return try {
            val updated = aiService.regenerateSingleProduct(
                product = product,
                brandConcept = brandConcept,
                analysis = analysis,
                action = action,
                directive = directive
            )
            analytics.logEvent(
                SnapBrandEvent.PRODUCT_REGENERATED,
                mapOf("productId" to product.id, "action" to action)
            )
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun createProduct(
        shopId: String,
        title: String,
        description: String,
        price: Double,
        currency: String = "USD",
        inventory: Int = 10,
        type: ProductType = ProductType.MERCH
    ): Result<Product> {
        val current = currentUser ?: return Result.failure(IllegalStateException("Authentication required"))
        val newProduct = Product(
            id = "prod_${UUID.randomUUID().toString().take(8)}",
            shopId = shopId,
            ownerUid = current.uid,
            title = title,
            description = description,
            price = price,
            currency = currency,
            inventory = inventory,
            type = type,
            status = "DRAFT"
        )
        val list = productsTable.getOrPut(current.uid) { mutableListOf() }
        list.add(0, newProduct)
        updateShopProductCount(current.uid, shopId)
        return Result.success(newProduct)
    }

    // ==========================================
    // CART ENGINE (Scoped Per-Store)
    // ==========================================
    fun getCart(storeId: String): CartState {
        return cartsByStore.getOrPut(storeId) { CartState(storeId = storeId) }
    }

    fun addToCart(
        storeId: String,
        productId: String,
        variantId: String? = null,
        quantity: Int = 1
    ): Result<CartState> {
        val shop = shopsTable.values.flatten().find { it.id == storeId }
            ?: return Result.failure(NoSuchElementException("Store not found."))

        val product = productsTable.values.flatten().find { it.id == productId && it.shopId == storeId }
            ?: return Result.failure(NoSuchElementException("Product not found in this store."))

        val currentCart = getCart(storeId)
        val result = CartEngine.addItem(
            currentCart = currentCart,
            shop = shop,
            product = product,
            variantId = variantId,
            quantity = quantity
        )

        if (result.isSuccess) {
            val newCart = result.getOrThrow()
            cartsByStore[storeId] = newCart
            analytics.logEvent(
                SnapBrandEvent.ADD_TO_CART,
                mapOf("storeId" to storeId, "productId" to productId, "quantity" to quantity)
            )
            return Result.success(newCart)
        }
        return result
    }

    fun updateCartQuantity(
        storeId: String,
        cartItemId: String,
        newQuantity: Int
    ): Result<CartState> {
        val currentCart = getCart(storeId)
        val cartItem = currentCart.items.find { it.id == cartItemId }
            ?: return Result.failure(NoSuchElementException("Item not found in cart."))

        val product = productsTable.values.flatten().find { it.id == cartItem.productId }
        val result = CartEngine.updateQuantity(
            currentCart = currentCart,
            cartItemId = cartItemId,
            newQuantity = newQuantity,
            productInventory = product?.inventory
        )
        if (result.isSuccess) {
            val newCart = result.getOrThrow()
            cartsByStore[storeId] = newCart
            return Result.success(newCart)
        }
        return result
    }

    fun removeFromCart(storeId: String, cartItemId: String): CartState {
        val currentCart = getCart(storeId)
        val updated = CartEngine.removeItem(currentCart, cartItemId)
        cartsByStore[storeId] = updated
        return updated
    }

    fun clearCart(storeId: String): CartState {
        val cleared = CartEngine.clear(storeId)
        cartsByStore[storeId] = cleared
        return cleared
    }

    // ==========================================
    // ORDERS (Strict Ownership Isolation & Checkout)
    // ==========================================
    fun getOrdersForCurrentAccount(filterStatus: OrderStatus? = null): List<Order> {
        val uid = currentUser?.uid ?: return emptyList()
        val userOrders = ordersTable[uid] ?: emptyList()
        return if (filterStatus != null) {
            userOrders.filter { it.status == filterStatus || it.orderStatus == filterStatus }
        } else {
            userOrders.toList()
        }
    }

    fun createCheckoutOrder(
        storeId: String,
        deliveryInfo: CustomerDeliveryInfo,
        items: List<CartItem>
    ): Result<Order> {
        val shop = shopsTable.values.flatten().find { it.id == storeId }
            ?: return Result.failure(NoSuchElementException("Store not found."))

        if (shop.status != StoreStatus.PUBLISHED) {
            return Result.failure(IllegalStateException("Cannot checkout from an unpublished store."))
        }

        if (items.isEmpty()) {
            return Result.failure(IllegalArgumentException("Cart is empty."))
        }

        val validation = CheckoutValidator.validateDeliveryInfo(deliveryInfo)
        if (!validation.isValid) {
            return Result.failure(IllegalArgumentException(validation.errors.firstOrNull() ?: "Invalid delivery information."))
        }

        // Revalidate stock and eligibility for each item
        val orderItems = mutableListOf<OrderItem>()
        var subtotal = 0L

        for (item in items) {
            val product = productsTable.values.flatten().find { it.id == item.productId && it.shopId == storeId }
                ?: return Result.failure(NoSuchElementException("Product ${item.titleSnapshot} is no longer available."))

            if (!product.isVisible || product.status.equals("DRAFT", ignoreCase = true) || product.status.equals("ARCHIVED", ignoreCase = true)) {
                return Result.failure(IllegalStateException("${product.title} is no longer available."))
            }

            if (product.inventory < item.quantity) {
                return Result.failure(IllegalStateException("Only ${product.inventory} available for ${product.title}."))
            }

            val itemSubtotal = Money.calculateItemSubtotal(item.unitPrice, item.quantity)
            subtotal += itemSubtotal

            orderItems.add(
                OrderItem(
                    id = "item_${UUID.randomUUID().toString().take(8)}",
                    storeId = storeId,
                    productId = product.id,
                    variantId = item.variantId,
                    titleSnapshot = item.titleSnapshot,
                    imageSnapshot = item.imageSnapshot,
                    unitPrice = item.unitPrice,
                    currency = item.currency,
                    quantity = item.quantity,
                    subtotal = itemSubtotal
                )
            )
        }

        val deliveryFee = if (shop.fixedDeliveryFee != null) Money.toMinorUnits(shop.fixedDeliveryFee!!) else 0L
        val total = Money.calculateTotal(subtotal, deliveryFee)
        val orderNumber = OrderNumberGenerator.generate()
        val orderId = "ord_${UUID.randomUUID().toString().take(8)}"
        val now = System.currentTimeMillis()

        val fullAddress = listOfNotNull(
            deliveryInfo.deliveryAddress.takeIf { it.isNotBlank() },
            deliveryInfo.city.takeIf { it.isNotBlank() },
            deliveryInfo.country.takeIf { it.isNotBlank() }
        ).joinToString(", ")

        val order = Order(
            id = orderId,
            orderNumber = orderNumber,
            storeId = storeId,
            sellerUid = shop.ownerUid,
            customerId = currentUser?.uid,
            customerName = deliveryInfo.customerName,
            customerEmail = deliveryInfo.customerEmail,
            customerPhone = deliveryInfo.customerPhone,
            deliveryAddress = fullAddress,
            deliveryNotes = deliveryInfo.deliveryNotes,
            items = orderItems,
            subtotal = subtotal,
            deliveryFee = deliveryFee,
            total = total,
            currency = shop.currency,
            paymentProvider = "PAYSTACK",
            paymentStatus = PaymentStatus.UNPAID,
            orderStatus = OrderStatus.PENDING_PAYMENT,
            createdAt = now,
            updatedAt = now
        )

        // Store in global lookup tables
        allOrdersById[order.id] = order
        allOrdersByNumber[order.orderNumber] = order

        // Add to seller's orders
        ordersTable.getOrPut(shop.ownerUid) { mutableListOf() }.add(0, order)

        // If authenticated customer, also add to customer's order list
        currentUser?.let { user ->
            if (user.uid != shop.ownerUid) {
                ordersTable.getOrPut(user.uid) { mutableListOf() }.add(0, order)
            }
        }

        analytics.logEvent(
            SnapBrandEvent.CHECKOUT_DETAILS_SUBMITTED,
            mapOf("orderId" to order.id, "storeId" to storeId, "total" to total)
        )
        analytics.logEvent(
            SnapBrandEvent.ORDER_CREATED,
            mapOf("orderId" to order.id, "orderNumber" to order.orderNumber)
        )

        return Result.success(order)
    }

    suspend fun initializeOrderPayment(orderId: String): Result<PaymentInitResponse> {
        val order = allOrdersById[orderId]
            ?: return Result.failure(NoSuchElementException("Order not found: $orderId"))

        if (order.paymentStatus == PaymentStatus.PAID) {
            return Result.failure(IllegalStateException("Order $orderId is already paid."))
        }

        val sellerUid = order.sellerUid
        if (sellerUid.isNotBlank() && !canAccessFeature(sellerUid, FeatureEntitlement.CUSTOMER_CHECKOUT)) {
            return Result.failure(IllegalStateException("Merchant checkout is unavailable: Seller does not have an active SnapBrand subscription."))
        }

        val initRequest = PaymentInitRequest(
            orderId = order.id,
            orderNumber = order.orderNumber,
            storeId = order.storeId,
            amountMinorUnits = order.total,
            currency = order.currency,
            customerEmail = order.customerEmail,
            customerName = order.customerName,
            callbackUrl = "https://snapbrand.site/checkout/callback?orderId=${order.id}"
        )

        val initResult = paymentProvider.initializePayment(initRequest)
        if (initResult.isFailure) {
            val error = initResult.exceptionOrNull()!!
            analytics.logEvent(
                SnapBrandEvent.PAYMENT_FAILED,
                mapOf("orderId" to order.id, "reason" to (error.message ?: "Init failed"))
            )
            return Result.failure(error)
        }

        val response = initResult.getOrThrow()
        val updatedOrder = order.copy(
            paymentReference = response.reference,
            paymentStatus = PaymentStatus.PENDING_VERIFICATION,
            updatedAt = System.currentTimeMillis()
        )
        updateOrderInStorage(updatedOrder)

        analytics.logEvent(
            SnapBrandEvent.PAYMENT_INITIALIZED,
            mapOf("orderId" to order.id, "reference" to response.reference)
        )

        return Result.success(response)
    }

    suspend fun verifyOrderPayment(
        orderId: String,
        paymentReference: String
    ): Result<Order> {
        val order = allOrdersById[orderId]
            ?: return Result.failure(NoSuchElementException("Order not found: $orderId"))

        val outcomeResult = paymentVerificationEngine.verifyAndCommitOrder(
            order = order,
            paymentReference = paymentReference,
            currentProductsProvider = { ids ->
                productsTable.values.flatten().filter { it.id in ids }
            },
            onInventoryDeducted = { productId, qty ->
                deductProductInventory(productId, qty)
            }
        )

        if (outcomeResult.isFailure) {
            val err = outcomeResult.exceptionOrNull()!!
            return Result.failure(err)
        }

        val outcome = outcomeResult.getOrThrow()
        val updatedOrder = outcome.updatedOrder
        updateOrderInStorage(updatedOrder)

        if (outcome.isSuccessful) {
            // Clear cart for this store
            clearCart(order.storeId)

            // Record transaction for the seller
            if (!outcome.isDuplicate) {
                createTransaction(
                    orderId = order.id,
                    shopId = order.storeId,
                    grossAmount = Money.toMajorUnits(order.total),
                    platformFee = Money.toMajorUnits(order.total) * 0.05,
                    reference = paymentReference
                )

                // Record / update customer record for the seller
                recordCustomerFromOrder(order)

                // Trigger Printify Order Synchronization if this order contains MERCH items
                syncPaidOrderToPrintify(updatedOrder)
            }
            return Result.success(updatedOrder)
        } else {
            return Result.failure(IllegalStateException(outcome.message))
        }
    }

    private fun deductProductInventory(productId: String, quantity: Int) {
        for ((_, list) in productsTable) {
            val index = list.indexOfFirst { it.id == productId }
            if (index >= 0) {
                val p = list[index]
                val newInventory = (p.inventory - quantity).coerceAtLeast(0)
                val newStatus = if (newInventory <= 0) "OUT_OF_STOCK" else p.status
                list[index] = p.copy(
                    inventory = newInventory,
                    status = newStatus,
                    updatedAt = System.currentTimeMillis()
                )
                break
            }
        }
    }

    private fun restoreProductInventory(productId: String, quantity: Int) {
        for ((_, list) in productsTable) {
            val index = list.indexOfFirst { it.id == productId }
            if (index >= 0) {
                val p = list[index]
                val newInventory = p.inventory + quantity
                val newStatus = if (p.status == "OUT_OF_STOCK" && newInventory > 0) "PUBLISHED" else p.status
                list[index] = p.copy(
                    inventory = newInventory,
                    status = newStatus,
                    updatedAt = System.currentTimeMillis()
                )
                break
            }
        }
    }

    private fun updateOrderInStorage(order: Order) {
        allOrdersById[order.id] = order
        allOrdersByNumber[order.orderNumber] = order

        // Update in seller's list
        ordersTable[order.sellerUid]?.let { list ->
            val idx = list.indexOfFirst { it.id == order.id }
            if (idx >= 0) list[idx] = order else list.add(0, order)
        }

        // Update in customer's list if present
        order.customerId?.let { custId ->
            ordersTable[custId]?.let { list ->
                val idx = list.indexOfFirst { it.id == order.id }
                if (idx >= 0) list[idx] = order else list.add(0, order)
            }
        }
    }

    private fun recordCustomerFromOrder(order: Order) {
        if (order.customerEmail.isBlank()) return
        val list = customersTable.getOrPut(order.sellerUid) { mutableListOf() }
        val existingIndex = list.indexOfFirst { it.email.equals(order.customerEmail, ignoreCase = true) }
        val orderAmount = Money.toMajorUnits(order.total)

        if (existingIndex >= 0) {
            val cust = list[existingIndex]
            list[existingIndex] = cust.copy(
                totalOrders = cust.totalOrders + 1,
                totalSpent = cust.totalSpent + orderAmount,
                lastOrderAt = System.currentTimeMillis()
            )
        } else {
            list.add(
                Customer(
                    id = "cust_${UUID.randomUUID().toString().take(8)}",
                    shopId = order.storeId,
                    email = order.customerEmail,
                    name = order.customerName,
                    totalOrders = 1,
                    totalSpent = orderAmount,
                    lastOrderAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun handlePaymentWebhook(payload: String, signature: String?): Result<WebhookProcessingResult> {
        val result = paymentProvider.handleWebhook(payload, signature)
        if (result.isFailure) return result

        val webhookResult = result.getOrThrow()
        if (webhookResult.paymentStatus == PaymentStatus.PAID && !webhookResult.isDuplicate) {
            // Locate order by paymentReference
            val order = allOrdersById.values.find { it.paymentReference == webhookResult.reference }
            if (order != null && order.paymentStatus != PaymentStatus.PAID) {
                verifyOrderPayment(order.id, webhookResult.reference)
            }
        }
        return result
    }

    fun updateOrderStatus(orderId: String, newStatus: OrderStatus): Result<Order> {
        val current = currentUser ?: return Result.failure(IllegalStateException("Authentication required"))
        val order = allOrdersById[orderId]
            ?: ordersTable[current.uid]?.find { it.id == orderId }
            ?: return Result.failure(NoSuchElementException("Order not found."))

        // Ownership isolation: only seller can update order status
        if (order.sellerUid != current.uid) {
            return Result.failure(SecurityException("PERMISSION_DENIED: Only the store owner can update order status."))
        }

        // Rule: Cannot mark an unpaid order as paid manually
        if (order.paymentStatus != PaymentStatus.PAID && (newStatus == OrderStatus.PAID || newStatus == OrderStatus.PROCESSING || newStatus == OrderStatus.COMPLETED)) {
            return Result.failure(IllegalStateException("Cannot mark an unpaid order as PAID without verified payment."))
        }

        // Permitted transitions:
        // PENDING_PAYMENT -> CANCELLED
        // PAID -> PROCESSING -> COMPLETED
        // PAID/PROCESSING -> CANCELLED
        val isPermitted = when (order.orderStatus) {
            OrderStatus.PENDING_PAYMENT -> newStatus == OrderStatus.CANCELLED
            OrderStatus.PAID -> newStatus == OrderStatus.PROCESSING || newStatus == OrderStatus.COMPLETED || newStatus == OrderStatus.CANCELLED
            OrderStatus.PROCESSING -> newStatus == OrderStatus.COMPLETED || newStatus == OrderStatus.CANCELLED
            OrderStatus.COMPLETED -> false
            OrderStatus.CANCELLED -> false
            else -> true // backward compat
        }

        if (!isPermitted) {
            return Result.failure(IllegalStateException("Transition from ${order.orderStatus.label} to ${newStatus.label} is not permitted."))
        }

        // If order was paid and is now cancelled, safely restore inventory
        if (newStatus == OrderStatus.CANCELLED && order.paymentStatus == PaymentStatus.PAID) {
            for (item in order.items) {
                restoreProductInventory(item.productId, item.quantity)
            }
        }

        val updatedOrder = order.copy(
            orderStatus = newStatus,
            updatedAt = System.currentTimeMillis()
        )
        updateOrderInStorage(updatedOrder)
        return Result.success(updatedOrder)
    }

    fun getPublicOrderConfirmation(orderId: String, orderNumber: String): Result<Order> {
        val order = allOrdersById[orderId]
            ?: allOrdersByNumber[orderNumber]
            ?: return Result.failure(NoSuchElementException("Order not found."))

        if (order.id != orderId && order.orderNumber != orderNumber) {
            return Result.failure(SecurityException("Order identifier mismatch."))
        }
        return Result.success(order)
    }

    fun createOrder(
        shopId: String,
        sellerUid: String,
        items: List<OrderItem>,
        totalAmount: Double,
        currency: String = "USD"
    ): Result<Order> {
        val current = currentUser ?: return Result.failure(IllegalStateException("Authentication required"))
        val totalMinorUnits = Money.toMinorUnits(totalAmount)
        val newOrder = Order(
            id = "ord_${UUID.randomUUID().toString().take(8)}",
            orderNumber = OrderNumberGenerator.generate(),
            storeId = shopId,
            sellerUid = sellerUid,
            customerId = current.uid,
            items = items,
            subtotal = totalMinorUnits,
            deliveryFee = 0L,
            total = totalMinorUnits,
            currency = currency,
            paymentStatus = PaymentStatus.UNPAID,
            orderStatus = OrderStatus.PENDING_PAYMENT,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        // Add to global and seller orders
        updateOrderInStorage(newOrder)
        return Result.success(newOrder)
    }

    // ==========================================
    // CUSTOMERS (Strict Per-Shop Isolation)
    // ==========================================
    fun getCustomersForCurrentAccount(): List<Customer> {
        val uid = currentUser?.uid ?: return emptyList()
        return customersTable[uid]?.toList() ?: emptyList()
    }

    fun createCustomer(
        shopId: String,
        email: String,
        name: String,
        totalOrders: Int = 1,
        totalSpent: Double = 35.0
    ): Result<Customer> {
        val current = currentUser ?: return Result.failure(IllegalStateException("Authentication required"))
        val newCustomer = Customer(
            id = "cust_${UUID.randomUUID().toString().take(8)}",
            shopId = shopId,
            email = email,
            name = name,
            totalOrders = totalOrders,
            totalSpent = totalSpent
        )
        customersTable.getOrPut(current.uid) { mutableListOf() }.add(0, newCustomer)
        return Result.success(newCustomer)
    }

    // ==========================================
    // TRANSACTIONS (Strict Financial Isolation)
    // ==========================================
    fun getTransactionsForCurrentAccount(): List<Transaction> {
        val uid = currentUser?.uid ?: return emptyList()
        return transactionsTable[uid]?.toList() ?: emptyList()
    }

    fun createTransaction(
        orderId: String,
        shopId: String,
        grossAmount: Double,
        platformFee: Double = grossAmount * 0.05,
        reference: String = "ref_${UUID.randomUUID().toString().take(8)}"
    ): Result<Transaction> {
        val current = currentUser ?: return Result.failure(IllegalStateException("Authentication required"))
        val newTx = Transaction(
            id = "tx_${UUID.randomUUID().toString().take(8)}",
            orderId = orderId,
            shopId = shopId,
            sellerUid = current.uid,
            grossAmount = grossAmount,
            platformFee = platformFee,
            netEarnings = grossAmount - platformFee,
            reference = reference
        )
        transactionsTable.getOrPut(current.uid) { mutableListOf() }.add(0, newTx)
        return Result.success(newTx)
    }

    // ==========================================
    // FIRESTORE RULES ENFORCEMENT & SECURITY SIMULATION
    // ==========================================

    fun accessUserProfileAs(requesterUid: String, targetUid: String): Result<UserProfile> {
        if (requesterUid != targetUid) {
            return Result.failure(SecurityException("PERMISSION_DENIED: User $requesterUid is not authorized to read private profile of $targetUid (Firestore Security Rule users/{userId})"))
        }
        val user = usersTable[targetUid] ?: return Result.failure(NoSuchElementException("User document not found"))
        return Result.success(user)
    }

    fun accessShopAs(requesterUid: String, shopId: String): Result<Shop> {
        val shop = shopsTable.values.flatten().find { it.id == shopId }
            ?: return Result.failure(NoSuchElementException("Shop not found"))
        if (shop.ownerUid != requesterUid) {
            return Result.failure(SecurityException("PERMISSION_DENIED: User $requesterUid is not authorized to access private shop $shopId owned by ${shop.ownerUid}"))
        }
        return Result.success(shop)
    }

    fun accessProductAs(requesterUid: String, productId: String): Result<Product> {
        val product = productsTable.values.flatten().find { it.id == productId }
            ?: return Result.failure(NoSuchElementException("Product not found"))
        if (product.ownerUid != requesterUid) {
            return Result.failure(SecurityException("PERMISSION_DENIED: User $requesterUid is not authorized to access product $productId owned by ${product.ownerUid}"))
        }
        return Result.success(product)
    }

    fun accessOrderAs(requesterUid: String, orderId: String): Result<Order> {
        val order = allOrdersById[orderId]
            ?: ordersTable.values.flatten().find { it.id == orderId }
            ?: return Result.failure(NoSuchElementException("Order not found"))
        if (order.sellerUid != requesterUid && order.buyerUid != requesterUid && order.customerId != requesterUid) {
            return Result.failure(SecurityException("PERMISSION_DENIED: User $requesterUid is not party to order $orderId"))
        }
        return Result.success(order)
    }

    fun accessCustomerAs(requesterUid: String, customerId: String): Result<Customer> {
        val customer = customersTable.values.flatten().find { it.id == customerId }
            ?: return Result.failure(NoSuchElementException("Customer record not found"))
        val belongsToRequester = customersTable[requesterUid]?.any { it.id == customerId } == true
        if (!belongsToRequester) {
            return Result.failure(SecurityException("PERMISSION_DENIED: User $requesterUid is not authorized to access customer record $customerId"))
        }
        return Result.success(customer)
    }

    fun accessTransactionAs(requesterUid: String, transactionId: String): Result<Transaction> {
        val tx = transactionsTable.values.flatten().find { it.id == transactionId }
            ?: return Result.failure(NoSuchElementException("Transaction not found"))
        if (tx.sellerUid != requesterUid) {
            return Result.failure(SecurityException("PERMISSION_DENIED: User $requesterUid cannot view private transaction $transactionId owned by ${tx.sellerUid}"))
        }
        return Result.success(tx)
    }

    fun accessPrivateSettingsAs(requesterUid: String, targetUid: String): Result<Boolean> {
        if (requesterUid != targetUid) {
            return Result.failure(SecurityException("PERMISSION_DENIED: User $requesterUid cannot read or modify private settings for $targetUid"))
        }
        val user = usersTable[targetUid] ?: return Result.failure(NoSuchElementException("User not found"))
        return Result.success(user.notificationsEnabled)
    }

    // ==========================================
    // TEST DATA SEEDING FOR ACCOUNT A & ACCOUNT B
    // ==========================================

    fun setupPrivateTestDataForAccountA(): Map<String, String> {
        signInWithAccountA()
        val shopA = shopsTable[ACCOUNT_A_UID]?.firstOrNull() ?: createShop(
            name = "VaporWave Threads",
            handle = "vaporwave",
            tagline = "Futuristic aesthetic apparel",
            description = "Sketches to shirts",
            mode = ShopMode.MERCH_SHOP
        ).getOrThrow()

        val productA = createProduct(
            shopId = shopA.id,
            title = "Cyber Neon Tee",
            description = "100% Ring-spun cotton with neon screenprint",
            price = 34.99
        ).getOrThrow()

        val orderA = createOrder(
            shopId = shopA.id,
            sellerUid = ACCOUNT_A_UID,
            items = listOf(OrderItem(productA.id, productA.title, 1, productA.price)),
            totalAmount = 34.99
        ).getOrThrow()

        val customerA = createCustomer(
            shopId = shopA.id,
            email = "collector_alpha@example.com",
            name = "Marcus Vance",
            totalOrders = 1,
            totalSpent = 34.99
        ).getOrThrow()

        val txA = createTransaction(
            orderId = orderA.id,
            shopId = shopA.id,
            grossAmount = 34.99
        ).getOrThrow()

        return mapOf(
            "shopId" to shopA.id,
            "productId" to productA.id,
            "orderId" to orderA.id,
            "customerId" to customerA.id,
            "transactionId" to txA.id
        )
    }

    fun setupPrivateTestDataForAccountB(): Map<String, String> {
        signInWithAccountB()
        val shopB = createShop(
            name = "Bianca Ceramics",
            handle = "biancaceramics",
            tagline = "Handmade porcelain pottery",
            description = "Handmade bowls, mugs, and vases",
            mode = ShopMode.REAL_SHOP
        ).getOrThrow()

        val productB = createProduct(
            shopId = shopB.id,
            title = "Speckled Clay Mug",
            description = "Wheel-thrown glazed coffee mug",
            price = 48.00,
            inventory = 12,
            type = ProductType.PHYSICAL
        ).getOrThrow()

        val orderB = createOrder(
            shopId = shopB.id,
            sellerUid = ACCOUNT_B_UID,
            items = listOf(OrderItem(productB.id, productB.title, 2, productB.price)),
            totalAmount = 96.00
        ).getOrThrow()

        val customerB = createCustomer(
            shopId = shopB.id,
            email = "tea_lover_beta@example.com",
            name = "Sarah Lin",
            totalOrders = 3,
            totalSpent = 144.00
        ).getOrThrow()

        val txB = createTransaction(
            orderId = orderB.id,
            shopId = shopB.id,
            grossAmount = 96.00
        ).getOrThrow()

        return mapOf(
            "shopId" to shopB.id,
            "productId" to productB.id,
            "orderId" to orderB.id,
            "customerId" to customerB.id,
            "transactionId" to txB.id
        )
    }

    // ==========================================
    // PHASE 1: SNAP ENGINE & COMMERCE ANALYSIS
    // ==========================================
    fun saveSnapAnalysis(analysis: SnapAnalysis): Result<SnapAnalysis> {
        val list = snapAnalysesTable.getOrPut(analysis.ownerUid) { mutableListOf() }
        list.add(0, analysis)
        return Result.success(analysis)
    }

    fun getSnapAnalysesForCurrentAccount(): List<SnapAnalysis> {
        val currentUser = (authState.value as? AuthState.Authenticated)?.user ?: return emptyList()
        return snapAnalysesTable[currentUser.uid]?.toList() ?: emptyList()
    }

    fun getSnapAnalysesForUser(uid: String): List<SnapAnalysis> {
        return snapAnalysesTable[uid]?.toList() ?: emptyList()
    }

    fun accessSnapAnalysisAs(requesterUid: String, analysisId: String): Result<SnapAnalysis> {
        for ((ownerUid, analyses) in snapAnalysesTable) {
            val found = analyses.find { it.id == analysisId }
            if (found != null) {
                if (found.ownerUid != requesterUid) {
                    return Result.failure(
                        SecurityException("Account Isolation Violation: User '$requesterUid' is not permitted to access SnapAnalysis '$analysisId' owned by '${found.ownerUid}'")
                    )
                }
                return Result.success(found)
            }
        }
        return Result.failure(NoSuchElementException("SnapAnalysis '$analysisId' not found"))
    }

    suspend fun analyzeSnap(
        ownerUid: String,
        photoUri: String,
        base64Image: String,
        mimeType: String = "image/jpeg"
    ): Result<SnapAnalysis> {
        analytics.logEvent(SnapBrandEvent.SNAP_ANALYSIS_STARTED, mapOf("ownerUid" to ownerUid))
        return try {
            val analysis = aiService.analyzeSnap(
                ownerUid = ownerUid,
                photoUri = photoUri,
                base64Image = base64Image,
                mimeType = mimeType
            )
            saveSnapAnalysis(analysis)
            analytics.logEvent(
                SnapBrandEvent.SNAP_ANALYSIS_COMPLETED,
                mapOf(
                    "analysisId" to analysis.id,
                    "category" to analysis.category,
                    "recommendedMode" to analysis.recommendedBusinessMode
                )
            )
            Result.success(analysis)
        } catch (e: Exception) {
            analytics.logEvent(
                SnapBrandEvent.SNAP_ANALYSIS_FAILED,
                mapOf("errorMessage" to (e.message ?: "Unknown error"))
            )
            Result.failure(e)
        }
    }

    // ==========================================
    // PHASE 2 — AI BRAND GENIUS METHODS
    // ==========================================

    fun saveBrandConcept(concept: BrandConcept): Result<BrandConcept> {
        val user = currentUser ?: return Result.failure(IllegalStateException("Must be signed in to save a brand concept."))
        if (concept.ownerUid != user.uid) {
            return Result.failure(SecurityException("Account Isolation Violation: Cannot save brand concept for another user."))
        }
        val list = brandConceptsTable.getOrPut(user.uid) { mutableListOf() }
        val existingIndex = list.indexOfFirst { it.id == concept.id }
        if (existingIndex >= 0) {
            list[existingIndex] = concept
        } else {
            list.add(concept)
        }
        return Result.success(concept)
    }

    fun getBrandConceptsForCurrentAccount(): List<BrandConcept> {
        val user = currentUser ?: return emptyList()
        return brandConceptsTable[user.uid]?.toList() ?: emptyList()
    }

    /**
     * Strictly verifies that only the owner UID can access the brand concept.
     */
    fun accessBrandConceptAs(requesterUid: String, conceptId: String): Result<BrandConcept> {
        for ((ownerUid, concepts) in brandConceptsTable) {
            val matched = concepts.find { it.id == conceptId }
            if (matched != null) {
                if (ownerUid != requesterUid) {
                    return Result.failure(
                        SecurityException("Account Isolation Violation: User $requesterUid cannot access brand concept owned by $ownerUid.")
                    )
                }
                return Result.success(matched)
            }
        }
        return Result.failure(NoSuchElementException("Brand concept $conceptId not found."))
    }

    suspend fun generateBrandConceptForSnap(
        analysis: SnapAnalysis,
        directive: String? = null
    ): Result<BrandConcept> {
        val user = currentUser ?: return Result.failure(IllegalStateException("Must be signed in to generate brand concept."))
        if (analysis.ownerUid != user.uid) {
            return Result.failure(SecurityException("Account Isolation Violation: Cannot generate brand from another user's snap."))
        }

        analytics.logEvent(
            SnapBrandEvent.BRAND_GENERATION_STARTED,
            mapOf(
                "analysisId" to analysis.id,
                "category" to analysis.category,
                "businessMode" to analysis.recommendedBusinessMode,
                "hasDirective" to (!directive.isNullOrBlank()).toString()
            )
        )

        return try {
            val concept = aiService.generateBrandConcept(
                analysis = analysis,
                directive = directive,
                userCountry = user.country
            )
            saveBrandConcept(concept)
            analytics.logEvent(
                SnapBrandEvent.BRAND_GENERATION_COMPLETED,
                mapOf(
                    "brandName" to concept.brandName,
                    "businessMode" to concept.businessMode
                )
            )
            Result.success(concept)
        } catch (e: Exception) {
            analytics.logEvent(
                SnapBrandEvent.BRAND_GENERATION_FAILED,
                mapOf("errorMessage" to (e.message ?: "Unknown error"))
            )
            Result.failure(e)
        }
    }

    suspend fun regenerateBrandName(
        current: BrandConcept,
        analysis: SnapAnalysis,
        directive: String? = null
    ): Result<BrandConcept> {
        val user = currentUser ?: return Result.failure(IllegalStateException("Not authenticated"))
        if (current.ownerUid != user.uid) {
            return Result.failure(SecurityException("Account Isolation Violation"))
        }

        return try {
            val (newName, newHandle) = aiService.regenerateBrandName(current, analysis, directive)
            val updated = current.copy(
                brandName = newName,
                usernameSuggestion = newHandle,
                updatedAt = System.currentTimeMillis()
            )
            saveBrandConcept(updated)
            analytics.logEvent(
                SnapBrandEvent.BRAND_NAME_REGENERATED,
                mapOf("brandName" to newName)
            )
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun regenerateTagline(
        current: BrandConcept,
        analysis: SnapAnalysis,
        directive: String? = null
    ): Result<BrandConcept> {
        val user = currentUser ?: return Result.failure(IllegalStateException("Not authenticated"))
        if (current.ownerUid != user.uid) {
            return Result.failure(SecurityException("Account Isolation Violation"))
        }

        return try {
            val newTagline = aiService.regenerateTagline(current, analysis, directive)
            val updated = current.copy(
                tagline = newTagline,
                updatedAt = System.currentTimeMillis()
            )
            saveBrandConcept(updated)
            analytics.logEvent(
                SnapBrandEvent.TAGLINE_REGENERATED,
                mapOf("tagline" to newTagline)
            )
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun regenerateBrandStory(
        current: BrandConcept,
        analysis: SnapAnalysis,
        directive: String? = null
    ): Result<BrandConcept> {
        val user = currentUser ?: return Result.failure(IllegalStateException("Not authenticated"))
        if (current.ownerUid != user.uid) {
            return Result.failure(SecurityException("Account Isolation Violation"))
        }

        return try {
            val newStory = aiService.regenerateBrandStory(current, analysis, directive)
            val updated = current.copy(
                brandStory = newStory,
                updatedAt = System.currentTimeMillis()
            )
            saveBrandConcept(updated)
            analytics.logEvent(
                SnapBrandEvent.BRAND_STORY_REGENERATED,
                mapOf("brandId" to current.id)
            )
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun updateBrandConcept(updated: BrandConcept): Result<BrandConcept> {
        val user = currentUser ?: return Result.failure(IllegalStateException("Not authenticated"))
        if (updated.ownerUid != user.uid) {
            return Result.failure(SecurityException("Account Isolation Violation"))
        }
        val saved = saveBrandConcept(updated.copy(updatedAt = System.currentTimeMillis()))
        analytics.logEvent(
            SnapBrandEvent.BRAND_EDITED,
            mapOf("brandId" to updated.id)
        )
        return saved
    }

    /**
     * Accepts a brand concept and creates or updates a DRAFT shop.
     * Prevents duplicate shops on resume by checking if a draft already exists
     * for this sourceSnapAnalysisId.
     */
    fun acceptBrandAndCreateDraftShop(concept: BrandConcept): Result<Shop> {
        val user = currentUser ?: return Result.failure(IllegalStateException("Not authenticated"))
        if (concept.ownerUid != user.uid) {
            return Result.failure(SecurityException("Account Isolation Violation"))
        }

        val userShops = shopsTable.getOrPut(user.uid) { mutableListOf() }
        val existingDraft = userShops.find { it.sourceSnapAnalysisId == concept.sourceSnapAnalysisId }

        val shopMode = if (concept.businessMode == "MERCH") ShopMode.MERCH_SHOP else ShopMode.REAL_SHOP
        val shopId = existingDraft?.id ?: ("shop_" + java.util.UUID.randomUUID().toString().take(10))

        // Determine theme from visual style / personality
        val styleLower = "${concept.visualStyle} ${concept.brandPersonality.joinToString(" ")}".lowercase()
        val assignedTheme = when {
            styleLower.contains("luxury") || styleLower.contains("premium") || styleLower.contains("artisan") || styleLower.contains("elegant") -> StorefrontTheme.LUXURY
            styleLower.contains("bold") || styleLower.contains("street") || styleLower.contains("modern") || styleLower.contains("punk") -> StorefrontTheme.BOLD
            styleLower.contains("creative") || styleLower.contains("playful") || styleLower.contains("art") || styleLower.contains("color") -> StorefrontTheme.CREATIVE
            else -> StorefrontTheme.MINIMAL
        }

        // Handle normalization & reservation
        var candidateHandle = StoreHandleSystem.normalize(concept.usernameSuggestion)
        if (candidateHandle.isBlank()) {
            candidateHandle = StoreHandleSystem.normalize(concept.brandName)
        }
        val handleCheck = checkHandleAvailability(candidateHandle, existingDraft?.id)
        val finalHandle = if (handleCheck.isAvailable) {
            candidateHandle
        } else {
            handleCheck.suggestions.firstOrNull() ?: "$candidateHandle-${System.currentTimeMillis() % 1000}"
        }
        reserveHandle(finalHandle, shopId, user.uid)

        val primaryColorHex = concept.suggestedColorDirection.firstOrNull()?.trim()

        val shop = if (existingDraft != null) {
            // Update existing draft with stable ID
            existingDraft.copy(
                name = concept.brandName,
                handle = finalHandle,
                tagline = concept.tagline,
                description = concept.shortDescription,
                story = concept.brandStory,
                targetAudience = concept.targetAudience,
                brandPersonality = concept.brandPersonality,
                visualDirection = "${concept.visualStyle} | Colors: ${concept.suggestedColorDirection.joinToString(", ")}",
                brandConceptId = concept.id,
                businessMode = shopMode,
                theme = assignedTheme,
                primaryColor = primaryColorHex,
                visualStyle = concept.visualStyle,
                status = StoreStatus.DRAFT,
                updatedAt = System.currentTimeMillis()
            ).also { updatedShop ->
                val index = userShops.indexOfFirst { it.id == updatedShop.id }
                if (index >= 0) userShops[index] = updatedShop
            }
        } else {
            // Create new draft shop
            val newShop = Shop(
                id = shopId,
                ownerUid = user.uid,
                name = concept.brandName,
                handle = finalHandle,
                tagline = concept.tagline,
                description = concept.shortDescription,
                story = concept.brandStory,
                targetAudience = concept.targetAudience,
                brandPersonality = concept.brandPersonality,
                visualDirection = "${concept.visualStyle} | Colors: ${concept.suggestedColorDirection.joinToString(", ")}",
                brandConceptId = concept.id,
                sourceSnapAnalysisId = concept.sourceSnapAnalysisId,
                businessMode = shopMode,
                currency = user.currency,
                theme = assignedTheme,
                primaryColor = primaryColorHex,
                visualStyle = concept.visualStyle,
                status = StoreStatus.DRAFT,
                productCount = 0,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            userShops.add(newShop)
            newShop
        }

        analytics.logEvent(
            SnapBrandEvent.BRAND_ACCEPTED,
            mapOf(
                "shopId" to shop.id,
                "brandName" to shop.name,
                "status" to shop.status
            )
        )

        return Result.success(shop)
    }

    fun getDraftShopForSnap(sourceAnalysisId: String): Shop? {
        val user = currentUser ?: return null
        return shopsTable[user.uid]?.find { it.sourceSnapAnalysisId == sourceAnalysisId && it.status == "DRAFT" }
    }

    // ==========================================
    // PHASE 6: SELLER SUBSCRIPTION & BILLING OPERATIONS
    // ==========================================

    /**
     * Retrieves the authoritative subscription record for a creator.
     * Evaluates expiration deterministically using SubscriptionStateMachine.
     */
    fun getAuthoritativeSubscription(ownerUid: String): Subscription {
        val existing = subscriptionsTable[ownerUid] ?: run {
            val empty = Subscription(
                id = "sub_none_$ownerUid",
                ownerUid = ownerUid,
                plan = SubscriptionPlan.NONE,
                status = SubscriptionStatus.INACTIVE,
                amountMinor = 0L,
                currency = "USD",
                currentPeriodEnd = 0L
            )
            subscriptionsTable[ownerUid] = empty
            empty
        }

        // Apply any deterministic expiration
        val evaluated = SubscriptionStateMachine.checkAndApplyExpiration(existing)
        if (evaluated.status != existing.status) {
            subscriptionsTable[ownerUid] = evaluated
            SubscriptionAnalytics.logEvent(
                SubscriptionAnalyticsEvent.SUBSCRIPTION_EXPIRED,
                ownerUid = ownerUid,
                plan = evaluated.plan.planId
            )
        }
        return evaluated
    }

    /**
     * Resolves the safe, read-only entitlement projection for a creator.
     * Separates payments from feature entitlements.
     */
    fun getEntitlements(ownerUid: String): UserEntitlementProjection {
        val sub = getAuthoritativeSubscription(ownerUid)
        return SubscriptionEntitlementResolver.resolveProjection(ownerUid, sub)
    }

    /**
     * Refreshes the entitlement state flow for the current active user.
     */
    fun refreshEntitlements(ownerUid: String): UserEntitlementProjection {
        val projection = getEntitlements(ownerUid)
        if (currentUser?.uid == ownerUid) {
            _entitlementsState.value = projection
        }
        return projection
    }

    /**
     * Initializes a subscription session with the payment provider.
     * Does NOT activate entitlements until verified.
     */
    suspend fun initializeSubscriptionSession(
        ownerUid: String,
        plan: SubscriptionPlan
    ): Result<SubscriptionSession> {
        val user = usersTable[ownerUid] ?: return Result.failure(
            IllegalArgumentException("Creator profile not found for uid: $ownerUid")
        )

        SubscriptionAnalytics.logEvent(
            SubscriptionAnalyticsEvent.SUBSCRIPTION_STARTED,
            ownerUid = ownerUid,
            plan = plan.planId,
            amountMinor = plan.priceUsdMonthlyMinor,
            currency = "USD"
        )

        val initResult = subscriptionProvider.initializeSubscription(
            ownerUid = ownerUid,
            plan = plan,
            customerEmail = user.email
        )

        if (initResult.isSuccess) {
            val session = initResult.getOrThrow()
            // Mark state as PENDING payment
            val current = getAuthoritativeSubscription(ownerUid)
            val pendingSub = current.copy(
                plan = plan,
                status = SubscriptionStatus.PENDING,
                providerReference = session.reference,
                amountMinor = plan.priceUsdMonthlyMinor,
                currency = session.currency,
                updatedAt = System.currentTimeMillis()
            )
            subscriptionsTable[ownerUid] = pendingSub
            refreshEntitlements(ownerUid)

            SubscriptionAnalytics.logEvent(
                SubscriptionAnalyticsEvent.PAYMENT_INITIALIZED,
                ownerUid = ownerUid,
                plan = plan.planId,
                properties = mapOf("reference" to session.reference)
            )
        }

        return initResult
    }

    /**
     * Authoritative backend verification of subscription payment.
     * Activates subscription and unlocks entitlements ONLY after provider verification.
     */
    suspend fun verifyAndActivateSubscription(
        ownerUid: String,
        reference: String,
        expectedPlan: SubscriptionPlan
    ): Result<Subscription> {
        val verification = subscriptionProvider.verifySubscription(
            ownerUid = ownerUid,
            reference = reference,
            expectedPlan = expectedPlan
        )

        if (verification.isFailure) {
            SubscriptionAnalytics.logEvent(
                SubscriptionAnalyticsEvent.SUBSCRIPTION_PAYMENT_FAILED,
                ownerUid = ownerUid,
                plan = expectedPlan.planId,
                properties = mapOf(
                    "reference" to reference,
                    "reason" to (verification.exceptionOrNull()?.message ?: "Verification failed")
                )
            )
            return Result.failure(verification.exceptionOrNull() ?: Exception("Verification failed"))
        }

        val result = verification.getOrThrow()
        if (!result.isVerified) {
            return Result.failure(IllegalStateException("Payment provider did not verify subscription: ${result.failureReason}"))
        }

        val now = System.currentTimeMillis()
        val current = getAuthoritativeSubscription(ownerUid)
        val periodEnd = now + (30L * 24 * 60 * 60 * 1000L) // 30-day billing cycle

        val activated = current.copy(
            id = "sub_${result.reference}",
            ownerUid = ownerUid,
            plan = expectedPlan,
            status = SubscriptionStatus.ACTIVE,
            amountMinor = result.amountMinor,
            currency = result.currency,
            providerReference = result.reference,
            providerSubscriptionId = result.providerSubscriptionId,
            providerCustomerId = result.providerCustomerId,
            currentPeriodStart = now,
            currentPeriodEnd = periodEnd,
            cancelAtPeriodEnd = false,
            updatedAt = now
        )

        subscriptionsTable[ownerUid] = activated

        // Record billing transaction
        val transactions = billingTransactionsTable.getOrPut(ownerUid) { mutableListOf() }
        transactions.add(
            BillingTransaction(
                id = "txn_${result.reference}",
                ownerUid = ownerUid,
                subscriptionId = activated.id,
                provider = subscriptionProvider.providerName,
                providerReference = result.reference,
                amountMinor = result.amountMinor,
                currency = result.currency,
                status = BillingTransactionStatus.PAID,
                paidAt = now,
                createdAt = now
            )
        )

        // Update user profile display strings
        usersTable[ownerUid]?.let { u ->
            usersTable[ownerUid] = u.copy(
                subscriptionPlan = expectedPlan.displayName,
                subscriptionStatus = SubscriptionStatus.ACTIVE.label,
                updatedAt = now
            )
        }

        refreshEntitlements(ownerUid)

        SubscriptionAnalytics.logEvent(
            SubscriptionAnalyticsEvent.SUBSCRIPTION_PAYMENT_SUCCESS,
            ownerUid = ownerUid,
            plan = expectedPlan.planId,
            amountMinor = result.amountMinor,
            currency = result.currency
        )
        SubscriptionAnalytics.logEvent(
            SubscriptionAnalyticsEvent.SUBSCRIPTION_ACTIVATED,
            ownerUid = ownerUid,
            plan = expectedPlan.planId
        )

        return Result.success(activated)
    }

    /**
     * Cancels a subscription at the end of the billing period or immediately.
     * Retains entitled access until currentPeriodEnd if cancelAtPeriodEnd = true.
     */
    suspend fun cancelSubscription(
        ownerUid: String,
        cancelAtPeriodEnd: Boolean = true
    ): Result<Subscription> {
        val current = getAuthoritativeSubscription(ownerUid)
        if (current.status != SubscriptionStatus.ACTIVE && current.status != SubscriptionStatus.PAST_DUE) {
            return Result.failure(IllegalStateException("No active subscription to cancel"))
        }

        SubscriptionAnalytics.logEvent(
            SubscriptionAnalyticsEvent.SUBSCRIPTION_CANCEL_STARTED,
            ownerUid = ownerUid,
            plan = current.plan.planId
        )

        val cancelResult = subscriptionProvider.cancelSubscription(current, cancelAtPeriodEnd)
        if (cancelResult.isFailure) {
            return cancelResult
        }

        val updated = cancelResult.getOrThrow()
        subscriptionsTable[ownerUid] = updated
        refreshEntitlements(ownerUid)

        SubscriptionAnalytics.logEvent(
            SubscriptionAnalyticsEvent.SUBSCRIPTION_CANCELLED,
            ownerUid = ownerUid,
            plan = current.plan.planId,
            properties = mapOf("cancelAtPeriodEnd" to cancelAtPeriodEnd)
        )

        return Result.success(updated)
    }

    /**
     * Authoritative plan change between SNAPBRAND and SNAPBRAND_PRO.
     */
    suspend fun changeSubscriptionPlan(
        ownerUid: String,
        newPlan: SubscriptionPlan
    ): Result<Subscription> {
        val current = getAuthoritativeSubscription(ownerUid)
        if (current.plan == newPlan) {
            return Result.success(current)
        }

        val isUpgrade = newPlan.priceUsdMonthlyMinor > current.plan.priceUsdMonthlyMinor

        SubscriptionAnalytics.logEvent(
            if (isUpgrade) SubscriptionAnalyticsEvent.PLAN_UPGRADE_STARTED else SubscriptionAnalyticsEvent.PLAN_DOWNGRADE_STARTED,
            ownerUid = ownerUid,
            properties = mapOf("fromPlan" to current.plan.planId, "toPlan" to newPlan.planId)
        )

        val result = SubscriptionStateMachine.changePlan(current, newPlan, isUpgrade)
        if (result.isSuccess) {
            val updated = result.getOrThrow()
            subscriptionsTable[ownerUid] = updated

            usersTable[ownerUid]?.let { u ->
                usersTable[ownerUid] = u.copy(
                    subscriptionPlan = newPlan.displayName,
                    updatedAt = System.currentTimeMillis()
                )
            }

            refreshEntitlements(ownerUid)

            SubscriptionAnalytics.logEvent(
                if (isUpgrade) SubscriptionAnalyticsEvent.PLAN_UPGRADED else SubscriptionAnalyticsEvent.PLAN_DOWNGRADE_STARTED,
                ownerUid = ownerUid,
                plan = newPlan.planId
            )
        }
        return result
    }

    /**
     * Tenant-Isolated Billing History Query.
     * Enforces that Creator A can NEVER read Creator B's billing transactions.
     */
    fun getBillingHistory(ownerUid: String, callerUid: String): Result<List<BillingTransaction>> {
        if (ownerUid != callerUid) {
            return Result.failure(
                SecurityException("Access denied: Tenant isolation violation. Caller $callerUid cannot access billing of $ownerUid")
            )
        }
        val transactions = billingTransactionsTable[ownerUid]?.toList() ?: emptyList()
        return Result.success(transactions.sortedByDescending { it.createdAt })
    }

    /**
     * Webhook event handler for server-authoritative recurring billing updates.
     * Fully idempotent, authenticated, and replay-resistant.
     */
    suspend fun handleSubscriptionWebhook(
        payload: String,
        signature: String?
    ): Result<SubscriptionWebhookResult> {
        return subscriptionWebhookHandler.processWebhook(
            payload = payload,
            signature = signature,
            subscriptionLookup = { uid -> subscriptionsTable[uid] },
            onSubscriptionUpdated = { updated ->
                subscriptionsTable[updated.ownerUid] = updated
                usersTable[updated.ownerUid]?.let { u ->
                    usersTable[updated.ownerUid] = u.copy(
                        subscriptionPlan = updated.plan.displayName,
                        subscriptionStatus = updated.status.label,
                        updatedAt = System.currentTimeMillis()
                    )
                }
                refreshEntitlements(updated.ownerUid)
            },
            onTransactionRecorded = { txn ->
                val list = billingTransactionsTable.getOrPut(txn.ownerUid) { mutableListOf() }
                list.add(txn)
            }
        )
    }

    /**
     * Account Deletion Subscription Cleanup.
     * Cancels active billing and cleans local subscription records.
     * Does NOT store secrets or claim external provider cancellations without backend.
     */
    suspend fun onAccountDeletion(ownerUid: String): Result<Unit> {
        val sub = subscriptionsTable[ownerUid]
        if (sub != null && sub.status == SubscriptionStatus.ACTIVE) {
            subscriptionProvider.cancelSubscription(sub, cancelAtPeriodEnd = false)
        }
        subscriptionsTable.remove(ownerUid)
        billingTransactionsTable.remove(ownerUid)
        return Result.success(Unit)
    }

    /**
     * Authoritative feature access gate check.
     */
    fun canAccessFeature(ownerUid: String, feature: FeatureEntitlement): Boolean {
        val sub = getAuthoritativeSubscription(ownerUid)
        return SubscriptionEntitlementResolver.canUseFeature(sub, feature)
    }

    // ==========================================
    // TRANSPARENT FIREBASE DIAGNOSTICS
    // ==========================================
    fun getFirebaseDiagnostics(): FirebaseDiagnosticsReport {
        // Honest audit of runtime environment:
        // google-services.json is absent in the container filesystem.
        // Google Services Gradle plugin uses MissingGoogleServicesStrategy.WARN.
        return FirebaseDiagnosticsReport(
            firebaseInitialized = false,
            googleServicesJsonPresent = false,
            authConfigured = true, // SDK dependencies declared, waiting on google-services.json
            firestoreConfigured = true, // firestore.rules present and hardened, waiting on google-services.json
            storageConfigured = true, // storage.rules present and hardened, waiting on google-services.json
            appCheckConfigured = true, // App Check debug tokens in gradle & manifest
            analyticsConfigured = true, // SnapBrandAnalyticsService event pipeline active
            message = "Firebase configuration: google-services.json is not yet uploaded to the project. Client SDKs, security rules, and architecture boundaries are in place; live connection activates upon providing google-services.json."
        )
    }

    // ==========================================
    // PHASE 7: PRINTIFY INTEGRATION & FULFILLMENT
    // ==========================================

    fun getPrintifyConnection(shopId: String): PrintifyShopConnection? {
        val current = currentUser ?: return null
        return printifyBackendService.getConnection(current.uid, shopId)
    }

    suspend fun connectPrintifyShop(
        shopId: String,
        printifyShopId: String,
        apiToken: String
    ): Result<PrintifyShopConnection> {
        val current = currentUser ?: return Result.failure(IllegalStateException("Authentication required"))
        return printifyBackendService.connectShop(
            ownerUid = current.uid,
            snapbrandShopId = shopId,
            printifyShopId = printifyShopId,
            tokenToValidate = apiToken
        )
    }

    fun disconnectPrintifyShop(shopId: String): Result<PrintifyShopConnection> {
        val current = currentUser ?: return Result.failure(IllegalStateException("Authentication required"))
        return printifyBackendService.disconnectShop(current.uid, shopId)
    }

    suspend fun getPrintifyBlueprints(): Result<List<PrintifyBlueprint>> {
        val current = currentUser ?: return Result.failure(IllegalStateException("Authentication required"))
        return printifyBackendService.getBlueprints(current.uid)
    }

    suspend fun getPrintifyPrintProviders(blueprintId: Int): Result<List<PrintifyPrintProvider>> {
        val current = currentUser ?: return Result.failure(IllegalStateException("Authentication required"))
        return printifyBackendService.getPrintProviders(current.uid, blueprintId)
    }

    suspend fun getPrintifyVariants(blueprintId: Int, providerId: Int): Result<List<PrintifyVariant>> {
        val current = currentUser ?: return Result.failure(IllegalStateException("Authentication required"))
        return printifyBackendService.getVariants(current.uid, blueprintId, providerId)
    }

    suspend fun getPrintifyShippingInfo(blueprintId: Int, providerId: Int): Result<PrintifyShippingEstimate?> {
        val current = currentUser ?: return Result.failure(IllegalStateException("Authentication required"))
        return printifyBackendService.getShippingInfo(current.uid, blueprintId, providerId)
    }

    fun getProduct(productId: String): Product? {
        val current = currentUser ?: return null
        return productsTable[current.uid]?.find { it.id == productId }
    }

    suspend fun createPrintifyProductForMerch(
        productId: String,
        blueprintId: Int,
        providerId: Int,
        variantIds: List<Int>
    ): Result<PrintifyProductMapping> {
        val current = currentUser ?: return Result.failure(IllegalStateException("Authentication required"))
        val product = getProduct(productId)
            ?: return Result.failure(NoSuchElementException("Product not found: $productId"))

        val mappingResult = printifyBackendService.createPrintifyProduct(
            callerUid = current.uid,
            product = product,
            blueprintId = blueprintId,
            providerId = providerId,
            variantIds = variantIds
        )

        if (mappingResult.isSuccess) {
            val mapping = mappingResult.getOrThrow()
            // Update product in repository storage with Printify mapping fields
            val updatedProduct = product.copy(
                printifyProductId = mapping.printifyProductId,
                printifyBlueprintId = mapping.printifyBlueprintId,
                printifyProviderId = mapping.printifyProviderId,
                selectedPrintifyVariantIds = mapping.selectedVariantIds,
                printifyStatus = mapping.printifyStatus,
                printifyLastSyncedAt = mapping.lastSyncedAt
            )
            saveProduct(updatedProduct)
        }

        return mappingResult
    }

    fun getPrintifyProductMapping(productId: String): PrintifyProductMapping? {
        val current = currentUser ?: return null
        return printifyBackendService.getProductMapping(current.uid, productId)
    }

    fun calculateMerchProfit(
        productId: String,
        productionCostMinor: Long? = null,
        shippingCostMinor: Long? = null
    ): PrintifyProfitBreakdown {
        val product = getProduct(productId)
        val sellingPriceMinor = if (product != null) {
            Money.toMinorUnits(product.price)
        } else 0L

        return PrintifyPricingCalculator.calculateBreakdown(
            sellingPriceMinor = sellingPriceMinor,
            productionCostMinor = productionCostMinor,
            shippingCostMinor = shippingCostMinor,
            currency = product?.currency ?: "USD"
        )
    }

    suspend fun syncPaidOrderToPrintify(order: Order): Result<PrintifyOrderMapping> {
        val allProds = productsTable.values.flatten().associateBy { it.id }
        val result = printifyBackendService.syncPaidOrderToPrintify(
            callerUid = order.sellerUid,
            order = order,
            productsById = allProds
        )
        if (result.isSuccess) {
            val mapping = result.getOrThrow()
            if (mapping.printifyOrderId != null) {
                val updatedOrder = order.copy(
                    printifyOrderId = mapping.printifyOrderId,
                    printifySyncStatus = mapping.syncStatus.name,
                    trackingNumber = mapping.trackingNumber ?: order.trackingNumber
                )
                updateOrderInStorage(updatedOrder)
            }
        }
        return result
    }

    fun getPrintifyOrderMapping(orderId: String): PrintifyOrderMapping? {
        val current = currentUser ?: return null
        return printifyBackendService.getOrderMapping(current.uid, orderId)
    }

    fun handlePrintifyWebhook(
        payload: String,
        signatureHeader: String?,
        secretOverride: String? = null
    ): Result<PrintifyWebhookResult> {
        return printifyBackendService.handleWebhook(payload, signatureHeader, secretOverride)
    }
}
