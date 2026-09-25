package com.example.printify.client

import android.util.Log
import com.example.printify.backend.PrintifyServerSecretProvider
import com.example.printify.model.PrintifyBlueprint
import com.example.printify.model.PrintifyPrintProvider
import com.example.printify.model.PrintifyShippingEstimate
import com.example.printify.model.PrintifyShopSummary
import com.example.printify.model.PrintifyVariant
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/**
 * Clean abstraction interface for Printify REST API v1.
 * Allows deterministic unit testing without live network calls.
 */
interface PrintifyHttpClient {
    suspend fun getShops(token: String): Result<List<PrintifyShopSummary>>
    suspend fun getBlueprints(token: String): Result<List<PrintifyBlueprint>>
    suspend fun getPrintProviders(token: String, blueprintId: Int): Result<List<PrintifyPrintProvider>>
    suspend fun getVariants(token: String, blueprintId: Int, providerId: Int): Result<List<PrintifyVariant>>
    suspend fun getShippingInfo(token: String, blueprintId: Int, providerId: Int): Result<PrintifyShippingEstimate?>
    suspend fun uploadArtwork(token: String, fileName: String, url: String): Result<String>
    suspend fun createProduct(token: String, shopId: String, payload: JSONObject): Result<JSONObject>
    suspend fun createOrder(token: String, shopId: String, payload: JSONObject): Result<JSONObject>
    suspend fun getOrder(token: String, shopId: String, orderId: String): Result<JSONObject>
}

/**
 * Production implementation of the Printify API Client.
 * Base URL: https://api.printify.com/v1/
 *
 * CRITICAL SECURITY:
 * Never logs raw API tokens or authorization headers.
 * Implements exponential backoff retry for transient network and 429 rate limit errors.
 */
class RealPrintifyHttpClient(
    private val baseUrl: String = "https://api.printify.com/v1"
) : PrintifyHttpClient {

    private val TAG = "PrintifyClient"
    private val CONNECT_TIMEOUT_MS = 10_000
    private val READ_TIMEOUT_MS = 15_000
    private val MAX_RETRIES = 3

    override suspend fun getShops(token: String): Result<List<PrintifyShopSummary>> {
        return executeRequestWithRetry(
            method = "GET",
            endpoint = "/shops.json",
            token = token,
            body = null
        ).map { responseStr ->
            val array = JSONArray(responseStr)
            val shops = mutableListOf<PrintifyShopSummary>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                shops.add(
                    PrintifyShopSummary(
                        id = obj.optString("id", obj.optLong("id", 0L).toString()),
                        title = obj.optString("title", "Printify Shop"),
                        salesChannel = obj.optString("sales_channel", null)
                    )
                )
            }
            shops
        }
    }

    override suspend fun getBlueprints(token: String): Result<List<PrintifyBlueprint>> {
        return executeRequestWithRetry(
            method = "GET",
            endpoint = "/catalog/blueprints.json",
            token = token,
            body = null
        ).map { responseStr ->
            val array = JSONArray(responseStr)
            val blueprints = mutableListOf<PrintifyBlueprint>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val imagesList = mutableListOf<String>()
                val imgArr = obj.optJSONArray("images")
                if (imgArr != null) {
                    for (j in 0 until imgArr.length()) {
                        imagesList.add(imgArr.getString(j))
                    }
                }
                blueprints.add(
                    PrintifyBlueprint(
                        id = obj.getInt("id"),
                        title = obj.optString("title", "Merch Product"),
                        description = obj.optString("description", ""),
                        brand = obj.optString("brand", ""),
                        model = obj.optString("model", ""),
                        images = imagesList
                    )
                )
            }
            blueprints
        }
    }

    override suspend fun getPrintProviders(token: String, blueprintId: Int): Result<List<PrintifyPrintProvider>> {
        return executeRequestWithRetry(
            method = "GET",
            endpoint = "/catalog/blueprints/$blueprintId/print_providers.json",
            token = token,
            body = null
        ).map { responseStr ->
            val array = JSONArray(responseStr)
            val providers = mutableListOf<PrintifyPrintProvider>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val locObj = obj.optJSONObject("location")
                val locStr = if (locObj != null) {
                    listOfNotNull(locObj.optString("city", null), locObj.optString("country", null)).joinToString(", ")
                } else null
                providers.add(
                    PrintifyPrintProvider(
                        id = obj.getInt("id"),
                        title = obj.optString("title", "Print Provider #${obj.getInt("id")}"),
                        location = locStr
                    )
                )
            }
            providers
        }
    }

    override suspend fun getVariants(token: String, blueprintId: Int, providerId: Int): Result<List<PrintifyVariant>> {
        return executeRequestWithRetry(
            method = "GET",
            endpoint = "/catalog/blueprints/$blueprintId/print_providers/$providerId/variants.json",
            token = token,
            body = null
        ).map { responseStr ->
            val root = JSONObject(responseStr)
            val array = root.optJSONArray("variants") ?: JSONArray()
            val variants = mutableListOf<PrintifyVariant>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val optionsMap = mutableMapOf<String, String>()
                val optObj = obj.optJSONObject("options")
                if (optObj != null) {
                    val keys = optObj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        optionsMap[key] = optObj.optString(key)
                    }
                }

                // Official cost where returned (Printify returns cost in cents/minor units or null)
                val costCents: Long? = if (obj.has("cost") && !obj.isNull("cost")) {
                    obj.optLong("cost")
                } else null

                variants.add(
                    PrintifyVariant(
                        id = obj.getInt("id"),
                        title = obj.optString("title", "Variant #${obj.getInt("id")}"),
                        options = optionsMap,
                        costMinorUnits = costCents,
                        isAvailable = obj.optBoolean("is_available", true)
                    )
                )
            }
            variants
        }
    }

    override suspend fun getShippingInfo(
        token: String,
        blueprintId: Int,
        providerId: Int
    ): Result<PrintifyShippingEstimate?> {
        return executeRequestWithRetry(
            method = "GET",
            endpoint = "/catalog/blueprints/$blueprintId/print_providers/$providerId/shipping.json",
            token = token,
            body = null
        ).map { responseStr ->
            val obj = JSONObject(responseStr)
            val standardObj = obj.optJSONObject("standard")
            val handlingObj = obj.optJSONObject("handling_time")

            val costMinor: Long? = if (standardObj != null && standardObj.has("cost") && !standardObj.isNull("cost")) {
                standardObj.optLong("cost")
            } else null

            val currency = standardObj?.optString("currency", "USD") ?: "USD"
            val minDays = handlingObj?.optInt("min", 2)
            val maxDays = handlingObj?.optInt("max", 5)

            PrintifyShippingEstimate(
                standardCostMinorUnits = costMinor,
                currency = currency,
                handlingDaysMin = minDays,
                handlingDaysMax = maxDays
            )
        }
    }

    override suspend fun uploadArtwork(token: String, fileName: String, url: String): Result<String> {
        val payload = JSONObject().apply {
            put("file_name", fileName)
            put("url", url)
        }
        return executeRequestWithRetry(
            method = "POST",
            endpoint = "/uploads/images.json",
            token = token,
            body = payload.toString()
        ).map { responseStr ->
            val obj = JSONObject(responseStr)
            obj.getString("id")
        }
    }

    override suspend fun createProduct(token: String, shopId: String, payload: JSONObject): Result<JSONObject> {
        return executeRequestWithRetry(
            method = "POST",
            endpoint = "/shops/$shopId/products.json",
            token = token,
            body = payload.toString()
        ).map { responseStr ->
            JSONObject(responseStr)
        }
    }

    override suspend fun createOrder(token: String, shopId: String, payload: JSONObject): Result<JSONObject> {
        return executeRequestWithRetry(
            method = "POST",
            endpoint = "/shops/$shopId/orders.json",
            token = token,
            body = payload.toString()
        ).map { responseStr ->
            JSONObject(responseStr)
        }
    }

    override suspend fun getOrder(token: String, shopId: String, orderId: String): Result<JSONObject> {
        return executeRequestWithRetry(
            method = "GET",
            endpoint = "/shops/$shopId/orders/$orderId.json",
            token = token,
            body = null
        ).map { responseStr ->
            JSONObject(responseStr)
        }
    }

    fun buildAuthHeaders(token: String): Map<String, String> {
        return mapOf(
            "Authorization" to "Bearer $token",
            "Content-Type" to "application/json",
            "User-Agent" to "SnapBrand/1.0"
        )
    }

    /**
     * Executes HTTPS request with exponential backoff on 429 (Rate Limit) and 5xx errors.
     * Sanitizes error messages to ensure Bearer tokens are NEVER present in exceptions or logs.
     */
    private fun executeRequestWithRetry(
        method: String,
        endpoint: String,
        token: String,
        body: String?
    ): Result<String> {
        if (token.isBlank()) {
            return Result.failure(SecurityException("Printify authentication token is missing."))
        }

        var attempt = 0
        var lastException: Exception? = null

        while (attempt < MAX_RETRIES) {
            attempt++
            var conn: HttpURLConnection? = null
            try {
                val fullUrl = URL(baseUrl.trimEnd('/') + endpoint)
                conn = (fullUrl.openConnection() as HttpURLConnection).apply {
                    requestMethod = method
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = READ_TIMEOUT_MS
                    setRequestProperty("Authorization", "Bearer $token")
                    setRequestProperty("User-Agent", "SnapBrand/1.0 (Android Merch Engine)")
                    setRequestProperty("Content-Type", "application/json;charset=utf-8")
                    setRequestProperty("Accept", "application/json")
                    useCaches = false
                }

                if (body != null && (method == "POST" || method == "PUT")) {
                    conn.doOutput = true
                    OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                        writer.write(body)
                        writer.flush()
                    }
                }

                val responseCode = conn.responseCode

                if (responseCode in 200..299) {
                    val reader = BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8))
                    val responseStr = reader.use { it.readText() }
                    return Result.success(responseStr)
                }

                // Handle Error responses safely
                val errorStream = conn.errorStream
                val errorBody = if (errorStream != null) {
                    BufferedReader(InputStreamReader(errorStream, Charsets.UTF_8)).use { it.readText() }
                } else ""

                val safeErrorMessage = PrintifyServerSecretProvider.sanitize(parsePrintifyError(responseCode, errorBody))

                if (responseCode == 429 || responseCode in 500..599) {
                    // Retryable error
                    if (attempt < MAX_RETRIES) {
                        val backoffMs = (1000L * Math.pow(2.0, (attempt - 1).toDouble())).toLong()
                        Thread.sleep(backoffMs)
                        continue
                    }
                }

                return Result.failure(PrintifyApiException(responseCode, safeErrorMessage))

            } catch (e: Exception) {
                lastException = e
                if (attempt < MAX_RETRIES) {
                    Thread.sleep(500L * attempt)
                }
            } finally {
                conn?.disconnect()
            }
        }

        val sanitizedError = PrintifyServerSecretProvider.sanitize(
            lastException?.message ?: "Printify API connection failed after $MAX_RETRIES attempts."
        )
        return Result.failure(IllegalStateException(sanitizedError))
    }

    private fun parsePrintifyError(code: Int, rawBody: String): String {
        return try {
            if (rawBody.isNotBlank()) {
                val json = JSONObject(rawBody)
                val msg = json.optString("message", json.optString("error", ""))
                if (msg.isNotBlank()) return "Printify API ($code): $msg"
            }
            when (code) {
                400 -> "Printify validation error. Please check product configuration."
                401 -> "Printify connection could not be verified. Invalid or expired token."
                403 -> "Printify access denied. Token lacks required shop scopes."
                404 -> "Printify resource not found."
                429 -> "Printify rate limit exceeded. Please wait a moment."
                in 500..599 -> "Printify temporarily rejected this request. Please try again."
                else -> "Printify request failed with HTTP $code."
            }
        } catch (_: Exception) {
            "Printify request failed with HTTP $code."
        }
    }
}

class PrintifyApiException(val httpStatusCode: Int, message: String) : Exception(message)
