package com.example.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.UserProfile
import com.example.ui.theme.SnapIndigoPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    user: UserProfile,
    isDarkTheme: Boolean,
    onToggleDarkTheme: (Boolean) -> Unit,
    onToggleNotifications: (Boolean) -> Unit,
    onSignOut: () -> Unit,
    onBack: () -> Unit,
    onNavigateToBilling: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showAccountDialog by remember { mutableStateOf(false) }
    var showFirebaseDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("settings_screen"),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 840.dp)
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                // Section: Preferences
                SettingsSectionTitle("PREFERENCES")

                SettingsToggleRow(
                    icon = Icons.Default.DarkMode,
                    title = "Dark Theme",
                    subtitle = "Toggle energetic dark or light aesthetic",
                    checked = isDarkTheme,
                    onCheckedChange = onToggleDarkTheme,
                    testTag = "dark_theme_switch"
                )

                SettingsToggleRow(
                    icon = Icons.Default.Notifications,
                    title = "Order & Shop Notifications",
                    subtitle = "Receive alerts when shops receive orders",
                    checked = user.notificationsEnabled,
                    onCheckedChange = onToggleNotifications,
                    testTag = "notifications_switch"
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Section: Subscription & Billing
                SettingsSectionTitle("SUBSCRIPTION & BILLING")

                SettingsClickableRow(
                    icon = Icons.Default.CreditCard,
                    title = "Creator Subscription",
                    subtitle = "Current plan: ${user.subscriptionPlan} • ${user.subscriptionStatus}",
                    onClick = onNavigateToBilling
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Section: Account & Security
                SettingsSectionTitle("ACCOUNT & SECURITY")

                SettingsClickableRow(
                    icon = Icons.Default.Person,
                    title = "Account Details",
                    subtitle = "UID: ${user.uid.take(16)}...",
                    onClick = { showAccountDialog = true }
                )

                SettingsClickableRow(
                    icon = Icons.Default.Security,
                    title = "Privacy Policy & Data Scoping",
                    subtitle = "Learn how your private data and designs are isolated",
                    onClick = { showPrivacyDialog = true }
                )

                SettingsClickableRow(
                    icon = Icons.Default.Description,
                    title = "Terms of Service",
                    subtitle = "SnapBrand creator and seller terms",
                    onClick = { showTermsDialog = true }
                )

                SettingsClickableRow(
                    icon = Icons.Default.Info,
                    title = "Firebase & Cloud Infrastructure",
                    subtitle = "Inspect security rules, App Check, and cloud status",
                    onClick = { showFirebaseDialog = true }
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Section: Sign Out
                OutlinedButton(
                    onClick = onSignOut,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("settings_sign_out_button")
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign Out", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }

        if (showAccountDialog) {
            AlertDialog(
                onDismissRequest = { showAccountDialog = false },
                title = { Text("Account Details", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Display Name: ${user.displayName}", style = MaterialTheme.typography.bodyMedium)
                        Text("Username: @${user.username}", style = MaterialTheme.typography.bodyMedium)
                        Text("Email: ${user.email}", style = MaterialTheme.typography.bodyMedium)
                        Text("Firebase UID: ${user.uid}", style = MaterialTheme.typography.bodySmall)
                        Text("Subscription Plan: ${user.subscriptionPlan}", style = MaterialTheme.typography.bodyMedium)
                        Text("Status: ${user.subscriptionStatus}", style = MaterialTheme.typography.bodyMedium)
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showAccountDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = SnapIndigoPrimary)
                    ) {
                        Text("Close", color = Color.White)
                    }
                }
            )
        }

        if (showPrivacyDialog) {
            AlertDialog(
                onDismissRequest = { showPrivacyDialog = false },
                title = { Text("Privacy Policy & Data Isolation", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Data Isolation Commitment:",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "SnapBrand enforces complete tenancy isolation. Account A cannot access Account B's shops, products, orders, or private settings. All access is protected via Firestore Security Rules and server-side authentication.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Uploaded Photos & Intellectual Property:",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Any photo or design you snap belongs to you. SnapBrand uses your input solely to synthesize your custom brand identity and catalog.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showPrivacyDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = SnapIndigoPrimary)
                    ) {
                        Text("Got It", color = Color.White)
                    }
                }
            )
        }

        if (showTermsDialog) {
            AlertDialog(
                onDismissRequest = { showTermsDialog = false },
                title = { Text("Terms of Service", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "SnapBrand Platform Terms (Phase 0 Foundation):",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "1. Merch Shops utilize on-demand printing partners (Printify). Sellers must have rights to uploaded designs.\n" +
                                   "2. Real Shops require sellers to honor fulfillment of physical goods.\n" +
                                   "3. All transactions are securely processed through trusted server-side gateways (Paystack).",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showTermsDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = SnapIndigoPrimary)
                    ) {
                        Text("Close", color = Color.White)
                    }
                }
            )
        }

        if (showFirebaseDialog) {
            AlertDialog(
                onDismissRequest = { showFirebaseDialog = false },
                title = {
                    Text(
                        text = "Firebase & Security Architecture",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
                    )
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = "Project Configuration & Status:",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "• Firebase Authentication: Ready (Email/Password active; Google OAuth requires google-services.json & Web Client ID)\n" +
                                   "• Cloud Firestore: Rules hardened (users, shops, products, orders, customers, transactions strictly owner-isolated)\n" +
                                   "• Firebase Storage: Rules enforced with ownerUid checks (/shops/{ownerUid}/..., /products/{ownerUid}/...)\n" +
                                   "• Cloud Functions: TypeScript boundary defined for Paystack webhooks & Printify fulfillment\n" +
                                   "• App Check: Debug token provider integrated in build variants\n" +
                                   "• Analytics: Event tracking active for auth, shops, and orders\n" +
                                   "• File Status: google-services.json is pending upload in Firebase Console settings.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showFirebaseDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = SnapIndigoPrimary)
                    ) {
                        Text("Acknowledge", color = Color.White)
                    }
                }
            )
        }
    }
}

@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = SnapIndigoPrimary
                ),
                modifier = Modifier.testTag(testTag)
            )
        }
    }
}

@Composable
fun SettingsClickableRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
