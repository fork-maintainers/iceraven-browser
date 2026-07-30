/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.helpers

import android.os.Handler
import android.os.HandlerThread
import android.os.StrictMode
import android.util.Log
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import mozilla.components.browser.engine.gecko.await
import mozilla.components.support.webextensions.WebExtensionSupport
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.helpers.Constants.TAG
import org.mozilla.fenix.helpers.TestHelper.appContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.time.Duration.Companion.milliseconds

/**
 * Generic installer for built-in web extension fixtures used by add-on UI tests.
 *
 * Extension descriptors ([BuiltInTestExtension]) and any extension-specific readiness conditions
 * belong in the test class that uses them, not here.
 */
object TestWebExtensionInstaller {
    /**
     * Descriptor for a built-in test extension packaged in the APK assets.
     */
    data class BuiltInTestExtension(
        val name: String,
        val id: String,
        val uri: String,
    )

    private val TIMEOUT_MS = 10_000.milliseconds
    private val POLL_INTERVAL_MS = 250.milliseconds

    /**
     * Ensures the given built-in test extension is installed and waits until [readyState] returns
     * a non-null description for the extension.
     *
     * @param extension Descriptor of the built-in test extension to install or reuse.
     * @param readyState Callback that returns a non-null readiness description once the extension
     * is ready for the calling test's next step. The default only checks that the extension is
     * present in [WebExtensionSupport.installedExtensions].
     */
    fun ensureBuiltInExtensionInstalled(
        extension: BuiltInTestExtension,
        readyState: (BuiltInTestExtension) -> String? = { builtInExtension ->
            WebExtensionSupport.installedExtensions[builtInExtension.id]?.let { "installed" }
        },
    ) = runBlocking {
            val existingReadyState = readyState(extension)
            if (existingReadyState != null) {
                Log.i(
                    TAG,
                    "ensureBuiltInExtensionInstalled: Reusing ${extension.name} with $existingReadyState",
                )
                return@runBlocking
            }

            Log.i(
                TAG,
                "ensureBuiltInExtensionInstalled: Installing ${extension.name} from ${extension.uri}",
            )
            installBuiltInExtension(appContext, extension)
            waitForExtensionReady(extension, readyState)
        }

    fun uninstallExtension(extension: BuiltInTestExtension) = runBlocking {
        val installed = WebExtensionSupport.installedExtensions[extension.id]
            ?: run {
                Log.i(TAG, "uninstallExtension: ${extension.name} was not installed")
                return@runBlocking
            }
        Log.i(TAG, "uninstallExtension: Uninstalling ${extension.name}")
        onExtensionHandlerThread {
            suspendCancellableCoroutine { continuation ->
                appContext.components.core.engine.uninstallWebExtension(
                    installed,
                    onSuccess = { continuation.resume(Unit) },
                    onError = { _, t -> continuation.resumeWithException(t) },
                )
            }
        }
    }

    private suspend fun installBuiltInExtension(
        context: android.content.Context,
        extension: BuiltInTestExtension,
    ) {
        onExtensionHandlerThread {
            val previousPolicy = StrictMode.allowThreadDiskReads()
            try {
                context.components.core.geckoRuntime.webExtensionController.ensureBuiltIn(
                    extension.uri,
                    extension.id,
                ).await()
            } finally {
                StrictMode.setThreadPolicy(previousPolicy)
            }
        }
    }

    private suspend fun <T> onExtensionHandlerThread(block: suspend () -> T): T {
        val handlerThread = HandlerThread("TestWebExtensionInstaller").apply { start() }
        val handlerDispatcher = Handler(handlerThread.looper).asCoroutineDispatcher()
        return try {
            withContext(handlerDispatcher) { block() }
        } finally {
            handlerThread.quitSafely()
            handlerThread.join()
        }
    }

    private suspend fun waitForExtensionReady(
        extension: BuiltInTestExtension,
        readyState: (BuiltInTestExtension) -> String?,
    ) {
        withTimeout(TIMEOUT_MS) {
            while (true) {
                val readyDescription = readyState(extension)
                if (readyDescription != null) {
                    Log.i(
                        TAG,
                        "ensureBuiltInExtensionInstalled: ${extension.name} ready with $readyDescription",
                    )
                    return@withTimeout
                }
                delay(POLL_INTERVAL_MS)
            }
        }
    }
}
