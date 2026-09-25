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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BrandConcept
import com.example.data.model.Product
import com.example.data.model.Shop
import com.example.data.model.ShopMode
import com.example.data.model.SnapAnalysis
import com.example.data.model.UserProfile
import com.example.service.SnapBrandEvent
import com.example.ui.screens.snap.SnapEngineWorkflowSheet
import com.example.ui.theme.SnapAmberAccent
import com.example.ui.theme.SnapBorderLight
import com.example.ui.theme.SnapCyanAI
import com.example.ui.theme.SnapEmeraldSuccess
import com.example.ui.theme.SnapIndigoContainer
import com.example.ui.theme.SnapIndigoOnContainer
import com.example.ui.theme.SnapIndigoPrimary
import com.example.ui.theme.SnapTealSecondary
import com.example.ui.theme.SnapTextPrimaryLight
import com.example.ui.theme.SnapTextSecondaryLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    currentUser: UserProfile?,
    onNavigateToShops: () -> Unit,
    discoverableProducts: List<Pair<Product, Shop>> = emptyList(),
    userShops: List<Shop> = emptyList(),
    onOpenShop: (Shop) -> Unit = {},
    onProductClick: (Product, Shop) -> Unit = { _, _ -> },
    onAnalyzeImage: suspend (photoUri: String, base64: String, mimeType: String) -> Result<SnapAnalysis> = { _, _, _ ->
        Result.failure(UnsupportedOperationException("AI Service not connected"))
    },
    onLogAnalytics: (SnapBrandEvent, Map<String, Any>) -> Unit = { _, _ -> },
    brandConcept: BrandConcept? = null,
    isGeneratingBrand: Boolean = false,
    isRegeneratingField: String? = null,
    brandError: String? = null,
    onGenerateBrand: suspend (analysis: SnapAnalysis, directive: String?) -> Result<BrandConcept> = { _, _ -> Result.failure(Exception("Not implemented")) },
    onRegenerateName: suspend (directive: String?) -> Result<BrandConcept> = { Result.failure(Exception("Not implemented")) },
    onRegenerateTagline: suspend (directive: String?) -> Result<BrandConcept> = { Result.failure(Exception("Not implemented")) },
    onRegenerateStory: suspend (directive: String?) -> Result<BrandConcept> = { Result.failure(Exception("Not implemented")) },
    onUpdateBrandConcept: (BrandConcept) -> Unit = {},
    onAcceptBrand: () -> Result<Shop> = { Result.failure(Exception("Not implemented")) },
    products: List<Product> = emptyList(),
    isGeneratingProducts: Boolean = false,
    isRegeneratingProduct: String? = null,
    productError: String? = null,
    onGenerateProducts: (shopId: String?, directive: String?, forceRegenerate: Boolean) -> Unit = { _, _, _ -> },
    onSaveProduct: (Product) -> Unit = {},
    onRemoveProduct: (String) -> Unit = {},
    onRegenerateSingleProduct: (productId: String, action: String, directive: String?) -> Unit = { _, _, _ -> },
    onSaveAllProducts: (String) -> Unit = {},
    onOpenStoreEditor: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var showWorkflowSheet by remember { mutableStateOf(false) }
    var workflowMode by remember { mutableStateOf("camera") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Spotlight PDP selection
    var spotlightSelectedProduct by remember { mutableStateOf<Pair<Product, Shop>?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.Start
        ) {
            // Top Platform Identity & Tagline Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = SnapIndigoContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(SnapEmeraldSuccess)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SNAPBRAND COMMERCE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.1.sp
                            ),
                            color = SnapIndigoPrimary
                        )
                    }
                }

                if (currentUser != null) {
                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "@${currentUser.username}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // BOLD TYPOGRAPHY HERO SECTION
            Text(
                text = "SNAP\nANYTHING.",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 42.sp,
                    lineHeight = 46.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1.0).sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "GET A SHOP.",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 42.sp,
                    lineHeight = 46.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1.0).sp
                ),
                color = SnapIndigoPrimary,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "Snap any drawing, mouse, laptop, dog, garment, or object. AI creates your brand, matches real Printify on-demand merch blueprints, and launches a real commerce storefront instantly.",
                style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp, bottom = 24.dp)
            )

            // PRIMARY & SECONDARY ACTION BUTTONS (Clean, modern rounded-2xl)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        workflowMode = "camera"
                        showWorkflowSheet = true
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SnapIndigoPrimary,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .testTag("primary_cta_snap_something")
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Snap something",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }

                Button(
                    onClick = {
                        workflowMode = "upload"
                        showWorkflowSheet = true
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = SnapIndigoPrimary
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SnapBorderLight),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .testTag("secondary_cta_upload_photo")
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = null,
                        tint = SnapIndigoPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Upload a photo",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = SnapIndigoPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ==========================================
            // SNAP INSPIRATION CHIPS (What Can You Snap?)
            // ==========================================
            Text(
                text = "WHAT CAN YOU SNAP?",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.0.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            val inspirationList = listOf(
                "🐕 Pets & Dogs" to "dog photo pet merch tees mugs",
                "🎨 Art & Drawings" to "sketch drawing illustration poster prints",
                "👕 Vintage Garments" to "streetwear apparel hoodie design",
                "☕ Still Life & Coffee" to "artisan coffee mug lifestyle",
                "💻 Gadgets & Gear" to "laptop tech mousepad electronics",
                "🚗 Custom Rides & Cars" to "car automobile automotive brand"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                inspirationList.forEach { (label, prompt) ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                workflowMode = "camera"
                                showWorkflowSheet = true
                            }
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ==========================================
            // THE 3-STEP COMMERCE PROCESS ("SNAP -> AI -> SHOP")
            // ==========================================
            Text(
                text = "HOW SNAPBRAND WORKS",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.0.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, SnapBorderLight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    WorkflowStepItem(
                        stepNum = "1",
                        icon = Icons.Default.CameraAlt,
                        iconTint = SnapIndigoPrimary,
                        title = "Snap Anything",
                        description = "Point your camera or upload any object, pet, artwork, fashion piece, or product."
                    )

                    HorizontalDivider(color = SnapBorderLight)

                    WorkflowStepItem(
                        stepNum = "2",
                        icon = Icons.Default.AutoAwesome,
                        iconTint = SnapTealSecondary,
                        title = "AI Commerce Engine",
                        description = "Gemini analyzes the image, identifies viral angles, queries real Printify catalog blueprints, and crafts your brand story."
                    )

                    HorizontalDivider(color = SnapBorderLight)

                    WorkflowStepItem(
                        stepNum = "3",
                        icon = Icons.Default.RocketLaunch,
                        iconTint = SnapEmeraldSuccess,
                        title = "Launch Storefront & Sell",
                        description = "Receive a live responsive storefront with pricing, shopping cart, and automated order checkout."
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ==========================================
            // DISCOVER PRODUCTS SPOTLIGHT
            // ==========================================
            if (discoverableProducts.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "COMMERCE SPOTLIGHT",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.0.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Discover Products from Real Shops",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    TextButton(onClick = onNavigateToShops) {
                        Text("View All", fontWeight = FontWeight.Bold, color = SnapIndigoPrimary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp), tint = SnapIndigoPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    discoverableProducts.take(4).forEach { (product, shop) ->
                        ProductSpotlightCard(
                            product = product,
                            shop = shop,
                            onClick = {
                                spotlightSelectedProduct = product to shop
                            },
                            onOpenShop = {
                                onOpenShop(shop)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }

            // ==========================================
            // USER'S ACTIVE SHOPS (If any)
            // ==========================================
            if (userShops.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "YOUR STOREFRONTS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.0.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${userShops.size} Active Shop${if (userShops.size > 1) "s" else ""}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    TextButton(onClick = onNavigateToShops) {
                        Text("Seller Studio", fontWeight = FontWeight.Bold, color = SnapIndigoPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    userShops.forEach { shop ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SnapBorderLight),
                            modifier = Modifier
                                .width(260.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onOpenShop(shop) }
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = shop.name,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = null,
                                        tint = SnapIndigoPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                Text(
                                    text = "snapbrand.site/${shop.handle}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SnapIndigoPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${shop.productCount} products",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = SnapEmeraldSuccess.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = shop.status,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = SnapEmeraldSuccess,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = { onOpenShop(shop) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SnapIndigoPrimary),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(36.dp)
                                ) {
                                    Text("Open Storefront", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }

            // ==========================================
            // SUPPORTED BUSINESS MODES
            // ==========================================
            Text(
                text = "SUPPORTED BUSINESS MODES",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.0.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            BusinessModeCard(
                icon = Icons.Default.Print,
                iconColor = SnapIndigoPrimary,
                badge = "MODE 1: MERCH",
                title = "Merch Shop (Print-on-Demand)",
                subtitle = "Turn artworks, drawings, pets, and designs into printed apparel & goods",
                features = listOf(
                    "Real Printify blueprint catalog alignment (Tees, Hoodies, Mugs, Posters)",
                    "Zero inventory required — manufactured on demand when a customer orders",
                    "Automated packaging, dispatch, and tracking integration"
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            BusinessModeCard(
                icon = Icons.Default.LocalShipping,
                iconColor = SnapAmberAccent,
                badge = "MODE 2: REAL SHOP",
                title = "Real Shop (Physical Inventory)",
                subtitle = "Direct commerce for actual products, crafts, and physical inventory",
                features = listOf(
                    "Snap any physical item to generate titles, descriptions, and price estimates",
                    "Manage your actual stock quantities and custom delivery guidelines",
                    "Direct customer checkout with delivery address capture"
                )
            )

            Spacer(modifier = Modifier.height(40.dp))
        }

        // ==========================================
        // SPOTLIGHT PRODUCT DETAIL DIALOG
        // ==========================================
        if (spotlightSelectedProduct != null) {
            val (p, s) = spotlightSelectedProduct!!
            AlertDialog(
                onDismissRequest = { spotlightSelectedProduct = null },
                confirmButton = {
                    Button(
                        onClick = {
                            spotlightSelectedProduct = null
                            onOpenShop(s)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SnapIndigoPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open Seller's Full Shop", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { spotlightSelectedProduct = null }) {
                        Text("Close")
                    }
                },
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = p.title,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { spotlightSelectedProduct = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Seller attribution
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Storefront, contentDescription = null, tint = SnapIndigoPrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = s.name,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "snapbrand.site/${s.handle}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SnapIndigoPrimary
                                    )
                                }
                            }
                        }

                        // Price and Mode
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$${String.format("%.2f", p.price)} ${p.currency}",
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                                color = SnapEmeraldSuccess
                            )

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (p.businessMode.equals("MERCH", ignoreCase = true)) SnapIndigoContainer
                                else SnapAmberAccent.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = if (p.businessMode.equals("MERCH", ignoreCase = true)) "PRINTIFY MERCH" else "PHYSICAL ITEM",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (p.businessMode.equals("MERCH", ignoreCase = true)) SnapIndigoPrimary else SnapAmberAccent,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Text(
                            text = p.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (p.sellingPoints.isNotEmpty()) {
                            Text(
                                text = "KEY HIGHLIGHTS",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            p.sellingPoints.forEach { pt ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SnapEmeraldSuccess, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = pt, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }

                        if (!p.targetCustomer.isNullOrBlank()) {
                            Text(
                                text = "Ideal for: ${p.targetCustomer}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            )
        }

        // ==========================================
        // SNAP ENGINE + BRAND GENIUS WORKFLOW SHEET
        // ==========================================
        if (showWorkflowSheet) {
            SnapEngineWorkflowSheet(
                sheetState = sheetState,
                initialMode = workflowMode,
                onDismiss = { showWorkflowSheet = false },
                onAnalyzeImage = onAnalyzeImage,
                onNavigateToShops = onNavigateToShops,
                onLogAnalytics = onLogAnalytics,
                brandConcept = brandConcept,
                isGeneratingBrand = isGeneratingBrand,
                isRegeneratingField = isRegeneratingField,
                brandError = brandError,
                onGenerateBrand = onGenerateBrand,
                onRegenerateName = onRegenerateName,
                onRegenerateTagline = onRegenerateTagline,
                onRegenerateStory = onRegenerateStory,
                onUpdateBrandConcept = onUpdateBrandConcept,
                onAcceptBrand = onAcceptBrand,
                products = products,
                isGeneratingProducts = isGeneratingProducts,
                isRegeneratingProduct = isRegeneratingProduct,
                productError = productError,
                onGenerateProducts = onGenerateProducts,
                onSaveProduct = onSaveProduct,
                onRemoveProduct = onRemoveProduct,
                onRegenerateSingleProduct = onRegenerateSingleProduct,
                onSaveAllProducts = onSaveAllProducts,
                onOpenStoreEditor = onOpenStoreEditor
            )
        }
    }
}

@Composable
private fun WorkflowStepItem(
    stepNum: String,
    icon: ImageVector,
    iconTint: Color,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = iconTint.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "STEP $stepNum",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Black),
                        color = iconTint,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun ProductSpotlightCard(
    product: Product,
    shop: Shop,
    onClick: () -> Unit,
    onOpenShop: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("spotlight_product_${product.id}")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (product.businessMode.equals("MERCH", ignoreCase = true)) SnapIndigoContainer
                        else SnapAmberAccent.copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (product.businessMode.equals("MERCH", ignoreCase = true)) Icons.Default.Print else Icons.Default.ShoppingBag,
                    contentDescription = null,
                    tint = if (product.businessMode.equals("MERCH", ignoreCase = true)) SnapIndigoPrimary else SnapAmberAccent,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = product.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = "by ${shop.name} • @${shop.handle}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "$${String.format("%.2f", product.price)}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                        color = SnapEmeraldSuccess
                    )

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (product.businessMode.equals("MERCH", ignoreCase = true)) SnapIndigoContainer
                        else SnapAmberAccent.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = if (product.businessMode.equals("MERCH", ignoreCase = true)) "PRINTIFY" else "PHYSICAL",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                            color = if (product.businessMode.equals("MERCH", ignoreCase = true)) SnapIndigoPrimary else SnapAmberAccent,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedButton(
                onClick = onOpenShop,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Text("Visit Shop", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun BusinessModeCard(
    icon: ImageVector,
    iconColor: Color,
    badge: String,
    title: String,
    subtitle: String,
    features: List<String>
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(iconColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = iconColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = iconColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            features.forEach { feature ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = SnapEmeraldSuccess,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = feature,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
