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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Product
import com.example.data.model.Shop
import com.example.data.model.StoreStatus
import com.example.store.PublicStorefrontMapper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorefrontPreviewScreen(
    shop: Shop,
    products: List<Product>,
    onBack: () -> Unit,
    onOpenEditor: () -> Unit
) {
    var viewMode by remember { mutableStateOf(StorefrontViewMode.SELLER_PREVIEW_MOBILE) }

    val storefront = remember(shop, products) {
        PublicStorefrontMapper.toPublicStorefront(shop, products)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Preview: ${shop.name}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            StoreStatusBadge(status = shop.status)
                        }
                        Text(
                            text = "snapbrand.site/${shop.handle}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("preview_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Mobile / Desktop View Toggle
                    FilterChip(
                        selected = viewMode == StorefrontViewMode.SELLER_PREVIEW_MOBILE,
                        onClick = { viewMode = StorefrontViewMode.SELLER_PREVIEW_MOBILE },
                        label = { Text("Mobile", style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = {
                            Icon(Icons.Default.PhoneAndroid, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        modifier = Modifier.testTag("preview_toggle_mobile")
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    FilterChip(
                        selected = viewMode == StorefrontViewMode.SELLER_PREVIEW_DESKTOP,
                        onClick = { viewMode = StorefrontViewMode.SELLER_PREVIEW_DESKTOP },
                        label = { Text("Desktop", style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = {
                            Icon(Icons.Default.DesktopWindows, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        modifier = Modifier.testTag("preview_toggle_desktop")
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = onOpenEditor,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("preview_open_editor_button")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Edit", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Informational status banner for unpublished or draft stores
            if (shop.status != StoreStatus.PUBLISHED) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "👀 PREVIEW MODE • This shop is currently ${shop.status}. Customers cannot access snapbrand.site/${shop.handle} until you hit Publish in the Store Editor.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            StorefrontRenderer(
                storefront = storefront,
                themeShop = shop,
                viewMode = viewMode,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun StoreStatusBadge(status: String) {
    val (bg, text, label) = when (status) {
        StoreStatus.PUBLISHED -> Triple(Color(0xFF10B981).copy(alpha = 0.2f), Color(0xFF059669), "LIVE")
        StoreStatus.UNPUBLISHED -> Triple(Color(0xFFEF4444).copy(alpha = 0.2f), Color(0xFFDC2626), "UNPUBLISHED")
        else -> Triple(Color(0xFFF59E0B).copy(alpha = 0.2f), Color(0xFFD97706), "DRAFT")
    }

    Surface(
        shape = RoundedCornerShape(100.dp),
        color = bg
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.8.sp,
            color = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}
