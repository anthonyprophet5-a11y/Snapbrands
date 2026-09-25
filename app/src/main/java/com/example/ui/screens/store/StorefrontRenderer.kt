package com.example.ui.screens.store

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import com.example.data.model.CartItem
import com.example.data.model.CustomerDeliveryInfo
import com.example.data.model.Order
import com.example.store.checkout.CartEngine
import com.example.store.checkout.CartState
import com.example.store.checkout.Money
import com.example.store.payment.PaymentInitResponse
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ProductDisplayStatus
import com.example.data.model.Shop
import com.example.data.model.StoreStatus
import com.example.store.PublicProduct
import com.example.store.PublicStorefront
import com.example.store.PublicStorefrontMapper
import com.example.store.StorefrontThemeResolver
import com.example.store.StorefrontThemeTokens
import java.text.NumberFormat
import java.util.Locale

enum class StorefrontViewMode {
    SELLER_PREVIEW_MOBILE,
    SELLER_PREVIEW_DESKTOP,
    PUBLIC_CUSTOMER_VIEW
}

/**
 * Shared Storefront Renderer.
 * Renders a seller-configured store with pure theme tokens (MINIMAL, BOLD, LUXURY, CREATIVE).
 * Never leaks private credentials, raw AI prompts, or internal cost models to customer view.
 */
@Composable
fun StorefrontRenderer(
    storefront: PublicStorefront,
    themeShop: Shop,
    viewMode: StorefrontViewMode = StorefrontViewMode.SELLER_PREVIEW_MOBILE,
    cartState: CartState? = null,
    onAddToCart: ((productId: String, variantId: String?, quantity: Int) -> Unit)? = null,
    onUpdateCartQuantity: ((cartItemId: String, newQuantity: Int) -> Unit)? = null,
    onRemoveFromCart: ((cartItemId: String) -> Unit)? = null,
    onClearCart: (() -> Unit)? = null,
    onCreateOrder: ((CustomerDeliveryInfo) -> Result<Order>)? = null,
    onInitializePayment: (suspend (orderId: String) -> Result<PaymentInitResponse>)? = null,
    onVerifyPayment: (suspend (orderId: String, reference: String) -> Result<Order>)? = null,
    modifier: Modifier = Modifier,
    onProductClick: ((PublicProduct) -> Unit)? = null
) {
    val tokens: StorefrontThemeTokens = remember(themeShop) {
        StorefrontThemeResolver.resolve(themeShop)
    }

    var localCart by remember(storefront.id) {
        mutableStateOf(
            CartState(
                storeId = storefront.id,
                currency = storefront.currency,
                deliveryFee = if (storefront.fixedDeliveryFee != null) Money.toMinorUnits(storefront.fixedDeliveryFee) else 0L
            )
        )
    }
    val activeCart = cartState ?: localCart

    var selectedProductForDetail by remember { mutableStateOf<PublicProduct?>(null) }
    var showContactModal by remember { mutableStateOf(false) }
    var showCartModal by remember { mutableStateOf(false) }
    var showCheckoutModal by remember { mutableStateOf(false) }

    val contentModifier = when (viewMode) {
        StorefrontViewMode.SELLER_PREVIEW_MOBILE -> {
            Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .shadow(16.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .border(2.dp, tokens.borderColor.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
                .background(tokens.backgroundColor)
        }
        StorefrontViewMode.SELLER_PREVIEW_DESKTOP -> {
            Modifier
                .widthIn(max = 840.dp)
                .fillMaxWidth()
                .background(tokens.backgroundColor)
        }
        StorefrontViewMode.PUBLIC_CUSTOMER_VIEW -> {
            Modifier
                .fillMaxWidth()
                .background(tokens.backgroundColor)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                if (viewMode == StorefrontViewMode.SELLER_PREVIEW_MOBILE)
                    MaterialTheme.colorScheme.scrim.copy(alpha = 0.35f)
                else tokens.backgroundColor
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = contentModifier
                .verticalScroll(rememberScrollState())
                .testTag("storefront_container")
        ) {
            // 1. Navigation Header
            StorefrontHeader(
                storefront = storefront,
                tokens = tokens,
                cartItemCount = activeCart.totalItemCount,
                onContactClick = { showContactModal = true },
                onCartClick = { showCartModal = true }
            )

            // 2. Hero Section
            StorefrontHeroSection(
                storefront = storefront,
                tokens = tokens
            )

            // 3. Brand Story / About Section
            if (!storefront.story.isNullOrBlank() || !storefront.description.isNullOrBlank()) {
                StorefrontAboutSection(
                    storefront = storefront,
                    tokens = tokens
                )
            }

            // 4. Featured Products Section
            if (storefront.featuredProducts.isNotEmpty()) {
                StorefrontFeaturedSection(
                    products = storefront.featuredProducts,
                    tokens = tokens,
                    onProductClick = {
                        selectedProductForDetail = it
                        onProductClick?.invoke(it)
                    }
                )
            }

            // 5. All Products Catalog Section
            StorefrontCatalogSection(
                products = storefront.products,
                tokens = tokens,
                onProductClick = {
                    selectedProductForDetail = it
                    onProductClick?.invoke(it)
                }
            )

            // 6. Commerce & Delivery Information (Informational, honest)
            StorefrontInfoAndPoliciesSection(
                storefront = storefront,
                tokens = tokens
            )

            // 7. Store Footer
            StorefrontFooter(
                storefront = storefront,
                tokens = tokens
            )
        }
    }

    // Product Quick Detail Dialog
    selectedProductForDetail?.let { product ->
        ProductDetailDialog(
            product = product,
            tokens = tokens,
            onDismiss = { selectedProductForDetail = null },
            onAddToCart = { prod, variantId, qty ->
                if (onAddToCart != null) {
                    onAddToCart(prod.id, variantId, qty)
                } else {
                    val unitPriceMinor = Money.toMinorUnits(prod.price)
                    val existing = activeCart.items.find { it.productId == prod.id && it.variantId == variantId }
                    val newItems = activeCart.items.toMutableList()
                    if (existing != null) {
                        val idx = newItems.indexOf(existing)
                        val updatedQty = existing.quantity + qty
                        newItems[idx] = existing.copy(
                            quantity = updatedQty
                        )
                    } else {
                        newItems.add(
                            CartItem(
                                id = "cart_item_${System.currentTimeMillis()}_${(100..999).random()}",
                                storeId = storefront.id,
                                productId = prod.id,
                                variantId = variantId,
                                titleSnapshot = prod.title,
                                imageSnapshot = prod.imageUrl,
                                unitPrice = unitPriceMinor,
                                currency = prod.currency,
                                quantity = qty
                            )
                        )
                    }
                    val fee = if (storefront.fixedDeliveryFee != null) Money.toMinorUnits(storefront.fixedDeliveryFee) else 0L
                    localCart = activeCart.copy(
                        items = newItems,
                        deliveryFee = fee
                    )
                }
                selectedProductForDetail = null
                showCartModal = true
            }
        )
    }

    // Cart Drawer Dialog
    if (showCartModal) {
        CartDrawerDialog(
            cart = activeCart,
            storefront = storefront,
            tokens = tokens,
            onDismiss = { showCartModal = false },
            onUpdateQuantity = { cartItemId, newQty ->
                if (onUpdateCartQuantity != null) {
                    onUpdateCartQuantity(cartItemId, newQty)
                } else {
                    val updateRes = CartEngine.updateQuantity(activeCart, cartItemId, newQty)
                    if (updateRes.isSuccess) localCart = updateRes.getOrThrow()
                }
            },
            onRemoveItem = { cartItemId ->
                if (onRemoveFromCart != null) {
                    onRemoveFromCart(cartItemId)
                } else {
                    localCart = CartEngine.removeItem(activeCart, cartItemId)
                }
            },
            onProceedToCheckout = {
                showCartModal = false
                showCheckoutModal = true
            }
        )
    }

    // Customer Checkout Dialog
    if (showCheckoutModal) {
        CustomerCheckoutDialog(
            storefront = storefront,
            cart = activeCart,
            tokens = tokens,
            onDismiss = { showCheckoutModal = false },
            onCreateOrder = { deliveryInfo ->
                if (onCreateOrder != null) {
                    onCreateOrder(deliveryInfo)
                } else {
                    val sub = activeCart.subtotal
                    val fee = activeCart.deliveryFee
                    val tot = Money.calculateTotal(sub, fee)
                    val dummyOrder = Order(
                        id = "ord_${System.currentTimeMillis()}",
                        orderNumber = com.example.store.checkout.OrderNumberGenerator.generate(),
                        storeId = storefront.id,
                        sellerUid = themeShop.ownerUid,
                        customerName = deliveryInfo.customerName,
                        customerEmail = deliveryInfo.customerEmail,
                        customerPhone = deliveryInfo.customerPhone,
                        deliveryAddress = "${deliveryInfo.deliveryAddress}, ${deliveryInfo.city}, ${deliveryInfo.country}",
                        deliveryNotes = deliveryInfo.deliveryNotes,
                        items = activeCart.items.map { ci ->
                            com.example.data.model.OrderItem(
                                id = "item_${ci.id}",
                                storeId = storefront.id,
                                productId = ci.productId,
                                variantId = ci.variantId,
                                titleSnapshot = ci.titleSnapshot,
                                imageSnapshot = ci.imageSnapshot,
                                unitPrice = ci.unitPrice,
                                currency = ci.currency,
                                quantity = ci.quantity
                            )
                        },
                        subtotal = sub,
                        deliveryFee = fee,
                        total = tot,
                        currency = storefront.currency,
                        paymentStatus = com.example.data.model.PaymentStatus.UNPAID,
                        orderStatus = com.example.data.model.OrderStatus.PENDING_PAYMENT,
                        createdAt = System.currentTimeMillis()
                    )
                    Result.success(dummyOrder)
                }
            },
            onInitializePayment = { orderId ->
                if (onInitializePayment != null) {
                    onInitializePayment(orderId)
                } else {
                    Result.success(
                        PaymentInitResponse(
                            reference = "pstk_ref_${System.currentTimeMillis()}",
                            authorizationUrl = "https://checkout.paystack.com/simulate",
                            accessCode = "sim_code_${System.currentTimeMillis()}",
                            provider = "PAYSTACK",
                            status = "success"
                        )
                    )
                }
            },
            onVerifyPayment = { orderId, reference ->
                if (onVerifyPayment != null) {
                    onVerifyPayment(orderId, reference)
                } else {
                    Result.success(
                        Order(
                            id = orderId,
                            orderNumber = com.example.store.checkout.OrderNumberGenerator.generate(),
                            storeId = storefront.id,
                            sellerUid = themeShop.ownerUid,
                            items = emptyList(),
                            subtotal = activeCart.subtotal,
                            deliveryFee = activeCart.deliveryFee,
                            total = activeCart.total,
                            currency = storefront.currency,
                            paymentReference = reference,
                            paymentStatus = com.example.data.model.PaymentStatus.PAID,
                            orderStatus = com.example.data.model.OrderStatus.PAID,
                            createdAt = System.currentTimeMillis()
                        )
                    )
                }
            },
            onOrderPlacedSuccess = {
                if (onClearCart != null) onClearCart()
                else localCart = CartEngine.clear(storefront.id)
            }
        )
    }

    // Contact Modal
    if (showContactModal) {
        ContactInformationDialog(
            storefront = storefront,
            tokens = tokens,
            onDismiss = { showContactModal = false }
        )
    }
}

// ==========================================
// 1. STORE HEADER
// ==========================================
@Composable
private fun StorefrontHeader(
    storefront: PublicStorefront,
    tokens: StorefrontThemeTokens,
    cartItemCount: Int = 0,
    onContactClick: () -> Unit,
    onCartClick: () -> Unit = {}
) {
    Surface(
        color = tokens.surfaceColor,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                // Store Avatar / Logo
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(tokens.primaryColor)
                        .border(1.dp, tokens.borderColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (!storefront.logoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = storefront.logoUrl,
                            contentDescription = storefront.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = storefront.name.take(2).uppercase(Locale.ROOT),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = tokens.onPrimaryColor
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = storefront.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = tokens.titleFontWeight,
                            color = tokens.onSurfaceColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "snapbrand.site/${storefront.handle}",
                        style = MaterialTheme.typography.labelSmall,
                        color = tokens.onSurfaceVariantColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Contact / Message Button
                Surface(
                    shape = tokens.buttonShape,
                    color = tokens.primaryColor.copy(alpha = 0.1f),
                    modifier = Modifier
                        .clip(tokens.buttonShape)
                        .clickable { onContactClick() }
                        .testTag("storefront_contact_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = "Contact Store",
                            tint = tokens.primaryColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Contact",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = tokens.primaryColor
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Cart Button with badge
                Surface(
                    shape = tokens.buttonShape,
                    color = tokens.primaryColor,
                    modifier = Modifier
                        .clip(tokens.buttonShape)
                        .clickable { onCartClick() }
                        .testTag("storefront_cart_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "Cart",
                            tint = tokens.onPrimaryColor,
                            modifier = Modifier.size(15.dp)
                        )
                        if (cartItemCount > 0) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "$cartItemCount",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Black,
                                color = tokens.onPrimaryColor
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 2. HERO SECTION
// ==========================================
@Composable
private fun StorefrontHeroSection(
    storefront: PublicStorefront,
    tokens: StorefrontThemeTokens
) {
    val heroBrush = Brush.verticalGradient(
        colors = listOf(
            tokens.primaryColor.copy(alpha = if (tokens.isDarkBackground) 0.35f else 0.12f),
            tokens.surfaceColor
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(heroBrush)
            .padding(horizontal = 24.dp, vertical = tokens.heroPaddingVertical),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                shape = tokens.chipShape,
                color = tokens.primaryColor.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(tokens.borderWidth, tokens.primaryColor.copy(alpha = 0.4f))
            ) {
                Text(
                    text = tokens.badgeStyleLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp,
                    color = tokens.primaryColor,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = storefront.name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = tokens.titleFontWeight,
                textAlign = TextAlign.Center,
                color = tokens.onSurfaceColor
            )

            if (!storefront.tagline.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = storefront.tagline,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = tokens.onSurfaceVariantColor,
                    modifier = Modifier.widthIn(max = 500.dp)
                )
            }

            if (!storefront.location.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = tokens.onSurfaceVariantColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = storefront.location,
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.onSurfaceVariantColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = tokens.chipShape,
                    color = tokens.surfaceColor,
                    border = androidx.compose.foundation.BorderStroke(tokens.borderWidth, tokens.borderColor)
                ) {
                    Text(
                        text = "${storefront.products.size} Products Listed",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = tokens.onSurfaceColor,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                if (storefront.featuredProducts.isNotEmpty()) {
                    Surface(
                        shape = tokens.chipShape,
                        color = tokens.surfaceColor,
                        border = androidx.compose.foundation.BorderStroke(tokens.borderWidth, tokens.borderColor)
                    ) {
                        Text(
                            text = "${storefront.featuredProducts.size} Featured",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = tokens.accentColor,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. ABOUT SECTION
// ==========================================
@Composable
private fun StorefrontAboutSection(
    storefront: PublicStorefront,
    tokens: StorefrontThemeTokens
) {
    Card(
        shape = tokens.cardShape,
        colors = CardDefaults.cardColors(containerColor = tokens.surfaceColor),
        border = androidx.compose.foundation.BorderStroke(tokens.borderWidth, tokens.borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "ABOUT THE BRAND",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = tokens.primaryColor
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = storefront.story ?: storefront.description ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = tokens.onSurfaceColor,
                lineHeight = 22.sp
            )
        }
    }
}

// ==========================================
// 4. FEATURED PRODUCTS SECTION
// ==========================================
@Composable
private fun StorefrontFeaturedSection(
    products: List<PublicProduct>,
    tokens: StorefrontThemeTokens,
    onProductClick: (PublicProduct) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = tokens.accentColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "FEATURED SHOWCASE",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    color = tokens.onSurfaceColor
                )
            }

            Text(
                text = "${products.size} highlighted",
                style = MaterialTheme.typography.labelSmall,
                color = tokens.onSurfaceVariantColor
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            products.forEach { product ->
                FeaturedProductCard(
                    product = product,
                    tokens = tokens,
                    onClick = { onProductClick(product) }
                )
            }
        }
    }
}

@Composable
private fun FeaturedProductCard(
    product: PublicProduct,
    tokens: StorefrontThemeTokens,
    onClick: () -> Unit
) {
    Card(
        shape = tokens.cardShape,
        colors = CardDefaults.cardColors(containerColor = tokens.surfaceColor),
        border = androidx.compose.foundation.BorderStroke(tokens.borderWidth, tokens.borderColor),
        modifier = Modifier
            .width(220.dp)
            .clip(tokens.cardShape)
            .clickable { onClick() }
    ) {
        Column {
            // Product Image or Concept Placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(tokens.primaryColor.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                if (!product.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = product.imageUrl,
                        contentDescription = product.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.ShoppingBag,
                        contentDescription = null,
                        tint = tokens.primaryColor.copy(alpha = 0.4f),
                        modifier = Modifier.size(48.dp)
                    )
                }

                Surface(
                    shape = tokens.chipShape,
                    color = tokens.primaryColor,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Text(
                        text = "FEATURED",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = tokens.onPrimaryColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = product.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = tokens.onSurfaceColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = formatCurrency(product.price, product.currency),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = tokens.primaryColor
                )

                Spacer(modifier = Modifier.height(6.dp))

                ProductStatusBadge(status = product.displayStatus, tokens = tokens)
            }
        }
    }
}

// ==========================================
// 5. ALL PRODUCTS CATALOG SECTION
// ==========================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StorefrontCatalogSection(
    products: List<PublicProduct>,
    tokens: StorefrontThemeTokens,
    onProductClick: (PublicProduct) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CATALOG COLLECTION",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                color = tokens.onSurfaceColor
            )

            Text(
                text = "${products.size} items",
                style = MaterialTheme.typography.labelSmall,
                color = tokens.onSurfaceVariantColor
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (products.isEmpty()) {
            Surface(
                shape = tokens.cardShape,
                color = tokens.surfaceColor,
                border = androidx.compose.foundation.BorderStroke(tokens.borderWidth, tokens.borderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingBag,
                        contentDescription = null,
                        tint = tokens.onSurfaceVariantColor,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No visible products yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = tokens.onSurfaceColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Use the Store Editor to toggle product visibility and showcase your collection.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = tokens.onSurfaceVariantColor
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                products.forEach { product ->
                    StoreProductRowItem(
                        product = product,
                        tokens = tokens,
                        onClick = { onProductClick(product) }
                    )
                }
            }
        }
    }
}

@Composable
private fun StoreProductRowItem(
    product: PublicProduct,
    tokens: StorefrontThemeTokens,
    onClick: () -> Unit
) {
    Card(
        shape = tokens.cardShape,
        colors = CardDefaults.cardColors(containerColor = tokens.surfaceColor),
        border = androidx.compose.foundation.BorderStroke(tokens.borderWidth, tokens.borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clip(tokens.cardShape)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(tokens.chipShape)
                    .background(tokens.primaryColor.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                if (!product.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = product.imageUrl,
                        contentDescription = product.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.ShoppingBag,
                        contentDescription = null,
                        tint = tokens.primaryColor.copy(alpha = 0.4f),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = tokens.onSurfaceColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (product.shortDescription.isNotBlank()) {
                    Text(
                        text = product.shortDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.onSurfaceVariantColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatCurrency(product.price, product.currency),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = tokens.primaryColor
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    ProductStatusBadge(status = product.displayStatus, tokens = tokens)
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "View Details",
                tint = tokens.onSurfaceVariantColor,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ==========================================
// 6. INFO & POLICIES SECTION (Honest information)
// ==========================================
@Composable
private fun StorefrontInfoAndPoliciesSection(
    storefront: PublicStorefront,
    tokens: StorefrontThemeTokens
) {
    Card(
        shape = tokens.cardShape,
        colors = CardDefaults.cardColors(containerColor = tokens.surfaceColor),
        border = androidx.compose.foundation.BorderStroke(tokens.borderWidth, tokens.borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocalShipping,
                    contentDescription = null,
                    tint = tokens.primaryColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "COMMERCE & SHIPPING NOTICE",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = tokens.onSurfaceColor
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            val deliveryText = storefront.deliveryInformation?.ifBlank { null }
                ?: "Orders are fulfilled and dispatched with care. Standard production and delivery estimates apply based on your destination."

            Text(
                text = deliveryText,
                style = MaterialTheme.typography.bodySmall,
                color = tokens.onSurfaceVariantColor,
                lineHeight = 18.sp
            )

            if (!storefront.returnPolicy.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Return Policy: ${storefront.returnPolicy}",
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.onSurfaceVariantColor
                )
            }

            if (!storefront.shippingPolicy.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Shipping: ${storefront.shippingPolicy}",
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.onSurfaceVariantColor
                )
            }
        }
    }
}

// ==========================================
// 7. STORE FOOTER
// ==========================================
@Composable
private fun StorefrontFooter(
    storefront: PublicStorefront,
    tokens: StorefrontThemeTokens
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalDivider(color = tokens.borderColor)
        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = storefront.name.uppercase(Locale.ROOT),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = tokens.onSurfaceColor
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "snapbrand.site/${storefront.handle}",
            style = MaterialTheme.typography.bodySmall,
            color = tokens.onSurfaceVariantColor
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(100.dp),
                color = tokens.primaryColor.copy(alpha = 0.1f)
            ) {
                Text(
                    text = "Powered by SnapBrand",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = tokens.primaryColor,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

// ==========================================
// PRODUCT STATUS BADGE
// ==========================================
@Composable
private fun ProductStatusBadge(
    status: ProductDisplayStatus,
    tokens: StorefrontThemeTokens
) {
    val (label, bg, fg) = when (status) {
        ProductDisplayStatus.AVAILABLE -> Triple(
            "In Stock",
            Color(0xFF10B981).copy(alpha = 0.15f),
            Color(0xFF059669)
        )
        ProductDisplayStatus.OUT_OF_STOCK -> Triple(
            "Out of Stock",
            Color(0xFFEF4444).copy(alpha = 0.15f),
            Color(0xFFDC2626)
        )
        ProductDisplayStatus.COMING_SOON -> Triple(
            "Coming Soon",
            Color(0xFFF59E0B).copy(alpha = 0.15f),
            Color(0xFFD97706)
        )
        ProductDisplayStatus.DRAFT_HIDDEN -> Triple(
            "Hidden (Draft)",
            tokens.borderColor,
            tokens.onSurfaceVariantColor
        )
    }

    Surface(
        shape = tokens.chipShape,
        color = bg
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = fg,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

// ==========================================
// PRODUCT DETAIL DIALOG
// ==========================================
@Composable
private fun ProductDetailDialog(
    product: PublicProduct,
    tokens: StorefrontThemeTokens,
    onDismiss: () -> Unit,
    onAddToCart: (product: PublicProduct, variantId: String?, quantity: Int) -> Unit
) {
    var selectedVariantId by remember {
        mutableStateOf(product.variants.firstOrNull()?.id)
    }
    var quantity by remember { mutableStateOf(1) }
    val isOutOfStock = product.displayStatus == ProductDisplayStatus.OUT_OF_STOCK || product.inventory <= 0

    AlertDialog(
        onDismissRequest = onDismiss,
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = tokens.buttonShape
            ) {
                Text("Close")
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!isOutOfStock) {
                        onAddToCart(product, selectedVariantId, quantity)
                    }
                },
                enabled = !isOutOfStock,
                shape = tokens.buttonShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isOutOfStock) tokens.borderColor else tokens.primaryColor
                ),
                modifier = Modifier.testTag("add_to_cart_button")
            ) {
                if (isOutOfStock) {
                    Text("Out of Stock", color = tokens.onSurfaceVariantColor)
                } else {
                    val unitPriceMinor = Money.toMinorUnits(product.price)
                    val totalMinor = Money.calculateItemSubtotal(unitPriceMinor, quantity)
                    Text(
                        text = "Add to Cart • ${Money.format(totalMinor, product.currency)}",
                        fontWeight = FontWeight.Bold,
                        color = tokens.onPrimaryColor
                    )
                }
            }
        },
        title = {
            Text(
                text = product.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(tokens.cardShape)
                        .background(tokens.primaryColor.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!product.imageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = product.imageUrl,
                            contentDescription = product.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.ShoppingBag,
                            contentDescription = null,
                            tint = tokens.primaryColor.copy(alpha = 0.4f),
                            modifier = Modifier.size(54.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatCurrency(product.price, product.currency),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = tokens.primaryColor
                    )
                    ProductStatusBadge(status = product.displayStatus, tokens = tokens)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = product.description,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 20.sp
                )

                if (product.variants.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Select Variant:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        product.variants.forEach { v ->
                            val isSelected = selectedVariantId == v.id
                            val variantLabel = listOfNotNull(v.size, v.color).joinToString(" / ").ifBlank { v.sku ?: "Option" }
                            Surface(
                                shape = tokens.chipShape,
                                color = if (isSelected) tokens.primaryColor else MaterialTheme.colorScheme.surfaceVariant,
                                border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, tokens.borderColor),
                                modifier = Modifier
                                    .clip(tokens.chipShape)
                                    .clickable { selectedVariantId = v.id }
                            ) {
                                Text(
                                    text = variantLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) tokens.onPrimaryColor else tokens.onSurfaceColor,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                if (!isOutOfStock) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Quantity",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = tokens.backgroundColor,
                                border = androidx.compose.foundation.BorderStroke(1.dp, tokens.borderColor),
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .clickable { if (quantity > 1) quantity-- }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(16.dp))
                                }
                            }

                            Text(
                                text = "$quantity",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 14.dp)
                            )

                            Surface(
                                shape = CircleShape,
                                color = tokens.backgroundColor,
                                border = androidx.compose.foundation.BorderStroke(1.dp, tokens.borderColor),
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        if (quantity < product.inventory) quantity++
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                if (product.sellingPoints.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Highlights:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    product.sellingPoints.forEach { point ->
                        Row(modifier = Modifier.padding(vertical = 2.dp)) {
                            Text("• ", fontWeight = FontWeight.Bold, color = tokens.primaryColor)
                            Text(text = point, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    )
}

// ==========================================
// CONTACT INFORMATION DIALOG
// ==========================================
@Composable
private fun ContactInformationDialog(
    storefront: PublicStorefront,
    tokens: StorefrontThemeTokens,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = tokens.buttonShape,
                colors = ButtonDefaults.buttonColors(containerColor = tokens.primaryColor)
            ) {
                Text("Close", color = tokens.onPrimaryColor)
            }
        },
        title = {
            Text(
                text = "Contact ${storefront.name}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!storefront.contactEmail.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Email, contentDescription = null, tint = tokens.primaryColor, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = storefront.contactEmail, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                if (!storefront.contactPhone.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Phone, contentDescription = null, tint = tokens.primaryColor, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = storefront.contactPhone, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                if (!storefront.whatsappNumber.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = Color(0xFF25D366), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "WhatsApp: ${storefront.whatsappNumber}", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                if (storefront.contactEmail.isNullOrBlank() && storefront.contactPhone.isNullOrBlank() && storefront.whatsappNumber.isNullOrBlank()) {
                    Text(
                        text = "The store owner hasn't listed direct contact channels yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = tokens.onSurfaceVariantColor
                    )
                }
            }
        }
    )
}

private fun formatCurrency(amount: Double, currencyCode: String): String {
    return try {
        val format = NumberFormat.getCurrencyInstance(Locale.US)
        format.currency = java.util.Currency.getInstance(currencyCode)
        format.format(amount)
    } catch (e: Exception) {
        "$${String.format(Locale.US, "%.2f", amount)}"
    }
}
