/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.webcompat

import mozilla.components.concept.engine.EngineSession
import mozilla.components.support.base.log.logger.Logger
import org.json.JSONObject
import org.mozilla.fenix.webcompat.middleware.WebCompatReporterRetrievalService
import org.mozilla.fenix.webcompat.store.WebCompatReporterState

/**
 * Interface for sending WebCompat info to webcompat.com.
 */
interface WebCompatReporterMoreInfoSender {

    /**
     * Send the WebCompat info to webcompat.com.
     */
    suspend fun sendMoreWebCompatInfo(
        reason: WebCompatReporterState.BrokenSiteReason?,
        problemDescription: String?,
        enteredUrl: String?,
        tabUrl: String?,
        engineSession: EngineSession?,
    )
}

/**
 * The default implementation of [WebCompatReporterMoreInfoSender].
 *
 * @param webCompatReporterRetrievalService The service used to retrieve WebCompat info.
 */
class DefaultWebCompatReporterMoreInfoSender(
    private val webCompatReporterRetrievalService: WebCompatReporterRetrievalService,
) : WebCompatReporterMoreInfoSender {

    private val logger = Logger("DefaultWebCompatReporterMoreInfoSender")

    override suspend fun sendMoreWebCompatInfo(
        reason: WebCompatReporterState.BrokenSiteReason?,
        problemDescription: String?,
        enteredUrl: String?,
        tabUrl: String?,
        engineSession: EngineSession?,
    ) {
        val webCompatInfo = webCompatReporterRetrievalService.retrieveInfo()
        webCompatInfo?.let {
            val info = JSONObject().apply {
                reason?.let {
                    put("reason", reason)
                }
                problemDescription?.let {
                    put("description", problemDescription)
                }
                put("endpointUrl", "https://webcompat.com/issues/new")

                if (enteredUrl == null && tabUrl != null) {
                    put("reportUrl", tabUrl)
                } else if (enteredUrl != null) {
                    put("reportUrl", enteredUrl)
                }

                put(
                    "reporterConfig",
                    JSONObject().apply {
                        put("src", "android-components-reporter")
                        put("utm_campaign", "report-site-issue-button")
                        put("utm_source", "android-components-reporter")
                    },
                )
                put("webcompatInfo", webCompatInfo)
            }

            engineSession?.sendMoreWebCompatInfo(
                info = info,
                onResult = {
                    logger.debug("SendMoreWebCompatInfo succeeded")
                },
                onException = {
                    logger.error("Error with SendMoreWebCompatInfo", it)
                },
            )
        }
    }
}
