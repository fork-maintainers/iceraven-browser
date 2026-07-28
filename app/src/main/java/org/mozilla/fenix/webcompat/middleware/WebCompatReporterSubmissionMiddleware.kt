/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.webcompat.middleware

import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import mozilla.components.lib.state.Middleware
import mozilla.components.lib.state.Store
import mozilla.components.support.ktx.kotlin.tryGetHostFromUrl
import org.json.JSONArray
import org.json.JSONObject
import org.mozilla.fenix.components.AppStore
import org.mozilla.fenix.components.appstate.AppAction
import org.mozilla.fenix.webcompat.GleanBrokenSiteReportSender
import org.mozilla.fenix.webcompat.store.PreviewReporterItem
import org.mozilla.fenix.webcompat.store.WebCompatReporterAction
import org.mozilla.fenix.webcompat.store.WebCompatReporterState

/**
 * [Middleware] that reacts to submission related [WebCompatReporterAction]s.
 *
 * @param appStore [AppStore] used to dispatch [AppAction]s.
 * @param webCompatReporterRetrievalService The service that handles submission requests.
 * @param gleanBrokenSiteReportSender The service that handles sending reports via Glean.
 * @param scope The [CoroutineScope] for launching coroutines.
 * @param nimbusExperimentsProvider A [NimbusExperimentsProvider] used to get active experiments.
 */
class WebCompatReporterSubmissionMiddleware(
    private val appStore: AppStore,
    private val webCompatReporterRetrievalService: WebCompatReporterRetrievalService,
    private val gleanBrokenSiteReportSender: GleanBrokenSiteReportSender,
    private val scope: CoroutineScope,
    private val nimbusExperimentsProvider: NimbusExperimentsProvider,
) : Middleware<WebCompatReporterState, WebCompatReporterAction> {

    override fun invoke(
        store: Store<WebCompatReporterState, WebCompatReporterAction>,
        next: (WebCompatReporterAction) -> Unit,
        action: WebCompatReporterAction,
    ) {
        next(action)

        when (action) {
            is WebCompatReporterAction.SendReportClicked -> {
                scope.launch {
                    handleSendReport(store)
                }
            }
            is WebCompatReporterAction.OpenPreviewClicked -> {
                scope.launch {
                    handleOpenPreviewClicked(store)
                }
            }
            else -> {}
        }
    }

    private suspend fun handleSendReport(store: Store<WebCompatReporterState, WebCompatReporterAction>) {
        val webCompatJSON = webCompatReporterRetrievalService.retrieveInfo()
        val sendTabSpecificInfo = checkReportedURLHostMatchesTab(store.state, webCompatJSON)

        val finalDetails = webCompatJSON?.addActiveExperimentsToWebCompatInfo(nimbusExperimentsProvider)

        gleanBrokenSiteReportSender.sendGleanBrokenSiteReport(
            details = finalDetails,
            description = store.state.problemDescription,
            reason = store.state.reason ?: WebCompatReporterState.BrokenSiteReason.Other,
            url = store.state.enteredUrl,
            sendTabSpecificInfo = sendTabSpecificInfo,
            sendBlockedUrls = store.state.includeEtpBlockedUrls,
        )

        store.dispatch(WebCompatReporterAction.ReportSubmitted)
        appStore.dispatch(AppAction.WebCompatAction.WebCompatReportSent)
    }

    private suspend fun handleOpenPreviewClicked(
        store: Store<WebCompatReporterState, WebCompatReporterAction>,
    ) {
        val webCompatInfo = webCompatReporterRetrievalService.retrieveInfo()

        val webCompatJSON = generatePreviewJSON(store.state, webCompatInfo)
        store.dispatch(
            WebCompatReporterAction.PreviewItemsUpdated(
                parseWebCompatPreviewJson(webCompatJSON),
            ),
        )
    }

    private fun checkReportedURLHostMatchesTab(
        state: WebCompatReporterState,
        webCompatJSON: JSONObject?,
    ): Boolean {
        val tabUrl = webCompatJSON?.getJSONObject("tabInfo")?.getJSONObject("url")
        val tabUrlHost = tabUrl?.getString("value")?.tryGetHostFromUrl()
        val enteredUrlHost = state.enteredUrl.tryGetHostFromUrl()
        return tabUrlHost == enteredUrlHost
    }

    private fun generatePreviewJSON(
        state: WebCompatReporterState,
        webCompatJSON: JSONObject?,
    ): JsonObject {
        val preview = buildJsonObject {
            put(
                "basic",
                buildJsonObject {
                    put("description", state.problemDescription)
                    put("reason", state.reason?.name)
                    put("url", state.enteredUrl)
                },
            )
        }

        if (webCompatJSON == null) {
            return preview
        }

        val noTabSpecificData = !checkReportedURLHostMatchesTab(state, webCompatJSON)
        val webCompatPreview = preview.addWebCompatInfo(webCompatJSON, noTabSpecificData)

        if (state.includeEtpBlockedUrls) {
            return webCompatPreview
        }

        return webCompatPreview.withoutNestedKey("antitracking", "blockedOrigins")
    }

    private fun JsonObject.withoutNestedKey(
        groupName: String,
        key: String,
    ): JsonObject {
        val values = this.mapValues { (name, element) ->
            if (name != groupName) {
                element
            } else {
                JsonObject(
                    element.jsonObject
                        .filterKeys { itemName -> itemName != key },
                )
            }
        }

        return JsonObject(values)
    }

    companion object {
        /**
         * Returns the given WebCompat JSON object with the active Nimbus experiments updated to match the ones specified in the given provider.
         *
         * @param nimbusExperimentsProvider A [NimbusExperimentsProvider] used to get active experiments.
         */
        @Suppress("MaxLineLength")
        fun JSONObject.addActiveExperimentsToWebCompatInfo(nimbusExperimentsProvider: NimbusExperimentsProvider): JSONObject {
            val experiments = JSONArray()
            for (experiment in nimbusExperimentsProvider.activeExperiments) {
                experiments.put(
                    JSONObject().apply {
                        put("branch", nimbusExperimentsProvider.getExperimentBranch(experiment.slug))
                        put("kind", "nimbusExperiment")
                        put("slug", experiment.slug)
                    },
                )
            }

            return this.apply {
                put(
                    "browserInfo",
                    optJSONObject("browserInfo")?.apply {
                        put(
                            "experiments",
                            optJSONObject("experiments")?.apply {
                                put("value", experiments)
                            },
                        )
                    },
                )
            }
        }

        /**
         * Adds a WebCompat JSONObject's contents to a JSON object meant for Report Broken Site preview feature.
         *
         * @param webCompatJSON the WebCompat JSONObject to add
         * @param noTabSpecificData whether or not to avoid tab-specific data
         */
        @Suppress("MaxLineLength")
        fun JsonObject.addWebCompatInfo(
            webCompatJSON: JSONObject?,
            noTabSpecificData: Boolean = false,
        ): JsonObject {
            if (webCompatJSON == null) {
              return this
            }

            // Filter out the screenshot and other not-to-be-previewed data, as well as
            // any data which is tab-specific if the user has changed the URL enough to
            // make it unlikely to be the URL for the tab we recorded.
            val webCompatJson = Json.parseToJsonElement(webCompatJSON.toString()).jsonObject

            val webCompatPreview = webCompatJson.mapNotNull { (groupName, groupElement) ->
                    val items = groupElement.jsonObject
                    if (noTabSpecificData && items["isTabSpecific"]?.jsonPrimitive?.booleanOrNull == true) {
                        return@mapNotNull null
                    }

                    val values = items
                        .filterKeys { it != "isTabSpecific" }
                        .filterNot { (_, itemElement) ->
                            val item = itemElement.jsonObject
                            val doNotPreview = item["doNotPreview"]?.jsonPrimitive?.booleanOrNull == true
                            doNotPreview || (noTabSpecificData && item["isTabSpecific"]?.jsonPrimitive?.booleanOrNull == true)
                        }
                        .mapValues { (_, itemElement) ->
                            itemElement.jsonObject["value"] ?: JsonNull
                        }

                    groupName to JsonObject(values)
                }.toMap()

            return JsonObject(this + webCompatPreview)
        }
    }

    /**
     * Dynamically parses a [JsonObject] into a list of [PreviewReporterItem].
     * It iterates through each top-level key (e.g., "basic", "app") and
     * collects its nested fields as a map of String to String.
     */
    @VisibleForTesting
    internal fun parseWebCompatPreviewJson(webCompatJSON: JsonObject): List<PreviewReporterItem> {
        return webCompatJSON.mapNotNull { (title, element) ->
            val sectionObject = element as? JsonObject ?: return@mapNotNull null
            val data = sectionObject.mapValues { (_, value) ->
                if (value is JsonPrimitive) {
                    value.content
                } else {
                    value.toString()
                }
            }

            PreviewReporterItem(title, data)
        }
    }
}
