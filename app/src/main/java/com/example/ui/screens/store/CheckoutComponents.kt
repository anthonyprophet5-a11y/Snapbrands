package com.example.ui.screens.store

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.CartItem
import com.example.data.model.CustomerDeliveryInfo
import com.example.data.model.Order
import com.example.data.model.PaymentStatus
import com.example.store.PublicStorefront
import com.example.store.StorefrontThemeTokens
import com.example.store.checkout.CartState
import com.example.store.checkout.CheckoutValidator
import com.example.store.checkout.Money
import com.example.store.payment.PaymentInitResponse
import com.example.ui.theme.SnapEmeraldSuccess
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ==========================================
// 1. CART DRAWER / DIALOG
// ==========================================
@Composable
fun CartDrawerDialog(
    cart: CartState,
    storefront: PublicStorefront,
    tokens: StorefrontThemeTokens,
    onDismiss: () -> Unit,
    onUpdateQuantity: (cartItemId: String, newQuantity: Int) -> Unit,
    onRemoveItem: (cartItemId: String) -> Unit,
    onProceedToCheckout: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("cart_drawer_dialog"),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        tint = tokens.primaryColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Your Cart (${cart.totalItemCount})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = tokens.onSurfaceColor
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = tokens.onSurfaceVariantColor)
                }
            }
        },
        confirmButton = {
            if (cart.items.isNotEmpty()) {
                Button(
                    onClick = onProceedToCheckout,
                    shape = tokens.buttonShape,
                    colors = ButtonDefaults.buttonColors(containerColor = tokens.primaryColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("proceed_to_checkout_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Checkout • ${Money.format(cart.total, cart.currency)}",
                            fontWeight = FontWeight.Bold,
                            color = tokens.onPrimaryColor
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = tokens.onPrimaryColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            } else {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = tokens.buttonShape,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Continue Browsing")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
            ) {
                if (cart.items.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.ShoppingBag,
                                contentDescription = null,
                                tint = tokens.onSurfaceVariantColor,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Your cart is empty",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = tokens.onSurfaceColor
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Explore products in the catalog to add them to your shopping bag.",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = tokens.onSurfaceVariantColor
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(cart.items, key = { it.id }) { item ->
                            CartItemRow(
                                item = item,
                                tokens = tokens,
                                onUpdateQuantity = { qty -> onUpdateQuantity(item.id, qty) },
                                onRemove = { onRemoveItem(item.id) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = tokens.borderColor)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Summary Block
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Subtotal", style = MaterialTheme.typography.bodyMedium, color = tokens.onSurfaceVariantColor)
                            Text(Money.format(cart.subtotal, cart.currency), fontWeight = FontWeight.SemiBold, color = tokens.onSurfaceColor)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Delivery", style = MaterialTheme.typography.bodyMedium, color = tokens.onSurfaceVariantColor)
                            if (cart.deliveryFee > 0) {
                                Text(Money.format(cart.deliveryFee, cart.currency), fontWeight = FontWeight.SemiBold, color = tokens.onSurfaceColor)
                            } else {
                                Text("Confirmed by seller", style = MaterialTheme.typography.labelSmall, color = tokens.onSurfaceVariantColor)
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Estimated Total", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = tokens.onSurfaceColor)
                            Text(
                                text = Money.format(cart.total, cart.currency),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = tokens.primaryColor
                            )
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun CartItemRow(
    item: CartItem,
    tokens: StorefrontThemeTokens,
    onUpdateQuantity: (Int) -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        shape = tokens.cardShape,
        color = tokens.surfaceColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, tokens.borderColor.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Product image
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(tokens.cardShape)
                    .background(tokens.backgroundColor),
                contentAlignment = Alignment.Center
            ) {
                if (!item.imageSnapshot.isNullOrBlank()) {
                    AsyncImage(
                        model = item.imageSnapshot,
                        contentDescription = item.titleSnapshot,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.ShoppingBag,
                        contentDescription = null,
                        tint = tokens.primaryColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.titleSnapshot,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = tokens.onSurfaceColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (!item.variantId.isNullOrBlank()) {
                    Text(
                        text = "Variant: ${item.variantId}",
                        style = MaterialTheme.typography.labelSmall,
                        color = tokens.onSurfaceVariantColor
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = Money.format(item.unitPrice, item.currency),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = tokens.primaryColor
                )
            }

            // Quantity stepper & remove
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = tokens.backgroundColor,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .clickable {
                            if (item.quantity > 1) onUpdateQuantity(item.quantity - 1)
                            else onRemove()
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (item.quantity > 1) Icons.Default.Remove else Icons.Default.DeleteOutline,
                            contentDescription = "Decrease",
                            modifier = Modifier.size(14.dp),
                            tint = tokens.onSurfaceColor
                        )
                    }
                }

                Text(
                    text = "${item.quantity}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = tokens.onSurfaceColor,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Surface(
                    shape = CircleShape,
                    color = tokens.backgroundColor,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .clickable { onUpdateQuantity(item.quantity + 1) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Increase",
                            modifier = Modifier.size(14.dp),
                            tint = tokens.onSurfaceColor
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 2. CHECKOUT DIALOG (Customer details -> Payment -> Confirmation)
// ==========================================
@Composable
fun CustomerCheckoutDialog(
    storefront: PublicStorefront,
    cart: CartState,
    tokens: StorefrontThemeTokens,
    onDismiss: () -> Unit,
    onCreateOrder: (CustomerDeliveryInfo) -> Result<Order>,
    onInitializePayment: suspend (orderId: String) -> Result<PaymentInitResponse>,
    onVerifyPayment: suspend (orderId: String, reference: String) -> Result<Order>,
    onOrderPlacedSuccess: (Order) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    // Form inputs
    var customerName by remember { mutableStateOf("") }
    var customerEmail by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var deliveryAddress by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("Nigeria") }
    var deliveryNotes by remember { mutableStateOf("") }

    // Checkout Flow States: FORM -> REVIEW -> PAYMENT_GATEWAY -> VERIFYING -> SUCCESS
    var step by remember { mutableStateOf("FORM") }
    var activeOrder by remember { mutableStateOf<Order?>(null) }
    var paymentInitResponse by remember { mutableStateOf<PaymentInitResponse?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = {
            if (!isProcessing && step != "VERIFYING") onDismiss()
        },
        modifier = Modifier.testTag("checkout_modal"),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (step == "SUCCESS") Icons.Default.CheckCircle else Icons.Default.Payment,
                        contentDescription = null,
                        tint = if (step == "SUCCESS") SnapEmeraldSuccess else tokens.primaryColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (step) {
                            "FORM" -> "Delivery Details"
                            "REVIEW" -> "Order Review"
                            "PAYMENT_GATEWAY" -> "Pay with Paystack"
                            "VERIFYING" -> "Verifying Payment..."
                            "SUCCESS" -> "Order Confirmation"
                            else -> "Checkout"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = tokens.onSurfaceColor
                    )
                }

                if (step != "VERIFYING" && step != "SUCCESS") {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = tokens.onSurfaceVariantColor)
                    }
                }
            }
        },
        confirmButton = {
            when (step) {
                "FORM" -> {
                    Button(
                        onClick = {
                            errorMessage = null
                            val info = CustomerDeliveryInfo(
                                customerName = customerName.trim(),
                                customerEmail = customerEmail.trim(),
                                customerPhone = customerPhone.trim(),
                                deliveryAddress = deliveryAddress.trim(),
                                city = city.trim(),
                                country = country.trim(),
                                deliveryNotes = deliveryNotes.trim()
                            )
                            val validation = CheckoutValidator.validateDeliveryInfo(info)
                            if (!validation.isValid) {
                                errorMessage = validation.errors.firstOrNull() ?: "Please check input fields."
                                return@Button
                            }
                            // Create Order in pending payment state
                            val orderResult = onCreateOrder(info)
                            if (orderResult.isSuccess) {
                                activeOrder = orderResult.getOrThrow()
                                step = "REVIEW"
                            } else {
                                errorMessage = orderResult.exceptionOrNull()?.message ?: "Failed to initiate order."
                            }
                        },
                        shape = tokens.buttonShape,
                        colors = ButtonDefaults.buttonColors(containerColor = tokens.primaryColor),
                        modifier = Modifier.testTag("review_and_pay_button")
                    ) {
                        Text("Continue to Review", color = tokens.onPrimaryColor, fontWeight = FontWeight.Bold)
                    }
                }
                "REVIEW" -> {
                    Button(
                        onClick = {
                            val order = activeOrder ?: return@Button
                            isProcessing = true
                            errorMessage = null
                            coroutineScope.launch {
                                val initResult = onInitializePayment(order.id)
                                isProcessing = false
                                if (initResult.isSuccess) {
                                    paymentInitResponse = initResult.getOrThrow()
                                    step = "PAYMENT_GATEWAY"
                                } else {
                                    errorMessage = initResult.exceptionOrNull()?.message ?: "Could not initialize payment gateway."
                                }
                            }
                        },
                        enabled = !isProcessing,
                        shape = tokens.buttonShape,
                        colors = ButtonDefaults.buttonColors(containerColor = tokens.primaryColor),
                        modifier = Modifier.testTag("pay_with_paystack_button")
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = tokens.onPrimaryColor, strokeWidth = 2.dp)
                        } else {
                            Text("Pay with Paystack", color = tokens.onPrimaryColor, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                "PAYMENT_GATEWAY" -> {
                    // In real production this launches Paystack Web SDK or native popup;
                    // here we simulate user completing authorization in test environment
                    Button(
                        onClick = {
                            val order = activeOrder ?: return@Button
                            val payResponse = paymentInitResponse ?: return@Button
                            step = "VERIFYING"
                            isProcessing = true
                            errorMessage = null

                            coroutineScope.launch {
                                val verifyResult = onVerifyPayment(order.id, payResponse.reference)
                                isProcessing = false
                                if (verifyResult.isSuccess) {
                                    val confirmedOrder = verifyResult.getOrThrow()
                                    activeOrder = confirmedOrder
                                    step = "SUCCESS"
                                    onOrderPlacedSuccess(confirmedOrder)
                                } else {
                                    step = "REVIEW"
                                    errorMessage = verifyResult.exceptionOrNull()?.message ?: "Payment verification failed."
                                }
                            }
                        },
                        shape = tokens.buttonShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0BA4DB)) // Paystack Blue
                    ) {
                        Text("Simulate Authorization (Test Mode)", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                "SUCCESS" -> {
                    Button(
                        onClick = onDismiss,
                        shape = tokens.buttonShape,
                        colors = ButtonDefaults.buttonColors(containerColor = SnapEmeraldSuccess),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Continue Shopping", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        dismissButton = {
            if (step == "REVIEW") {
                OutlinedButton(
                    onClick = { step = "FORM" },
                    shape = tokens.buttonShape
                ) {
                    Text("Back")
                }
            } else if (step == "PAYMENT_GATEWAY") {
                OutlinedButton(
                    onClick = { step = "REVIEW" },
                    shape = tokens.buttonShape
                ) {
                    Text("Cancel")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                errorMessage?.let { msg ->
                    Surface(
                        shape = tokens.cardShape,
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                when (step) {
                    "FORM" -> {
                        Text(
                            text = "Provide delivery coordinates to order directly from ${storefront.name}.",
                            style = MaterialTheme.typography.bodySmall,
                            color = tokens.onSurfaceVariantColor
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = customerName,
                            onValueChange = { customerName = it },
                            label = { Text("Full Name *") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = tokens.primaryColor) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("checkout_name_input")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = customerEmail,
                            onValueChange = { customerEmail = it },
                            label = { Text("Email Address *") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = tokens.primaryColor) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("checkout_email_input")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = customerPhone,
                            onValueChange = { customerPhone = it },
                            label = { Text("Phone Number *") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = tokens.primaryColor) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("checkout_phone_input")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = deliveryAddress,
                            onValueChange = { deliveryAddress = it },
                            label = { Text("Street Address *") },
                            leadingIcon = { Icon(Icons.Default.LocalShipping, contentDescription = null, tint = tokens.primaryColor) },
                            singleLine = false,
                            maxLines = 2,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("checkout_address_input")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = city,
                                onValueChange = { city = it },
                                label = { Text("City *") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("checkout_city_input")
                            )

                            OutlinedTextField(
                                value = country,
                                onValueChange = { country = it },
                                label = { Text("Country *") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("checkout_country_input")
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = deliveryNotes,
                            onValueChange = { deliveryNotes = it },
                            label = { Text("Delivery Instructions (Optional)") },
                            singleLine = false,
                            maxLines = 2,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("checkout_notes_input")
                        )
                    }

                    "REVIEW" -> {
                        val order = activeOrder
                        if (order != null) {
                            Text("Order Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(10.dp))

                            order.items.forEach { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.titleSnapshot, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                        Text("${item.quantity} × ${Money.format(item.unitPrice, item.currency)}", style = MaterialTheme.typography.bodySmall, color = tokens.onSurfaceVariantColor)
                                    }
                                    Text(Money.format(item.subtotal, item.currency), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = tokens.borderColor)
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Subtotal", style = MaterialTheme.typography.bodyMedium)
                                Text(Money.format(order.subtotal, order.currency), style = MaterialTheme.typography.bodyMedium)
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Delivery Fee", style = MaterialTheme.typography.bodyMedium)
                                if (order.deliveryFee > 0) {
                                    Text(Money.format(order.deliveryFee, order.currency), style = MaterialTheme.typography.bodyMedium)
                                } else {
                                    Text("Confirmed by seller", style = MaterialTheme.typography.labelSmall, color = tokens.onSurfaceVariantColor)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    text = Money.format(order.total, order.currency),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = tokens.primaryColor
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Surface(
                                shape = tokens.cardShape,
                                color = tokens.surfaceColor,
                                border = androidx.compose.foundation.BorderStroke(1.dp, tokens.borderColor),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Delivery Destination", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(order.customerName, style = MaterialTheme.typography.bodySmall)
                                    Text(order.deliveryAddress, style = MaterialTheme.typography.bodySmall, color = tokens.onSurfaceVariantColor)
                                    Text(order.customerPhone, style = MaterialTheme.typography.bodySmall, color = tokens.onSurfaceVariantColor)
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = SnapEmeraldSuccess, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Secured 256-bit payment checkout", style = MaterialTheme.typography.labelSmall, color = tokens.onSurfaceVariantColor)
                            }
                        }
                    }

                    "PAYMENT_GATEWAY" -> {
                        val order = activeOrder
                        val payResponse = paymentInitResponse
                        if (order != null && payResponse != null) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF0BA4DB).copy(alpha = 0.15f),
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Payment,
                                            contentDescription = null,
                                            tint = Color(0xFF0BA4DB),
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "Paystack Checkout Gateway",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "Payment Reference: ${payResponse.reference}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = tokens.onSurfaceVariantColor
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Card(
                                    shape = tokens.cardShape,
                                    colors = CardDefaults.cardColors(containerColor = tokens.surfaceColor),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, tokens.borderColor),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Paying to", style = MaterialTheme.typography.bodySmall, color = tokens.onSurfaceVariantColor)
                                            Text(storefront.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Customer", style = MaterialTheme.typography.bodySmall, color = tokens.onSurfaceVariantColor)
                                            Text(order.customerEmail, style = MaterialTheme.typography.bodySmall)
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Amount Due", style = MaterialTheme.typography.bodySmall, color = tokens.onSurfaceVariantColor)
                                            Text(
                                                text = Money.format(order.total, order.currency),
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFF0BA4DB)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "Simulated secure sandbox gateway: Click below to authorize transaction.",
                                    style = MaterialTheme.typography.labelSmall,
                                    textAlign = TextAlign.Center,
                                    color = tokens.onSurfaceVariantColor
                                )
                            }
                        }
                    }

                    "VERIFYING" -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = tokens.primaryColor)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Verifying Transaction with Paystack...",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Deducting stock atomically and confirming order.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = tokens.onSurfaceVariantColor
                                )
                            }
                        }
                    }

                    "SUCCESS" -> {
                        val order = activeOrder
                        if (order != null) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("order_confirmation_screen"),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = SnapEmeraldSuccess.copy(alpha = 0.15f),
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = SnapEmeraldSuccess,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "Your order has been received.",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    textAlign = TextAlign.Center,
                                    color = tokens.onSurfaceColor
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "Order #${order.orderNumber}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = tokens.primaryColor
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Surface(
                                    shape = tokens.cardShape,
                                    color = tokens.surfaceColor,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, tokens.borderColor),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Payment Status", style = MaterialTheme.typography.bodySmall, color = tokens.onSurfaceVariantColor)
                                            Text(order.paymentStatus.label, fontWeight = FontWeight.Bold, color = SnapEmeraldSuccess)
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Total Paid", style = MaterialTheme.typography.bodySmall, color = tokens.onSurfaceVariantColor)
                                            Text(Money.format(order.total, order.currency), fontWeight = FontWeight.Black)
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Deliver to", style = MaterialTheme.typography.bodySmall, color = tokens.onSurfaceVariantColor)
                                            Text(order.customerName, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "The store owner has been notified and will process your order. We've recorded this transaction under #${order.orderNumber}.",
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center,
                                    color = tokens.onSurfaceVariantColor
                                )
                            }
                        }
                    }
                }
            }
        }
    )
}
