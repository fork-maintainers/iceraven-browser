/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.menu.share

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.annotation.RequiresApi
import org.mozilla.fenix.R
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Utility class to download and save QR code images to the device's Downloads folder.
 * @param onResponse A callback function that is invoked with the [Context] and a [Boolean]
 * indicating success or failure of the save operation. By default, it shows a Toast message.
 * @param currentTimeMillis provider for the current time in milliseconds, injectable for testing.
 */
class QRCodeDownloader(
    private val onResponse: (Context, Boolean) -> Unit = { context, isSuccess ->
        Toast.makeText(
            context,
            if (isSuccess) R.string.qr_code_download_success else R.string.qr_code_download_failure,
            Toast.LENGTH_SHORT,
        ).show()
    },
    private val currentTimeMillis: () -> Long = { System.currentTimeMillis() },
) {

    /**
     * Saves the QR code image represented by the given [qrCodeUri] to the Downloads folder.
     *
     * @param qrCodeUri The [Uri] of the QR code image to be saved.
     * @param contentResolver The [ContentResolver] to access image data.
     * @param context The [Context] used to show Toast messages.
     */
    fun saveQRCodeToDownloads(qrCodeUri: Uri, contentResolver: ContentResolver, context: Context) {
        val bitmap = contentResolver.openInputStream(qrCodeUri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream)
        }

        if (bitmap != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveToMediaStoreDownloadsFolder(contentResolver, bitmap, context)
            } else {
                saveToDirectoryDownloads(bitmap, context)
            }
        } else {
            onResponse(context, false)
        }
    }

    private fun saveToDirectoryDownloads(bitmap: Bitmap, context: Context) {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadsDir, "qr_code_${currentTimeMillis()}.png")

        try {
            FileOutputStream(file).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, outputStream)
            }
            onResponse(context, true)
        } catch (e: IOException) {
            onResponse(context, false)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveToMediaStoreDownloadsFolder(
        contentResolver: ContentResolver,
        bitmap: Bitmap,
        context: Context,
    ) {
        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, "qr_code_${currentTimeMillis()}.png")
            put(MediaStore.Downloads.MIME_TYPE, "image/png")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let {
            contentResolver.openOutputStream(it)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, outputStream)
            }
            onResponse(context, true)
        } ?: run {
            onResponse(context, false)
        }
    }

    companion object {
        private const val PNG_QUALITY = 100
    }
}
