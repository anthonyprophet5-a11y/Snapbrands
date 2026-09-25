package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.subscription.BillingTransaction
import com.example.subscription.BillingTransactionStatus
import com.example.subscription.Subscription
import com.example.subscription.SubscriptionPlan
import com.example.subscription.SubscriptionProductCatalog
import com.example.subscription.SubscriptionStatus
import com.example.subscription.UserEntitlementProjection
import com.example.ui.theme.SnapIndigoPrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * SNAPBRAND BILLING & SUBSCRIPTIONS SCREEN — PHASE 6
 * Centralized, server-authoritative billing UI.
 * Displays:
 * - Current Plan & Entitlement Status
 * - Plan Comparison ($20 Core vs $30 Pro)
 * - Subscription Lifecycle Management (Upgrade, Downgrade, Cancel at Period End)
 * - Historical Billing Transactions / Receipts
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillingScreen(
    subscription: Subscription?,
    entitlement: UserEntitlementProjection,
    billingHistory: List<BillingTransaction>,
    isLoading: Boolean,
    errorMessage: String?,
    onSubscribe: (SubscriptionPlan) -> Unit,
    onUpgradeOrDowngrade: (SubscriptionPlan) -> Unit,
    onCancelSubscription: (Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var planToConfirmChange by remember { mutableStateOf<SubscriptionPlan?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Subscription & Billing",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Manage your creator plan & entitlements",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("billing_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("billing_screen"),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 840.dp)
                    .padding(horizontal = 16.dp)
            ) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.fillMaxWidth().testTag("billing_tabs")
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Overview & Status") },
                        icon = { Icon(Icons.Default.CreditCard, contentDescription = null) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Compare Plans") },
                        icon = { Icon(Icons.Default.Star, contentDescription = null) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Billing History") },
                        icon = { Icon(Icons.Default.Receipt, contentDescription = null) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (errorMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorMessage,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(36.dp))
                    }
                }

                when (selectedTab) {
                    0 -> BillingOverviewTab(
                        subscription = subscription,
                        entitlement = entitlement,
                        onUpgradeClick = { selectedTab = 1 },
                        onCancelClick = { showCancelDialog = true }
                    )
                    1 -> PlanComparisonTab(
                        currentPlan = entitlement.plan,
                        subscriptionStatus = entitlement.status,
                        onSelectPlan = { plan ->
                            if (entitlement.plan == SubscriptionPlan.NONE || entitlement.status == SubscriptionStatus.EXPIRED || entitlement.status == SubscriptionStatus.INACTIVE) {
                                onSubscribe(plan)
                            } else {
                                planToConfirmChange = plan
                            }
                        }
                    )
                    2 -> BillingHistoryTab(
                        transactions = billingHistory
                    )
                }
            }
        }
    }

    // Cancel Subscription Confirmation Dialog
    if (showCancelDialog && subscription != null) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancel Subscription", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Your subscription will remain active until the end of your billing period (${formatDate(subscription.currentPeriodEnd)}).",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Your products, storefronts, and order history will NOT be deleted. Only live publishing and active commerce features will be paused after expiration.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCancelDialog = false
                        onCancelSubscription(true)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Confirm Cancellation")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCancelDialog = false }) {
                    Text("Keep My Plan")
                }
            }
        )
    }

    // Change Plan Confirmation Dialog
    planToConfirmChange?.let { targetPlan ->
        val isUpgrade = targetPlan.priceUsdMonthlyMinor > entitlement.plan.priceUsdMonthlyMinor
        AlertDialog(
            onDismissRequest = { planToConfirmChange = null },
            title = {
                Text(
                    text = if (isUpgrade) "Upgrade to ${targetPlan.displayName}" else "Switch to ${targetPlan.displayName}",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Change your plan from ${entitlement.plan.displayName} to ${targetPlan.displayName} (${SubscriptionProductCatalog.formatPrice(targetPlan.priceUsdMonthlyMinor)}/month).",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isUpgrade) {
                            "Upgrading activates full SnapBrand Pro entitlements immediately upon provider verification."
                        } else {
                            "Downgrading will adjust your plan at the next renewal cycle."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val plan = targetPlan
                        planToConfirmChange = null
                        onUpgradeOrDowngrade(plan)
                    }
                ) {
                    Text(if (isUpgrade) "Confirm Upgrade" else "Confirm Plan Change")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { planToConfirmChange = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Tab 1: Current Plan & Entitlements Overview
 */
@Composable
private fun BillingOverviewTab(
    subscription: Subscription?,
    entitlement: UserEntitlementProjection,
    onUpgradeClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth().testTag("billing_overview_tab")
    ) {
        item {
            // Main Subscription Status Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "CURRENT PLAN",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = entitlement.plan.displayName,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        StatusBadge(status = entitlement.status, cancelAtPeriodEnd = entitlement.cancelAtPeriodEnd)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Price",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${SubscriptionProductCatalog.formatPrice(entitlement.plan.priceUsdMonthlyMinor)} / month",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (entitlement.cancelAtPeriodEnd) "Access Ends On" else "Renews On",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (entitlement.currentPeriodEnd > 0) formatDate(entitlement.currentPeriodEnd) else "N/A",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (entitlement.statusMessage.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = entitlement.statusMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (entitlement.isPastDue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (entitlement.plan != SubscriptionPlan.SNAPBRAND_PRO) {
                            Button(
                                onClick = onUpgradeClick,
                                modifier = Modifier.weight(1f).testTag("billing_upgrade_pro_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = SnapIndigoPrimary)
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Upgrade to Pro ($30/mo)", fontWeight = FontWeight.Bold)
                            }
                        }

                        if (subscription != null && subscription.status == SubscriptionStatus.ACTIVE && !subscription.cancelAtPeriodEnd) {
                            OutlinedButton(
                                onClick = onCancelClick,
                                modifier = Modifier.testTag("billing_cancel_btn")
                            ) {
                                Text("Cancel Subscription")
                            }
                        }
                    }
                }
            }
        }

        item {
            // Entitlements Breakdown Card
            Text(
                text = "YOUR ACTIVE ENTITLEMENTS",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    EntitlementRow(
                        title = "Core Commerce & Store Engine",
                        subtitle = "Snap cataloging, AI brand identity, store publishing & orders",
                        isUnlocked = entitlement.hasCoreAccess
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    EntitlementRow(
                        title = "Customer Checkout & Payments",
                        subtitle = "Cart system & live customer payment processing",
                        isUnlocked = entitlement.hasCoreAccess
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    EntitlementRow(
                        title = "AI Marketing Engine (SnapBrand Pro)",
                        subtitle = "Phase 10 AI scripts, social captions, TikTok/Reels & campaigns",
                        isUnlocked = entitlement.hasProAccess,
                        isProBadge = true
                    )
                }
            }
        }

        item {
            // Data Security & Retention Notice
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Creator Data Protection Guarantee",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Even if a subscription expires or is cancelled, your brands, products, stores, and order records are preserved permanently. Data ownership remains yours.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tab 2: Plan Comparison ($20 Core vs $30 Pro)
 */
@Composable
private fun PlanComparisonTab(
    currentPlan: SubscriptionPlan,
    subscriptionStatus: SubscriptionStatus,
    onSelectPlan: (SubscriptionPlan) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth().testTag("plan_comparison_tab")
    ) {
        item {
            Text(
                text = "Choose the plan that fits your growth",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "All plans include unlimited products. Never capped by arbitrary per-product limits.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            // Plan Card: SNAPBRAND ($20/month)
            PlanCard(
                plan = SubscriptionPlan.SNAPBRAND,
                isCurrent = currentPlan == SubscriptionPlan.SNAPBRAND && subscriptionStatus == SubscriptionStatus.ACTIVE,
                features = listOf(
                    "Snap Engine — Turn any photo into catalog products",
                    "AI Brand Genius — Instant brand name, logo & visual design",
                    "Product Engine — Full catalog generation & editing",
                    "Store Engine — Instant customizable digital storefront",
                    "Store Publishing & Custom Handle Hosting",
                    "Real Customer Checkout & Paystack Payments",
                    "Merchant Order Management Dashboard"
                ),
                futureFeatures = emptyList(),
                onSelect = { onSelectPlan(SubscriptionPlan.SNAPBRAND) }
            )
        }

        item {
            // Plan Card: SNAPBRAND PRO ($30/month)
            PlanCard(
                plan = SubscriptionPlan.SNAPBRAND_PRO,
                isCurrent = currentPlan == SubscriptionPlan.SNAPBRAND_PRO && subscriptionStatus == SubscriptionStatus.ACTIVE,
                isRecommended = true,
                features = listOf(
                    "Everything in SnapBrand ($20/mo)",
                    "Priority Commerce & Storefront Infrastructure",
                    "Multi-store Management"
                ),
                futureFeatures = listOf(
                    "Phase 10: AI TikTok & Instagram Reels Content Ideas",
                    "Phase 10: AI Video Scripts & Voiceover Prompts",
                    "Phase 10: AI Social Captions & Viral Hashtag Generator",
                    "Phase 10: AI Ad Copy for Instagram & Facebook Ads",
                    "Phase 10: WhatsApp Broadcast & Direct Marketing Copy",
                    "Phase 10: Advanced Traffic & Conversion Analytics"
                ),
                onSelect = { onSelectPlan(SubscriptionPlan.SNAPBRAND_PRO) }
            )
        }
    }
}

@Composable
private fun PlanCard(
    plan: SubscriptionPlan,
    isCurrent: Boolean,
    isRecommended: Boolean = false,
    features: List<String>,
    futureFeatures: List<String>,
    onSelect: () -> Unit
) {
    val borderColor = if (isRecommended) SnapIndigoPrimary else MaterialTheme.colorScheme.outlineVariant
    val borderWidth = if (isRecommended) 2.dp else 1.dp

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(borderWidth, borderColor, RoundedCornerShape(16.dp))
            .testTag("plan_card_${plan.planId}")
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    if (isRecommended) {
                        Text(
                            text = "MOST POPULAR",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = SnapIndigoPrimary
                        )
                    }
                    Text(
                        text = plan.displayName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = plan.targetAudience,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = SubscriptionProductCatalog.formatPrice(plan.priceUsdMonthlyMinor),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "per month",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(16.dp))

            // Feature list
            features.forEach { feat ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = feat, style = MaterialTheme.typography.bodyMedium)
                }
            }

            if (futureFeatures.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "FUTURE PRO CAPABILITIES (PHASE 10 PREPARED)",
                    style = MaterialTheme.typography.labelSmall,
                    color = SnapIndigoPrimary,
                    fontWeight = FontWeight.Bold
                )
                futureFeatures.forEach { ffeat ->
                    Row(
                        modifier = Modifier.padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = SnapIndigoPrimary.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = ffeat,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (isCurrent) {
                OutlinedButton(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.fillMaxWidth().testTag("current_plan_btn_${plan.planId}")
                ) {
                    Text("Current Plan")
                }
            } else {
                Button(
                    onClick = onSelect,
                    modifier = Modifier.fillMaxWidth().testTag("select_plan_btn_${plan.planId}"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRecommended) SnapIndigoPrimary else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = "Choose ${plan.displayName} • ${SubscriptionProductCatalog.formatPrice(plan.priceUsdMonthlyMinor)}/mo",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Tab 3: Historical Billing Transactions / Receipts
 */
@Composable
private fun BillingHistoryTab(
    transactions: List<BillingTransaction>
) {
    if (transactions.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Receipt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No Billing History Found",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Transactions and receipts will appear here once verified by the payment provider.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth().testTag("billing_history_list")
        ) {
            items(transactions, key = { it.id }) { txn ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = SubscriptionProductCatalog.formatPrice(txn.amountMinor, txn.currency),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                TransactionStatusBadge(txn.status)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Ref: ${txn.providerReference}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatDate(txn.paidAt ?: txn.createdAt),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: SubscriptionStatus, cancelAtPeriodEnd: Boolean) {
    val (bgColor, textColor, text) = when {
        status == SubscriptionStatus.CANCELLED || cancelAtPeriodEnd ->
            Triple(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f), MaterialTheme.colorScheme.onErrorContainer, "Cancelling at Period End")
        status == SubscriptionStatus.ACTIVE ->
            Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), "Active")
        status == SubscriptionStatus.PAST_DUE ->
            Triple(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.error, "Past Due")
        status == SubscriptionStatus.PENDING ->
            Triple(Color(0xFFFFF3E0), Color(0xFFE65100), "Pending")
        status == SubscriptionStatus.EXPIRED ->
            Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, "Expired")
        else ->
            Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, "Inactive")
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun TransactionStatusBadge(status: BillingTransactionStatus) {
    val (color, label) = when (status) {
        BillingTransactionStatus.PAID -> Color(0xFF2E7D32) to "Paid"
        BillingTransactionStatus.FAILED -> Color(0xFFC62828) to "Failed"
        BillingTransactionStatus.REFUNDED -> Color(0xFF1565C0) to "Refunded"
        BillingTransactionStatus.PENDING -> Color(0xFFEF6C00) to "Pending"
    }

    Text(
        text = "• $label",
        color = color,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun EntitlementRow(
    title: String,
    subtitle: String,
    isUnlocked: Boolean,
    isProBadge: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isUnlocked) Icons.Default.CheckCircle else Icons.Default.Lock,
            contentDescription = null,
            tint = if (isUnlocked) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (isProBadge) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(SnapIndigoPrimary.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "PRO",
                            color = SnapIndigoPrimary,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatDate(timestamp: Long): String {
    if (timestamp <= 0L) return "N/A"
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
