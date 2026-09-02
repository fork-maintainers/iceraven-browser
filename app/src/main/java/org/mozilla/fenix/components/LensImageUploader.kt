/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.annotation.VisibleForTesting
import androidx.core.graphics.scale
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mozilla.components.concept.fetch.Client
import mozilla.components.concept.fetch.MutableHeaders
import mozilla.components.concept.fetch.Request
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.max

/**
 * Handles image processing and upload to Google Lens.
 */
class LensImageUploader(
    private val context: Context,
    private val client: Client,
    private val userAgent: String,
    private val currentTimeMillis: () -> Long = { System.currentTimeMillis() },
) {

    /**
     * The outcome of an image upload to Google Lens.
     *
     * @property resultUrl The Lens results URL on success, or null on failure.
     * @property httpStatusCode The HTTP status code of the Lens upload request, or null when the
     * request was never made (for example, the image could not be decoded or downloaded).
     */
    data class UploadResult(
        val resultUrl: String?,
        val httpStatusCode: Int? = null,
    )

    /**
     * Decodes, scales, compresses, and uploads the image at [imageUri] to Google Lens.
     *
     * @param imageUri The content URI of the image to upload.
     * @param isPrivate When true, the upload runs in GeckoView's private cookie context so the
     * returned Lens session matches the private tab the result is opened in.
     */
    suspend fun upload(imageUri: Uri, isPrivate: Boolean): UploadResult = withContext(Dispatchers.IO) {
        val bitmap = decodeBitmap(imageUri) ?: return@withContext UploadResult(resultUrl = null)
        uploadBitmap(bitmap, isPrivate)
    }

    /**
     * Fetches the image at [imageUrl], then scales, compresses, and uploads it to Google Lens.
     * The browser's User-Agent and cookies are used to fetch the image, which succeeds for hosts
     * that block Lens's own server-side fetcher.
     *
     * @param imageUrl The URL of the image to fetch and upload.
     * @param isPrivate When true, both the image download and the Lens upload run in GeckoView's
     * private cookie context so the returned Lens session matches the private tab the result is
     * opened in.
     */
    suspend fun uploadFromUrl(imageUrl: String, isPrivate: Boolean): UploadResult =
        withContext(Dispatchers.IO) {
            val bitmap = fetchBitmap(imageUrl, isPrivate)
                ?: return@withContext UploadResult(resultUrl = null)
            uploadBitmap(bitmap, isPrivate)
        }

    private fun uploadBitmap(bitmap: Bitmap, isPrivate: Boolean): UploadResult {
        val scaled = scaleBitmap(bitmap)
        val scaledWidth = scaled.width
        val scaledHeight = scaled.height

        val jpegData = compressToJpeg(scaled)

        if (scaled !== bitmap) scaled.recycle()
        bitmap.recycle()

        val uploadUrl = "$UPLOAD_ENDPOINT?${commonParams()}&ep=$EP_BY_BYTES"

        val boundary = "----LensBoundary${System.nanoTime()}"

        val bodyStream = ByteArrayOutputStream()
        bodyStream.write("--$boundary\r\n".toByteArray())
        bodyStream.write(
            "Content-Disposition: form-data; name=\"encoded_image\"; filename=\"image.jpg\"\r\n"
                .toByteArray(),
        )
        bodyStream.write("Content-Type: image/jpeg\r\n\r\n".toByteArray())
        bodyStream.write(jpegData)
        bodyStream.write("\r\n".toByteArray())
        bodyStream.write("--$boundary\r\n".toByteArray())
        bodyStream.write(
            "Content-Disposition: form-data; name=\"processed_image_dimensions\"\r\n\r\n"
                .toByteArray(),
        )
        bodyStream.write("$scaledWidth,$scaledHeight\r\n".toByteArray())
        bodyStream.write("--$boundary--\r\n".toByteArray())
        val bodyBytes = bodyStream.toByteArray()

        val request = Request(
            url = uploadUrl,
            method = Request.Method.POST,
            headers = MutableHeaders(
                "Content-Type" to "multipart/form-data; boundary=$boundary",
                "User-Agent" to userAgent,
            ),
            body = Request.Body(ByteArrayInputStream(bodyBytes)),
            cookiePolicy = Request.CookiePolicy.INCLUDE,
            private = isPrivate,
            connectTimeout = Pair(CONNECT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS),
            readTimeout = Pair(READ_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS),
            useCaches = true,
        )

        return client.fetch(request).use { response ->
            val resultUrl = if (response.status in SUCCESS_RANGE && response.url != uploadUrl) {
                response.url
            } else {
                null
            }
            UploadResult(resultUrl = resultUrl, httpStatusCode = response.status)
        }
    }

    @VisibleForTesting
    internal fun decodeBitmap(uri: Uri): Bitmap? {
        val bitmap = context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input)
        } ?: return null

        // Camera2 writes the capture orientation as an EXIF tag rather than rotating pixels,
        // and BitmapFactory.decodeStream discards EXIF. Re-read the tag from a fresh stream and
        // apply it so the upload reaches Lens upright.
        val orientation = context.contentResolver.openInputStream(uri)?.use { input ->
            ExifInterface(input).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
        } ?: ExifInterface.ORIENTATION_NORMAL

        return applyExifOrientation(bitmap, orientation)
    }

    private fun applyExifOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(ROTATE_90)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(ROTATE_180)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(ROTATE_270)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(ROTATE_90)
                matrix.preScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(ROTATE_270)
                matrix.preScale(-1f, 1f)
            }
            else -> return bitmap
        }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated !== bitmap) bitmap.recycle()
        return rotated
    }

    @VisibleForTesting
    internal fun fetchBitmap(imageUrl: String, isPrivate: Boolean): Bitmap? {
        val request = Request(
            url = imageUrl,
            method = Request.Method.GET,
            headers = MutableHeaders("User-Agent" to userAgent),
            cookiePolicy = Request.CookiePolicy.INCLUDE,
            private = isPrivate,
            connectTimeout = Pair(CONNECT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS),
            readTimeout = Pair(READ_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS),
            useCaches = true,
        )

        return try {
            client.fetch(request).use { response ->
                if (response.status !in SUCCESS_RANGE) return@use null
                val bytes = response.body.useStream { readAtMost(it, MAX_DOWNLOAD_BYTES) }
                    ?: return@use null
                val bitmap = decodeSampledBitmap(bytes) ?: return@use null
                // BitmapFactory.decodeByteArray discards EXIF the same way decodeStream does;
                // web JPEGs frequently carry an Orientation tag, so apply it here too.
                val orientation = ExifInterface(ByteArrayInputStream(bytes)).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
                applyExifOrientation(bitmap, orientation)
            }
        } catch (_: IOException) {
            null
        }
    }

    /**
     * Reads up to [max] bytes from [stream] and returns them, or null if the stream carries more
     * than [max] bytes. Guards against oversized remote responses before decoding.
     */
    private fun readAtMost(stream: InputStream, max: Int): ByteArray? {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(READ_BUFFER_BYTES)
        var total = 0
        while (true) {
            val read = stream.read(buffer)
            if (read < 0) return out.toByteArray()
            total += read
            if (total > max) return null
            out.write(buffer, 0, read)
        }
    }

    /**
     * Decodes [bytes] into a Bitmap, subsampling so neither dimension exceeds
     * [MAX_IMAGE_DIMENSION] and avoiding a full-resolution allocation for oversized images.
     */
    private fun decodeSampledBitmap(bytes: ByteArray): Bitmap? {
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOptions)
        if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) return null

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = computeSampleSize(boundsOptions.outWidth, boundsOptions.outHeight)
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
    }

    private fun computeSampleSize(width: Int, height: Int): Int {
        var sampleSize = 1
        while (max(width, height) / sampleSize > MAX_IMAGE_DIMENSION) {
            sampleSize *= 2
        }
        return sampleSize
    }

    /**
     * Builds the query parameters common to both Lens endpoints: language override and the
     * viewport dimensions and start time used for server-side rendering and latency tracking.
     */
    private fun commonParams(): String {
        val metrics = context.resources.displayMetrics
        return "hl=${Locale.getDefault().language}" +
            "&vpw=${metrics.widthPixels}" +
            "&vph=${metrics.heightPixels}" +
            "&st=${currentTimeMillis()}"
    }

    private fun scaleBitmap(bitmap: Bitmap): Bitmap {
        val longestDim = max(bitmap.width, bitmap.height)
        if (longestDim <= MAX_IMAGE_DIMENSION) return bitmap

        val scale = MAX_IMAGE_DIMENSION.toFloat() / longestDim
        val newWidth = (bitmap.width * scale).toInt()
        val newHeight = (bitmap.height * scale).toInt()
        return bitmap.scale(newWidth, newHeight, true)
    }

    private fun compressToJpeg(bitmap: Bitmap): ByteArray {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, baos)
        return baos.toByteArray()
    }

    companion object {
        @VisibleForTesting
        internal const val UPLOAD_ENDPOINT = "https://lens.google.com/upload"

        // Entry-point identifier assigned to Mozilla by Google for attribution.
        @VisibleForTesting
        internal const val EP_BY_BYTES = "fntpubb"

        private const val MAX_IMAGE_DIMENSION = 1000
        private const val JPEG_QUALITY = 40
        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 30_000
        private const val ROTATE_90 = 90f
        private const val ROTATE_180 = 180f
        private const val ROTATE_270 = 270f

        /** Hard cap on the bytes read from a remote image. Images above this are discarded. */
        private const val MAX_DOWNLOAD_BYTES = 20 * 1024 * 1024
        private const val READ_BUFFER_BYTES = 8 * 1024
        private val SUCCESS_RANGE = 200..299
    }
}
