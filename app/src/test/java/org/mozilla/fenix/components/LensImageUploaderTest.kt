/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components

import android.content.ContentResolver
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.util.DisplayMetrics
import androidx.exifinterface.media.ExifInterface
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import mozilla.components.concept.fetch.Client
import mozilla.components.concept.fetch.MutableHeaders
import mozilla.components.concept.fetch.Request
import mozilla.components.concept.fetch.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import kotlin.test.assertNotNull

@RunWith(RobolectricTestRunner::class)
class LensImageUploaderTest {

    @Test
    fun `GIVEN jpeg with no exif orientation WHEN decoded THEN dimensions are preserved`() {
        val jpegBytes = encodeJpeg(width = 80, height = 40)

        val bitmap = createUploader(jpegBytes).decodeBitmap(mockk())

        assertNotNull(bitmap)
        assertEquals(80, bitmap.width)
        assertEquals(40, bitmap.height)
    }

    @Test
    fun `GIVEN jpeg with rotate 90 exif WHEN decoded THEN width and height are swapped`() {
        val jpegBytes = encodeJpeg(width = 80, height = 40)
            .withExifOrientation(ExifInterface.ORIENTATION_ROTATE_90)

        val bitmap = createUploader(jpegBytes).decodeBitmap(mockk())

        assertNotNull(bitmap)
        assertEquals(40, bitmap.width)
        assertEquals(80, bitmap.height)
    }

    @Test
    fun `GIVEN jpeg with rotate 180 exif WHEN decoded THEN dimensions are preserved`() {
        val jpegBytes = encodeJpeg(width = 80, height = 40)
            .withExifOrientation(ExifInterface.ORIENTATION_ROTATE_180)

        val bitmap = createUploader(jpegBytes).decodeBitmap(mockk())

        assertNotNull(bitmap)
        assertEquals(80, bitmap.width)
        assertEquals(40, bitmap.height)
    }

    @Test
    fun `GIVEN jpeg with rotate 270 exif WHEN decoded THEN width and height are swapped`() {
        val jpegBytes = encodeJpeg(width = 80, height = 40)
            .withExifOrientation(ExifInterface.ORIENTATION_ROTATE_270)

        val bitmap = createUploader(jpegBytes).decodeBitmap(mockk())

        assertNotNull(bitmap)
        assertEquals(40, bitmap.width)
        assertEquals(80, bitmap.height)
    }

    @Test
    fun `GIVEN content resolver returns null WHEN decoded THEN result is null`() {
        val uploader = LensImageUploader(
            context = mockk<Context>().apply {
                every { contentResolver } returns mockk<ContentResolver>().apply {
                    every { openInputStream(any()) } returns null
                }
            },
            client = mockk<Client>(),
            userAgent = "test",
        )

        assertEquals(null, uploader.decodeBitmap(mockk()))
    }

    @Test
    fun `GIVEN fetched jpeg with no exif orientation WHEN fetched THEN dimensions are preserved`() {
        val jpegBytes = encodeJpeg(width = 80, height = 40)

        val bitmap = createUploaderWithFetch(jpegBytes).fetchBitmap("https://example.com/i.jpg", isPrivate = false)

        assertNotNull(bitmap)
        assertEquals(80, bitmap.width)
        assertEquals(40, bitmap.height)
    }

    @Test
    fun `GIVEN fetched jpeg with rotate 90 exif WHEN fetched THEN width and height are swapped`() {
        val jpegBytes = encodeJpeg(width = 80, height = 40)
            .withExifOrientation(ExifInterface.ORIENTATION_ROTATE_90)

        val bitmap = createUploaderWithFetch(jpegBytes).fetchBitmap("https://example.com/i.jpg", isPrivate = false)

        assertNotNull(bitmap)
        assertEquals(40, bitmap.width)
        assertEquals(80, bitmap.height)
    }

    @Test
    fun `GIVEN fetched jpeg with rotate 180 exif WHEN fetched THEN dimensions are preserved`() {
        val jpegBytes = encodeJpeg(width = 80, height = 40)
            .withExifOrientation(ExifInterface.ORIENTATION_ROTATE_180)

        val bitmap = createUploaderWithFetch(jpegBytes).fetchBitmap("https://example.com/i.jpg", isPrivate = false)

        assertNotNull(bitmap)
        assertEquals(80, bitmap.width)
        assertEquals(40, bitmap.height)
    }

    @Test
    fun `GIVEN fetched jpeg with rotate 270 exif WHEN fetched THEN width and height are swapped`() {
        val jpegBytes = encodeJpeg(width = 80, height = 40)
            .withExifOrientation(ExifInterface.ORIENTATION_ROTATE_270)

        val bitmap = createUploaderWithFetch(jpegBytes).fetchBitmap("https://example.com/i.jpg", isPrivate = false)

        assertNotNull(bitmap)
        assertEquals(40, bitmap.width)
        assertEquals(80, bitmap.height)
    }

    @Test
    fun `GIVEN an image WHEN upload THEN posts to the upload endpoint with ep fntpubb`() = runTest {
        val requestSlot = slot<Request>()
        val client = mockk<Client>()
        every { client.fetch(capture(requestSlot)) } answers {
            Response(
                url = "https://lens.google.com/search?results",
                status = 200,
                headers = MutableHeaders(),
                body = Response.Body(ByteArrayInputStream(ByteArray(0))),
            )
        }
        val uploader = LensImageUploader(
            context = metricsContext(jpegBytes = encodeJpeg(width = 80, height = 40)),
            client = client,
            userAgent = "test",
        )

        uploader.upload(mockk(), isPrivate = false)

        val request = requestSlot.captured
        assertTrue(request.url.startsWith("${LensImageUploader.UPLOAD_ENDPOINT}?"))
        assertTrue(request.url.contains("ep=${LensImageUploader.EP_BY_BYTES}"))
        assertEquals(Request.Method.POST, request.method)
        assertEquals(false, request.private)
    }

    @Test
    fun `GIVEN a private upload WHEN upload THEN the request runs in the private context`() = runTest {
        val requestSlot = slot<Request>()
        val client = mockk<Client>()
        every { client.fetch(capture(requestSlot)) } answers {
            Response(
                url = "https://lens.google.com/search?results",
                status = 200,
                headers = MutableHeaders(),
                body = Response.Body(ByteArrayInputStream(ByteArray(0))),
            )
        }
        val uploader = LensImageUploader(
            context = metricsContext(jpegBytes = encodeJpeg(width = 80, height = 40)),
            client = client,
            userAgent = "test",
        )

        uploader.upload(mockk(), isPrivate = true)

        assertEquals(true, requestSlot.captured.private)
    }

    @Test
    fun `GIVEN a private uploadFromUrl WHEN fetching the image THEN the request runs in the private context`() {
        val requestSlot = slot<Request>()
        val client = mockk<Client>()
        every { client.fetch(capture(requestSlot)) } answers {
            Response(
                url = "https://example.com/i.jpg",
                status = 200,
                headers = MutableHeaders(),
                body = Response.Body(ByteArrayInputStream(encodeJpeg(width = 80, height = 40))),
            )
        }
        val uploader = LensImageUploader(
            context = mockk<Context>(),
            client = client,
            userAgent = "test",
        )

        uploader.fetchBitmap("https://example.com/i.jpg", isPrivate = true)

        assertEquals(true, requestSlot.captured.private)
    }

    private fun createUploaderWithFetch(jpegBytes: ByteArray): LensImageUploader {
        val client = mockk<Client>()
        every { client.fetch(any()) } answers {
            Response(
                url = "https://example.com/i.jpg",
                status = 200,
                headers = MutableHeaders(),
                body = Response.Body(ByteArrayInputStream(jpegBytes)),
            )
        }
        return LensImageUploader(
            context = mockk<Context>(),
            client = client,
            userAgent = "test",
        )
    }

    private fun metricsContext(jpegBytes: ByteArray? = null): Context {
        val context = mockk<Context>()
        val resources = mockk<Resources>()
        every { resources.displayMetrics } returns DisplayMetrics().apply {
            widthPixels = 1080
            heightPixels = 1920
        }
        every { context.resources } returns resources
        if (jpegBytes != null) {
            val contentResolver = mockk<ContentResolver>()
            every { contentResolver.openInputStream(any<Uri>()) } answers {
                ByteArrayInputStream(jpegBytes)
            }
            every { context.contentResolver } returns contentResolver
        }
        return context
    }

    private fun createUploader(jpegBytes: ByteArray): LensImageUploader {
        val contentResolver = mockk<ContentResolver>()
        // decodeBitmap opens the stream twice: once for the bitmap, once for EXIF.
        every { contentResolver.openInputStream(any<Uri>()) } answers {
            ByteArrayInputStream(jpegBytes)
        }
        val context = mockk<Context>()
        every { context.contentResolver } returns contentResolver
        return LensImageUploader(
            context = context,
            client = mockk<Client>(),
            userAgent = "test",
        )
    }

    private fun encodeJpeg(width: Int, height: Int): ByteArray {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).drawColor(Color.RED)
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        return out.toByteArray()
    }

    // ExifInterface mutation requires a seekable target; write to a temp file, tag, read back.
    private fun ByteArray.withExifOrientation(orientation: Int): ByteArray {
        val temp = File.createTempFile("lens-upload-test", ".jpg").apply { deleteOnExit() }
        temp.writeBytes(this)
        ExifInterface(temp.absolutePath).apply {
            setAttribute(ExifInterface.TAG_ORIENTATION, orientation.toString())
            saveAttributes()
        }
        return FileInputStream(temp).use { it.readBytes() }
    }

    companion object {
        private const val JPEG_QUALITY = 90
    }
}
