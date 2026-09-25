package com.example.service

/**
 * Admin Governance and Moderation Architecture.
 *
 * CRITICAL SECURITY PRINCIPLE:
 * Admin rights are governed by Firebase Custom Claims (e.g. `auth.token.admin == true`)
 * and enforced server-side in Firestore Security Rules and Cloud Functions.
 * Client UI checks are purely for convenience and never grant elevated Firestore access.
 */
interface AdminService {
    suspend fun getPlatformMetrics(): PlatformMetrics
    suspend fun reviewAbuseReports(): List<AbuseReport>
    suspend fun suspendShop(shopId: String, reason: String)
    suspend fun getAIUsageQuota(uid: String): AIUsageQuota
}

data class PlatformMetrics(
    val totalUsers: Long,
    val totalShops: Long,
    val totalOrders: Long,
    val totalGrossVolume: Double,
    val activeSubscriptions: Long,
    val aiTokensConsumed: Long
)

data class AbuseReport(
    val id: String,
    val targetType: String, // "shop", "product", "user"
    val targetId: String,
    val reportedByUid: String,
    val reason: String,
    val status: String, // "pending", "resolved", "dismissed"
    val timestamp: Long
)

data class AIUsageQuota(
    val uid: String,
    val snapsUsedThisMonth: Int,
    val snapsLimit: Int,
    val canGenerate: Boolean
)

class SnapBrandAdminService : AdminService {
    override suspend fun getPlatformMetrics(): PlatformMetrics {
        throw UnsupportedOperationException("Admin metrics require server-side custom claim authorization.")
    }

    override suspend fun reviewAbuseReports(): List<AbuseReport> {
        throw UnsupportedOperationException("Moderation queue is managed through the secure Admin Portal.")
    }

    override suspend fun suspendShop(shopId: String, reason: String) {
        throw UnsupportedOperationException("Shop suspension requires verified admin claims.")
    }

    override suspend fun getAIUsageQuota(uid: String): AIUsageQuota {
        throw UnsupportedOperationException("AI quota telemetry will connect to Cloud Functions in Phase 1.")
    }
}
