package com.example.data.model

import java.util.UUID

enum class OrderStatus(val label: String) {
    PENDING_PAYMENT("Pending Payment"),
    PAID("Paid"),
    PAYMENT_FAILED("Payment Failed"),
    PROCESSING("Processing"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled"),

    // Compatibility aliases for legacy references:
    PENDING("Pending"),
    SHIPPED("Shipped"),
    DELIVERED("Delivered"),
    REFUNDED("Refunded")
}

enum class PaymentStatus(val label: String) {
    UNPAID("Unpaid"),
    PENDING_VERIFICATION("Pending Verification"),
    PAID("Paid"),
    FAILED("Payment Failed"),
    CANCELLED("Cancelled")
}

data class CartItem(
    val id: String,
    val storeId: String,
    val productId: String,
    val variantId: String? = null,
    val titleSnapshot: String,
    val imageSnapshot: String? = null,
    val unitPrice: Long, // in minor units (e.g. 10000 for GH₵ 100.00)
    val currency: String = "USD",
    val quantity: Int
) {
    val subtotal: Long get() = unitPrice * quantity
}

data class OrderItem(
    val id: String = UUID.randomUUID().toString(),
    val storeId: String = "",
    val productId: String,
    val variantId: String? = null,
    val titleSnapshot: String,
    val imageSnapshot: String? = null,
    val unitPrice: Long, // in minor units
    val currency: String = "USD",
    val quantity: Int,
    val subtotal: Long = unitPrice * quantity
) {
    // Secondary constructor for existing code (Double-based unit price)
    constructor(
        productId: String,
        title: String,
        quantity: Int,
        unitPrice: Double,
        imageUrl: String? = null
    ) : this(
        productId = productId,
        titleSnapshot = title,
        quantity = quantity,
        unitPrice = Math.round(unitPrice * 100.0),
        imageSnapshot = imageUrl,
        variantId = null,
        currency = "USD"
    )

    val title: String get() = titleSnapshot
    val imageUrl: String? get() = imageSnapshot
    val unitPriceDouble: Double get() = unitPrice / 100.0
}

data class ShippingAddress(
    val recipientName: String,
    val street: String,
    val city: String,
    val state: String,
    val postalCode: String,
    val country: String
)

data class CustomerDeliveryInfo(
    val customerName: String,
    val customerEmail: String,
    val customerPhone: String,
    val country: String = "Ghana",
    val city: String = "",
    val deliveryAddress: String = "",
    val deliveryNotes: String? = null
)

data class Order(
    val id: String,
    val orderNumber: String,
    val storeId: String,
    val sellerUid: String,
    val customerId: String? = null,
    val customerName: String = "",
    val customerEmail: String = "",
    val customerPhone: String = "",
    val deliveryAddress: String = "",
    val deliveryNotes: String? = null,
    val items: List<OrderItem> = emptyList(),
    val subtotal: Long = 0L, // in minor units
    val deliveryFee: Long = 0L, // in minor units
    val total: Long = subtotal + deliveryFee, // in minor units
    val currency: String = "USD",
    val paymentProvider: String = "PAYSTACK",
    val paymentReference: String? = null,
    val paymentStatus: PaymentStatus = PaymentStatus.UNPAID,
    val orderStatus: OrderStatus = OrderStatus.PENDING_PAYMENT,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val paidAt: Long? = null,

    // Compatibility fields with previous phases:
    val shopId: String = storeId,
    val buyerUid: String = customerId ?: "",
    val status: OrderStatus = orderStatus,
    val totalAmount: Double = total / 100.0,
    val shippingAddress: ShippingAddress? = null,
    val trackingNumber: String? = null,
    val printifyOrderId: String? = null,
    val printifySyncStatus: String = "NOT_REQUIRED"
) {
    // Secondary constructor for existing seed data and test calls
    constructor(
        id: String,
        orderNumber: String,
        shopId: String,
        sellerUid: String,
        buyerUid: String,
        items: List<OrderItem> = emptyList(),
        totalAmount: Double,
        currency: String = "USD",
        status: OrderStatus = OrderStatus.PENDING,
        shippingAddress: ShippingAddress? = null,
        trackingNumber: String? = null,
        paymentReference: String? = null,
        createdAt: Long = System.currentTimeMillis(),
        updatedAt: Long = System.currentTimeMillis()
    ) : this(
        id = id,
        orderNumber = orderNumber,
        storeId = shopId,
        sellerUid = sellerUid,
        customerId = buyerUid,
        customerName = shippingAddress?.recipientName ?: "Customer",
        customerEmail = "buyer@example.com",
        customerPhone = "",
        deliveryAddress = listOfNotNull(shippingAddress?.street, shippingAddress?.city, shippingAddress?.country).joinToString(", "),
        deliveryNotes = null,
        items = items,
        subtotal = Math.round(totalAmount * 100.0),
        deliveryFee = 0L,
        total = Math.round(totalAmount * 100.0),
        currency = currency,
        paymentProvider = "PAYSTACK",
        paymentReference = paymentReference,
        paymentStatus = if (status == OrderStatus.PAID) PaymentStatus.PAID else PaymentStatus.UNPAID,
        orderStatus = if (status == OrderStatus.PENDING) OrderStatus.PENDING_PAYMENT else status,
        createdAt = createdAt,
        updatedAt = updatedAt,
        paidAt = if (status == OrderStatus.PAID) updatedAt else null,
        shippingAddress = shippingAddress,
        trackingNumber = trackingNumber
    )
}
