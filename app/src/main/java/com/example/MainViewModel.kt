package com.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.BrandConcept
import com.example.data.model.CartItem
import com.example.data.model.CustomerDeliveryInfo
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.Product
import com.example.data.model.Shop
import com.example.data.model.ShopMode
import com.example.data.model.SnapAnalysis
import com.example.data.model.StoreStatus
import com.example.data.model.StorefrontTheme
import com.example.data.model.UserProfile
import com.example.data.repository.AuthState
import com.example.data.repository.SnapBrandRepository
import com.example.service.AnalyticsService
import com.example.service.SnapBrandAnalyticsService
import com.example.service.SnapBrandEvent
import com.example.store.HandleAvailabilityResult
import com.example.store.PublicStorefront
import com.example.store.PublishValidationResult
import com.example.store.checkout.CartState
import com.example.store.payment.PaymentInitResponse
import com.example.subscription.BillingTransaction
import com.example.subscription.FeatureEntitlement
import com.example.subscription.Subscription
import com.example.subscription.SubscriptionPlan
import com.example.subscription.SubscriptionStatus
import com.example.subscription.UserEntitlementProjection
import com.example.subscription.payment.SubscriptionSession
import com.example.ui.components.AppDestination
import com.example.printify.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    val repository: SnapBrandRepository = SnapBrandRepository(),
    private val analytics: AnalyticsService = SnapBrandAnalyticsService()
) : ViewModel() {

    val authState: StateFlow<AuthState> = repository.authState

    private val _currentDestination = MutableStateFlow(AppDestination.HOME)
    val currentDestination: StateFlow<AppDestination> = _currentDestination.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _isInSettings = MutableStateFlow(false)
    val isInSettings: StateFlow<Boolean> = _isInSettings.asStateFlow()

    private val _isInBilling = MutableStateFlow(false)
    val isInBilling: StateFlow<Boolean> = _isInBilling.asStateFlow()

    // Phase 6 Subscription & Billing State
    val entitlements: StateFlow<UserEntitlementProjection> = repository.entitlementsState

    private val _currentSubscription = MutableStateFlow<Subscription?>(null)
    val currentSubscription: StateFlow<Subscription?> = _currentSubscription.asStateFlow()

    private val _billingHistory = MutableStateFlow<List<BillingTransaction>>(emptyList())
    val billingHistory: StateFlow<List<BillingTransaction>> = _billingHistory.asStateFlow()

    private val _isSubscriptionLoading = MutableStateFlow(false)
    val isSubscriptionLoading: StateFlow<Boolean> = _isSubscriptionLoading.asStateFlow()

    private val _subscriptionError = MutableStateFlow<String?>(null)
    val subscriptionError: StateFlow<String?> = _subscriptionError.asStateFlow()

    // Phase 7 Printify Integration State
    private val _printifyConnection = MutableStateFlow<PrintifyShopConnection?>(null)
    val printifyConnection: StateFlow<PrintifyShopConnection?> = _printifyConnection.asStateFlow()

    private val _printifyBlueprints = MutableStateFlow<List<PrintifyBlueprint>>(emptyList())
    val printifyBlueprints: StateFlow<List<PrintifyBlueprint>> = _printifyBlueprints.asStateFlow()

    private val _printifyProviders = MutableStateFlow<List<PrintifyPrintProvider>>(emptyList())
    val printifyProviders: StateFlow<List<PrintifyPrintProvider>> = _printifyProviders.asStateFlow()

    private val _printifyVariants = MutableStateFlow<List<PrintifyVariant>>(emptyList())
    val printifyVariants: StateFlow<List<PrintifyVariant>> = _printifyVariants.asStateFlow()

    private val _printifyShipping = MutableStateFlow<PrintifyShippingEstimate?>(null)
    val printifyShipping: StateFlow<PrintifyShippingEstimate?> = _printifyShipping.asStateFlow()

    private val _isPrintifyLoading = MutableStateFlow(false)
    val isPrintifyLoading: StateFlow<Boolean> = _isPrintifyLoading.asStateFlow()

    private val _printifyError = MutableStateFlow<String?>(null)
    val printifyError: StateFlow<String?> = _printifyError.asStateFlow()

    // Discovery & Marketplace State
    private val _discoverableProducts = MutableStateFlow<List<Pair<Product, Shop>>>(emptyList())
    val discoverableProducts: StateFlow<List<Pair<Product, Shop>>> = _discoverableProducts.asStateFlow()

    private val _shops = MutableStateFlow<List<Shop>>(emptyList())
    val shops: StateFlow<List<Shop>> = _shops.asStateFlow()

    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders.asStateFlow()

    // Phase 1 Snap Engine State
    private val _snapAnalyses = MutableStateFlow<List<SnapAnalysis>>(emptyList())
    val snapAnalyses: StateFlow<List<SnapAnalysis>> = _snapAnalyses.asStateFlow()

    private val _currentSnapAnalysis = MutableStateFlow<SnapAnalysis?>(null)
    val currentSnapAnalysis: StateFlow<SnapAnalysis?> = _currentSnapAnalysis.asStateFlow()

    private val _isAnalyzingSnap = MutableStateFlow(false)
    val isAnalyzingSnap: StateFlow<Boolean> = _isAnalyzingSnap.asStateFlow()

    private val _snapError = MutableStateFlow<String?>(null)
    val snapError: StateFlow<String?> = _snapError.asStateFlow()

    // Phase 2 Brand Genius State
    private val _currentBrandConcept = MutableStateFlow<BrandConcept?>(null)
    val currentBrandConcept: StateFlow<BrandConcept?> = _currentBrandConcept.asStateFlow()

    private val _isGeneratingBrand = MutableStateFlow(false)
    val isGeneratingBrand: StateFlow<Boolean> = _isGeneratingBrand.asStateFlow()

    private val _isRegeneratingField = MutableStateFlow<String?>(null)
    val isRegeneratingField: StateFlow<String?> = _isRegeneratingField.asStateFlow()

    private val _brandError = MutableStateFlow<String?>(null)
    val brandError: StateFlow<String?> = _brandError.asStateFlow()

    private val _draftShopCreated = MutableStateFlow<Shop?>(null)
    val draftShopCreated: StateFlow<Shop?> = _draftShopCreated.asStateFlow()

    // Phase 3 Product Engine State
    private val _currentProducts = MutableStateFlow<List<Product>>(emptyList())
    val currentProducts: StateFlow<List<Product>> = _currentProducts.asStateFlow()

    private val _isGeneratingProducts = MutableStateFlow(false)
    val isGeneratingProducts: StateFlow<Boolean> = _isGeneratingProducts.asStateFlow()

    private val _isRegeneratingProduct = MutableStateFlow<String?>(null)
    val isRegeneratingProduct: StateFlow<String?> = _isRegeneratingProduct.asStateFlow()

    private val _productError = MutableStateFlow<String?>(null)
    val productError: StateFlow<String?> = _productError.asStateFlow()

    // Phase 4 Store Engine State
    private val _editingShopId = MutableStateFlow<String?>(null)
    val editingShopId: StateFlow<String?> = _editingShopId.asStateFlow()

    private val _activePreviewShop = MutableStateFlow<Shop?>(null)
    val activePreviewShop: StateFlow<Shop?> = _activePreviewShop.asStateFlow()

    private val _isStoreActionLoading = MutableStateFlow(false)
    val isStoreActionLoading: StateFlow<Boolean> = _isStoreActionLoading.asStateFlow()

    private val _storeActionError = MutableStateFlow<String?>(null)
    val storeActionError: StateFlow<String?> = _storeActionError.asStateFlow()

    init {
        viewModelScope.launch {
            repository.authState.collect { state ->
                if (state is AuthState.Authenticated) {
                    refreshUserData()
                } else {
                    _shops.value = emptyList()
                    _orders.value = emptyList()
                    _snapAnalyses.value = emptyList()
                    _currentSnapAnalysis.value = null
                    _currentBrandConcept.value = null
                    _draftShopCreated.value = null
                    _currentProducts.value = emptyList()
                    _productError.value = null
                }
            }
        }
    }

    fun navigateTo(destination: AppDestination) {
        _isInSettings.value = false
        _currentDestination.value = destination
    }

    fun openSettings() {
        _isInSettings.value = true
    }

    fun closeSettings() {
        _isInSettings.value = false
    }

    fun toggleDarkTheme(enabled: Boolean) {
        _isDarkTheme.value = enabled
    }

    fun refreshUserData() {
        _shops.value = repository.getShopsForCurrentAccount()
        _orders.value = repository.getOrdersForCurrentAccount()
        _snapAnalyses.value = repository.getSnapAnalysesForCurrentAccount()
        _discoverableProducts.value = repository.getAllDiscoverableProducts()
        repository.currentUser?.uid?.let { uid ->
            _currentSubscription.value = repository.getAuthoritativeSubscription(uid)
            repository.refreshEntitlements(uid)
            val historyResult = repository.getBillingHistory(uid, uid)
            if (historyResult.isSuccess) {
                _billingHistory.value = historyResult.getOrThrow()
            }
        }
    }

    fun openBilling() {
        _isInSettings.value = false
        _isInBilling.value = true
        loadSubscriptionData()
    }

    fun closeBilling() {
        _isInBilling.value = false
    }

    fun loadSubscriptionData() {
        val uid = repository.currentUser?.uid ?: return
        _isSubscriptionLoading.value = true
        _subscriptionError.value = null
        try {
            _currentSubscription.value = repository.getAuthoritativeSubscription(uid)
            repository.refreshEntitlements(uid)
            val historyResult = repository.getBillingHistory(uid, uid)
            if (historyResult.isSuccess) {
                _billingHistory.value = historyResult.getOrThrow()
            }
        } catch (e: Exception) {
            _subscriptionError.value = e.message
        } finally {
            _isSubscriptionLoading.value = false
        }
    }

    suspend fun initializeSubscription(plan: SubscriptionPlan): Result<SubscriptionSession> {
        val uid = repository.currentUser?.uid ?: return Result.failure(IllegalStateException("Not authenticated"))
        _isSubscriptionLoading.value = true
        _subscriptionError.value = null
        val result = repository.initializeSubscriptionSession(uid, plan)
        _isSubscriptionLoading.value = false
        if (result.isFailure) {
            _subscriptionError.value = result.exceptionOrNull()?.message ?: "Failed to initialize subscription"
        }
        return result
    }

    suspend fun verifySubscriptionPayment(reference: String, plan: SubscriptionPlan): Result<Subscription> {
        val uid = repository.currentUser?.uid ?: return Result.failure(IllegalStateException("Not authenticated"))
        _isSubscriptionLoading.value = true
        _subscriptionError.value = null
        val result = repository.verifyAndActivateSubscription(uid, reference, plan)
        _isSubscriptionLoading.value = false
        if (result.isSuccess) {
            _currentSubscription.value = result.getOrThrow()
            loadSubscriptionData()
        } else {
            _subscriptionError.value = result.exceptionOrNull()?.message ?: "Subscription payment verification failed"
        }
        return result
    }

    suspend fun cancelSubscription(cancelAtPeriodEnd: Boolean = true): Result<Subscription> {
        val uid = repository.currentUser?.uid ?: return Result.failure(IllegalStateException("Not authenticated"))
        _isSubscriptionLoading.value = true
        _subscriptionError.value = null
        val result = repository.cancelSubscription(uid, cancelAtPeriodEnd)
        _isSubscriptionLoading.value = false
        if (result.isSuccess) {
            _currentSubscription.value = result.getOrThrow()
            loadSubscriptionData()
        } else {
            _subscriptionError.value = result.exceptionOrNull()?.message ?: "Failed to cancel subscription"
        }
        return result
    }

    suspend fun changeSubscriptionPlan(newPlan: SubscriptionPlan): Result<Subscription> {
        val uid = repository.currentUser?.uid ?: return Result.failure(IllegalStateException("Not authenticated"))
        _isSubscriptionLoading.value = true
        _subscriptionError.value = null
        val result = repository.changeSubscriptionPlan(uid, newPlan)
        _isSubscriptionLoading.value = false
        if (result.isSuccess) {
            _currentSubscription.value = result.getOrThrow()
            loadSubscriptionData()
        } else {
            _subscriptionError.value = result.exceptionOrNull()?.message ?: "Failed to change plan"
        }
        return result
    }

    fun canAccessFeature(feature: FeatureEntitlement): Boolean {
        val uid = repository.currentUser?.uid ?: return false
        return repository.canAccessFeature(uid, feature)
    }

    // Auth actions
    fun signInWithAccountA() {
        repository.signInWithAccountA()
        refreshUserData()
    }

    fun signInWithAccountB() {
        repository.signInWithAccountB()
        refreshUserData()
    }

    fun signInAsGuest() {
        repository.signInAsGuest()
        refreshUserData()
    }

    fun signInWithEmail(email: String, pass: String): Result<UserProfile> {
        val res = repository.signInWithEmail(email, pass)
        if (res.isSuccess) {
            refreshUserData()
        }
        return res
    }

    fun signUpWithEmail(email: String, pass: String, displayName: String = ""): Result<UserProfile> {
        val res = repository.signUpWithEmail(email, pass, displayName)
        if (res.isSuccess) {
            refreshUserData()
        }
        return res
    }

    fun signInWithGoogle(): Result<UserProfile> {
        val res = repository.signInWithGoogle()
        refreshUserData()
        return res
    }

    fun getFirebaseDiagnostics() = repository.getFirebaseDiagnostics()

    fun signOut() {
        repository.signOut()
        _shops.value = emptyList()
        _orders.value = emptyList()
        _snapAnalyses.value = emptyList()
        _currentSnapAnalysis.value = null
        _isInSettings.value = false
    }

    // Profile actions
    fun updateProfile(name: String, username: String, country: String, currency: String, bio: String?) {
        repository.updateProfile(name, username, country, currency, bio)
    }

    fun updateNotifications(enabled: Boolean) {
        repository.updateNotifications(enabled)
    }

    // Shop actions
    fun createShop(name: String, handle: String, tagline: String?, desc: String?, mode: ShopMode) {
        repository.createShop(name, handle, tagline, desc, mode)
        _shops.value = repository.getShopsForCurrentAccount()
    }

    // ==========================================
    // PHASE 1 SNAP ENGINE ACTIONS
    // ==========================================
    suspend fun analyzeSnapPhoto(
        photoUri: String,
        base64Image: String,
        mimeType: String = "image/jpeg"
    ): Result<SnapAnalysis> {
        val currentUser = (authState.value as? AuthState.Authenticated)?.user
        val ownerUid = currentUser?.uid ?: "guest_user"

        _isAnalyzingSnap.value = true
        _snapError.value = null

        val result = repository.analyzeSnap(
            ownerUid = ownerUid,
            photoUri = photoUri,
            base64Image = base64Image,
            mimeType = mimeType
        )

        _isAnalyzingSnap.value = false

        if (result.isSuccess) {
            val analysis = result.getOrThrow()
            _currentSnapAnalysis.value = analysis
            _snapAnalyses.value = repository.getSnapAnalysesForCurrentAccount()
        } else {
            val ex = result.exceptionOrNull()
            _snapError.value = ex?.message ?: "An unexpected error occurred during photo analysis."
        }

        return result
    }

    fun clearCurrentSnap() {
        _currentSnapAnalysis.value = null
        _snapError.value = null
    }

    fun selectSnapAnalysis(analysis: SnapAnalysis) {
        _currentSnapAnalysis.value = analysis
    }

    // ==========================================
    // PHASE 2 — BRAND GENIUS ACTIONS
    // ==========================================

    suspend fun startBrandGeneration(
        analysis: SnapAnalysis,
        directive: String? = null
    ): Result<BrandConcept> {
        _isGeneratingBrand.value = true
        _brandError.value = null

        val result = repository.generateBrandConceptForSnap(analysis, directive)
        _isGeneratingBrand.value = false

        if (result.isSuccess) {
            _currentBrandConcept.value = result.getOrThrow()
        } else {
            val ex = result.exceptionOrNull()
            _brandError.value = ex?.message ?: "Failed to generate brand concept."
        }

        return result
    }

    suspend fun regenerateBrandName(directive: String? = null): Result<BrandConcept> {
        val current = _currentBrandConcept.value ?: return Result.failure(IllegalStateException("No brand concept loaded"))
        val analysis = _currentSnapAnalysis.value ?: return Result.failure(IllegalStateException("No snap analysis loaded"))

        _isRegeneratingField.value = "name"
        val result = repository.regenerateBrandName(current, analysis, directive)
        _isRegeneratingField.value = null

        if (result.isSuccess) {
            _currentBrandConcept.value = result.getOrThrow()
        }
        return result
    }

    suspend fun regenerateTagline(directive: String? = null): Result<BrandConcept> {
        val current = _currentBrandConcept.value ?: return Result.failure(IllegalStateException("No brand concept loaded"))
        val analysis = _currentSnapAnalysis.value ?: return Result.failure(IllegalStateException("No snap analysis loaded"))

        _isRegeneratingField.value = "tagline"
        val result = repository.regenerateTagline(current, analysis, directive)
        _isRegeneratingField.value = null

        if (result.isSuccess) {
            _currentBrandConcept.value = result.getOrThrow()
        }
        return result
    }

    suspend fun regenerateBrandStory(directive: String? = null): Result<BrandConcept> {
        val current = _currentBrandConcept.value ?: return Result.failure(IllegalStateException("No brand concept loaded"))
        val analysis = _currentSnapAnalysis.value ?: return Result.failure(IllegalStateException("No snap analysis loaded"))

        _isRegeneratingField.value = "story"
        val result = repository.regenerateBrandStory(current, analysis, directive)
        _isRegeneratingField.value = null

        if (result.isSuccess) {
            _currentBrandConcept.value = result.getOrThrow()
        }
        return result
    }

    fun updateBrandConcept(updated: BrandConcept): Result<BrandConcept> {
        val result = repository.updateBrandConcept(updated)
        if (result.isSuccess) {
            _currentBrandConcept.value = result.getOrThrow()
        }
        return result
    }

    fun acceptBrandAndCreateDraftShop(): Result<Shop> {
        val concept = _currentBrandConcept.value ?: return Result.failure(IllegalStateException("No brand concept loaded"))
        val result = repository.acceptBrandAndCreateDraftShop(concept)
        if (result.isSuccess) {
            val shop = result.getOrThrow()
            _draftShopCreated.value = shop
            _shops.value = repository.getShopsForCurrentAccount()
        }
        return result
    }

    fun getExistingDraftShopForSnap(analysisId: String): Shop? {
        return repository.getDraftShopForSnap(analysisId)
    }

    fun clearBrandConcept() {
        _currentBrandConcept.value = null
        _draftShopCreated.value = null
        _brandError.value = null
        _currentProducts.value = emptyList()
        _productError.value = null
    }

    // ==========================================
    // Phase 3 Product Engine Operations
    // ==========================================
    fun generateProductsForShop(
        shopId: String? = null,
        directive: String? = null,
        forceRegenerate: Boolean = false
    ) {
        val analysis = _currentSnapAnalysis.value ?: return
        val brand = _currentBrandConcept.value
        val effectiveShopId = shopId ?: _draftShopCreated.value?.id ?: ("shop_" + analysis.id)
        val mode = _draftShopCreated.value?.businessMode?.name ?: analysis.recommendedBusinessMode

        if (!forceRegenerate) {
            val existing = repository.getProductsForShop(effectiveShopId)
            if (existing.isNotEmpty()) {
                _currentProducts.value = existing
                return
            }
        }

        viewModelScope.launch {
            _isGeneratingProducts.value = true
            _productError.value = null
            val result = repository.generateProductsForSnap(
                shopId = effectiveShopId,
                analysis = analysis,
                brandConcept = brand,
                businessMode = mode,
                currency = "USD",
                directive = directive
            )
            _isGeneratingProducts.value = false
            if (result.isSuccess) {
                _currentProducts.value = result.getOrThrow()
                repository.saveProducts(effectiveShopId, _currentProducts.value)
                _shops.value = repository.getShopsForCurrentAccount()
            } else {
                _productError.value = result.exceptionOrNull()?.message ?: "Failed to generate products"
            }
        }
    }

    fun loadProductsForShop(shopId: String) {
        val products = repository.getProductsForShop(shopId)
        _currentProducts.value = products
    }

    fun saveProduct(product: Product): Result<Product> {
        val result = repository.saveProduct(product)
        if (result.isSuccess) {
            val updated = result.getOrThrow()
            _currentProducts.value = _currentProducts.value.map {
                if (it.id == updated.id) updated else it
            }
            _shops.value = repository.getShopsForCurrentAccount()
        }
        return result
    }

    fun saveAllProducts(shopId: String): Result<List<Product>> {
        val result = repository.saveProducts(shopId, _currentProducts.value)
        if (result.isSuccess) {
            _currentProducts.value = result.getOrThrow()
            _shops.value = repository.getShopsForCurrentAccount()
        }
        return result
    }

    fun removeProduct(productId: String) {
        val result = repository.removeProduct(productId)
        if (result.isSuccess) {
            _currentProducts.value = _currentProducts.value.filter { it.id != productId }
            _shops.value = repository.getShopsForCurrentAccount()
        }
    }

    fun updateProductLocally(product: Product) {
        val validatedInventory = if (product.inventory < 0) 0 else product.inventory
        val validatedStatus = if (validatedInventory == 0) "OUT_OF_STOCK" else product.status
        val updated = product.copy(
            inventory = validatedInventory,
            status = validatedStatus,
            updatedAt = System.currentTimeMillis()
        )
        _currentProducts.value = _currentProducts.value.map {
            if (it.id == updated.id) updated else it
        }
        repository.saveProduct(updated)
        _shops.value = repository.getShopsForCurrentAccount()
    }

    fun regenerateSingleProduct(productId: String, action: String, directive: String? = null) {
        val product = _currentProducts.value.find { it.id == productId } ?: return
        val brand = _currentBrandConcept.value
        val analysis = _currentSnapAnalysis.value

        viewModelScope.launch {
            _isRegeneratingProduct.value = productId
            val result = repository.regenerateSingleProduct(product, brand, analysis, action, directive)
            _isRegeneratingProduct.value = null
            if (result.isSuccess) {
                val updated = result.getOrThrow()
                _currentProducts.value = _currentProducts.value.map {
                    if (it.id == updated.id) updated else it
                }
                repository.saveProduct(updated)
                _shops.value = repository.getShopsForCurrentAccount()
            }
        }
    }

    // ==========================================
    // PHASE 4 STORE ENGINE METHODS
    // ==========================================
    fun openStoreEditor(shopId: String) {
        _editingShopId.value = shopId
        loadProductsForShop(shopId)
        analytics.logEvent(SnapBrandEvent.STORE_EDITOR_OPENED, mapOf("shopId" to shopId))
    }

    fun closeStoreEditor() {
        _editingShopId.value = null
        _storeActionError.value = null
    }

    fun openStorePreview(shop: Shop) {
        _activePreviewShop.value = shop
        loadProductsForShop(shop.id)
        analytics.logEvent(SnapBrandEvent.STORE_PREVIEW_OPENED, mapOf("shopId" to shop.id))
    }

    fun closeStorePreview() {
        _activePreviewShop.value = null
    }

    fun getShopById(shopId: String): Shop? {
        return repository.getShopById(shopId)
    }

    fun getProductsForShop(shopId: String): List<Product> {
        return repository.getProductsForShop(shopId)
    }

    fun checkHandle(handle: String, storeId: String? = null): HandleAvailabilityResult {
        return repository.checkHandleAvailability(handle, storeId)
    }

    fun updateStore(shop: Shop): Result<Shop> {
        _isStoreActionLoading.value = true
        _storeActionError.value = null
        val result = repository.updateStore(shop)
        _isStoreActionLoading.value = false
        if (result.isSuccess) {
            refreshUserData()
            if (_activePreviewShop.value?.id == shop.id) {
                _activePreviewShop.value = result.getOrThrow()
            }
        } else {
            _storeActionError.value = result.exceptionOrNull()?.message ?: "Failed to update store"
        }
        return result
    }

    fun setProductVisibility(productId: String, isVisible: Boolean): Result<Product> {
        val result = repository.setProductVisibility(productId, isVisible)
        if (result.isSuccess) {
            val updated = result.getOrThrow()
            _currentProducts.value = _currentProducts.value.map {
                if (it.id == updated.id) updated else it
            }
            refreshUserData()
        }
        return result
    }

    fun setFeaturedProducts(shopId: String, featuredProductIds: List<String>): Result<Shop> {
        val result = repository.setFeaturedProducts(shopId, featuredProductIds)
        if (result.isSuccess) {
            val updatedShop = result.getOrThrow()
            _currentProducts.value = _currentProducts.value.map {
                it.copy(isFeatured = featuredProductIds.contains(it.id))
            }
            refreshUserData()
            if (_activePreviewShop.value?.id == shopId) {
                _activePreviewShop.value = updatedShop
            }
        }
        return result
    }

    fun validateForPublish(shopId: String): PublishValidationResult {
        return repository.validateShopForPublish(shopId)
    }

    fun publishStore(shopId: String): Result<Shop> {
        _isStoreActionLoading.value = true
        _storeActionError.value = null
        val result = repository.publishStore(shopId)
        _isStoreActionLoading.value = false
        if (result.isSuccess) {
            val published = result.getOrThrow()
            refreshUserData()
            if (_activePreviewShop.value?.id == shopId) {
                _activePreviewShop.value = published
            }
        } else {
            _storeActionError.value = result.exceptionOrNull()?.message ?: "Publishing failed"
        }
        return result
    }

    fun unpublishStore(shopId: String): Result<Shop> {
        _isStoreActionLoading.value = true
        _storeActionError.value = null
        val result = repository.unpublishStore(shopId)
        _isStoreActionLoading.value = false
        if (result.isSuccess) {
            val unpublished = result.getOrThrow()
            refreshUserData()
            if (_activePreviewShop.value?.id == shopId) {
                _activePreviewShop.value = unpublished
            }
        } else {
            _storeActionError.value = result.exceptionOrNull()?.message ?: "Unpublishing failed"
        }
        return result
    }

    fun resolvePublicStorefront(handle: String): Result<PublicStorefront> {
        return repository.resolvePublicStorefront(handle)
    }

    fun logAnalyticsEvent(event: SnapBrandEvent, params: Map<String, Any> = emptyMap()) {
        analytics.logEvent(event, params)
    }

    fun updateOrderStatus(orderId: String, newStatus: OrderStatus): Result<Order> {
        val result = repository.updateOrderStatus(orderId, newStatus)
        if (result.isSuccess) {
            refreshUserData()
        }
        return result
    }

    // Phase 5 Cart and Checkout flows
    fun getCart(storeId: String): CartState = repository.getCart(storeId)

    fun addToCart(storeId: String, productId: String, variantId: String? = null, quantity: Int = 1): Result<CartState> {
        return repository.addToCart(storeId, productId, variantId, quantity)
    }

    fun updateCartQuantity(storeId: String, cartItemId: String, newQuantity: Int): Result<CartState> {
        return repository.updateCartQuantity(storeId, cartItemId, newQuantity)
    }

    fun removeFromCart(storeId: String, cartItemId: String): CartState {
        return repository.removeFromCart(storeId, cartItemId)
    }

    fun clearCart(storeId: String): CartState {
        return repository.clearCart(storeId)
    }

    fun createCheckoutOrder(storeId: String, deliveryInfo: CustomerDeliveryInfo, items: List<CartItem>): Result<Order> {
        val result = repository.createCheckoutOrder(storeId, deliveryInfo, items)
        if (result.isSuccess) {
            refreshUserData()
        }
        return result
    }

    suspend fun initializeOrderPayment(orderId: String): Result<PaymentInitResponse> {
        return repository.initializeOrderPayment(orderId)
    }

    suspend fun verifyOrderPayment(orderId: String, reference: String): Result<Order> {
        val result = repository.verifyOrderPayment(orderId, reference)
        if (result.isSuccess) {
            refreshUserData()
        }
        return result
    }

    // Phase 7 Printify Integration flows
    fun loadPrintifyConnection(shopId: String) {
        _printifyConnection.value = repository.getPrintifyConnection(shopId)
    }

    fun connectPrintifyShop(shopId: String, printifyShopId: String, apiToken: String, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _isPrintifyLoading.value = true
            _printifyError.value = null
            val result = repository.connectPrintifyShop(shopId, printifyShopId, apiToken)
            _isPrintifyLoading.value = false
            if (result.isSuccess) {
                _printifyConnection.value = result.getOrThrow()
                onComplete(true, null)
            } else {
                val err = result.exceptionOrNull()?.message ?: "Failed to connect Printify"
                _printifyError.value = err
                onComplete(false, err)
            }
        }
    }

    fun disconnectPrintifyShop(shopId: String) {
        val result = repository.disconnectPrintifyShop(shopId)
        if (result.isSuccess) {
            _printifyConnection.value = result.getOrThrow()
        }
    }

    fun loadPrintifyCatalog() {
        viewModelScope.launch {
            _isPrintifyLoading.value = true
            val result = repository.getPrintifyBlueprints()
            _isPrintifyLoading.value = false
            if (result.isSuccess) {
                _printifyBlueprints.value = result.getOrThrow()
            } else {
                _printifyError.value = result.exceptionOrNull()?.message
            }
        }
    }

    fun loadPrintifyProviders(blueprintId: Int) {
        viewModelScope.launch {
            _isPrintifyLoading.value = true
            val result = repository.getPrintifyPrintProviders(blueprintId)
            _isPrintifyLoading.value = false
            if (result.isSuccess) {
                _printifyProviders.value = result.getOrThrow()
            }
        }
    }

    fun loadPrintifyVariants(blueprintId: Int, providerId: Int) {
        viewModelScope.launch {
            _isPrintifyLoading.value = true
            val result = repository.getPrintifyVariants(blueprintId, providerId)
            val shippingResult = repository.getPrintifyShippingInfo(blueprintId, providerId)
            _isPrintifyLoading.value = false
            if (result.isSuccess) {
                _printifyVariants.value = result.getOrThrow()
            }
            if (shippingResult.isSuccess) {
                _printifyShipping.value = shippingResult.getOrNull()
            }
        }
    }

    fun createPrintifyProduct(
        productId: String,
        blueprintId: Int,
        providerId: Int,
        variantIds: List<Int>,
        onComplete: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            _isPrintifyLoading.value = true
            _printifyError.value = null
            val result = repository.createPrintifyProductForMerch(productId, blueprintId, providerId, variantIds)
            _isPrintifyLoading.value = false
            if (result.isSuccess) {
                refreshUserData()
                onComplete(true, null)
            } else {
                val err = result.exceptionOrNull()?.message ?: "Failed to create Printify product"
                _printifyError.value = err
                onComplete(false, err)
            }
        }
    }

    fun getPrintifyProductMapping(productId: String): PrintifyProductMapping? {
        return repository.getPrintifyProductMapping(productId)
    }

    fun calculateMerchProfit(productId: String, prodCost: Long? = null, shipCost: Long? = null): PrintifyProfitBreakdown {
        return repository.calculateMerchProfit(productId, prodCost, shipCost)
    }
}
