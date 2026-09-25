package com.example.data.model

data class Customer(
    val id: String,
    val shopId: String,
    val uid: String? = null,
    val email: String,
    val name: String,
    val totalOrders: Int = 0,
    val totalSpent: Double = 0.0,
    val currency: String = "USD",
    val lastOrderAt: Long = System.currentTimeMillis()
)

data class Subscription(
    val id: String,
    val uid: String,
    val plan: String, // "Free", "Starter", "Pro", "Enterprise"
    val status: String, // "active", "trial", "past_due", "cancelled"
    val amount: Double = 0.0,
    val currency: String = "USD",
    val currentPeriodStart: Long = System.currentTimeMillis(),
    val currentPeriodEnd: Long = System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000,
    val cancelAtPeriodEnd: Boolean = false
)

data class Transaction(
    val id: String,
    val orderId: String,
    val shopId: String,
    val sellerUid: String,
    val grossAmount: Double,
    val platformFee: Double,
    val netEarnings: Double,
    val currency: String = "USD",
    val status: String = "successful", // "pending", "successful", "failed", "refunded"
    val paymentProvider: String = "paystack",
    val reference: String,
    val createdAt: Long = System.currentTimeMillis()
)

data class MarketingCampaign(
    val id: String,
    val shopId: String,
    val ownerUid: String,
    val campaignName: String,
    val platform: String = "instagram", // "instagram", "tiktok", "x", "email"
    val status: String = "draft", // "draft", "scheduled", "active"
    val generatedCopy: String? = null,
    val mediaUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class SnapBattle(
    val id: String,
    val title: String,
    val prompt: String,
    val participantUids: List<String> = emptyList(),
    val status: String = "voting", // "voting", "completed"
    val winnerUid: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class NotificationItem(
    val id: String,
    val recipientUid: String,
    val title: String,
    val message: String,
    val type: String = "system", // "order", "shop", "system", "snap"
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
