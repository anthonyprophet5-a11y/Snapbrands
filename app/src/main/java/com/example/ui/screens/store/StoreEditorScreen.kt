package com.example.ui.screens.store

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Product
import com.example.data.model.Shop
import com.example.data.model.StoreStatus
import com.example.data.model.StorefrontTheme
import com.example.store.HandleAvailabilityResult
import com.example.store.PublicStorefrontMapper
import com.example.store.PublishValidationResult
import com.example.store.PublishValidator
import com.example.store.StoreHandleSystem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreEditorScreen(
    shop: Shop,
    products: List<Product>,
    isLoading: Boolean,
    actionError: String?,
    onBack: () -> Unit,
    onSaveStore: (Shop) -> Unit,
    onPreviewStore: (Shop) -> Unit,
    onPublishStore: (String) -> Unit,
    onUnpublishStore: (String) -> Unit,
    onToggleProductVisibility: (productId: String, isVisible: Boolean) -> Unit,
    onUpdateFeaturedProducts: (shopId: String, List<String>) -> Unit,
    onCheckHandle: (String, storeId: String?) -> HandleAvailabilityResult
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Identity", "Themes", "Products", "Commerce", "Live Preview")

    // Form state initialized from shop
    var name by remember(shop.id) { mutableStateOf(shop.name) }
    var rawHandle by remember(shop.id) { mutableStateOf(shop.handle) }
    var tagline by remember(shop.id) { mutableStateOf(shop.tagline ?: "") }
    var description by remember(shop.id) { mutableStateOf(shop.description ?: "") }
    var story by remember(shop.id) { mutableStateOf(shop.story ?: "") }
    var contactEmail by remember(shop.id) { mutableStateOf(shop.contactEmail ?: "") }
    var contactPhone by remember(shop.id) { mutableStateOf(shop.contactPhone ?: "") }
    var whatsappNumber by remember(shop.id) { mutableStateOf(shop.whatsappNumber ?: "") }
    var location by remember(shop.id) { mutableStateOf(shop.location ?: "") }

    var selectedTheme by remember(shop.id) { mutableStateOf(shop.theme) }
    var customPrimaryColor by remember(shop.id) { mutableStateOf(shop.primaryColor ?: "") }

    var deliveryInformation by remember(shop.id) { mutableStateOf(shop.deliveryInformation ?: "") }
    var returnPolicy by remember(shop.id) { mutableStateOf(shop.returnPolicy ?: "") }
    var shippingPolicy by remember(shop.id) { mutableStateOf(shop.shippingPolicy ?: "") }
    var privacyPolicy by remember(shop.id) { mutableStateOf(shop.privacyPolicy ?: "") }

    // Featured products local order tracking
    var featuredIds by remember(shop.id, shop.featuredProductIds) {
        mutableStateOf(shop.featuredProductIds)
    }

    // Real-time handle validation state
    var handleAvailability by remember {
        mutableStateOf(onCheckHandle(rawHandle, shop.id))
    }

    LaunchedEffect(rawHandle) {
        handleAvailability = onCheckHandle(rawHandle, shop.id)
    }

    LaunchedEffect(actionError) {
        actionError?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    // Modal dialogs
    var showPublishChecklistDialog by remember { mutableStateOf(false) }
    var showUnpublishConfirmDialog by remember { mutableStateOf(false) }

    // Current working snapshot of shop for saving or previewing
    val workingShop = remember(
        name, rawHandle, tagline, description, story, contactEmail, contactPhone,
        whatsappNumber, location, selectedTheme, customPrimaryColor, deliveryInformation,
        returnPolicy, shippingPolicy, privacyPolicy, featuredIds
    ) {
        shop.copy(
            name = name.trim(),
            handle = StoreHandleSystem.normalize(rawHandle),
            tagline = tagline.trim().ifBlank { null },
            description = description.trim().ifBlank { null },
            story = story.trim().ifBlank { null },
            contactEmail = contactEmail.trim().ifBlank { null },
            contactPhone = contactPhone.trim().ifBlank { null },
            whatsappNumber = whatsappNumber.trim().ifBlank { null },
            location = location.trim().ifBlank { null },
            theme = selectedTheme,
            primaryColor = customPrimaryColor.trim().ifBlank { null },
            deliveryInformation = deliveryInformation.trim().ifBlank { null },
            returnPolicy = returnPolicy.trim().ifBlank { null },
            shippingPolicy = shippingPolicy.trim().ifBlank { null },
            privacyPolicy = privacyPolicy.trim().ifBlank { null },
            featuredProductIds = featuredIds
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Store Editor",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            StoreStatusBadge(status = shop.status)
                        }
                        Text(
                            text = "snapbrand.site/${workingShop.handle}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("store_editor_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Preview Button
                    OutlinedButton(
                        onClick = { onPreviewStore(workingShop) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .height(38.dp)
                            .testTag("store_editor_preview_button")
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Preview", style = MaterialTheme.typography.labelMedium)
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Publish / Unpublish Action Button
                    if (shop.status == StoreStatus.PUBLISHED) {
                        OutlinedButton(
                            onClick = { showUnpublishConfirmDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier
                                .height(38.dp)
                                .testTag("store_editor_unpublish_button")
                        ) {
                            Text("Unpublish", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = { showPublishChecklistDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            modifier = Modifier
                                .height(38.dp)
                                .testTag("store_editor_publish_action_button")
                        ) {
                            Icon(Icons.Default.Public, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Publish", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Save Button
                    Button(
                        onClick = {
                            if (!handleAvailability.isAvailable) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Please fix handle: ${handleAvailability.reason}")
                                }
                            } else {
                                onSaveStore(workingShop)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Store configuration saved successfully.")
                                }
                            }
                        },
                        enabled = !isLoading && handleAvailability.isAvailable,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .height(38.dp)
                            .testTag("store_editor_save_button")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier.testTag("store_tab_$title")
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (selectedTab) {
                    0 -> StoreIdentityTab(
                        name = name,
                        onNameChange = { name = it },
                        rawHandle = rawHandle,
                        onHandleChange = { rawHandle = it },
                        handleAvailability = handleAvailability,
                        onSelectSuggestion = { rawHandle = it },
                        tagline = tagline,
                        onTaglineChange = { tagline = it },
                        description = description,
                        onDescriptionChange = { description = it },
                        story = story,
                        onStoryChange = { story = it },
                        contactEmail = contactEmail,
                        onContactEmailChange = { contactEmail = it },
                        contactPhone = contactPhone,
                        onContactPhoneChange = { contactPhone = it },
                        whatsappNumber = whatsappNumber,
                        onWhatsappNumberChange = { whatsappNumber = it },
                        location = location,
                        onLocationChange = { location = it }
                    )
                    1 -> StoreThemesTab(
                        selectedTheme = selectedTheme,
                        onThemeSelected = { selectedTheme = it },
                        customPrimaryColor = customPrimaryColor,
                        onColorChange = { customPrimaryColor = it },
                        visualStyle = shop.visualStyle
                    )
                    2 -> StoreProductsTab(
                        products = products,
                        featuredIds = featuredIds,
                        onToggleVisibility = { pid, isVis ->
                            onToggleProductVisibility(pid, isVis)
                        },
                        onToggleFeatured = { pid ->
                            val updated = if (featuredIds.contains(pid)) {
                                featuredIds - pid
                            } else {
                                featuredIds + pid
                            }
                            featuredIds = updated
                            onUpdateFeaturedProducts(shop.id, updated)
                        },
                        onMoveFeaturedUp = { index ->
                            if (index > 0) {
                                val list = featuredIds.toMutableList()
                                val item = list.removeAt(index)
                                list.add(index - 1, item)
                                featuredIds = list
                                onUpdateFeaturedProducts(shop.id, list)
                            }
                        },
                        onMoveFeaturedDown = { index ->
                            if (index < featuredIds.size - 1) {
                                val list = featuredIds.toMutableList()
                                val item = list.removeAt(index)
                                list.add(index + 1, item)
                                featuredIds = list
                                onUpdateFeaturedProducts(shop.id, list)
                            }
                        }
                    )
                    3 -> StoreCommerceTab(
                        currency = shop.currency,
                        country = shop.country,
                        businessMode = shop.businessMode.displayName,
                        deliveryInformation = deliveryInformation,
                        onDeliveryChange = { deliveryInformation = it },
                        returnPolicy = returnPolicy,
                        onReturnPolicyChange = { returnPolicy = it },
                        shippingPolicy = shippingPolicy,
                        onShippingPolicyChange = { shippingPolicy = it },
                        privacyPolicy = privacyPolicy,
                        onPrivacyPolicyChange = { privacyPolicy = it }
                    )
                    4 -> {
                        val publicStore = remember(workingShop, products) {
                            PublicStorefrontMapper.toPublicStorefront(workingShop, products)
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            StorefrontRenderer(
                                storefront = publicStore,
                                themeShop = workingShop,
                                viewMode = StorefrontViewMode.SELLER_PREVIEW_MOBILE,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }

    // 1. Publish Validation Dialog
    if (showPublishChecklistDialog) {
        val validation = PublishValidator.validate(
            shop = workingShop,
            products = products,
            isHandleTakenByOther = { !handleAvailability.isAvailable }
        )

        PublishChecklistDialog(
            validation = validation,
            shop = workingShop,
            onConfirmPublish = {
                showPublishChecklistDialog = false
                onPublishStore(shop.id)
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("🎉 Store published live! Accessible at snapbrand.site/${workingShop.handle}")
                }
            },
            onDismiss = { showPublishChecklistDialog = false }
        )
    }

    // 2. Unpublish Confirmation Dialog
    if (showUnpublishConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showUnpublishConfirmDialog = false },
            title = {
                Text("Unpublish Storefront?", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Your store will be hidden from public customer access. All your listings, theme styling, and products remain 100% saved and intact. You can republish at any time.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showUnpublishConfirmDialog = false
                        onUnpublishStore(shop.id)
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Store unpublished. Status changed to UNPUBLISHED.")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Unpublish Store", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnpublishConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ==========================================
// TAB 1: IDENTITY
// ==========================================
@Composable
private fun StoreIdentityTab(
    name: String,
    onNameChange: (String) -> Unit,
    rawHandle: String,
    onHandleChange: (String) -> Unit,
    handleAvailability: HandleAvailabilityResult,
    onSelectSuggestion: (String) -> Unit,
    tagline: String,
    onTaglineChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    story: String,
    onStoryChange: (String) -> Unit,
    contactEmail: String,
    onContactEmailChange: (String) -> Unit,
    contactPhone: String,
    onContactPhoneChange: (String) -> Unit,
    whatsappNumber: String,
    onWhatsappNumberChange: (String) -> Unit,
    location: String,
    onLocationChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Store Name Field
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Store Name *") },
            placeholder = { Text("e.g. VaporWave Threads") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("store_name_input")
        )

        // Web Handle Field with live availability
        Column {
            OutlinedTextField(
                value = rawHandle,
                onValueChange = onHandleChange,
                label = { Text("Store Web Handle (URL) *") },
                prefix = { Text("snapbrand.site/") },
                trailingIcon = {
                    if (handleAvailability.isAvailable) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Available", tint = Color(0xFF10B981))
                    } else {
                        Icon(Icons.Default.ErrorOutline, contentDescription = "Unavailable", tint = MaterialTheme.colorScheme.error)
                    }
                },
                isError = !handleAvailability.isAvailable,
                supportingText = {
                    if (!handleAvailability.isAvailable) {
                        Text(handleAvailability.reason ?: "Handle is unavailable", color = MaterialTheme.colorScheme.error)
                    } else {
                        Text("Customers access your storefront via this link", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("store_handle_input")
            )

            // Handle Suggestions
            if (!handleAvailability.isAvailable && handleAvailability.suggestions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Try:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    handleAvailability.suggestions.forEach { suggestion ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSelectSuggestion(suggestion) }
                        ) {
                            Text(
                                text = suggestion,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Tagline
        OutlinedTextField(
            value = tagline,
            onValueChange = onTaglineChange,
            label = { Text("Tagline / Catchphrase") },
            placeholder = { Text("e.g. Futuristic aesthetic apparel & prints") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("store_tagline_input")
        )

        // Short Description
        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            label = { Text("Store Description") },
            placeholder = { Text("A concise overview of what your shop offers...") },
            minLines = 2,
            maxLines = 4,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("store_description_input")
        )

        // Brand Story
        OutlinedTextField(
            value = story,
            onValueChange = onStoryChange,
            label = { Text("Brand Origin & Story") },
            placeholder = { Text("Share the creative vision behind this brand...") },
            minLines = 3,
            maxLines = 6,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("store_story_input")
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        Text(
            text = "Direct Customer Contact Channels",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        // Contact Email
        OutlinedTextField(
            value = contactEmail,
            onValueChange = onContactEmailChange,
            label = { Text("Customer Service Email") },
            placeholder = { Text("support@yourbrand.com") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("store_email_input")
        )

        // Contact Phone
        OutlinedTextField(
            value = contactPhone,
            onValueChange = onContactPhoneChange,
            label = { Text("Customer Service Phone") },
            placeholder = { Text("+1 (555) 019-2831") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("store_phone_input")
        )

        // WhatsApp Number
        OutlinedTextField(
            value = whatsappNumber,
            onValueChange = onWhatsappNumberChange,
            label = { Text("WhatsApp Number") },
            placeholder = { Text("+15550192831") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("store_whatsapp_input")
        )

        // Location / City
        OutlinedTextField(
            value = location,
            onValueChange = onLocationChange,
            label = { Text("Studio / City Location") },
            placeholder = { Text("e.g. Austin, TX") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("store_location_input")
        )

        Spacer(modifier = Modifier.height(20.dp))
    }
}

// ==========================================
// TAB 2: THEMES
// ==========================================
@Composable
private fun StoreThemesTab(
    selectedTheme: StorefrontTheme,
    onThemeSelected: (StorefrontTheme) -> Unit,
    customPrimaryColor: String,
    onColorChange: (String) -> Unit,
    visualStyle: String?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Storefront Presentation Themes",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Select a curated layout aesthetic for your public storefront. All themes share the same underlying product listings and policies.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (!visualStyle.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AI Brand Recommendation: $visualStyle",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Theme selection cards
        StorefrontTheme.entries.forEach { theme ->
            ThemeOptionCard(
                theme = theme,
                isSelected = selectedTheme == theme,
                onSelect = { onThemeSelected(theme) }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Custom Accent Color Override",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = customPrimaryColor,
            onValueChange = onColorChange,
            label = { Text("Primary Hex Color Code") },
            placeholder = { Text("e.g. #4338CA or leave blank to use theme default") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("theme_color_override_input")
        )

        // Palette presets
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val presets = listOf("#18181B", "#4338CA", "#D4AF37", "#F43F5E", "#059669", "#7C3AED", "#2563EB", "#EA580C")
            presets.forEach { hex ->
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(android.graphics.Color.parseColor(hex)))
                        .border(
                            width = if (customPrimaryColor.equals(hex, ignoreCase = true)) 3.dp else 1.dp,
                            color = if (customPrimaryColor.equals(hex, ignoreCase = true)) MaterialTheme.colorScheme.primary else Color.LightGray,
                            shape = CircleShape
                        )
                        .clickable { onColorChange(hex) }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun ThemeOptionCard(
    theme: StorefrontTheme,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onSelect() }
            .testTag("theme_option_${theme.code.lowercase()}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = theme.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // Color swatches
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(theme.primaryHex)))
                        )
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(theme.secondaryHex)))
                        )
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(theme.backgroundHex)))
                                .border(0.5.dp, Color.Gray, CircleShape)
                        )
                    }
                }

                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = theme.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ==========================================
// TAB 3: PRODUCTS & SHOWCASE
// ==========================================
@Composable
private fun StoreProductsTab(
    products: List<Product>,
    featuredIds: List<String>,
    onToggleVisibility: (productId: String, isVisible: Boolean) -> Unit,
    onToggleFeatured: (productId: String) -> Unit,
    onMoveFeaturedUp: (index: Int) -> Unit,
    onMoveFeaturedDown: (index: Int) -> Unit
) {
    val visibleCount = products.count { it.isVisible }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Storefront Visibility",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$visibleCount of ${products.size} products shown on storefront",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = RoundedCornerShape(100.dp),
                color = if (visibleCount > 0) Color(0xFF10B981).copy(alpha = 0.15f) else MaterialTheme.colorScheme.errorContainer
            ) {
                Text(
                    text = if (visibleCount > 0) "$visibleCount VISIBLE" else "NO VISIBLE PRODUCTS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (visibleCount > 0) Color(0xFF059669) else MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        if (visibleCount == 0) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Your store cannot be published without at least one visible product. Toggle on visibility for the items you want customers to see.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        if (products.isEmpty()) {
            Text(
                text = "No products found for this shop. Generate products in Product Studio first.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            products.forEach { product ->
                val isFeatured = featuredIds.contains(product.id)
                ProductVisibilityRow(
                    product = product,
                    isFeatured = isFeatured,
                    onToggleVisibility = { onToggleVisibility(product.id, it) },
                    onToggleFeatured = { onToggleFeatured(product.id) }
                )
            }
        }

        if (featuredIds.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Featured Products Order",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Adjust the display sequence of your featured showcase items on the storefront hero.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            featuredIds.forEachIndexed { index, fid ->
                val prod = products.find { it.id == fid }
                if (prod != null) {
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${index + 1}. ${prod.title}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )

                            Row {
                                IconButton(
                                    onClick = { onMoveFeaturedUp(index) },
                                    enabled = index > 0,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", modifier = Modifier.size(16.dp))
                                }
                                IconButton(
                                    onClick = { onMoveFeaturedDown(index) },
                                    enabled = index < featuredIds.size - 1,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun ProductVisibilityRow(
    product: Product,
    isFeatured: Boolean,
    onToggleVisibility: (Boolean) -> Unit,
    onToggleFeatured: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (product.isVisible) MaterialTheme.colorScheme.surface
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (product.isVisible) MaterialTheme.colorScheme.outlineVariant
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("product_visibility_card_${product.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Image
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    val img = product.finalProductImageUrl ?: product.sourcePhotoUrl
                    if (!img.isNullOrBlank()) {
                        AsyncImage(
                            model = img,
                            contentDescription = product.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(24.dp))
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$${String.format(java.util.Locale.US, "%.2f", product.price)} • ${product.category}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (product.isVisible) "Visible" else "Hidden",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (product.isVisible) Color(0xFF059669) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Switch(
                        checked = product.isVisible,
                        onCheckedChange = onToggleVisibility,
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF10B981)),
                        modifier = Modifier.testTag("switch_visible_${product.id}")
                    )
                }
            }

            if (product.isVisible) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = if (isFeatured) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isFeatured) "Featured on Storefront Hero" else "Not Featured",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    FilterChip(
                        selected = isFeatured,
                        onClick = onToggleFeatured,
                        label = { Text(if (isFeatured) "★ Featured" else "Make Featured", style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.testTag("chip_feature_${product.id}")
                    )
                }
            }
        }
    }
}

// ==========================================
// TAB 4: COMMERCE & POLICIES
// ==========================================
@Composable
private fun StoreCommerceTab(
    currency: String,
    country: String,
    businessMode: String,
    deliveryInformation: String,
    onDeliveryChange: (String) -> Unit,
    returnPolicy: String,
    onReturnPolicyChange: (String) -> Unit,
    shippingPolicy: String,
    onShippingPolicyChange: (String) -> Unit,
    privacyPolicy: String,
    onPrivacyPolicyChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Commerce Information & Customer Policies",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        // Read-only settings
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "Store Configuration", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Text(text = "Business Mode: $businessMode", style = MaterialTheme.typography.bodyMedium)
                Text(text = "Base Currency: $currency", style = MaterialTheme.typography.bodyMedium)
                Text(text = "Country of Operation: $country", style = MaterialTheme.typography.bodyMedium)
            }
        }

        // Informational notice about real fulfillment in later phases
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Phase 4 provides informational policy publishing. Live payment processing via Paystack and Printify production unlock in subsequent phases.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        // Delivery Information Notice
        OutlinedTextField(
            value = deliveryInformation,
            onValueChange = onDeliveryChange,
            label = { Text("Delivery & Fulfillment Notice") },
            placeholder = { Text("e.g. Items are printed and shipped within 3-5 business days...") },
            minLines = 2,
            maxLines = 4,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("store_delivery_input")
        )

        // Return Policy
        OutlinedTextField(
            value = returnPolicy,
            onValueChange = onReturnPolicyChange,
            label = { Text("Return & Exchange Policy") },
            placeholder = { Text("e.g. 14-day replacement guarantee on defective items...") },
            minLines = 2,
            maxLines = 4,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("store_returns_input")
        )

        // Shipping Policy
        OutlinedTextField(
            value = shippingPolicy,
            onValueChange = onShippingPolicyChange,
            label = { Text("Shipping Guidelines") },
            placeholder = { Text("e.g. Domestic orders ship via standard tracked courier...") },
            minLines = 2,
            maxLines = 4,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("store_shipping_input")
        )

        // Privacy Policy
        OutlinedTextField(
            value = privacyPolicy,
            onValueChange = onPrivacyPolicyChange,
            label = { Text("Privacy Policy Notice") },
            placeholder = { Text("e.g. Customer shipping details are processed securely...") },
            minLines = 2,
            maxLines = 4,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("store_privacy_input")
        )

        Spacer(modifier = Modifier.height(20.dp))
    }
}

// ==========================================
// PUBLISH CHECKLIST DIALOG
// ==========================================
@Composable
private fun PublishChecklistDialog(
    validation: PublishValidationResult,
    shop: Shop,
    onConfirmPublish: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (validation.canPublish) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = if (validation.canPublish) Color(0xFF10B981) else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (validation.canPublish) "Ready to Publish!" else "Publishing Requirements",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (validation.canPublish)
                        "Your shop meets all launch requirements and will be immediately live to visitors at snapbrand.site/${shop.handle}."
                    else
                        "Complete the following requirements before your store can be published live:",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Checklist Items
                PublishCriterionItem(
                    label = "Store Name",
                    isSatisfied = shop.name.isNotBlank(),
                    detail = shop.name.ifBlank { "Missing" }
                )

                PublishCriterionItem(
                    label = "Web Handle",
                    isSatisfied = StoreHandleSystem.isValid(shop.handle),
                    detail = "snapbrand.site/${shop.handle}"
                )

                PublishCriterionItem(
                    label = "Brand Identity",
                    isSatisfied = !shop.tagline.isNullOrBlank() || !shop.description.isNullOrBlank(),
                    detail = shop.tagline ?: shop.description ?: "Missing"
                )

                PublishCriterionItem(
                    label = "At Least 1 Visible Product",
                    isSatisfied = validation.missingRequirements.none { it.contains("visible product", ignoreCase = true) },
                    detail = if (validation.missingRequirements.any { it.contains("visible product", ignoreCase = true) })
                        "No products set to visible" else "Products ready"
                )

                // Warnings advisory
                if (validation.warnings.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Advisory Notice:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            validation.warnings.forEach { w ->
                                Text(
                                    text = "• $w",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmPublish,
                enabled = validation.canPublish,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                modifier = Modifier.testTag("confirm_publish_button")
            ) {
                Text("Publish Live Now", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (validation.canPublish) "Cancel" else "Go Back & Edit")
            }
        }
    )
}

@Composable
private fun PublishCriterionItem(
    label: String,
    isSatisfied: Boolean,
    detail: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isSatisfied) Icons.Default.CheckCircle else Icons.Default.Close,
            contentDescription = null,
            tint = if (isSatisfied) Color(0xFF10B981) else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = if (isSatisfied) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
