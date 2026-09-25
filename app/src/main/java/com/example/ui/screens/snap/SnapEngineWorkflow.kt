package com.example.ui.screens.snap

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.model.BrandConcept
import com.example.data.model.Product
import com.example.data.model.Shop
import com.example.data.model.SnapAnalysis
import com.example.data.model.SuggestedProduct
import com.example.service.SnapBrandEvent
import com.example.service.gemini.GeminiConfig
import com.example.ui.screens.brand.BrandGeniusView
import com.example.ui.screens.product.ProductStudioView
import com.example.ui.theme.SnapCyanAI
import com.example.ui.theme.SnapEmeraldSuccess
import com.example.ui.theme.SnapLilacContainer
import com.example.ui.theme.SnapLilacOnContainer
import com.example.ui.theme.SnapAmberAccent
import com.example.ui.theme.SnapVioletContainer
import com.example.ui.theme.SnapVioletOnContainer
import com.example.ui.theme.SnapIndigoPrimary
import com.example.util.ImageUtils
import com.example.util.ProcessedImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class SnapStage {
    CAMERA,
    PERMISSION_DENIED,
    PHOTO_PREVIEW,
    ANALYZING,
    ERROR,
    RESULTS,
    BRAND_GENIUS,
    PRODUCT_STUDIO,
    SHOP_READY
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnapEngineWorkflowSheet(
    sheetState: SheetState,
    initialMode: String, // "camera" or "upload"
    onDismiss: () -> Unit,
    onAnalyzeImage: suspend (photoUri: String, base64: String, mimeType: String) -> Result<SnapAnalysis>,
    onNavigateToShops: () -> Unit,
    onLogAnalytics: (SnapBrandEvent, Map<String, Any>) -> Unit,
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
    onOpenStoreEditor: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var stage by remember {
        mutableStateOf(if (initialMode == "camera") SnapStage.CAMERA else SnapStage.PHOTO_PREVIEW)
    }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var processedImage by remember { mutableStateOf<ProcessedImage?>(null) }
    var sourceMode by remember { mutableStateOf(initialMode) } // "camera" or "upload"

    var currentAnalysis by remember { mutableStateOf<SnapAnalysis?>(null) }
    var selectedBusinessMode by remember { mutableStateOf<String>("MERCH") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showPhase2Modal by remember { mutableStateOf(false) }
    var createdDraftShop by remember { mutableStateOf<Shop?>(null) }

    // Analytics logging upon mount
    LaunchedEffect(Unit) {
        onLogAnalytics(SnapBrandEvent.SNAP_STARTED, mapOf("initialMode" to initialMode))
    }

    // Photo Picker launcher (Android zero-permission photo picker)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            sourceMode = "upload"
            onLogAnalytics(SnapBrandEvent.PHOTO_UPLOADED, emptyMap())

            // Process & validate selected image
            try {
                val processed = ImageUtils.processImageUri(context, uri)
                processedImage = processed
                selectedBitmap = processed.bitmap
                stage = SnapStage.PHOTO_PREVIEW
                errorMessage = null
            } catch (e: Exception) {
                Log.e("SnapEngine", "Failed to process selected image", e)
                errorMessage = e.message ?: "Failed to validate image"
                stage = SnapStage.ERROR
            }
        }
    }

    // Fallback legacy content picker if PickVisualMedia is unavailable
    val legacyPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            sourceMode = "upload"
            onLogAnalytics(SnapBrandEvent.PHOTO_UPLOADED, emptyMap())
            try {
                val processed = ImageUtils.processImageUri(context, uri)
                processedImage = processed
                selectedBitmap = processed.bitmap
                stage = SnapStage.PHOTO_PREVIEW
                errorMessage = null
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to validate image"
                stage = SnapStage.ERROR
            }
        }
    }

    fun launchPhotoPicker() {
        try {
            photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        } catch (e: Exception) {
            legacyPickerLauncher.launch("image/*")
        }
    }

    // Camera permission request launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            onLogAnalytics(SnapBrandEvent.CAMERA_OPENED, emptyMap())
            stage = SnapStage.CAMERA
        } else {
            stage = SnapStage.PERMISSION_DENIED
        }
    }

    // If initial mode is camera, verify camera permission
    LaunchedEffect(initialMode) {
        if (initialMode == "camera") {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED

            if (hasPermission) {
                onLogAnalytics(SnapBrandEvent.CAMERA_OPENED, emptyMap())
                stage = SnapStage.CAMERA
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        } else {
            launchPhotoPicker()
        }
    }

    // Function to run analysis
    fun triggerAnalysis() {
        val currentProcessed = processedImage
        if (currentProcessed == null) {
            errorMessage = "No image available for analysis. Please choose a photo."
            stage = SnapStage.ERROR
            return
        }

        stage = SnapStage.ANALYZING
        errorMessage = null

        coroutineScope.launch {
            val photoRef = selectedImageUri?.toString() ?: "captured_photo_${System.currentTimeMillis()}"
            val result = onAnalyzeImage(
                photoRef,
                currentProcessed.base64Data,
                currentProcessed.mimeType
            )

            if (result.isSuccess) {
                val analysis = result.getOrThrow()
                currentAnalysis = analysis
                selectedBusinessMode = analysis.recommendedBusinessMode
                stage = SnapStage.RESULTS
            } else {
                val ex = result.exceptionOrNull()
                Log.e("SnapEngine", "Analysis failed", ex)
                errorMessage = ex?.message ?: "Commerce analysis failed. Please try again."
                stage = SnapStage.ERROR
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            when (stage) {
                SnapStage.CAMERA -> {
                    CameraPreviewView(
                        onPhotoCaptured = { uri, bitmap ->
                            selectedImageUri = uri
                            selectedBitmap = bitmap
                            sourceMode = "camera"
                            onLogAnalytics(SnapBrandEvent.PHOTO_CAPTURED, emptyMap())

                            try {
                                val processed = ImageUtils.processBitmap(bitmap)
                                processedImage = processed
                                stage = SnapStage.PHOTO_PREVIEW
                            } catch (e: Exception) {
                                errorMessage = "Failed to process captured photo: ${e.message}"
                                stage = SnapStage.ERROR
                            }
                        },
                        onSwitchToUpload = {
                            sourceMode = "upload"
                            launchPhotoPicker()
                        },
                        onClose = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(520.dp)
                    )
                }

                SnapStage.PERMISSION_DENIED -> {
                    PermissionDeniedView(
                        onRequestPermissionAgain = {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        },
                        onChooseUpload = {
                            sourceMode = "upload"
                            launchPhotoPicker()
                        },
                        onDismiss = onDismiss
                    )
                }

                SnapStage.PHOTO_PREVIEW -> {
                    PhotoPreviewView(
                        bitmap = selectedBitmap,
                        uri = selectedImageUri,
                        sourceMode = sourceMode,
                        onUsePhoto = { triggerAnalysis() },
                        onRetakeOrChooseAnother = {
                            if (sourceMode == "camera") {
                                stage = SnapStage.CAMERA
                            } else {
                                launchPhotoPicker()
                            }
                        },
                        onDismiss = onDismiss
                    )
                }

                SnapStage.ANALYZING -> {
                    AnalyzingProgressView(
                        bitmap = selectedBitmap,
                        uri = selectedImageUri
                    )
                }

                SnapStage.ERROR -> {
                    AnalysisErrorView(
                        errorMessage = errorMessage ?: "Unknown error occurred",
                        onRetry = { triggerAnalysis() },
                        onChooseAnother = {
                            if (sourceMode == "camera") {
                                stage = SnapStage.CAMERA
                            } else {
                                launchPhotoPicker()
                            }
                        },
                        onDismiss = onDismiss
                    )
                }

                SnapStage.RESULTS -> {
                    currentAnalysis?.let { analysis ->
                        SnapResultsView(
                            analysis = analysis,
                            bitmap = selectedBitmap,
                            selectedMode = selectedBusinessMode,
                            onModeChanged = { selectedBusinessMode = it },
                            onCreateShop = {
                                onLogAnalytics(SnapBrandEvent.CREATE_SHOP_CLICKED, mapOf("analysisId" to analysis.id))
                                stage = SnapStage.BRAND_GENIUS
                                val updatedAnalysis = analysis.copy(recommendedBusinessMode = selectedBusinessMode)
                                coroutineScope.launch {
                                    onGenerateBrand(updatedAnalysis, null)
                                }
                            },
                            onStartOver = {
                                onLogAnalytics(SnapBrandEvent.SNAP_RESTARTED, emptyMap())
                                selectedBitmap = null
                                selectedImageUri = null
                                processedImage = null
                                currentAnalysis = null
                                stage = if (sourceMode == "camera") SnapStage.CAMERA else SnapStage.PHOTO_PREVIEW
                                if (sourceMode == "upload") {
                                    launchPhotoPicker()
                                }
                            },
                            onDismiss = onDismiss
                        )
                    }
                }

                SnapStage.BRAND_GENIUS -> {
                    currentAnalysis?.let { analysis ->
                        val effectiveAnalysis = analysis.copy(recommendedBusinessMode = selectedBusinessMode)
                        BrandGeniusView(
                            analysis = effectiveAnalysis,
                            photoBitmap = selectedBitmap,
                            photoUri = selectedImageUri,
                            brandConcept = brandConcept,
                            isGenerating = isGeneratingBrand,
                            isRegeneratingField = isRegeneratingField,
                            errorMessage = brandError,
                            onGenerateBrand = { directive ->
                                coroutineScope.launch {
                                    onGenerateBrand(effectiveAnalysis, directive)
                                }
                            },
                            onRegenerateName = { directive ->
                                coroutineScope.launch {
                                    onRegenerateName(directive)
                                }
                            },
                            onRegenerateTagline = { directive ->
                                coroutineScope.launch {
                                    onRegenerateTagline(directive)
                                }
                            },
                            onRegenerateStory = { directive ->
                                coroutineScope.launch {
                                    onRegenerateStory(directive)
                                }
                            },
                            onUpdateBrandConcept = onUpdateBrandConcept,
                            onAcceptBrand = {
                                val result = onAcceptBrand()
                                if (result.isSuccess) {
                                    val shop = result.getOrThrow()
                                    createdDraftShop = shop
                                    onGenerateProducts(shop.id, null, false)
                                    stage = SnapStage.PRODUCT_STUDIO
                                }
                            },
                            onBackToAnalysis = {
                                stage = SnapStage.RESULTS
                            },
                            onDismiss = onDismiss,
                            onLogAnalytics = onLogAnalytics
                        )
                    }
                }
                SnapStage.PRODUCT_STUDIO -> {
                    val analysis = currentAnalysis
                    if (analysis != null) {
                        val effectiveAnalysis = analysis.copy(recommendedBusinessMode = selectedBusinessMode)
                        val shopId = createdDraftShop?.id ?: ("shop_" + analysis.id)
                        ProductStudioView(
                            shopId = shopId,
                            ownerUid = analysis.ownerUid,
                            sourceAnalysis = effectiveAnalysis,
                            brandConcept = brandConcept,
                            businessMode = selectedBusinessMode,
                            products = products,
                            isLoading = isGeneratingProducts,
                            regeneratingProductId = isRegeneratingProduct,
                            errorMessage = productError,
                            onSaveProduct = onSaveProduct,
                            onRemoveProduct = onRemoveProduct,
                            onRegenerateProduct = onRegenerateSingleProduct,
                            onRegenerateAllProducts = { dir -> onGenerateProducts(shopId, dir, true) },
                            onAcceptAndFinish = {
                                onSaveAllProducts(shopId)
                                stage = SnapStage.SHOP_READY
                            },
                            onBack = {
                                stage = SnapStage.BRAND_GENIUS
                            }
                        )
                    }
                }
                SnapStage.SHOP_READY -> {
                    ShopReadyView(
                        shop = createdDraftShop,
                        productsCount = products.size,
                        onGoToShops = {
                            onDismiss()
                            onNavigateToShops()
                        },
                        onOpenStoreEditor = { shopId ->
                            onDismiss()
                            onOpenStoreEditor?.invoke(shopId)
                        },
                        onDismiss = onDismiss
                    )
                }
            }
        }
    }

    if (showPhase2Modal) {
        Phase2BoundaryDialog(
            subject = currentAnalysis?.detectedSubject ?: "Snap Commerce Idea",
            businessMode = selectedBusinessMode,
            onDismiss = { showPhase2Modal = false },
            onGoToShops = {
                showPhase2Modal = false
                onDismiss()
                onNavigateToShops()
            }
        )
    }
}

// ==========================================
// 0. SHOP READY VIEW (Phase 3 Completion)
// ==========================================
@Composable
fun ShopReadyView(
    shop: Shop?,
    productsCount: Int,
    onGoToShops: () -> Unit,
    onOpenStoreEditor: ((String) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(SnapEmeraldSuccess.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = SnapEmeraldSuccess,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "DRAFT SHOP CREATED!",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = SnapEmeraldSuccess,
            letterSpacing = 0.5.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Your product listings and brand concept have been saved to your draft storefront.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = shop?.name ?: "Draft Store",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SnapIndigoPrimary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = if (shop?.businessMode?.name == "MERCH") "MERCH SHOP" else "REAL SHOP",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = SnapIndigoPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                if (!shop?.handle.isNullOrBlank()) {
                    Text(
                        text = shop?.handle ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "STATUS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Draft (Not Live)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "PRODUCTS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$productsCount items listed",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = SnapEmeraldSuccess
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Inventory2,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Live Printify production fulfillment and Paystack payment checkout will unlock in future platform phases.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (shop != null && onOpenStoreEditor != null) {
            Button(
                onClick = { onOpenStoreEditor(shop.id) },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SnapIndigoPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("open_store_editor_button")
            ) {
                Text("Customize & Publish Storefront →", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onGoToShops,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("View in My Shops")
            }
        } else {
            Button(
                onClick = onGoToShops,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SnapIndigoPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("view_draft_shop_button")
            ) {
                Text("View in My Shops →", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        TextButton(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Done")
        }
    }
}

// ==========================================
// 1. PERMISSION DENIED VIEW
// ==========================================
@Composable
fun PermissionDeniedView(
    onRequestPermissionAgain: () -> Unit,
    onChooseUpload: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(SnapLilacContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                tint = SnapLilacOnContainer,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Camera Permission Required",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "SnapBrand needs camera access so you can take real-time photos of items, drawings, and pets to build storefronts.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onRequestPermissionAgain,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("request_camera_permission_button")
        ) {
            Text("Grant Camera Permission", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = onChooseUpload,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("upload_instead_from_denied_button")
        ) {
            Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Upload a photo instead", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onDismiss,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cancel")
        }
    }
}

// ==========================================
// 2. PHOTO PREVIEW VIEW ("Is this what you want to build from?")
// ==========================================
@Composable
fun PhotoPreviewView(
    bitmap: Bitmap?,
    uri: Uri?,
    sourceMode: String,
    onUsePhoto: () -> Unit,
    onRetakeOrChooseAnother: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = SnapLilacContainer
            ) {
                Text(
                    text = "CONFIRM PHOTO",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.1.sp
                    ),
                    color = SnapLilacOnContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Is this what you want to build from?",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Gemini Vision will analyze colors, subject matter, product ideas, and brand opportunities.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Large High-Quality Photo Container
        Box(
            modifier = Modifier
                .size(240.dp)
                .clip(RoundedCornerShape(20.dp))
                .border(2.dp, SnapIndigoPrimary, RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Selected Photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else if (uri != null) {
                AsyncImage(
                    model = uri,
                    contentDescription = "Selected Photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action Buttons
        Button(
            onClick = onUsePhoto,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SnapIndigoPrimary,
                contentColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .testTag("use_this_photo_button")
        ) {
            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Use this photo",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = onRetakeOrChooseAnother,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("retake_photo_button")
        ) {
            Text(
                text = if (sourceMode == "camera") "Retake photo" else "Choose another photo",
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ==========================================
// 3. TRUTHFUL PROCESSING VIEW
// ==========================================
@Composable
fun AnalyzingProgressView(
    bitmap: Bitmap?,
    uri: Uri?
) {
    val stages = remember {
        listOf(
            "Analyzing your photo...",
            "Understanding the object...",
            "Finding product opportunities...",
            "Preparing your business idea..."
        )
    }

    var stageIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1500)
            stageIndex = (stageIndex + 1) % stages.size
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(2.dp, SnapIndigoPrimary.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else if (uri != null) {
                AsyncImage(
                    model = uri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        CircularProgressIndicator(
            color = SnapIndigoPrimary,
            strokeWidth = 3.5.dp,
            modifier = Modifier.size(44.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        AnimatedContent(targetState = stages[stageIndex], label = "processing_stage") { text ->
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Google Gemini 3.5 Flash Multimodal Vision is processing image features and commercial viability.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))
    }
}

// ==========================================
// 4. ERROR VIEW
// ==========================================
@Composable
fun AnalysisErrorView(
    errorMessage: String,
    onRetry: () -> Unit,
    onChooseAnother: () -> Unit,
    onDismiss: () -> Unit
) {
    val isConfigMissing = !GeminiConfig.isConfigured() ||
            errorMessage.contains("API key", ignoreCase = true) ||
            errorMessage.contains("configuration", ignoreCase = true)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(if (isConfigMissing) SnapAmberAccent.copy(alpha = 0.2f) else MaterialTheme.colorScheme.errorContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isConfigMissing) Icons.Default.Lock else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isConfigMissing) SnapAmberAccent else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (isConfigMissing) "AI Configuration Required" else "Photo Analysis Issue",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isConfigMissing) {
                "Real AI processing requires GEMINI_API_KEY. Add your API key in the AI Studio Secrets panel to enable multimodal vision analysis without mock data."
            } else {
                errorMessage
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SnapIndigoPrimary),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("retry_analysis_button")
        ) {
            Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Try again", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = onChooseAnother,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("choose_another_photo_error_button")
        ) {
            Text("Choose another photo", fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onDismiss,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ==========================================
// 5. RESULTS VIEW
// ==========================================
@Composable
fun SnapResultsView(
    analysis: SnapAnalysis,
    bitmap: Bitmap?,
    selectedMode: String,
    onModeChanged: (String) -> Unit,
    onCreateShop: () -> Unit,
    onStartOver: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = SnapVioletContainer
            ) {
                Text(
                    text = "YOUR SNAP",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.1.sp
                    ),
                    color = SnapVioletOnContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Top Hero Card: Original Photo + AI Subject Identification
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.5.dp, SnapIndigoPrimary, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Analyzed Photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        AsyncImage(
                            model = analysis.photoUri,
                            contentDescription = "Analyzed Photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "AI SEES",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = analysis.detectedSubject,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = analysis.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Metadata Pills Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = SnapLilacContainer,
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text(
                        text = "CATEGORY",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = SnapLilacOnContainer
                    )
                    Text(
                        text = analysis.category,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text(
                        text = "POTENTIAL BUSINESS",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = if (selectedMode == "MERCH") "Custom Merchandise" else "Physical Products",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // RECOMMENDED MODE SELECTOR (MERCH vs REAL SHOP)
        Text(
            text = "RECOMMENDED BUSINESS MODE",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Merch Mode Button
            Surface(
                onClick = { onModeChanged("MERCH") },
                shape = RoundedCornerShape(14.dp),
                color = if (selectedMode == "MERCH") SnapVioletContainer else MaterialTheme.colorScheme.surfaceVariant,
                border = if (selectedMode == "MERCH") BorderStroke(2.dp, SnapIndigoPrimary) else null,
                modifier = Modifier
                    .weight(1f)
                    .testTag("mode_merch_option")
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = if (selectedMode == "MERCH") SnapIndigoPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Merch Shop",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (analysis.recommendedBusinessMode == "MERCH") {
                            Text(
                                text = "Recommended",
                                style = MaterialTheme.typography.labelSmall,
                                color = SnapIndigoPrimary
                            )
                        }
                    }
                }
            }

            // Real Shop Mode Button
            Surface(
                onClick = { onModeChanged("REAL_SHOP") },
                shape = RoundedCornerShape(14.dp),
                color = if (selectedMode == "REAL_SHOP") SnapVioletContainer else MaterialTheme.colorScheme.surfaceVariant,
                border = if (selectedMode == "REAL_SHOP") BorderStroke(2.dp, SnapIndigoPrimary) else null,
                modifier = Modifier
                    .weight(1f)
                    .testTag("mode_real_shop_option")
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Storefront,
                        contentDescription = null,
                        tint = if (selectedMode == "REAL_SHOP") SnapIndigoPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Real Shop",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (analysis.recommendedBusinessMode == "REAL_SHOP") {
                            Text(
                                text = "Recommended",
                                style = MaterialTheme.typography.labelSmall,
                                color = SnapIndigoPrimary
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // PRODUCT IDEAS SECTION
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "PRODUCT IDEAS",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "AI recommendations only",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        analysis.suggestedProducts.forEach { product ->
            ProductIdeaCard(product = product)
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (analysis.viralAngles.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "VIRAL MARKETING ANGLES",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            analysis.viralAngles.forEach { angle ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                    Icon(Icons.Default.FlashOn, contentDescription = null, tint = SnapAmberAccent, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${angle.title}: ${angle.hook}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        if (selectedMode == "MERCH" && analysis.printifyMerchOptions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                border = BorderStroke(1.dp, SnapIndigoPrimary.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Print, contentDescription = null, tint = SnapIndigoPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "PRINTIFY BLUEPRINT COMPATIBILITY",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = SnapIndigoPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    analysis.printifyMerchOptions.forEach { opt ->
                        Text("• $opt", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // BUSINESS OPPORTUNITY & AUDIENCE
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "TARGET CUSTOMER",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = analysis.targetAudience,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                )

                Text(
                    text = "BRAND OPPORTUNITY",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = analysis.brandOpportunities,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ESTIMATED CATALOG RANGE",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "AI estimate",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = analysis.suggestedPriceRange,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = SnapEmeraldSuccess,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // RIGHTS PROTECTION WARNING
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (analysis.rightsWarning != null) {
                    SnapAmberAccent.copy(alpha = 0.12f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                }
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = if (analysis.rightsWarning != null) Icons.Default.Warning else Icons.Default.Security,
                    contentDescription = null,
                    tint = if (analysis.rightsWarning != null) SnapAmberAccent else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (analysis.rightsWarning != null) "Commercial Rights Notice" else "Intellectual Property Notice",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = analysis.rightsWarning
                            ?: "Make sure you own this image or have permission to use it commercially. This is a warning, not legal advice.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // PRIMARY ACTION: CREATE MY SHOP →
        Button(
            onClick = onCreateShop,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SnapIndigoPrimary,
                contentColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .testTag("create_my_shop_button")
        ) {
            Text(
                text = "CREATE MY SHOP",
                fontWeight = FontWeight.Black,
                fontSize = 17.sp,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // SECONDARY ACTION: START OVER
        OutlinedButton(
            onClick = onStartOver,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("snap_start_over_button")
        ) {
            Text(
                text = "Start over",
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun ProductIdeaCard(product: SuggestedProduct) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SnapLilacContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (product.iconType) {
                        "tshirt" -> "👕"
                        "mug" -> "☕"
                        "poster" -> "🖼️"
                        "bag" -> "👜"
                        "electronics" -> "💻"
                        else -> "📦"
                    },
                    fontSize = 20.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = product.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = product.estimatedPriceRange,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = SnapEmeraldSuccess
                )
                Text(
                    text = "AI estimate",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ==========================================
// 6. PHASE 2 BOUNDARY DIALOG
// ==========================================
@Composable
fun Phase2BoundaryDialog(
    subject: String,
    businessMode: String,
    onDismiss: () -> Unit,
    onGoToShops: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SnapLilacContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = SnapIndigoPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Phase 2: Brand Engine",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "Your snap analysis for \"$subject\" is complete!",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "In Phase 2, the Brand Engine will automatically generate store names, brand taglines, logo concepts, and product blueprints directly from this analysis.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = SnapIndigoPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Selected Mode: ${if (businessMode == "MERCH") "Merchandise Shop" else "Physical Shop"}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onGoToShops,
                colors = ButtonDefaults.buttonColors(containerColor = SnapIndigoPrimary)
            ) {
                Text("View Shops Dashboard")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Keep Exploring")
            }
        }
    )
}
