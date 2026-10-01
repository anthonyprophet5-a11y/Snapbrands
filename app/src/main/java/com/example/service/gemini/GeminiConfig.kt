package com.example.service.gemini

import com.example.BuildConfig

sealed class GeminiException(message: String, cause: Throwable? = null) : Exception(message, cause)

class GeminiConfigurationException(message: String = "AI configuration needs attention.") : GeminiException(message)
class GeminiUnavailableException(message: String = "Gemini is temporarily unavailable. Please try again.", cause: Throwable? = null) : GeminiException(message, cause)
class GeminiRateLimitException(message: String = "AI is temporarily busy. Please try again shortly.") : GeminiException(message)
class GeminiInvalidRequestException(message: String = "SnapBrand couldn't process this image.", cause: Throwable? = null) : GeminiException(message, cause)
class GeminiNetworkException(message: String = "Check your internet connection and try again.", cause: Throwable? = null) : GeminiException(message, cause)
class GeminiParseException(message: String = "SnapBrand couldn't process this image.", cause: Throwable? = null) : GeminiException(message, cause)

object GeminiConfig {
    const val MODEL = "gemini-3.5-flash"
    val FALLBACK_MODELS = listOf(
        "gemini-3.5-flash",
        "gemini-flash-latest",
        "gemini-2.5-flash",
        "gemini-3.1-pro-preview",
        "gemini-3.8-flash"
    )
    const val API_ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent"
    const val BACKEND_API_URL = "http://10.0.2.2:3000/api/generate-store"

    fun getEndpointForModel(model: String): String {
        return "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent"
    }

    fun getApiKey(): String {
        return try {
            BuildConfig.GEMINI_API_KEY.trim()
        } catch (e: Throwable) {
            ""
        }
    }

    fun isConfigured(): Boolean {
        val key = getApiKey()
        return key.isNotEmpty() &&
                key != "MY_GEMINI_API_KEY" &&
                !key.contains("PLACEHOLDER", ignoreCase = true) &&
                key.length > 8
    }
}
