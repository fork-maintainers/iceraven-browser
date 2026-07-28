/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.webcompat

import kotlinx.coroutines.suspendCancellableCoroutine
import mozilla.components.browser.state.selector.selectedTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.support.base.log.logger.Logger
import org.json.JSONObject
import org.mozilla.fenix.webcompat.store.WebCompatReporterState
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Interface for sending a broken site report via Glean.
 */
interface GleanBrokenSiteReportSender {

    /**
     * Send a broken site report via Glean.
     */
    suspend fun sendGleanBrokenSiteReport(
        details: JSONObject?,
        description: String?,
        reason: WebCompatReporterState.BrokenSiteReason,
        url: String,
        sendTabSpecificInfo: Boolean,
        sendBlockedUrls: Boolean,
    )
}

/**
 * The default implementation of [GleanBrokenSiteReportSender].
 */
class DefaultGleanBrokenSiteReportSender(
    private val browserStore: BrowserStore,
) : GleanBrokenSiteReportSender {

    private val logger = Logger("DefaultGleanBrokenSiteReportSender")

    override suspend fun sendGleanBrokenSiteReport(
        details: JSONObject?,
        description: String?,
        reason: WebCompatReporterState.BrokenSiteReason,
        url: String,
        sendTabSpecificInfo: Boolean,
        sendBlockedUrls: Boolean,
    ) {
        val session = browserStore.state.selectedTab?.engineState?.engineSession
            ?: return

        return suspendCancellableCoroutine { continuation ->
            session.sendGleanBrokenSiteReport(
                details = details,
                description = description,
                reason = reason.name.lowercase(),
                url = url,
                sendTabSpecificInfo = sendTabSpecificInfo,
                sendBlockedUrls = sendBlockedUrls,
                onResult = {
                    if (continuation.isActive) {
                        logger.debug("SendGleanBrokenSiteReport succeeded")
                        continuation.resume(Unit)
                    }
                },
                onException = {
                    logger.error("Error with SendGleanBrokenSiteReport", it)
                    if (continuation.isActive) {
                        continuation.resumeWithException(it)
                    }
                },
            )
        }
    }
}
