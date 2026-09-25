package com.example.ui.screens.store

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.Shop
import com.example.data.model.StoreStatus
import com.example.data.model.CustomerDeliveryInfo
import com.example.data.model.Order
import com.example.store.PublicStorefront
import com.example.store.checkout.CartState
import com.example.store.payment.PaymentInitResponse

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicStorefrontView(
    storefront: PublicStorefront?,
    themeShop: Shop?,
    isLoading: Boolean,
    errorMessage: String?,
    cartState: CartState? = null,
    onAddToCart: ((productId: String, variantId: String?, quantity: Int) -> Unit)? = null,
    onUpdateCartQuantity: ((cartItemId: String, newQuantity: Int) -> Unit)? = null,
    onRemoveFromCart: ((cartItemId: String) -> Unit)? = null,
    onClearCart: (() -> Unit)? = null,
    onCreateOrder: ((CustomerDeliveryInfo) -> Result<Order>)? = null,
    onInitializePayment: (suspend (orderId: String) -> Result<PaymentInitResponse>)? = null,
    onVerifyPayment: (suspend (orderId: String, reference: String) -> Result<Order>)? = null,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = storefront?.name ?: "Storefront",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("public_storefront_back")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Loading storefront...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                errorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Store Unavailable",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = onBack) {
                            Text("Back to Stores")
                        }
                    }
                }
                storefront != null && themeShop != null -> {
                    if (storefront.status != StoreStatus.PUBLISHED) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Coming Soon",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${storefront.name} is currently preparing its catalog and storefront. Check back soon for the official launch!",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(onClick = onBack) {
                                Text("Return")
                            }
                        }
                    } else {
                        StorefrontRenderer(
                            storefront = storefront,
                            themeShop = themeShop,
                            viewMode = StorefrontViewMode.PUBLIC_CUSTOMER_VIEW,
                            cartState = cartState,
                            onAddToCart = onAddToCart,
                            onUpdateCartQuantity = onUpdateCartQuantity,
                            onRemoveFromCart = onRemoveFromCart,
                            onClearCart = onClearCart,
                            onCreateOrder = onCreateOrder,
                            onInitializePayment = onInitializePayment,
                            onVerifyPayment = onVerifyPayment
                        )
                    }
                }
            }
        }
    }
}
