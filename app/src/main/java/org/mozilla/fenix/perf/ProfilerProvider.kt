/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.perf

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.UriMatcher
import android.database.Cursor
import android.net.Uri
import android.os.Binder
import android.os.ParcelFileDescriptor
import android.os.Process
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mozilla.components.support.base.log.logger.Logger
import org.mozilla.fenix.ext.components
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Content Provider that enables stopping the Firefox Profiler and retrieving profile data via ADB.
 * The caller will receive the profiler data as a streams as a  raw gzip-compressed profile data.
 *
 * Usage: adb shell content read --uri content://<applicationId>.profiler/stop-and-upload > profile.gz
 *
 * Note: Access is restricted to the ADB process (shell UID) through DUMP permission in the Manifest.
 */
class ProfilerProvider : ContentProvider() {

    companion object {
        private const val PATH_STOP_AND_UPLOAD = "stop-and-upload"
        private const val CODE_STOP_AND_UPLOAD = 1
    }

    private val logger = Logger("ProfilerProvider")

    // Needed to inject dispatcher for tests
    internal var ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    // Needed to inject ProfilerUtils for tests
    @androidx.annotation.VisibleForTesting
    internal var saveProfileUrl: (ByteArray, Context) -> String = { data, context ->
        ProfilerUtils.saveProfileUrlToClipboard(data, context)
    }

    private lateinit var userDictionary: String
    private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH)

    override fun onCreate(): Boolean {
        // Use a per-build user_dictionary (authority) since there is multiple variants: <applicationId>.profiler
        userDictionary = context!!.packageName + ".profiler"
        // Creates the uri to stop the profiler from adb (content://userDictionary/stop-and-upload)
        uriMatcher.addURI(userDictionary, PATH_STOP_AND_UPLOAD, CODE_STOP_AND_UPLOAD)
        return true
    }

    override fun getType(uri: Uri): String? = when (match(uri)) {
        CODE_STOP_AND_UPLOAD -> "application/octet-stream"
        else -> null
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        enforceShellCaller()

        return when (match(uri)) {
            CODE_STOP_AND_UPLOAD -> openStopAndUploadPipe()
            else -> throw UnsupportedOperationException("Unknown URI: $uri")
        }
    }

    /**
     * Creates a pipe to handle profiler stop operations and stream raw profile data asynchronously.
     */
    private fun openStopAndUploadPipe(): ParcelFileDescriptor {
        val pipe = ParcelFileDescriptor.createPipe()
        val appContext = context!!.applicationContext

        CoroutineScope(ioDispatcher).launch {
            ParcelFileDescriptor.AutoCloseOutputStream(pipe[1]).use { os ->
                val profiler = appContext.components.core.engine.profiler
                if (profiler == null || !profiler.isProfilerActive()) {
                    throw IllegalStateException("Profiler is not active")
                }

                val data = withContext(Dispatchers.Main) {
                    suspendCancellableCoroutine { continuation ->
                        profiler.stopProfiler(
                            onSuccess = { data -> continuation.resume(data) },
                            onError = { throwable ->
                                continuation.resumeWithException(throwable)
                            },
                        )
                        continuation.invokeOnCancellation {
                            logger.info("Profiler stop operation cancelled")
                        }
                    }
                }

                if (data == null) {
                    throw IOException("Profiler returned empty data")
                }

                // Stream raw profile data directly through the pipe
                os.write(data)
                os.flush()
            }
        }
        return pipe[0]
    }

    /**
     * Enforces that the caller is ADB shell or system.
     *
     * @throws SecurityException if the caller's UID is not SHELL_UID, SYSTEM_UID, or root (0)
     */
    private fun enforceShellCaller() {
        val uid = Binder.getCallingUid()
        if (uid != Process.SHELL_UID && uid != Process.SYSTEM_UID && uid != 0) {
            throw SecurityException("Caller not allowed: uid=$uid")
        }
    }

    /**
     * Matches a URI against the registered pattern CODE_STOP_AND_UPLOAD
     * to determine the operation code.
     */
    private fun match(uri: Uri): Int = when (uriMatcher.match(uri)) {
        CODE_STOP_AND_UPLOAD -> CODE_STOP_AND_UPLOAD
        else -> -1
    }
}
