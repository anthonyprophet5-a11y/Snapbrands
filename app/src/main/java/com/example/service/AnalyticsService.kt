package com.example.service

import android.util.Log

enum class SnapBrandEvent(val eventName: String) {
    APP_OPEN("app_open"),
    SIGN_UP("sign_up"),
    LOGIN("login"),
    SNAP_STARTED("snap_started"),
    CAMERA_OPENED("camera_opened"),
    PHOTO_CAPTURED("photo_captured"),
    PHOTO_UPLOADED("photo_uploaded"),
    SNAP_ANALYSIS_STARTED("snap_analysis_started"),
    SNAP_ANALYSIS_COMPLETED("snap_analysis_completed"),
    SNAP_ANALYSIS_FAILED("snap_analysis_failed"),
    CREATE_SHOP_CLICKED("create_shop_clicked"),
    SNAP_RESTARTED("snap_restarted"),
    BRAND_GENERATION_STARTED("brand_generation_started"),
    BRAND_GENERATION_COMPLETED("brand_generation_completed"),
    BRAND_GENERATION_FAILED("brand_generation_failed"),
    BRAND_NAME_REGENERATED("brand_name_regenerated"),
    TAGLINE_REGENERATED("tagline_regenerated"),
    BRAND_STORY_REGENERATED("brand_story_regenerated"),
    BRAND_EDITED("brand_edited"),
    BRAND_ACCEPTED("brand_accepted"),
    CREATE_SHOP_FLOW_ABANDONED("create_shop_flow_abandoned"),
    SHOP_CREATED("shop_created"),
    PRODUCT_CREATED("product_created"),
    PRODUCT_GENERATION_STARTED("product_generation_started"),
    PRODUCT_GENERATION_COMPLETED("product_generation_completed"),
    PRODUCT_GENERATION_FAILED("product_generation_failed"),
    PRODUCT_ACCEPTED("product_accepted"),
    PRODUCT_EDITED("product_edited"),
    PRODUCT_REMOVED("product_removed"),
    PRODUCT_REGENERATED("product_regenerated"),
    PRODUCT_ENGINE_ABANDONED("product_engine_abandoned"),
    STORE_EDITOR_OPENED("store_editor_opened"),
    STORE_UPDATED("store_updated"),
    STORE_PREVIEW_OPENED("store_preview_opened"),
    PRODUCT_VISIBILITY_CHANGED("product_visibility_changed"),
    FEATURED_PRODUCT_CHANGED("featured_product_changed"),
    STORE_PUBLISH_STARTED("store_publish_started"),
    STORE_PUBLISHED("store_published"),
    STORE_UNPUBLISHED("store_unpublished"),
    HANDLE_CHECKED("handle_checked"),
    PRODUCT_VIEWED("product_viewed"),
    ADD_TO_CART("add_to_cart"),
    CART_VIEWED("cart_viewed"),
    CHECKOUT_STARTED("checkout_started"),
    CHECKOUT_DETAILS_SUBMITTED("checkout_details_submitted"),
    PAYMENT_INITIALIZED("payment_initialized"),
    PAYMENT_FAILED("payment_failed"),
    PAYMENT_VERIFICATION_PENDING("payment_verification_pending"),
    PAYMENT_VERIFIED("payment_verified"),
    ORDER_CREATED("order_created"),
    ORDER_VIEWED("order_viewed"),
    PURCHASE_COMPLETED("purchase_completed"),
    SUBSCRIPTION_STARTED("subscription_started"),
    MARKETING_GENERATED("marketing_generated"),
    SHOP_SHARED("shop_shared")
}

interface AnalyticsService {
    fun logEvent(event: SnapBrandEvent, params: Map<String, Any> = emptyMap())
    fun setUserId(uid: String?)
    fun setUserProperty(name: String, value: String)
}

class SnapBrandAnalyticsService : AnalyticsService {
    private val TAG = "SnapBrandAnalytics"

    override fun logEvent(event: SnapBrandEvent, params: Map<String, Any>) {
        // Logs event structured for Firebase Analytics / Google Analytics 4
        Log.d(TAG, "Event logged: ${event.eventName} with params: $params")
    }

    override fun setUserId(uid: String?) {
        Log.d(TAG, "User ID set: $uid")
    }

    override fun setUserProperty(name: String, value: String) {
        Log.d(TAG, "User property: $name = $value")
    }
}
