package com.example.ui.screens.brand

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.BrandConcept
import com.example.data.model.Shop
import com.example.data.model.SnapAnalysis
import com.example.service.SnapBrandEvent
import com.example.ui.theme.SnapEmeraldSuccess
import com.example.ui.theme.SnapIndigoContainer
import com.example.ui.theme.SnapIndigoPrimary
import kotlinx.coroutines.launch

/**
 * Phase 2 — AI Brand Genius UI
 *
 * Transforms the Phase 1 SnapAnalysis into a fully editable, AI-generated brand concept:
 * - Brand Name, Tagline, Story, Target Audience, Personality, Visual Direction, and Logo Concept
 * - Targeted regeneration (name, tagline, story) without unnecessary full-regeneration costs
 * - User customization directive ("MAKE IT MORE...")
 * - Full manual editing sheet
 * - Draft Shop creation with ownership enforcement and stable IDs for resume flow
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BrandGeniusView(
    analysis: SnapAnalysis,
    photoBitmap: Bitmap?,
    photoUri: Uri?,
    brandConcept: BrandConcept?,
    isGenerating: Boolean,
    isRegeneratingField: String?, // "name", "tagline", "story"
    errorMessage: String?,
    onGenerateBrand: (directive: String?) -> Unit,
    onRegenerateName: (directive: String?) -> Unit,
    onRegenerateTagline: (directive: String?) -> Unit,
    onRegenerateStory: (directive: String?) -> Unit,
    onUpdateBrandConcept: (BrandConcept) -> Unit,
    onAcceptBrand: () -> Unit,
    onBackToAnalysis: () -> Unit,
    onDismiss: () -> Unit,
    onLogAnalytics: (SnapBrandEvent, Map<String, Any>) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var showEditSheet by remember { mutableStateOf(false) }
    var userDirective by remember { mutableStateOf("") }
    var showAcceptSuccessDialog by remember { mutableStateOf(false) }

    // Kick off initial brand generation if not already loaded or in progress
    LaunchedEffect(analysis.id) {
        if (brandConcept == null && !isGenerating && errorMessage == null) {
            onGenerateBrand(null)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Top Navigation & Context Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBackToAnalysis,
                    modifier = Modifier.testTag("brand_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Snap Analysis",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = "AI BRAND GENIUS",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = SnapIndigoPrimary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Phase 2: Brand Identity",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("brand_close_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // ==========================================
        // PHOTO CONTEXT BANNER (Never lose connection)
        // ==========================================
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            border = BorderStroke(1.dp, SnapIndigoContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Photo Thumbnail
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, SnapIndigoPrimary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                ) {
                    if (photoBitmap != null) {
                        Image(
                            bitmap = photoBitmap.asImageBitmap(),
                            contentDescription = "Source Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (photoUri != null) {
                        AsyncImage(
                            model = photoUri,
                            contentDescription = "Source Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = SnapIndigoPrimary,
                            modifier = Modifier
                                .size(24.dp)
                                .align(Alignment.Center)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "YOUR SOURCE PHOTO",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = SnapIndigoPrimary,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = analysis.detectedSubject,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (analysis.recommendedBusinessMode == "MERCH") SnapIndigoContainer else SnapEmeraldSuccess.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (analysis.recommendedBusinessMode == "MERCH") "Merchandise Mode" else "Real Shop Mode",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = if (analysis.recommendedBusinessMode == "MERCH") SnapIndigoPrimary else SnapEmeraldSuccess,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // ==========================================
        // GENERATING / ERROR / CONTENT STATES
        // ==========================================
        if (isGenerating) {
            BrandGeneratingCard(detectedSubject = analysis.detectedSubject)
        } else if (errorMessage != null) {
            BrandErrorCard(
                errorMessage = errorMessage,
                onRetry = { onGenerateBrand(userDirective.takeIf { it.isNotBlank() }) }
            )
        } else if (brandConcept != null) {
            // Main Brand Concept Display
            BrandContentSection(
                concept = brandConcept,
                analysis = analysis,
                isRegeneratingField = isRegeneratingField,
                userDirective = userDirective,
                onUserDirectiveChanged = { userDirective = it },
                onRegenerateName = { onRegenerateName(userDirective.takeIf { it.isNotBlank() }) },
                onRegenerateTagline = { onRegenerateTagline(userDirective.takeIf { it.isNotBlank() }) },
                onRegenerateStory = { onRegenerateStory(userDirective.takeIf { it.isNotBlank() }) },
                onRegenerateAll = { onGenerateBrand(userDirective.takeIf { it.isNotBlank() }) },
                onOpenEditSheet = { showEditSheet = true },
                onAcceptBrand = {
                    onAcceptBrand()
                    showAcceptSuccessDialog = true
                }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    // Full Brand Edit Sheet
    if (showEditSheet && brandConcept != null) {
        EditBrandBottomSheet(
            concept = brandConcept,
            onSave = { updated ->
                onUpdateBrandConcept(updated)
                showEditSheet = false
            },
            onDismiss = { showEditSheet = false }
        )
    }

    // Success Dialog on Accept
    if (showAcceptSuccessDialog && brandConcept != null) {
        AlertDialog(
            onDismissRequest = {
                showAcceptSuccessDialog = false
                onDismiss()
            },
            icon = {
                Surface(
                    shape = CircleShape,
                    color = SnapEmeraldSuccess.copy(alpha = 0.15f),
                    modifier = Modifier.size(54.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = SnapEmeraldSuccess,
                        modifier = Modifier
                            .padding(12.dp)
                            .size(30.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "Draft Shop Created! 🎉",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = brandConcept.brandName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = SnapIndigoPrimary
                    )
                    Text(
                        text = brandConcept.usernameSuggestion,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = SnapIndigoPrimary,
                                    modifier = Modifier.padding(end = 6.dp)
                                ) {
                                    Text(
                                        text = "STATUS: DRAFT",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = "Ready for Phase 3",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Your brand identity is securely saved to your account. In Phase 3 (Product Engine), automated merchandise mockups and product listings will be generated from your original photo.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAcceptSuccessDialog = false
                        onDismiss()
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SnapIndigoPrimary),
                    modifier = Modifier.testTag("draft_shop_dialog_done_button")
                ) {
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

// ==========================================
// TRUTHFUL LOADING VIEW
// ==========================================
@Composable
private fun BrandGeneratingCard(detectedSubject: String) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        border = BorderStroke(1.dp, SnapIndigoContainer),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
        ) {
            CircularProgressIndicator(
                color = SnapIndigoPrimary,
                strokeWidth = 3.5.dp,
                modifier = Modifier.size(54.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "CRAFTING YOUR BRAND",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = SnapIndigoPrimary,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Deriving brand name, tagline, audience profile, and visual direction from \"$detectedSubject\"...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ==========================================
// ERROR CARD
// ==========================================
@Composable
private fun BrandErrorCard(
    errorMessage: String,
    onRetry: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Brand Generation Paused",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("retry_brand_button")
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Retry Brand Generation", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ==========================================
// MAIN BRAND CONTENT SECTION
// ==========================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BrandContentSection(
    concept: BrandConcept,
    analysis: SnapAnalysis,
    isRegeneratingField: String?,
    userDirective: String,
    onUserDirectiveChanged: (String) -> Unit,
    onRegenerateName: () -> Unit,
    onRegenerateTagline: () -> Unit,
    onRegenerateStory: () -> Unit,
    onRegenerateAll: () -> Unit,
    onOpenEditSheet: () -> Unit,
    onAcceptBrand: () -> Unit
) {
    Column {
        // ==========================================
        // 1. BRAND NAME & USERNAME
        // ==========================================
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
            border = BorderStroke(1.dp, SnapIndigoContainer),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("brand_name_card")
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "YOUR BRAND",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = SnapIndigoPrimary,
                        letterSpacing = 1.sp
                    )
                    Row {
                        IconButton(
                            onClick = onRegenerateName,
                            enabled = isRegeneratingField == null,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("regenerate_name_button")
                        ) {
                            if (isRegeneratingField == "name") {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Regenerate name",
                                    tint = SnapIndigoPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        IconButton(
                            onClick = onOpenEditSheet,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("edit_name_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit brand",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = concept.brandName,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.testTag("brand_name_text")
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SnapIndigoContainer.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = concept.usernameSuggestion,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = SnapIndigoPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ==========================================
        // 2. TAGLINE
        // ==========================================
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
            border = BorderStroke(1.dp, SnapIndigoContainer.copy(alpha = 0.7f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TAGLINE",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = SnapIndigoPrimary,
                        letterSpacing = 0.8.sp
                    )
                    IconButton(
                        onClick = onRegenerateTagline,
                        enabled = isRegeneratingField == null,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("regenerate_tagline_button")
                    ) {
                        if (isRegeneratingField == "tagline") {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Regenerate tagline",
                                tint = SnapIndigoPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Text(
                    text = "\"${concept.tagline}\"",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ==========================================
        // 3. BRAND STORY
        // ==========================================
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
            border = BorderStroke(1.dp, SnapIndigoContainer.copy(alpha = 0.7f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "BRAND STORY",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = SnapIndigoPrimary,
                        letterSpacing = 0.8.sp
                    )
                    IconButton(
                        onClick = onRegenerateStory,
                        enabled = isRegeneratingField == null,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("regenerate_story_button")
                    ) {
                        if (isRegeneratingField == "story") {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Regenerate story",
                                tint = SnapIndigoPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Text(
                    text = concept.brandStory,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 22.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ==========================================
        // 4. WHO WILL BUY THIS? (Target Customer)
        // ==========================================
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
            border = BorderStroke(1.dp, SnapIndigoContainer.copy(alpha = 0.7f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "WHO WILL BUY THIS?",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = SnapIndigoPrimary,
                            letterSpacing = 0.8.sp
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = SnapIndigoContainer.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = "AI Audience Suggestion",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = SnapIndigoPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = concept.targetAudience,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ==========================================
        // 5. BRAND PERSONALITY CHIPS
        // ==========================================
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
            border = BorderStroke(1.dp, SnapIndigoContainer.copy(alpha = 0.7f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "BRAND PERSONALITY",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = SnapIndigoPrimary,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    concept.brandPersonality.forEach { trait ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SnapIndigoContainer,
                            border = BorderStroke(1.dp, SnapIndigoPrimary.copy(alpha = 0.2f))
                        ) {
                            Text(
                                text = trait,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = SnapIndigoPrimary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ==========================================
        // 6. VISUAL DIRECTION & COLOR PALETTE
        // ==========================================
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
            border = BorderStroke(1.dp, SnapIndigoContainer.copy(alpha = 0.7f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "FUTURE SHOP VISUAL DIRECTION",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = SnapIndigoPrimary,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Style: ${concept.visualStyle}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Typography: ${concept.typographyPersonality}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Suggested Brand Palette (applied to your future storefront):",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    concept.suggestedColorDirection.forEach { hex ->
                        val color = try {
                            val cleanHex = if (hex.startsWith("#")) hex else "#$hex"
                            Color(android.graphics.Color.parseColor(cleanHex))
                        } catch (e: Exception) {
                            SnapIndigoPrimary
                        }
                        Surface(
                            shape = CircleShape,
                            color = color,
                            border = BorderStroke(1.dp, Color.LightGray),
                            modifier = Modifier.size(32.dp)
                        ) {}
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ==========================================
        // 7. LOGO CONCEPT (Textual)
        // ==========================================
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
            border = BorderStroke(1.dp, SnapIndigoContainer.copy(alpha = 0.7f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "LOGO CONCEPT",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = SnapIndigoPrimary,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = "Asset generation in Phase 3",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = concept.logoConcept,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ==========================================
        // 8. POLISHED BRAND PREVIEW CARD
        // ==========================================
        Text(
            text = "STOREFRONT PREVIEW",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = SnapIndigoPrimary,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(2.dp, SnapIndigoPrimary),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("brand_preview_card")
        ) {
            Column(modifier = Modifier.padding(22.dp)) {
                // Header badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = SnapIndigoPrimary,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storefront,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier
                                    .padding(6.dp)
                                    .size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = concept.usernameSuggestion,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = SnapIndigoPrimary
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = SnapEmeraldSuccess.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "PREVIEW",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = SnapEmeraldSuccess,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = concept.brandName.uppercase(),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "\"${concept.tagline}\"",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = SnapIndigoPrimary,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = concept.shortDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = SnapIndigoContainer)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "TARGET AUDIENCE",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = concept.targetAudience.take(38) + if (concept.targetAudience.length > 38) "..." else "",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ==========================================
        // 9. USER CUSTOMIZATION ("MAKE IT MORE...")
        // ==========================================
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SnapIndigoContainer.copy(alpha = 0.45f)),
            border = BorderStroke(1.dp, SnapIndigoPrimary.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = SnapIndigoPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "MAKE IT MORE...",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = SnapIndigoPrimary
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Give Gemini custom direction to refine your brand personality:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = userDirective,
                    onValueChange = onUserDirectiveChanged,
                    placeholder = { Text("e.g. more playful, more Ghanaian, more minimalist, more streetwear...") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("make_it_more_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Suggestion chips
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("more playful", "more premium", "more Ghanaian", "more minimalist", "more streetwear", "more luxury").forEach { suggestion ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, SnapIndigoPrimary.copy(alpha = 0.25f)),
                            modifier = Modifier.clickable { onUserDirectiveChanged(suggestion) }
                        ) {
                            Text(
                                text = "+ $suggestion",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                color = SnapIndigoPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onRegenerateAll,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("apply_directive_button")
                ) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = SnapIndigoPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Regenerate Brand with This Direction", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(modifier = Modifier.height(26.dp))

        // ==========================================
        // 10. PRIMARY ACTIONS
        // ==========================================
        Button(
            onClick = onAcceptBrand,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SnapIndigoPrimary,
                contentColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .testTag("continue_to_products_button")
        ) {
            Text(
                text = "CONTINUE TO PRODUCTS →",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                color = Color.White,
                letterSpacing = 0.5.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onOpenEditSheet,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("edit_full_brand_button")
            ) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Edit Brand", fontWeight = FontWeight.SemiBold)
            }

            OutlinedButton(
                onClick = onRegenerateAll,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("regenerate_all_brand_button")
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Regenerate", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ==========================================
// EDIT BRAND BOTTOM SHEET
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditBrandBottomSheet(
    concept: BrandConcept,
    onSave: (BrandConcept) -> Unit,
    onDismiss: () -> Unit
) {
    var editName by remember { mutableStateOf(concept.brandName) }
    var editHandle by remember { mutableStateOf(concept.usernameSuggestion) }
    var editTagline by remember { mutableStateOf(concept.tagline) }
    var editDescription by remember { mutableStateOf(concept.shortDescription) }
    var editStory by remember { mutableStateOf(concept.brandStory) }
    var editAudience by remember { mutableStateOf(concept.targetAudience) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Edit Brand Identity",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            Text(
                text = "Refine any details. AI suggestions are suggestions, not permanent decisions.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Brand Name
            OutlinedTextField(
                value = editName,
                onValueChange = { editName = it },
                label = { Text("Brand Name") },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("edit_brand_name_field")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Username / Handle
            OutlinedTextField(
                value = editHandle,
                onValueChange = { editHandle = it },
                label = { Text("Social Username / Handle") },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("edit_brand_handle_field")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Tagline
            OutlinedTextField(
                value = editTagline,
                onValueChange = { editTagline = it },
                label = { Text("Tagline") },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("edit_brand_tagline_field")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Short Description
            OutlinedTextField(
                value = editDescription,
                onValueChange = { editDescription = it },
                label = { Text("Short Description") },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("edit_brand_description_field")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Brand Story
            OutlinedTextField(
                value = editStory,
                onValueChange = { editStory = it },
                label = { Text("Brand Story") },
                shape = RoundedCornerShape(12.dp),
                minLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("edit_brand_story_field")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Target Audience
            OutlinedTextField(
                value = editAudience,
                onValueChange = { editAudience = it },
                label = { Text("Target Audience") },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("edit_brand_audience_field")
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val updated = concept.copy(
                        brandName = editName.trim(),
                        usernameSuggestion = if (editHandle.trim().startsWith("@")) editHandle.trim() else "@${editHandle.trim()}",
                        tagline = editTagline.trim(),
                        shortDescription = editDescription.trim(),
                        brandStory = editStory.trim(),
                        targetAudience = editAudience.trim(),
                        updatedAt = System.currentTimeMillis()
                    )
                    onSave(updated)
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SnapIndigoPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_edited_brand_button")
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Changes", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}
