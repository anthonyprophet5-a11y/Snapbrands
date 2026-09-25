package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentStatus
import com.example.store.checkout.Money
import com.example.ui.components.EmptyStateView
import com.example.ui.components.StatusBadge
import com.example.ui.theme.SnapAmberAccent
import com.example.ui.theme.SnapAmberWarning
import com.example.ui.theme.SnapBorderLight
import com.example.ui.theme.SnapEmeraldSuccess
import com.example.ui.theme.SnapIndigoContainer
import com.example.ui.theme.SnapIndigoPrimary
import com.example.ui.theme.SnapTealSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun OrdersScreen(
    orders: List<Order>,
    onUpdateOrderStatus: ((orderId: String, newStatus: OrderStatus) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf<OrderStatus?>(null) }
    var selectedOrderForDetail by remember { mutableStateOf<Order?>(null) }
    var actionErrorMessage by remember { mutableStateOf<String?>(null) }
    val chipScrollState = rememberScrollState()

    val filteredOrders = remember(orders, selectedFilter) {
        if (selectedFilter == null) orders
        else orders.filter { it.orderStatus == selectedFilter || it.status == selectedFilter }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("orders_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 840.dp)
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Header
            Column {
                Text(
                    text = "Orders",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Track customer orders, payments, and fulfillment",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // State Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(chipScrollState),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == null,
                    onClick = { selectedFilter = null },
                    label = { Text("All States") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SnapIndigoContainer,
                        selectedLabelColor = SnapIndigoPrimary
                    )
                )

                listOf(
                    OrderStatus.PENDING_PAYMENT,
                    OrderStatus.PAID,
                    OrderStatus.PROCESSING,
                    OrderStatus.COMPLETED,
                    OrderStatus.CANCELLED
                ).forEach { status ->
                    FilterChip(
                        selected = selectedFilter == status,
                        onClick = { selectedFilter = if (selectedFilter == status) null else status },
                        label = { Text(status.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SnapIndigoContainer,
                            selectedLabelColor = SnapIndigoPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Orders list or Empty State
            if (filteredOrders.isEmpty()) {
                val emptyMessage = if (selectedFilter != null) {
                    "No ${selectedFilter?.label?.lowercase()} orders right now. When customer transactions occur, they will update here automatically."
                } else {
                    "No customer orders yet. When customers checkout products from your published stores, orders will appear here in real time."
                }

                EmptyStateView(
                    icon = Icons.Default.ShoppingBag,
                    title = "No orders yet.",
                    description = emptyMessage,
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredOrders, key = { it.id }) { order ->
                        OrderCard(
                            order = order,
                            onClick = { selectedOrderForDetail = order }
                        )
                    }
                }
            }
        }
    }

    // Order Detail Modal
    selectedOrderForDetail?.let { currentOrder ->
        // Keep order details fresh from orders list if status was updated
        val freshOrder = orders.find { it.id == currentOrder.id } ?: currentOrder

        OrderDetailDialog(
            order = freshOrder,
            onDismiss = { selectedOrderForDetail = null },
            onUpdateStatus = { newStatus ->
                try {
                    onUpdateOrderStatus?.invoke(freshOrder.id, newStatus)
                } catch (e: Exception) {
                    actionErrorMessage = e.message
                }
            }
        )
    }

    // Action Error Dialog
    actionErrorMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { actionErrorMessage = null },
            title = { Text("Order Update Note", fontWeight = FontWeight.Bold) },
            text = { Text(msg) },
            confirmButton = {
                Button(onClick = { actionErrorMessage = null }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun OrderCard(
    order: Order,
    onClick: () -> Unit
) {
    val dateStr = remember(order.createdAt) {
        SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()).format(Date(order.createdAt))
    }

    val (badgeBg, badgeText) = when (order.orderStatus) {
        OrderStatus.PENDING_PAYMENT, OrderStatus.PENDING -> Pair(SnapAmberWarning.copy(alpha = 0.15f), SnapAmberWarning)
        OrderStatus.PAID -> Pair(SnapEmeraldSuccess.copy(alpha = 0.15f), SnapEmeraldSuccess)
        OrderStatus.PROCESSING -> Pair(SnapIndigoContainer, SnapIndigoPrimary)
        OrderStatus.COMPLETED, OrderStatus.DELIVERED -> Pair(SnapEmeraldSuccess.copy(alpha = 0.2f), SnapEmeraldSuccess)
        OrderStatus.CANCELLED, OrderStatus.PAYMENT_FAILED -> Pair(Color.Gray.copy(alpha = 0.2f), Color.Gray)
        OrderStatus.SHIPPED -> Pair(SnapTealSecondary.copy(alpha = 0.15f), SnapTealSecondary)
        OrderStatus.REFUNDED -> Pair(SnapAmberAccent.copy(alpha = 0.15f), SnapAmberAccent)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            SnapBorderLight
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("order_card_${order.orderNumber}")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = null,
                        tint = SnapIndigoPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = order.orderNumber,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                StatusBadge(text = order.orderStatus.label, backgroundColor = badgeBg, textColor = badgeText)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Customer Name & Item Count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val customerDisplay = order.customerName.ifBlank { "Customer" }
                Text(
                    text = customerDisplay,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val itemCount = order.items.sumOf { it.quantity }
                Text(
                    text = "$itemCount ${if (itemCount == 1) "item" else "items"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = Money.format(order.total, order.currency),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                    color = SnapIndigoPrimary
                )

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (order.paymentStatus == PaymentStatus.PAID)
                        SnapEmeraldSuccess.copy(alpha = 0.15f)
                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = "Payment: ${order.paymentStatus.label}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (order.paymentStatus == PaymentStatus.PAID) SnapEmeraldSuccess else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = dateStr,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun OrderDetailDialog(
    order: Order,
    onDismiss: () -> Unit,
    onUpdateStatus: (OrderStatus) -> Unit
) {
    val dateStr = remember(order.createdAt) {
        SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()).format(Date(order.createdAt))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Order Details",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = order.orderNumber,
                        style = MaterialTheme.typography.labelMedium,
                        color = SnapIndigoPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SnapIndigoPrimary)
            ) {
                Text("Close")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Placed on $dateStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Payment and Fulfillment Status Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Payment", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(order.paymentStatus.label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            order.paymentReference?.let {
                                Text("Ref: ${it.take(12)}...", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Fulfillment", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(order.orderStatus.label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                // Customer Information
                Text("Customer & Delivery", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                if (order.customerName.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(order.customerName, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                if (order.customerEmail.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(order.customerEmail, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                if (order.customerPhone.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(order.customerPhone, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                if (order.deliveryAddress.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(order.deliveryAddress, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                if (!order.deliveryNotes.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Note: ${order.deliveryNotes}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                // Items list
                Text("Items (${order.items.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                order.items.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
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
                                Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(20.dp), tint = SnapIndigoPrimary)
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.titleSnapshot, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = "Qty: ${item.quantity} × ${Money.format(item.unitPrice, item.currency)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = Money.format(item.subtotal, item.currency),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(10.dp))

                // Totals
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Subtotal", style = MaterialTheme.typography.bodyMedium)
                    Text(Money.format(order.subtotal, order.currency), style = MaterialTheme.typography.bodyMedium)
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Delivery Fee", style = MaterialTheme.typography.bodyMedium)
                    if (order.deliveryFee > 0) {
                        Text(Money.format(order.deliveryFee, order.currency), style = MaterialTheme.typography.bodyMedium)
                    } else {
                        Text("Confirmed by seller", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(Money.format(order.total, order.currency), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = SnapIndigoPrimary)
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                // Operational Status Transitions
                Text("Update Order State", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                if (order.paymentStatus != PaymentStatus.PAID) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SnapAmberWarning.copy(alpha = 0.1f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = SnapAmberWarning, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Awaiting verified customer payment. Unpaid orders cannot be moved to Paid/Fulfillment manually.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    if (order.orderStatus != OrderStatus.CANCELLED) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { onUpdateStatus(OrderStatus.CANCELLED) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cancel Order")
                        }
                    }
                } else {
                    // Paid Order Operations: PAID -> PROCESSING -> COMPLETED or CANCEL
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (order.orderStatus == OrderStatus.PAID) {
                            Button(
                                onClick = { onUpdateStatus(OrderStatus.PROCESSING) },
                                colors = ButtonDefaults.buttonColors(containerColor = SnapIndigoPrimary)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Mark as Processing")
                            }
                        }

                        if (order.orderStatus == OrderStatus.PROCESSING) {
                            Button(
                                onClick = { onUpdateStatus(OrderStatus.COMPLETED) },
                                colors = ButtonDefaults.buttonColors(containerColor = SnapEmeraldSuccess)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Mark as Completed")
                            }
                        }

                        if (order.orderStatus != OrderStatus.CANCELLED && order.orderStatus != OrderStatus.COMPLETED) {
                            OutlinedButton(
                                onClick = { onUpdateStatus(OrderStatus.CANCELLED) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Cancel Order & Restore Stock")
                            }
                        }
                    }
                }
            }
        }
    )
}
