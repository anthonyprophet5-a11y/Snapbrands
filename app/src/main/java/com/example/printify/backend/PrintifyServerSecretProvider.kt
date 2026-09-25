package com.example.printify.backend

import java.io.File
import java.util.concurrent.atomic.AtomicReference

/**
 * SNAPBRAND — SERVER-SIDE SECRET CONFIGURATION PROVIDER
 *
 * CRITICAL SECURITY CONSTRAINTS:
 * 1. This class is strictly server-side / backend.
 * 2. PRINTIFY_API_KEY is NEVER exposed to Android client-side components,
 *    BuildConfig, SharedPreferences, Firestore, or logs.
 * 3. Secrets are loaded strictly from server environment variables
 *    or server-side .env file (which is gitignored).
 * 4. Token sanitization utilities ensure tokens and authorization headers
 *    are never printed or echoed in logs or error messages.
 */
object PrintifyServerSecretProvider {

    private const val ENV_KEY_NAME = "PRINTIFY_API_KEY"
    private const val LEGACY_KEY_NAME = "PRINTIFY_API_TOKEN"
    private const val WEBHOOK_SECRET_NAME = "PRINTIFY_WEBHOOK_SECRET"

    private val cachedApiKey = AtomicReference<String?>(null)
    private val cachedWebhookSecret = AtomicReference<String?>(null)

    @Volatile
    private var testOverrideActive = false
    private val testOverrideKey = AtomicReference<String?>(null)

    /**
     * Resolves PRINTIFY_API_KEY strictly from server-side environment or server .env file.
     * Never returns placeholder values.
     */
    fun getPrintifyApiKey(): String? {
        if (testOverrideActive) {
            return testOverrideKey.get()
        }

        val cached = cachedApiKey.get()
        if (!cached.isNullOrBlank()) {
            return cached
        }

        val resolved = resolveSecret(ENV_KEY_NAME, LEGACY_KEY_NAME)
        if (!resolved.isNullOrBlank()) {
            cachedApiKey.set(resolved)
        }
        return resolved
    }

    /**
     * Resolves PRINTIFY_WEBHOOK_SECRET strictly from server-side environment or server .env file.
     */
    fun getPrintifyWebhookSecret(): String? {
        val cached = cachedWebhookSecret.get()
        if (!cached.isNullOrBlank()) {
            return cached
        }

        val resolved = resolveSecret(WEBHOOK_SECRET_NAME)
        if (!resolved.isNullOrBlank()) {
            cachedWebhookSecret.set(resolved)
        }
        return resolved
    }

    /**
     * Allows setting server-side secret dynamically in runtime memory
     * (e.g. during authenticated session).
     */
    fun setServerApiKeyInMemory(secret: String?) {
        cachedApiKey.set(secret?.trim()?.takeIf { it.isNotBlank() })
    }

    /**
     * Explicit test override for hermetic testing, ignoring environment variables.
     */
    fun setTestOverrideKey(secret: String?) {
        testOverrideActive = true
        testOverrideKey.set(secret?.trim()?.takeIf { it.isNotBlank() })
    }

    fun clearTestOverride() {
        testOverrideActive = false
        testOverrideKey.set(null)
    }

    fun isApiKeyConfigured(): Boolean {
        return !getPrintifyApiKey().isNullOrBlank()
    }

    /**
     * Sanitizes strings to prevent accidental leakage of API keys, Bearer tokens, or authorization headers.
     */
    fun sanitize(message: String?): String {
        if (message == null) return ""
        var sanitized = message

        val currentKey = cachedApiKey.get()
        if (!currentKey.isNullOrBlank()) {
            sanitized = sanitized.replace(currentKey, "[REDACTED_API_KEY]")
        }

        val webhookSecret = cachedWebhookSecret.get()
        if (!webhookSecret.isNullOrBlank()) {
            sanitized = sanitized.replace(webhookSecret, "[REDACTED_WEBHOOK_SECRET]")
        }

        // Scrub general Bearer token patterns
        sanitized = sanitized.replace(Regex("(?i)Bearer\\s+[A-Za-z0-9_.-]+"), "Bearer [REDACTED_TOKEN]")
        sanitized = sanitized.replace(Regex("(?i)PRINTIFY_API_KEY\\s*=\\s*\\S+"), "PRINTIFY_API_KEY=[REDACTED]")
        sanitized = sanitized.replace(Regex("(?i)PRINTIFY_API_TOKEN\\s*=\\s*\\S+"), "PRINTIFY_API_TOKEN=[REDACTED]")
        sanitized = sanitized.replace(Regex("(?i)Authorization:\\s*\\S+"), "Authorization: [REDACTED]")

        return sanitized
    }

    /**
     * Reloads configuration from disk/env (e.g. after user updates .env via AI Studio Secrets).
     */
    fun reload() {
        cachedApiKey.set(null)
        cachedWebhookSecret.set(null)
        getPrintifyApiKey()
        getPrintifyWebhookSecret()
    }

    private fun resolveSecret(vararg keys: String): String? {
        // 1. Check process environment variables
        for (key in keys) {
            val envVal = System.getenv(key)
            if (isValidSecretValue(envVal)) {
                return envVal!!.trim()
            }
        }

        // 2. Check server-side .env file in potential root locations
        val candidateLocations = listOf(
            File(".env"),
            File(System.getProperty("user.dir", "."), ".env"),
            File("/applet/.env"),
            File("/.env"),
            File("../.env")
        )

        for (file in candidateLocations) {
            if (file.exists() && file.isFile && file.canRead()) {
                val secretFromDotEnv = parseKeyFromDotEnv(file, keys.toList())
                if (secretFromDotEnv != null) {
                    return secretFromDotEnv
                }
            }
        }

        return null
    }

    private fun parseKeyFromDotEnv(file: File, targetKeys: List<String>): String? {
        return try {
            file.useLines { lines ->
                for (rawLine in lines) {
                    val line = rawLine.trim()
                    if (line.isNotBlank() && !line.startsWith("#")) {
                        for (targetKey in targetKeys) {
                            if (line.startsWith("$targetKey=")) {
                                val rawValue = line.substringAfter("=").trim()
                                val cleaned = rawValue
                                    .removeSurrounding("\"")
                                    .removeSurrounding("'")
                                    .trim()
                                if (isValidSecretValue(cleaned)) {
                                    return@useLines cleaned
                                }
                            }
                        }
                    }
                }
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun isValidSecretValue(value: String?): Boolean {
        if (value.isNullOrBlank()) return false
        val trimmed = value.trim()
        if (trimmed.startsWith("MY_") || trimmed.equals("YOUR_PRINTIFY_API_KEY", ignoreCase = true) || trimmed == "placeholder") {
            return false
        }
        return true
    }
}
