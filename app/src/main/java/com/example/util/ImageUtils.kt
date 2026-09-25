package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.math.max

class ImageValidationException(message: String, cause: Throwable? = null) : Exception(message, cause)

data class ProcessedImage(
    val bitmap: Bitmap,
    val base64Data: String,
    val mimeType: String,
    val sizeBytes: Long,
    val width: Int,
    val height: Int
)

object ImageUtils {
    const val MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024L // 10MB
    const val MAX_DIMENSION = 1280

    /**
     * Reads, validates, scales down if needed, and encodes an image into JPEG base64 for Gemini multimodal input.
     */
    fun processImageUri(context: Context, uri: Uri): ProcessedImage {
        val contentResolver = context.contentResolver

        // 1. Verify stream openable
        val fileSize = try {
            contentResolver.openFileDescriptor(uri, "r")?.use {
                it.statSize
            } ?: -1L
        } catch (e: Exception) {
            -1L
        }

        if (fileSize > MAX_FILE_SIZE_BYTES) {
            val sizeMb = fileSize / (1024 * 1024)
            throw ImageValidationException("Selected image is too large (${sizeMb}MB). Maximum allowed size is 10MB.")
        }

        // 2. Decode bounds only first to check dimensions & prevent OOM
        val boundsOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        try {
            contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            } ?: throw ImageValidationException("Cannot open the selected image.")
        } catch (e: ImageValidationException) {
            throw e
        } catch (e: Exception) {
            throw ImageValidationException("Failed to read image header. The file may be damaged.", e)
        }

        if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) {
            throw ImageValidationException("Could not decode image dimensions. Ensure the file is a valid image (JPEG, PNG, or WEBP).")
        }

        // 3. Compute inSampleSize
        val largestDimension = max(boundsOptions.outWidth, boundsOptions.outHeight)
        var inSampleSize = 1
        while (largestDimension / inSampleSize > MAX_DIMENSION * 1.5) {
            inSampleSize *= 2
        }

        // 4. Decode actual bitmap with sample size
        val decodeOptions = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        val decodedBitmap: Bitmap = try {
            contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: throw ImageValidationException("Could not decode image content.")
        } catch (e: ImageValidationException) {
            throw e
        } catch (e: Exception) {
            throw ImageValidationException("Failed to decode image data.", e)
        }

        // 5. Proportionally scale down if still exceeds MAX_DIMENSION
        val finalBitmap = scaleBitmapToMaxDimension(decodedBitmap, MAX_DIMENSION)

        // 6. Compress to JPEG
        val outputStream = ByteArrayOutputStream()
        finalBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        val jpegBytes = outputStream.toByteArray()

        val base64String = Base64.encodeToString(jpegBytes, Base64.NO_WRAP)

        return ProcessedImage(
            bitmap = finalBitmap,
            base64Data = base64String,
            mimeType = "image/jpeg",
            sizeBytes = jpegBytes.size.toLong(),
            width = finalBitmap.width,
            height = finalBitmap.height
        )
    }

    /**
     * Scales bitmap so neither width nor height exceeds maxDimension while preserving aspect ratio.
     */
    fun scaleBitmapToMaxDimension(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDimension && height <= maxDimension) {
            return bitmap
        }

        val ratio = width.toFloat() / height.toFloat()
        val newWidth: Int
        val newHeight: Int
        if (width > height) {
            newWidth = maxDimension
            newHeight = (maxDimension / ratio).toInt()
        } else {
            newHeight = maxDimension
            newWidth = (maxDimension * ratio).toInt()
        }

        return Bitmap.createScaledBitmap(bitmap, max(1, newWidth), max(1, newHeight), true)
    }

    /**
     * Converts an in-memory Bitmap directly to base64 JPEG.
     */
    fun processBitmap(bitmap: Bitmap): ProcessedImage {
        val scaled = scaleBitmapToMaxDimension(bitmap, MAX_DIMENSION)
        val outputStream = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        val bytes = outputStream.toByteArray()
        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
        return ProcessedImage(
            bitmap = scaled,
            base64Data = base64,
            mimeType = "image/jpeg",
            sizeBytes = bytes.size.toLong(),
            width = scaled.width,
            height = scaled.height
        )
    }
}
