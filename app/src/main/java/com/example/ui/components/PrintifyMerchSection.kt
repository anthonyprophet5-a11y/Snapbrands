package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Product
import com.example.printify.model.*
import com.example.printify.service.PrintifyDesignValidator
import com.example.printify.service.PrintifyPricingCalculator
import com.example.store.checkout.Money

/**
 * SNAPBRAND — PHASE 7 PRINTIFY ON-DEMAND PRODUCTION CARD
 *
 * Provides honest, transparent status for MERCH products:
 * - Connection status: Not Connected / Connecting / Connected
 * - Design status: Ready / Artwork Required / Configuration Required
 * - Blueprints, providers, and variant selection
 * - Exact integer minor-unit profit calculations (never fabricated)
 * - M3 compliant design
 */
@Composable
fun PrintifyMerchSection(
    product: Product,
    connection: PrintifyShopConnection?,
    mapping: PrintifyProductMapping?,
    isLoading: Boolean,
    blueprints: List<PrintifyBlueprint>,
    providers: List<PrintifyPrintProvider>,
    variants: List<PrintifyVariant>,
    shippingEstimate: PrintifyShippingEstimate?,
    onConnectShopClick: () -> Unit,
    onDisconnectShopClick: () -> Unit,
    onSelectBlueprint: (Int) -> Unit,
    onSelectProvider: (Int, Int) -> Unit,
    onCreateProduct: (blueprintId: Int, providerId: Int, variantIds: List<Int>) -> Unit,
    modifier: Modifier = Modifier
) {
    // Only display for MERCH products
    if (!product.businessMode.equals("MERCH", ignoreCase = true)) {
        return
    }

    val designValidation = remember(product) {
        PrintifyDesignValidator.validateForPrintify(product)
    }

    var selectedBlueprintId by remember { mutableStateOf(mapping?.printifyBlueprintId ?: blueprints.firstOrNull()?.id ?: 12) }
    var selectedProviderId by remember { mutableStateOf(mapping?.printifyProviderId ?: providers.firstOrNull()?.id ?: 1) }
    var selectedVariantIds by remember { mutableStateOf(mapping?.selectedVariantIds ?: listOf(1)) }

    val isConnected = connection?.status == PrintifyConnectionStatus.CONNECTED
    val isMapped = mapping != null || !product.printifyProductId.isNullOrBlank()

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = modifier
            .fillMaxWidth()
            .testTag("printify_merch_section")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Title & Connection Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Print,
                        contentDescription = "Printify Integration",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "PRINTIFY PRODUCTION",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Connection Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (connection?.status) {
                        PrintifyConnectionStatus.CONNECTED -> Color(0xFF1B5E20).copy(alpha = 0.15f)
                        PrintifyConnectionStatus.ERROR -> Color(0xFFB00020).copy(alpha = 0.15f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    border = BorderStroke(
                        1.dp,
                        when (connection?.status) {
                            PrintifyConnectionStatus.CONNECTED -> Color(0xFF2E7D32)
                            PrintifyConnectionStatus.ERROR -> Color(0xFFD32F2F)
                            else -> MaterialTheme.colorScheme.outlineVariant
                        }
                    )
                ) {
                    Text(
                        text = when (connection?.status) {
                            PrintifyConnectionStatus.CONNECTED -> "Connected"
                            PrintifyConnectionStatus.ERROR -> "Connection Error"
                            PrintifyConnectionStatus.DISCONNECTED -> "Disconnected"
                            else -> "Not Connected"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when (connection?.status) {
                            PrintifyConnectionStatus.CONNECTED -> Color(0xFF2E7D32)
                            PrintifyConnectionStatus.ERROR -> Color(0xFFD32F2F)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // 1. Connection State CTA
            if (!isConnected) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Connect Your Printify Account",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Link your Printify shop to enable on-demand fulfillment, automated routing, and production cost tracking.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = onConnectShopClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("connect_printify_button")
                    ) {
                        Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Connect Printify Shop")
                    }
                }
            } else {
                // Connected Shop Details
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = connection?.printifyShopTitle ?: "Printify Shop #${connection?.printifyShopId}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Shop ID: ${connection?.printifyShopId}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = onDisconnectShopClick) {
                        Text("Disconnect", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // 2. Design Asset Readiness Status
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (designValidation.status) {
                        PrintifyDesignStatus.READY -> Icons.Default.CheckCircle
                        PrintifyDesignStatus.ARTWORK_REQUIRED -> Icons.Default.Warning
                        PrintifyDesignStatus.CONFIGURATION_REQUIRED -> Icons.Default.Palette
                        else -> Icons.Default.Info
                    },
                    contentDescription = null,
                    tint = when (designValidation.status) {
                        PrintifyDesignStatus.READY -> Color(0xFF2E7D32)
                        PrintifyDesignStatus.ARTWORK_REQUIRED -> Color(0xFFE65100)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Design Status: ${designValidation.status.name.replace('_', ' ')}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = designValidation.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // 3. Printify Product Status / Mapping
            if (isMapped) {
                val printifyId = mapping?.printifyProductId ?: product.printifyProductId ?: ""
                val statusText = mapping?.printifyStatus ?: product.printifyStatus ?: "Draft"

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1B5E20).copy(alpha = 0.08f))
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Printify Product #$printifyId",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                        Text(
                            text = "Status: $statusText",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF2E7D32)
                        )
                    }
                    Text(
                        text = "Blueprint: ${mapping?.printifyBlueprintId ?: product.printifyBlueprintId ?: "Standard"} | Provider: ${mapping?.printifyProviderId ?: product.printifyProviderId ?: "Default"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            } else if (isConnected) {
                // Creation Action Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(10.dp)
                ) {
                    Text(
                        text = "Sync to Printify",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Create this MERCH item in Printify with verified artwork.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            onCreateProduct(selectedBlueprintId, selectedProviderId, selectedVariantIds)
                        },
                        enabled = !isLoading && designValidation.isPrintReady,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("publish_to_printify_button")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Syncing to Printify...")
                        } else {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (designValidation.isPrintReady) "Create on Printify"
                                else "Artwork Required"
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // 4. Financial Profit Breakdown (Exact Minor Units)
            val sellingPriceMinor = Money.toMinorUnits(product.price)
            val breakdown = remember(product.price) {
                PrintifyPricingCalculator.calculateBreakdown(
                    sellingPriceMinor = sellingPriceMinor,
                    productionCostMinor = null, // Transparently unavailable until provider syncs
                    shippingCostMinor = null,
                    currency = product.currency
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(10.dp)
            ) {
                Text(
                    text = "PRICING & PROFIT BREAKDOWN",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Selling Price:", style = MaterialTheme.typography.bodySmall)
                    Text(
                        Money.format(breakdown.sellingPriceMinor, breakdown.currency),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Printify Production Cost:", style = MaterialTheme.typography.bodySmall)
                    Text(
                        PrintifyPricingCalculator.formatOrUnavailable(breakdown.productionCostMinor, breakdown.currency),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Estimated Shipping:", style = MaterialTheme.typography.bodySmall)
                    Text(
                        PrintifyPricingCalculator.formatOrUnavailable(breakdown.shippingCostMinor, breakdown.currency),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Platform Fee (5%):", style = MaterialTheme.typography.bodySmall)
                    Text(
                        Money.format(breakdown.platformFeesMinor ?: 0L, breakdown.currency),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Divider(Modifier.padding(vertical = 4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Estimated Seller Margin:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (breakdown.estimatedSellerMarginMinor != null) {
                            "ESTIMATE: " + Money.format(breakdown.estimatedSellerMarginMinor!!, breakdown.currency)
                        } else {
                            "Unavailable (Cost pending)"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (breakdown.estimatedSellerMarginMinor != null) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
