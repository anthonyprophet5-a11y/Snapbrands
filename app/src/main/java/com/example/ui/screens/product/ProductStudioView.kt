package com.example.ui.screens.product

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalMall
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.BrandConcept
import com.example.data.model.Product
import com.example.data.model.SnapAnalysis
import com.example.ui.theme.SnapAmberWarning
import com.example.ui.theme.SnapCrimsonError
import com.example.ui.theme.SnapEmeraldSuccess
import com.example.ui.theme.SnapAmberAccent
import com.example.ui.theme.SnapIndigoPrimary

/**
 * Phase 3 — Product Studio
 * Displays AI-generated product recommendations with honest mockup placeholders,
 * clear "AI estimate" pricing, editable attributes/inventory, and commercial rights awareness.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductStudioView(
    shopId: String,
    ownerUid: String,
    sourceAnalysis: SnapAnalysis,
    brandConcept: BrandConcept?,
    businessMode: String,
    products: List<Product>,
    isLoading: Boolean,
    regeneratingProductId: String?,
    errorMessage: String?,
    onSaveProduct: (Product) -> Unit,
    onRemoveProduct: (String) -> Unit,
    onRegenerateProduct: (productId: String, action: String, directive: String?) -> Unit,
    onRegenerateAllProducts: (directive: String?) -> Unit,
    onAcceptAndFinish: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var showRegenerateAllDialog by remember { mutableStateOf(false) }
    var customDirective by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "PRODUCT STUDIO",
                            style = MaterialTheme.typography.labelSmall,
                            letterSpacing = 1.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = SnapAmberAccent
                        )
                        Text(
                            text = brandConcept?.brandName ?: "Your Products",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("product_studio_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Brand Genius"
                        )
                    }
                },
                actions = {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Text(
                            text = if (businessMode == "MERCH") "MERCH MODE" else "REAL SHOP MODE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showRegenerateAllDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("regenerate_all_products_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Regenerate Ideas", maxLines = 1)
                        }

                        Button(
                            onClick = onAcceptAndFinish,
                            enabled = products.isNotEmpty() && !isLoading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SnapIndigoPrimary
                            ),
                            modifier = Modifier
                                .weight(1.3f)
                                .testTag("save_products_to_shop_button")
                        ) {
                            Text(
                                text = "Save to Shop",
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        },
        modifier = modifier.testTag("product_studio_screen")
    ) { innerPadding ->
        if (isLoading) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        color = SnapIndigoPrimary,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "AI Product Engine Generating...",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (businessMode == "MERCH")
                            "Crafting merchandise catalog inspired by ${sourceAnalysis.detectedSubject}"
                        else
                            "Preparing listing and specifications for ${sourceAnalysis.detectedSubject}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    bottom = innerPadding.calculateBottomPadding() + 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // 1. Source Context Header
                item {
                    ProductStudioHeader(
                        sourceAnalysis = sourceAnalysis,
                        brandConcept = brandConcept,
                        businessMode = businessMode
                    )
                }

                // 2. Commercial Rights Reminder Banner (if warning present)
                if (!sourceAnalysis.rightsWarning.isNullOrBlank()) {
                    item {
                        CommercialRightsBanner(warning = sourceAnalysis.rightsWarning)
                    }
                }

                // 3. Error Banner (if any)
                if (!errorMessage.isNullOrBlank()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SnapCrimsonError.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SnapCrimsonError.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = SnapCrimsonError,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = errorMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SnapCrimsonError
                                )
                            }
                        }
                    }
                }

                // 4. Products Title & Count
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "RECOMMENDED PRODUCTS (${products.size})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Status: Draft Store",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // 5. Product Cards
                items(products, key = { it.id }) { product ->
                    ProductCardItem(
                        product = product,
                        businessMode = businessMode,
                        isRegenerating = regeneratingProductId == product.id,
                        onEdit = { editingProduct = product },
                        onRemove = { onRemoveProduct(product.id) }
                    )
                }

                item {
                    Spacer(Modifier.height(20.dp))
                }
            }
        }
    }

    // Modal Bottom Sheet: Product Editor
    editingProduct?.let { productToEdit ->
        ProductEditorSheet(
            product = productToEdit,
            businessMode = businessMode,
            onDismiss = { editingProduct = null },
            onSave = { updated ->
                onSaveProduct(updated)
                editingProduct = null
            },
            onRegenerateIdea = { action, directive ->
                onRegenerateProduct(productToEdit.id, action, directive)
            }
        )
    }

    // Dialog: Regenerate All Products
    if (showRegenerateAllDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showRegenerateAllDialog = false },
            title = { Text("Regenerate Product Ideas") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Ask AI to generate a fresh set of products. You can provide an optional creative direction below:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = customDirective,
                        onValueChange = { customDirective = it },
                        label = { Text("Directive (optional)") },
                        placeholder = { Text("e.g. Focus on premium minimalist apparel") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRegenerateAllDialog = false
                        onRegenerateAllProducts(customDirective.ifBlank { null })
                    }
                ) {
                    Text("Regenerate")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRegenerateAllDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Top Context Card showing the photo thumbnail and brand details.
 */
@Composable
private fun ProductStudioHeader(
    sourceAnalysis: SnapAnalysis,
    brandConcept: BrandConcept?,
    businessMode: String
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(14.dp)
        ) {
            // Source Image Thumbnail
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                if (sourceAnalysis.photoUri.isNotBlank()) {
                    AsyncImage(
                        model = sourceAnalysis.photoUri,
                        contentDescription = "Source Photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.ShoppingBag,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = brandConcept?.brandName ?: sourceAnalysis.detectedSubject,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!brandConcept?.tagline.isNullOrBlank()) {
                    Text(
                        text = "\"${brandConcept?.tagline}\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = SnapAmberAccent.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = sourceAnalysis.detectedSubject,
                            style = MaterialTheme.typography.labelSmall,
                            color = SnapAmberAccent,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (businessMode == "MERCH") "On-Demand Catalog" else "Physical Inventory",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Commercial Rights Banner as required by Requirement 13.
 */
@Composable
private fun CommercialRightsBanner(warning: String) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SnapAmberWarning.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(1.dp, SnapAmberWarning.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth().testTag("commercial_rights_reminder_banner")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = "Commercial Rights Warning",
                tint = SnapAmberWarning,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = "Commercial Rights Reminder",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = SnapAmberWarning
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Make sure you own this image/design or have permission to use it commercially. SnapBrand does not legally verify ownership.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = warning,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Individual Product Card.
 * Adheres strictly to:
 * - Honest visual placeholders for merchandise (mockup pending in Phase 5).
 * - Visible "AI estimate" badge for pricing.
 * - Out-of-stock indicators when inventory is 0.
 */
@Composable
private fun ProductCardItem(
    product: Product,
    businessMode: String,
    isRegenerating: Boolean,
    onEdit: () -> Unit,
    onRemove: () -> Unit
) {
    val isOutOfStock = product.inventory <= 0
    val isMerch = businessMode.equals("MERCH", ignoreCase = true)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("product_card_${product.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Honest Mockup/Photo Placeholder + Info
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Honest Visual Placement Box
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isMerch) SnapIndigoPrimary.copy(alpha = 0.08f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (!isMerch && !product.sourcePhotoUrl.isNullOrBlank()) {
                        // Real Shop: Show actual seller photograph
                        AsyncImage(
                            model = product.sourcePhotoUrl,
                            contentDescription = product.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // Merch: Honest placeholder clearly labeled
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(4.dp)
                        ) {
                            Icon(
                                imageVector = when {
                                    product.title.contains("mug", true) -> Icons.Default.LocalMall
                                    product.title.contains("tote", true) || product.title.contains("bag", true) -> Icons.Default.ShoppingBag
                                    else -> Icons.Default.Inventory2
                                },
                                contentDescription = null,
                                tint = SnapIndigoPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Design Concept",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = SnapIndigoPrimary
                            )
                            Text(
                                text = "Mockup in Phase 5",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 7.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(Modifier.width(14.dp))

                // Product Details
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = product.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(
                            onClick = onRemove,
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("remove_product_button_${product.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove product",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    // Badges row: Category + Mode/Condition
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = product.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        if (!product.condition.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer
                            ) {
                                Text(
                                    text = product.condition,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        if (isOutOfStock) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = SnapCrimsonError.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "Out of Stock",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = SnapCrimsonError,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    // Price with "AI estimate" Label (CRITICAL REQUIREMENT)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.testTag("product_price_${product.id}")
                    ) {
                        Text(
                            text = String.format("$%.2f", product.price),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = SnapEmeraldSuccess.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = product.priceType, // "AI estimate"
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = SnapEmeraldSuccess,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Description
            Text(
                text = product.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            // Merch Variant preview or Real Shop Inventory
            Spacer(Modifier.height(8.dp))
            if (isMerch && product.variants.isNotEmpty()) {
                Text(
                    text = "Sizes: " + product.variants.mapNotNull { it.size }.distinct().joinToString(", "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            } else if (!isMerch) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (product.inventory > 0) "Stock: ${product.inventory}" else "Out of stock",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (product.inventory > 0) MaterialTheme.colorScheme.onSurface else SnapCrimsonError
                    )
                }
            }

            // Bottom Actions Bar
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Draft Listing",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("edit_product_button_${product.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Edit", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

/**
 * Product Editor Sheet: Allows modifying title, price, description, inventory, and category.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductEditorSheet(
    product: Product,
    businessMode: String,
    onDismiss: () -> Unit,
    onSave: (Product) -> Unit,
    onRegenerateIdea: (action: String, directive: String?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember { mutableStateOf(product.title) }
    var priceText by remember { mutableStateOf(String.format("%.2f", product.price)) }
    var description by remember { mutableStateOf(product.description) }
    var category by remember { mutableStateOf(product.category) }
    var inventory by remember { mutableIntStateOf(product.inventory) }
    var condition by remember { mutableStateOf(product.condition ?: "Pre-owned - Good") }
    var targetCustomer by remember { mutableStateOf(product.targetCustomer) }
    var editDirective by remember { mutableStateOf("") }

    val isMerch = businessMode.equals("MERCH", ignoreCase = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.testTag("product_editor_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Edit Product",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(Modifier.height(14.dp))

            // Title
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Product Title") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("edit_product_title_input")
            )

            Spacer(Modifier.height(10.dp))

            // Price & Inventory Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("Price (USD)") },
                    supportingText = { Text("AI estimate - editable") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("edit_product_price_input")
                )

                if (!isMerch) {
                    // Inventory Stepper (ensuring stock >= 0)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("edit_product_inventory_stepper")
                    ) {
                        Text(
                            text = "Inventory Stock",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            IconButton(
                                onClick = { if (inventory > 0) inventory-- },
                                enabled = inventory > 0,
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("decrease_inventory_button")
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease")
                            }
                            Text(
                                text = inventory.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (inventory == 0) SnapCrimsonError else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.testTag("inventory_count_text")
                            )
                            IconButton(
                                onClick = { inventory++ },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("increase_inventory_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase")
                            }
                        }
                        if (inventory == 0) {
                            Text(
                                text = "Out of stock",
                                style = MaterialTheme.typography.labelSmall,
                                color = SnapCrimsonError
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Category & Condition
            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("Category") },
                modifier = Modifier.fillMaxWidth()
            )

            if (!isMerch) {
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = condition,
                    onValueChange = { condition = it },
                    label = { Text("Condition") },
                    placeholder = { Text("e.g. Pre-owned - Good, New in box") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(10.dp))

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                minLines = 3,
                maxLines = 6,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("edit_product_description_input")
            )

            Spacer(Modifier.height(10.dp))

            // Target Customer
            OutlinedTextField(
                value = targetCustomer,
                onValueChange = { targetCustomer = it },
                label = { Text("Target Customer") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            // Targeted Single Product AI Helpers
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = SnapAmberAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "AI Product Refinements",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = SnapAmberAccent
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                onRegenerateIdea("IMPROVE_DESCRIPTION", editDirective.ifBlank { null })
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Improve Description", style = MaterialTheme.typography.labelSmall)
                        }

                        OutlinedButton(
                            onClick = {
                                onRegenerateIdea("REGENERATE_IDEA", editDirective.ifBlank { null })
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("New Idea", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Save Button
            Button(
                onClick = {
                    val parsedPrice = priceText.toDoubleOrNull() ?: product.price
                    val updated = product.copy(
                        title = title.trim(),
                        price = if (parsedPrice > 0) parsedPrice else product.price,
                        description = description.trim(),
                        category = category.trim(),
                        inventory = if (inventory < 0) 0 else inventory,
                        condition = if (!isMerch) condition.trim() else null,
                        targetCustomer = targetCustomer.trim(),
                        status = if (inventory <= 0) "OUT_OF_STOCK" else product.status,
                        updatedAt = System.currentTimeMillis()
                    )
                    onSave(updated)
                },
                colors = ButtonDefaults.buttonColors(containerColor = SnapIndigoPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("save_product_edits_button")
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Save Changes", fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
