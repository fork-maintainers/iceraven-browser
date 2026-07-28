/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.webcompat.middleware

import kotlinx.coroutines.suspendCancellableCoroutine
import mozilla.components.browser.state.selector.selectedTab
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.support.base.log.logger.Logger
import org.json.JSONObject
import kotlin.coroutines.resume

/**
 * Service that handles the submission requests for the report broken site feature.
 */
interface WebCompatReporterRetrievalService {

    /**
     * Returns [JSONObject] or null if the services fails to retrieve the data.
     */
    suspend fun retrieveInfo(): JSONObject?
}

/**
 * The default implementation of [WebCompatReporterRetrievalService].
 *
 * @param browserStore [BrowserStore] used to access [BrowserState].
 */
class DefaultWebCompatReporterRetrievalService(
    private val browserStore: BrowserStore,
) : WebCompatReporterRetrievalService {

    private val logger = Logger("DefaultWebCompatReporterRetrievalService")

    override suspend fun retrieveInfo(): JSONObject? {
        val session = browserStore.state.selectedTab?.engineState?.engineSession
            ?: return null

        return suspendCancellableCoroutine { continuation ->
            session.getBrokenSiteReport(
                onResult = { details ->
                    if (continuation.isActive) {
                        continuation.resume(details)
                    }
                },
                onException = { exception ->
                    logger.error("Error retrieving web compat info from engine", exception)
                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                },
            )
        }
    }
}
