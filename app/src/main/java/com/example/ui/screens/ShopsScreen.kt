package com.example.ui.screens

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Product
import com.example.data.model.Shop
import com.example.data.model.ShopMode
import com.example.data.model.StoreStatus
import com.example.ui.components.EmptyStateView
import com.example.ui.components.StatusBadge
import com.example.ui.screens.store.StoreStatusBadge
import com.example.ui.theme.SnapAmberAccent
import com.example.ui.theme.SnapBorderLight
import com.example.ui.theme.SnapEmeraldSuccess
import com.example.ui.theme.SnapIndigoContainer
import com.example.ui.theme.SnapIndigoPrimary
import com.example.ui.theme.SnapTealSecondary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopsScreen(
    shops: List<Shop>,
    discoverableProducts: List<Pair<Product, Shop>> = emptyList(),
    onCreateShop: (name: String, handle: String, tagline: String?, desc: String?, mode: ShopMode) -> Unit,
    onEditShop: (String) -> Unit = {},
    onPreviewShop: (Shop) -> Unit = {},
    onAddToCart: (storeId: String, productId: String, variantId: String?, quantity: Int) -> Unit = { _, _, _, _ -> },
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showCreateDialog by remember { mutableStateOf(false) }

    // Search and filter state for product discovery
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("ALL") }

    // Selected product for PDP bottom sheet
    var selectedProductItem by remember { mutableStateOf<Pair<Product, Shop>?>(null) }
    val pdpSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (selectedTab == 1 && shops.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = SnapIndigoPrimary,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("create_shop_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create New Shop"
                    )
                }
            }
        },
        containerColor = Color.Transparent
    ) { padding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("shops_screen"),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 840.dp)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Screen Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Commerce & Shops",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-0.5).sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Discover products across shops or manage your storefronts",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (selectedTab == 1 && shops.isNotEmpty()) {
                        Button(
                            onClick = { showCreateDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SnapIndigoPrimary),
                            modifier = Modifier.testTag("create_shop_header_button")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("New Shop", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Navigation Tab Row
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = SnapIndigoPrimary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, SnapBorderLight, RoundedCornerShape(14.dp))
                        .testTag("commerce_tab_row")
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        modifier = Modifier.testTag("tab_discover_products"),
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Discover Products (${discoverableProducts.size})",
                                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    )

                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        modifier = Modifier.testTag("tab_my_shops"),
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Seller Studio (${shops.size})",
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // TAB 0: DISCOVER PRODUCTS MARKETPLACE
                if (selectedTab == 0) {
                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear search")
                                }
                            }
                        },
                        placeholder = { Text("Search products, merch, shops, or keywords...") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("discover_search_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Category Filter Chips
                    val categories = listOf("ALL", "MERCH", "PHYSICAL", "APPAREL", "DRINKWARE", "DECOR")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.forEach { cat ->
                            val isSelected = selectedCategoryFilter == cat
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategoryFilter = cat },
                                label = {
                                    Text(
                                        text = when (cat) {
                                            "ALL" -> "All Products"
                                            "MERCH" -> "Printify Merch"
                                            "PHYSICAL" -> "Physical Goods"
                                            "APPAREL" -> "Apparel"
                                            "DRINKWARE" -> "Mugs & Drinkware"
                                            "DECOR" -> "Decor & Posters"
                                            else -> cat
                                        },
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SnapIndigoContainer,
                                    selectedLabelColor = SnapIndigoPrimary
                                ),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Filtered product items
                    val filteredProducts = remember(discoverableProducts, searchQuery, selectedCategoryFilter) {
                        discoverableProducts.filter { (product, shop) ->
                            val matchesSearch = searchQuery.isBlank() ||
                                product.title.contains(searchQuery, ignoreCase = true) ||
                                product.description.contains(searchQuery, ignoreCase = true) ||
                                shop.name.contains(searchQuery, ignoreCase = true) ||
                                shop.handle.contains(searchQuery, ignoreCase = true)

                            val matchesCategory = when (selectedCategoryFilter) {
                                "ALL" -> true
                                "MERCH" -> product.businessMode.equals("MERCH", ignoreCase = true)
                                "PHYSICAL" -> product.businessMode.equals("REAL_SHOP", ignoreCase = true) || product.businessMode.equals("PHYSICAL", ignoreCase = true)
                                "APPAREL" -> product.category.contains("Apparel", ignoreCase = true) || product.title.contains("Tee", ignoreCase = true) || product.title.contains("Hoodie", ignoreCase = true)
                                "DRINKWARE" -> product.category.contains("Drinkware", ignoreCase = true) || product.title.contains("Mug", ignoreCase = true) || product.title.contains("Tumbler", ignoreCase = true)
                                "DECOR" -> product.category.contains("Home", ignoreCase = true) || product.title.contains("Poster", ignoreCase = true) || product.title.contains("Canvas", ignoreCase = true)
                                else -> true
                            }

                            matchesSearch && matchesCategory
                        }
                    }

                    if (filteredProducts.isEmpty()) {
                        EmptyStateView(
                            icon = Icons.Default.Search,
                            title = "No products found",
                            description = if (searchQuery.isNotBlank()) "No products matched '$searchQuery'. Try adjusting your filters."
                            else "No discoverable products available yet. Snap an image to create your first shop and products!",
                            actionLabel = if (searchQuery.isNotBlank()) "Clear Filter" else null,
                            onAction = {
                                searchQuery = ""
                                selectedCategoryFilter = "ALL"
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f)
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(filteredProducts, key = { it.first.id }) { (product, shop) ->
                                DiscoverProductRow(
                                    product = product,
                                    shop = shop,
                                    onViewProduct = { selectedProductItem = product to shop },
                                    onOpenShop = { onPreviewShop(shop) }
                                )
                            }
                        }
                    }
                }

                // TAB 1: SELLER STUDIO (User's Shops)
                if (selectedTab == 1) {
                    if (shops.isEmpty()) {
                        EmptyStateView(
                            icon = Icons.Default.Storefront,
                            title = "You don't have a shop yet.",
                            description = "Every great brand starts with a single snap. Create your first shop to start selling merchandise or your physical products.",
                            actionLabel = "Create your first shop",
                            onAction = { showCreateDialog = true },
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f)
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(shops, key = { it.id }) { shop ->
                                ShopCard(
                                    shop = shop,
                                    onEdit = { onEditShop(shop.id) },
                                    onPreview = { onPreviewShop(shop) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // PRODUCT DETAIL PAGE (PDP) BOTTOM SHEET
        // ==========================================
        if (selectedProductItem != null) {
            val (product, shop) = selectedProductItem!!
            var selectedSize by remember { mutableStateOf("M") }
            var quantity by remember { mutableIntStateOf(1) }
            val isMerch = product.businessMode.equals("MERCH", ignoreCase = true)

            ModalBottomSheet(
                onDismissRequest = { selectedProductItem = null },
                sheetState = pdpSheetState,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 36.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isMerch) SnapIndigoContainer else SnapAmberAccent.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (isMerch) "PRINTIFY ON-DEMAND MERCH" else "PHYSICAL INVENTORY",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isMerch) SnapIndigoPrimary else SnapAmberAccent,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        IconButton(onClick = { selectedProductItem = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    // Product Title & Price
                    Text(
                        text = product.title,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$${String.format("%.2f", product.price)} ${product.currency}",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                            color = SnapEmeraldSuccess
                        )

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = SnapEmeraldSuccess.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = if (product.inventory > 0) "In Stock (${product.inventory})" else "Available On-Demand",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = SnapEmeraldSuccess,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // SELLER CARD WITH DIRECT "OPEN SELLER'S FULL SHOP" ACTION
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(SnapIndigoContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Storefront,
                                        contentDescription = null,
                                        tint = SnapIndigoPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = shop.name,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Text(
                                        text = "snapbrand.site/${shop.handle}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SnapIndigoPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    selectedProductItem = null
                                    onPreviewShop(shop)
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SnapIndigoPrimary),
                                modifier = Modifier.testTag("open_sellers_full_shop_button")
                            ) {
                                Text("Open Shop", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                        }
                    }

                    // Product Description
                    Text(
                        text = "DESCRIPTION",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = product.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 22.sp
                    )

                    // FULFILLMENT SPECS
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isMerch) Icons.Default.Print else Icons.Default.LocalShipping,
                                    contentDescription = null,
                                    tint = if (isMerch) SnapIndigoPrimary else SnapAmberAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isMerch) "PRINTIFY FULFILLMENT INTEGRATION" else "DIRECT SELLER FULFILLMENT",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isMerch) SnapIndigoPrimary else SnapAmberAccent
                                )
                            }
                            Text(
                                text = if (isMerch) "Manufactured via Printify Print Provider network upon order. High-definition direct-to-garment / dye-sublimation print standards."
                                else "Direct merchant inventory. Carefully packed and dispatched by verified seller.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Size Selector (for merch/apparel)
                    if (isMerch && product.category.contains("Apparel", ignoreCase = true)) {
                        Text(
                            text = "SELECT SIZE",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("S", "M", "L", "XL", "2XL").forEach { size ->
                                FilterChip(
                                    selected = selectedSize == size,
                                    onClick = { selectedSize = size },
                                    label = { Text(size, fontWeight = FontWeight.Bold) },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }

                    // Quantity Stepper
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "QUANTITY",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = { if (quantity > 1) quantity-- },
                                shape = CircleShape,
                                modifier = Modifier.size(36.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                            ) {
                                Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }

                            Text(text = "$quantity", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))

                            OutlinedButton(
                                onClick = { quantity++ },
                                shape = CircleShape,
                                modifier = Modifier.size(36.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                            ) {
                                Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // ACTION BUTTONS: ADD TO CART & BUY NOW
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                onAddToCart(shop.id, product.id, selectedSize, quantity)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Added $quantity × ${product.title} to cart!")
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("pdp_add_to_cart_button")
                        ) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add to Cart", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                onAddToCart(shop.id, product.id, selectedSize, quantity)
                                selectedProductItem = null
                                onPreviewShop(shop)
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SnapIndigoPrimary),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("pdp_buy_now_button")
                        ) {
                            Text("Buy Now", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }

        // CREATE SHOP DIALOG
        if (showCreateDialog) {
            CreateShopDialog(
                onDismiss = { showCreateDialog = false },
                onConfirm = { name, handle, tagline, desc, mode ->
                    onCreateShop(name, handle, tagline, desc, mode)
                    showCreateDialog = false
                }
            )
        }
    }
}

@Composable
private fun DiscoverProductRow(
    product: Product,
    shop: Shop,
    onViewProduct: () -> Unit,
    onOpenShop: () -> Unit
) {
    val isMerch = product.businessMode.equals("MERCH", ignoreCase = true)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onViewProduct() }
            .testTag("discover_product_row_${product.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isMerch) SnapIndigoContainer else SnapAmberAccent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isMerch) Icons.Default.Print else Icons.Default.ShoppingBag,
                            contentDescription = null,
                            tint = if (isMerch) SnapIndigoPrimary else SnapAmberAccent,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = product.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "by ${shop.name} • @${shop.handle}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isMerch) SnapIndigoContainer else SnapAmberAccent.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (isMerch) "PRINTIFY MERCH" else "PHYSICAL GOODS",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                        color = if (isMerch) SnapIndigoPrimary else SnapAmberAccent,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = product.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$${String.format("%.2f", product.price)} ${product.currency}",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                    color = SnapEmeraldSuccess
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onOpenShop,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Visit Shop", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = onViewProduct,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SnapIndigoPrimary),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("View Product", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ShopCard(
    shop: Shop,
    onEdit: () -> Unit = {},
    onPreview: () -> Unit = {}
) {
    val dateStr = remember(shop.createdAt) {
        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(shop.createdAt))
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("shop_card_${shop.id}")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (shop.businessMode == ShopMode.MERCH_SHOP) SnapIndigoContainer
                                else SnapAmberAccent.copy(alpha = 0.15f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = null,
                            tint = if (shop.businessMode == ShopMode.MERCH_SHOP) SnapIndigoPrimary else SnapAmberAccent,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = shop.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "snapbrand.site/${shop.handle}",
                            style = MaterialTheme.typography.bodySmall,
                            color = SnapIndigoPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StoreStatusBadge(status = shop.status)

                    StatusBadge(
                        text = shop.businessMode.displayName,
                        backgroundColor = if (shop.businessMode == ShopMode.MERCH_SHOP)
                            SnapIndigoContainer
                        else SnapAmberAccent.copy(alpha = 0.15f),
                        textColor = if (shop.businessMode == ShopMode.MERCH_SHOP) SnapIndigoPrimary else SnapAmberAccent
                    )
                }
            }

            if (!shop.tagline.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = shop.tagline,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (!shop.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = shop.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!shop.story.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "BRAND STORY",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                            color = SnapIndigoPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = shop.story,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (!shop.targetAudience.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Target: ${shop.targetAudience}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(14.dp))

            // Action Row: Info + Store Engine Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Theme: ${shop.theme.displayName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${shop.productCount} products listed",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = SnapEmeraldSuccess
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onPreview,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("shop_preview_button_${shop.id}")
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Preview", fontSize = 12.sp)
                    }

                    Button(
                        onClick = onEdit,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SnapIndigoPrimary),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("shop_edit_button_${shop.id}")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit Store", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun CreateShopDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, handle: String, tagline: String?, desc: String?, mode: ShopMode) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var handle by remember { mutableStateOf("") }
    var tagline by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedMode by remember { mutableStateOf(ShopMode.MERCH_SHOP) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Create Your Shop",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (handle.isBlank() || handle == name.dropLast(1).lowercase().replace(" ", "-")) {
                            handle = it.lowercase().trim().replace(" ", "-")
                        }
                    },
                    label = { Text("Shop Name") },
                    placeholder = { Text("e.g. VaporWave Threads") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("shop_name_input")
                )

                OutlinedTextField(
                    value = handle,
                    onValueChange = { handle = it.lowercase().replace(" ", "-") },
                    label = { Text("Handle (Slug)") },
                    placeholder = { Text("e.g. vaporwave") },
                    prefix = { Text("@") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("shop_handle_input")
                )

                OutlinedTextField(
                    value = tagline,
                    onValueChange = { tagline = it },
                    label = { Text("Tagline (Optional)") },
                    placeholder = { Text("Short phrase describing your shop") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Business Mode",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(
                        selected = selectedMode == ShopMode.MERCH_SHOP,
                        onClick = { selectedMode = ShopMode.MERCH_SHOP }
                    )
                    Column {
                        Text("Merch Shop (Print-on-Demand)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text("AI generates merch via Printify", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(
                        selected = selectedMode == ShopMode.REAL_SHOP,
                        onClick = { selectedMode = ShopMode.REAL_SHOP }
                    )
                    Column {
                        Text("Real Shop (Physical Inventory)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text("List your actual products & inventory", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank() || handle.isBlank()) {
                        errorMessage = "Please enter both a shop name and a handle."
                    } else {
                        onConfirm(name, handle, tagline, description, selectedMode)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SnapIndigoPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("submit_create_shop_button")
            ) {
                Text("Create Shop", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
