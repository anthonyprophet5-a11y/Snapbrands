package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.repository.AuthState
import com.example.ui.components.AccountSwitcherSheet
import com.example.ui.components.AppDestination
import com.example.ui.components.SnapBrandBottomNav
import com.example.ui.components.SnapBrandNavigationRail
import com.example.ui.components.SnapBrandTopBar
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.BillingScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.OrdersScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ShopsScreen
import com.example.ui.screens.store.StoreEditorScreen
import com.example.ui.screens.store.StorefrontPreviewScreen
import com.example.ui.theme.SnapBrandTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()
            SnapBrandTheme(darkTheme = isDarkTheme) {
                SnapBrandApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnapBrandApp(viewModel: MainViewModel) {
    val authState by viewModel.authState.collectAsState()
    val currentDestination by viewModel.currentDestination.collectAsState()
    val isInSettings by viewModel.isInSettings.collectAsState()
    val isInBilling by viewModel.isInBilling.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val shops by viewModel.shops.collectAsState()
    val discoverableProducts by viewModel.discoverableProducts.collectAsState()
    val orders by viewModel.orders.collectAsState()
    val currentBrandConcept by viewModel.currentBrandConcept.collectAsState()
    val isGeneratingBrand by viewModel.isGeneratingBrand.collectAsState()
    val isRegeneratingField by viewModel.isRegeneratingField.collectAsState()
    val brandError by viewModel.brandError.collectAsState()
    val currentProducts by viewModel.currentProducts.collectAsState()
    val isGeneratingProducts by viewModel.isGeneratingProducts.collectAsState()
    val isRegeneratingProduct by viewModel.isRegeneratingProduct.collectAsState()
    val productError by viewModel.productError.collectAsState()

    // Phase 6 Subscription & Billing State
    val entitlements by viewModel.entitlements.collectAsState()
    val currentSubscription by viewModel.currentSubscription.collectAsState()
    val billingHistory by viewModel.billingHistory.collectAsState()
    val isSubscriptionLoading by viewModel.isSubscriptionLoading.collectAsState()
    val subscriptionError by viewModel.subscriptionError.collectAsState()

    val editingShopId by viewModel.editingShopId.collectAsState()
    val activePreviewShop by viewModel.activePreviewShop.collectAsState()
    val isStoreActionLoading by viewModel.isStoreActionLoading.collectAsState()
    val storeActionError by viewModel.storeActionError.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    var showAccountSwitcher by remember { mutableStateOf(false) }
    val accountSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    when (val state = authState) {
        is AuthState.Unauthenticated -> {
            AuthScreen(
                onSignInWithEmail = { email, pass -> viewModel.signInWithEmail(email, pass) },
                onSignUpWithEmail = { email, pass, name -> viewModel.signUpWithEmail(email, pass, name) },
                onSignInWithGoogle = { viewModel.signInWithGoogle() },
                onSignInAsGuest = { viewModel.signInAsGuest() },
                onSelectAccountA = { viewModel.signInWithAccountA() },
                onSelectAccountB = { viewModel.signInWithAccountB() }
            )
        }
        is AuthState.Loading -> {
            // Smooth transition
        }
        is AuthState.Authenticated, is AuthState.Error -> {
            val user = (state as? AuthState.Authenticated)?.user

            if (isInBilling && user != null) {
                BillingScreen(
                    subscription = currentSubscription,
                    entitlement = entitlements,
                    billingHistory = billingHistory,
                    isLoading = isSubscriptionLoading,
                    errorMessage = subscriptionError,
                    onSubscribe = { plan ->
                        coroutineScope.launch {
                            val initResult = viewModel.initializeSubscription(plan)
                            if (initResult.isSuccess) {
                                val session = initResult.getOrThrow()
                                viewModel.verifySubscriptionPayment(session.reference, plan)
                            }
                        }
                    },
                    onUpgradeOrDowngrade = { newPlan ->
                        coroutineScope.launch {
                            viewModel.changeSubscriptionPlan(newPlan)
                        }
                    },
                    onCancelSubscription = { cancelAtEnd ->
                        coroutineScope.launch {
                            viewModel.cancelSubscription(cancelAtEnd)
                        }
                    },
                    onBack = { viewModel.closeBilling() }
                )
            } else if (isInSettings && user != null) {
                SettingsScreen(
                    user = user,
                    isDarkTheme = isDarkTheme,
                    onToggleDarkTheme = { viewModel.toggleDarkTheme(it) },
                    onToggleNotifications = { viewModel.updateNotifications(it) },
                    onSignOut = { viewModel.signOut() },
                    onBack = { viewModel.closeSettings() },
                    onNavigateToBilling = { viewModel.openBilling() }
                )
            } else if (editingShopId != null) {
                val editingShop = shops.find { it.id == editingShopId } ?: viewModel.getShopById(editingShopId!!)
                if (editingShop != null) {
                    val shopProducts = remember(editingShopId, currentProducts) {
                        viewModel.getProductsForShop(editingShop.id)
                    }
                    StoreEditorScreen(
                        shop = editingShop,
                        products = shopProducts,
                        isLoading = isStoreActionLoading,
                        actionError = storeActionError,
                        onBack = { viewModel.closeStoreEditor() },
                        onSaveStore = { updated -> viewModel.updateStore(updated) },
                        onPreviewStore = { shop -> viewModel.openStorePreview(shop) },
                        onPublishStore = { shopId -> viewModel.publishStore(shopId) },
                        onUnpublishStore = { shopId -> viewModel.unpublishStore(shopId) },
                        onToggleProductVisibility = { productId, isVisible ->
                            viewModel.setProductVisibility(productId, isVisible)
                        },
                        onUpdateFeaturedProducts = { shopId, featuredIds ->
                            viewModel.setFeaturedProducts(shopId, featuredIds)
                        },
                        onCheckHandle = { handle, storeId ->
                            viewModel.checkHandle(handle, storeId)
                        }
                    )
                } else {
                    viewModel.closeStoreEditor()
                }
            } else if (activePreviewShop != null) {
                val previewShop = activePreviewShop!!
                val previewProducts = remember(previewShop.id, currentProducts) {
                    viewModel.getProductsForShop(previewShop.id)
                }
                StorefrontPreviewScreen(
                    shop = previewShop,
                    products = previewProducts,
                    onBack = { viewModel.closeStorePreview() },
                    onOpenEditor = {
                        viewModel.openStoreEditor(previewShop.id)
                        viewModel.closeStorePreview()
                    }
                )
            } else {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val isExpanded = maxWidth >= 600.dp

                    Scaffold(
                        topBar = {
                            SnapBrandTopBar(
                                currentDestination = currentDestination,
                                currentUser = user,
                                onSwitchAccountClick = { showAccountSwitcher = true },
                                onSettingsClick = { viewModel.openSettings() }
                            )
                        },
                        bottomBar = {
                            if (!isExpanded) {
                                SnapBrandBottomNav(
                                    currentDestination = currentDestination,
                                    onNavigate = { viewModel.navigateTo(it) }
                                )
                            }
                        }
                    ) { innerPadding ->
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            if (isExpanded) {
                                SnapBrandNavigationRail(
                                    currentDestination = currentDestination,
                                    onNavigate = { viewModel.navigateTo(it) }
                                )
                            }

                            when (currentDestination) {
                                AppDestination.HOME -> {
                                    HomeScreen(
                                        currentUser = user,
                                        onNavigateToShops = { viewModel.navigateTo(AppDestination.DISCOVER) },
                                        discoverableProducts = discoverableProducts,
                                        userShops = shops,
                                        onOpenShop = { shop -> viewModel.openStorePreview(shop) },
                                        onAnalyzeImage = { photoUri, base64, mimeType ->
                                            viewModel.analyzeSnapPhoto(photoUri, base64, mimeType)
                                        },
                                        onLogAnalytics = { event, params ->
                                            viewModel.logAnalyticsEvent(event, params)
                                        },
                                        brandConcept = currentBrandConcept,
                                        isGeneratingBrand = isGeneratingBrand,
                                        isRegeneratingField = isRegeneratingField,
                                        brandError = brandError,
                                        onGenerateBrand = { analysis, directive ->
                                            viewModel.startBrandGeneration(analysis, directive)
                                        },
                                        onRegenerateName = { directive ->
                                            viewModel.regenerateBrandName(directive)
                                        },
                                        onRegenerateTagline = { directive ->
                                            viewModel.regenerateTagline(directive)
                                        },
                                        onRegenerateStory = { directive ->
                                            viewModel.regenerateBrandStory(directive)
                                        },
                                        onUpdateBrandConcept = { updated ->
                                            viewModel.updateBrandConcept(updated)
                                        },
                                        onAcceptBrand = {
                                            viewModel.acceptBrandAndCreateDraftShop()
                                        },
                                        products = currentProducts,
                                        isGeneratingProducts = isGeneratingProducts,
                                        isRegeneratingProduct = isRegeneratingProduct,
                                        productError = productError,
                                        onGenerateProducts = { shopId, directive, forceRegen ->
                                            viewModel.generateProductsForShop(shopId, directive, forceRegen)
                                        },
                                        onSaveProduct = { product ->
                                            viewModel.saveProduct(product)
                                        },
                                        onRemoveProduct = { productId ->
                                            viewModel.removeProduct(productId)
                                        },
                                        onRegenerateSingleProduct = { id, action, directive ->
                                            viewModel.regenerateSingleProduct(id, action, directive)
                                        },
                                        onSaveAllProducts = { shopId ->
                                            viewModel.saveAllProducts(shopId)
                                        },
                                        onOpenStoreEditor = { shopId ->
                                            viewModel.openStoreEditor(shopId)
                                        }
                                    )
                                }
                                AppDestination.CREATE -> {
                                    HomeScreen(
                                        currentUser = user,
                                        onNavigateToShops = { viewModel.navigateTo(AppDestination.DISCOVER) },
                                        discoverableProducts = discoverableProducts,
                                        userShops = shops,
                                        onOpenShop = { shop -> viewModel.openStorePreview(shop) },
                                        onAnalyzeImage = { photoUri, base64, mimeType ->
                                            viewModel.analyzeSnapPhoto(photoUri, base64, mimeType)
                                        },
                                        onLogAnalytics = { event, params ->
                                            viewModel.logAnalyticsEvent(event, params)
                                        },
                                        brandConcept = currentBrandConcept,
                                        isGeneratingBrand = isGeneratingBrand,
                                        isRegeneratingField = isRegeneratingField,
                                        brandError = brandError,
                                        onGenerateBrand = { analysis, directive ->
                                            viewModel.startBrandGeneration(analysis, directive)
                                        },
                                        onRegenerateName = { directive ->
                                            viewModel.regenerateBrandName(directive)
                                        },
                                        onRegenerateTagline = { directive ->
                                            viewModel.regenerateTagline(directive)
                                        },
                                        onRegenerateStory = { directive ->
                                            viewModel.regenerateBrandStory(directive)
                                        },
                                        onUpdateBrandConcept = { updated ->
                                            viewModel.updateBrandConcept(updated)
                                        },
                                        onAcceptBrand = {
                                            viewModel.acceptBrandAndCreateDraftShop()
                                        },
                                        products = currentProducts,
                                        isGeneratingProducts = isGeneratingProducts,
                                        isRegeneratingProduct = isRegeneratingProduct,
                                        productError = productError,
                                        onGenerateProducts = { shopId, directive, forceRegen ->
                                            viewModel.generateProductsForShop(shopId, directive, forceRegen)
                                        },
                                        onSaveProduct = { product ->
                                            viewModel.saveProduct(product)
                                        },
                                        onRemoveProduct = { productId ->
                                            viewModel.removeProduct(productId)
                                        },
                                        onRegenerateSingleProduct = { id, action, directive ->
                                            viewModel.regenerateSingleProduct(id, action, directive)
                                        },
                                        onSaveAllProducts = { shopId ->
                                            viewModel.saveAllProducts(shopId)
                                        },
                                        onOpenStoreEditor = { shopId ->
                                            viewModel.openStoreEditor(shopId)
                                        }
                                    )
                                }
                                AppDestination.DISCOVER -> {
                                    ShopsScreen(
                                        shops = shops,
                                        discoverableProducts = discoverableProducts,
                                        onCreateShop = { name, handle, tagline, desc, mode ->
                                            viewModel.createShop(name, handle, tagline, desc, mode)
                                        },
                                        onEditShop = { shopId ->
                                            viewModel.openStoreEditor(shopId)
                                        },
                                        onPreviewShop = { shop ->
                                            viewModel.openStorePreview(shop)
                                        },
                                        onAddToCart = { storeId, productId, variantId, qty ->
                                            viewModel.addToCart(storeId, productId, variantId, qty)
                                        }
                                    )
                                }
                                AppDestination.ORDERS -> {
                                    OrdersScreen(
                                        orders = orders,
                                        onUpdateOrderStatus = { orderId, newStatus ->
                                            viewModel.updateOrderStatus(orderId, newStatus)
                                        }
                                    )
                                }
                                AppDestination.PROFILE -> {
                                    if (user != null) {
                                        ProfileScreen(
                                            user = user,
                                            onUpdateProfile = { name, username, country, currency, bio ->
                                                viewModel.updateProfile(name, username, country, currency, bio)
                                            },
                                            onSignOut = { viewModel.signOut() },
                                            onSwitchAccountClick = { showAccountSwitcher = true },
                                            onManageSubscriptionClick = { viewModel.openBilling() }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (showAccountSwitcher) {
                        AccountSwitcherSheet(
                            sheetState = accountSheetState,
                            currentUser = user,
                            onSelectAccountA = { viewModel.signInWithAccountA() },
                            onSelectAccountB = { viewModel.signInWithAccountB() },
                            onSelectGuest = { viewModel.signInAsGuest() },
                            onSignOutToLogin = { viewModel.signOut() },
                            onDismiss = { showAccountSwitcher = false }
                        )
                    }
                }
            }
        }
    }
}
