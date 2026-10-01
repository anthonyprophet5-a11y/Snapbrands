package com.example.service.gemini

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

interface GeminiClient {
    suspend fun generateContent(
        prompt: String,
        base64Image: String,
        mimeType: String = "image/jpeg"
    ): String

    suspend fun generateText(
        prompt: String,
        asJson: Boolean = true
    ): String = ""
}

class RealGeminiClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
) : GeminiClient {

    private val TAG = "SnapBrandGeminiClient"

    override suspend fun generateContent(
        prompt: String,
        base64Image: String,
        mimeType: String
    ): String = withContext(Dispatchers.IO) {
        if (!GeminiConfig.isConfigured()) {
            Log.e(TAG, "GEMINI_API_KEY is not configured or is a placeholder.")
            throw GeminiConfigurationException(
                "Gemini API key is not configured. Please add your GEMINI_API_KEY in the AI Studio Secrets panel."
            )
        }

        val payload = JSONObject().apply {
            val contentsArray = JSONArray().apply {
                val contentObj = JSONObject().apply {
                    val partsArray = JSONArray().apply {
                        // 1. Text Instruction Prompt
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                        // 2. Multimodal Inline Image Data
                        put(JSONObject().apply {
                            put("inlineData", JSONObject().apply {
                                put("mimeType", mimeType)
                                put("data", base64Image)
                            })
                        })
                    }
                    put("parts", partsArray)
                }
                put(contentObj)
            }
            put("contents", contentsArray)

            // Force JSON Response format
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.2)
            })
        }

        val models = GeminiConfig.FALLBACK_MODELS
        var lastException: Exception? = null

        for (model in models) {
            val endpoint = GeminiConfig.getEndpointForModel(model)
            val apiKey = GeminiConfig.getApiKey()
            val url = "$endpoint?key=$apiKey"

            try {
                Log.d(TAG, "Attempting Gemini request with model: $model")
                return@withContext executeGeminiRequest(url, payload, retriesLeft = 1)
            } catch (e: GeminiConfigurationException) {
                Log.e(TAG, "API key configuration issue for model $model: ${e.message}", e)
                lastException = e
                break // Don't retry other models if API key is invalid/unauthorized
            } catch (e: GeminiRateLimitException) {
                Log.w(TAG, "Model $model was rate-limited / quota-exhausted (429): ${e.message}. Trying fallback model...", e)
                lastException = e
            } catch (e: GeminiUnavailableException) {
                Log.w(TAG, "Model $model was unavailable (503/500). Trying fallback model...", e)
                lastException = e
            } catch (e: Exception) {
                Log.w(TAG, "Model $model request failed: ${e.message}. Trying fallback model...", e)
                lastException = e
            }
        }

        throw lastException ?: GeminiUnavailableException("Gemini is temporarily unavailable. Please try again.")
    }

    override suspend fun generateText(
        prompt: String,
        asJson: Boolean
    ): String = withContext(Dispatchers.IO) {
        if (!GeminiConfig.isConfigured()) {
            Log.e(TAG, "GEMINI_API_KEY is not configured or is a placeholder.")
            throw GeminiConfigurationException(
                "Gemini API key is not configured. Please add your GEMINI_API_KEY in the AI Studio Secrets panel."
            )
        }

        val payload = JSONObject().apply {
            val contentsArray = JSONArray().apply {
                val contentObj = JSONObject().apply {
                    val partsArray = JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    }
                    put("parts", partsArray)
                }
                put(contentObj)
            }
            put("contents", contentsArray)

            val genConfig = JSONObject().apply {
                put("temperature", 0.4)
                if (asJson) {
                    put("responseMimeType", "application/json")
                }
            }
            put("generationConfig", genConfig)
        }

        val models = GeminiConfig.FALLBACK_MODELS
        var lastException: Exception? = null

        for (model in models) {
            val endpoint = GeminiConfig.getEndpointForModel(model)
            val apiKey = GeminiConfig.getApiKey()
            val url = "$endpoint?key=$apiKey"

            try {
                Log.d(TAG, "Attempting Gemini text request with model: $model")
                return@withContext executeGeminiRequest(url, payload, retriesLeft = 1)
            } catch (e: GeminiConfigurationException) {
                Log.e(TAG, "API key configuration issue for model $model: ${e.message}", e)
                lastException = e
                break // Don't retry other models if API key is invalid
            } catch (e: GeminiRateLimitException) {
                Log.w(TAG, "Model $model text rate-limited / quota-exhausted (429): ${e.message}. Trying fallback model...", e)
                lastException = e
            } catch (e: GeminiUnavailableException) {
                Log.w(TAG, "Model $model text unavailable (503/500). Trying fallback model...", e)
                lastException = e
            } catch (e: Exception) {
                Log.w(TAG, "Model $model text request failed: ${e.message}. Trying fallback model...", e)
                lastException = e
            }
        }

        throw lastException ?: GeminiUnavailableException("Gemini is temporarily unavailable. Please try again.")
    }

    private fun executeGeminiRequest(url: String, payload: JSONObject, retriesLeft: Int = 1): String {
        val apiKey = GeminiConfig.getApiKey()
        val requestBody = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("Content-Type", "application/json")
            .addHeader("x-goog-api-key", apiKey)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                val code = response.code

                Log.d(TAG, "Gemini API HTTP Response code: $code")

                if (!response.isSuccessful) {
                    val snippet = responseBody.take(300).replace("\n", " ")
                    when (code) {
                        401, 403 -> {
                            Log.e(TAG, "Gemini Auth failure (HTTP $code): $snippet")
                            throw GeminiConfigurationException(
                                "Gemini API authentication failed (HTTP $code): $snippet"
                            )
                        }
                        429 -> {
                            Log.w(TAG, "Gemini Rate Limit / Quota Exceeded (HTTP 429): $snippet")
                            val isQuota = snippet.contains("quota", ignoreCase = true) || snippet.contains("resource_exhausted", ignoreCase = true)
                            val userMsg = if (isQuota) {
                                "Gemini token quota exceeded for current model (RESOURCE_EXHAUSTED). Trying next available model..."
                            } else {
                                "AI is temporarily busy (HTTP 429). Retrying with fallback model..."
                            }
                            throw GeminiRateLimitException(userMsg)
                        }
                        404 -> {
                            Log.w(TAG, "Gemini Model Not Found (HTTP 404): $snippet")
                            throw GeminiInvalidRequestException(
                                "Gemini model endpoint not found (HTTP 404): $snippet"
                            )
                        }
                        400, 422 -> {
                            Log.e(TAG, "Gemini Invalid Request (HTTP $code): $snippet")
                            throw GeminiInvalidRequestException(
                                "SnapBrand request rejected by model (HTTP $code): $snippet"
                            )
                        }
                        500, 502, 503, 504 -> {
                            Log.e(TAG, "Gemini Server error (HTTP $code): $snippet")
                            if (retriesLeft > 0) {
                                Log.w(TAG, "Retrying Gemini request after transient HTTP $code")
                                try {
                                    Thread.sleep(800)
                                } catch (_: InterruptedException) {}
                                return executeGeminiRequest(url, payload, retriesLeft - 1)
                            }
                            throw GeminiUnavailableException(
                                "Gemini is temporarily unavailable. Please try again."
                            )
                        }
                        else -> {
                            Log.e(TAG, "Gemini API Error (HTTP $code): $snippet")
                            if (code in 400..499) {
                                throw GeminiInvalidRequestException(
                                    "SnapBrand couldn't process this image."
                                )
                            } else {
                                throw GeminiUnavailableException(
                                    "Gemini is temporarily unavailable. Please try again."
                                )
                            }
                        }
                    }
                }

                // Parse Candidates from response
                try {
                    val root = JSONObject(responseBody)
                    val candidates = root.optJSONArray("candidates")
                    if (candidates == null || candidates.length() == 0) {
                        val promptFeedback = root.optJSONObject("promptFeedback")
                        val blockReason = promptFeedback?.optString("blockReason")
                        Log.w(TAG, "Gemini returned empty candidates. Reason: $blockReason")
                        throw GeminiInvalidRequestException("SnapBrand couldn't process this image.")
                    }

                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.getJSONObject("content")
                    val parts = content.getJSONArray("parts")
                    if (parts.length() == 0) {
                        throw GeminiInvalidRequestException("SnapBrand couldn't process this image.")
                    }

                    return parts.getJSONObject(0).getString("text")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse Gemini response candidates", e)
                    if (e is GeminiException) throw e
                    throw GeminiParseException("Failed to parse Gemini API response candidates: ${e.message}", e)
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network IO error communicating with Gemini API", e)
            if (retriesLeft > 0) {
                Log.w(TAG, "Retrying Gemini request after IOException")
                try {
                    Thread.sleep(600)
                } catch (_: InterruptedException) {}
                return executeGeminiRequest(url, payload, retriesLeft - 1)
            }
            throw GeminiNetworkException(
                "Check your internet connection and try again.",
                e
            )
        }
    }
}
