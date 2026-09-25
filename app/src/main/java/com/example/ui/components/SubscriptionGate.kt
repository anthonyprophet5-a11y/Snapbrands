package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.subscription.FeatureEntitlement
import com.example.subscription.SubscriptionPlan
import com.example.subscription.SubscriptionProductCatalog
import com.example.subscription.UserEntitlementProjection

/**
 * Reusable Subscription Gate for Composable screens.
 * Renders content if entitled, or an elegant locked state with an upgrade prompt.
 */
@Composable
fun RequirePlan(
    requiredPlan: SubscriptionPlan,
    projection: UserEntitlementProjection,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val isEntitled = when (requiredPlan) {
        SubscriptionPlan.NONE -> true
        SubscriptionPlan.SNAPBRAND -> projection.hasCoreAccess
        SubscriptionPlan.SNAPBRAND_PRO -> projection.hasProAccess
    }

    if (isEntitled) {
        content()
    } else {
        SubscriptionLockCard(
            requiredPlan = requiredPlan,
            onUpgradeClick = onUpgradeClick,
            modifier = modifier
        )
    }
}

@Composable
fun RequireFeature(
    feature: FeatureEntitlement,
    projection: UserEntitlementProjection,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val isEntitled = if (feature.isProOnly) {
        projection.hasProAccess
    } else {
        projection.hasCoreAccess
    }

    if (isEntitled) {
        content()
    } else {
        SubscriptionLockCard(
            requiredPlan = if (feature.isProOnly) SubscriptionPlan.SNAPBRAND_PRO else SubscriptionPlan.SNAPBRAND,
            featureName = feature.description,
            onUpgradeClick = onUpgradeClick,
            modifier = modifier
        )
    }
}

@Composable
fun SubscriptionLockCard(
    requiredPlan: SubscriptionPlan,
    featureName: String? = null,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(16.dp)
            )
            .testTag("subscription_gate_lock_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Feature Locked",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (featureName != null) "Unlock $featureName" else "Subscription Required",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (requiredPlan == SubscriptionPlan.SNAPBRAND_PRO) {
                    "This feature is part of SnapBrand Pro ($30/month). Upgrade to access advanced AI marketing & strategic tools."
                } else {
                    "This feature requires an active SnapBrand subscription ($20/month) to publish and process live customer commerce."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onUpgradeClick,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.testTag("subscription_gate_upgrade_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (requiredPlan == SubscriptionPlan.SNAPBRAND_PRO) {
                            "Get SnapBrand Pro • $30/mo"
                        } else {
                            "Get SnapBrand • $20/mo"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun SubscriptionUpgradeDialog(
    requiredPlan: SubscriptionPlan,
    featureName: String,
    onDismiss: () -> Unit,
    onNavigateToBilling: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Upgrade to ${requiredPlan.displayName}",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "$featureName is available on the ${requiredPlan.displayName} plan (${SubscriptionProductCatalog.formatPrice(requiredPlan.priceUsdMonthlyMinor)}/month).",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (requiredPlan == SubscriptionPlan.SNAPBRAND_PRO) {
                        "SnapBrand Pro includes full commerce infrastructure plus advanced AI scripts, social captions, TikTok/Reels strategies, and WhatsApp copy."
                    } else {
                        "SnapBrand includes end-to-end creation: Snap Engine, Brand Genius, Product Engine, Storefront hosting, and Paystack checkout."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onDismiss()
                    onNavigateToBilling()
                }
            ) {
                Text("View Plans & Upgrade")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Not Now")
            }
        }
    )
}
