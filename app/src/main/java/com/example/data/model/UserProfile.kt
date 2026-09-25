package com.example.data.model

data class UserProfile(
    val uid: String,
    val displayName: String,
    val email: String,
    val photoURL: String? = null,
    val username: String = "",
    val country: String = "US",
    val currency: String = "USD",
    val subscriptionPlan: String = "Free",
    val subscriptionStatus: String = "Active",
    val role: String = "user",
    val bio: String? = null,
    val notificationsEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val isAnonymous: Boolean
        get() = uid.startsWith("guest_")
}
