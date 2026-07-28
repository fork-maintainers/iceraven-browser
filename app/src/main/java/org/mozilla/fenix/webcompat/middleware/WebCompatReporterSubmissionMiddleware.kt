/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.webcompat.middleware

import androidx.annotation.VisibleForTesting
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import mozilla.components.lib.state.Middleware
import mozilla.components.lib.state.Store
import org.mozilla.fenix.GleanMetrics.BrokenSiteReport
import org.mozilla.fenix.GleanMetrics.BrokenSiteReportBrowserInfo
import org.mozilla.fenix.GleanMetrics.BrokenSiteReportBrowserInfoApp
import org.mozilla.fenix.GleanMetrics.BrokenSiteReportBrowserInfoGraphics
import org.mozilla.fenix.GleanMetrics.BrokenSiteReportBrowserInfoPrefs
import org.mozilla.fenix.GleanMetrics.BrokenSiteReportBrowserInfoSystem
import org.mozilla.fenix.GleanMetrics.BrokenSiteReportTabInfo
import org.mozilla.fenix.GleanMetrics.BrokenSiteReportTabInfoAntitracking
import org.mozilla.fenix.GleanMetrics.BrokenSiteReportTabInfoFrameworks
import org.mozilla.fenix.GleanMetrics.Pings
import org.mozilla.fenix.components.AppStore
import org.mozilla.fenix.components.appstate.AppAction
import org.mozilla.fenix.webcompat.middleware.WebCompatInfoDto.Companion.addWebCompatInfo
import org.mozilla.fenix.webcompat.store.PreviewReporterItem
import org.mozilla.fenix.webcompat.store.WebCompatReporterAction
import org.mozilla.fenix.webcompat.store.WebCompatReporterState

/**
 * [Middleware] that reacts to submission related [WebCompatReporterAction]s.
 *
 * @param appStore [AppStore] used to dispatch [AppAction]s.
 * @param webCompatReporterRetrievalService The service that handles submission requests.
 * @param scope The [CoroutineScope] for launching coroutines.
 * @param nimbusExperimentsProvider A [NimbusExperimentsProvider] used to get active experiments.
 */
class WebCompatReporterSubmissionMiddleware(
    private val appStore: AppStore,
    private val webCompatReporterRetrievalService: WebCompatReporterRetrievalService,
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
        val webCompatInfo = webCompatReporterRetrievalService.retrieveInfo()

        webCompatInfo?.let {
            val tabUrlHost = webCompatInfo.tabInfo.url.value.toUri().host
            val enteredUrlHost = store.state.enteredUrl.toUri().host
            val sendTabSpecificData = tabUrlHost == enteredUrlHost

            if (sendTabSpecificData) {
                setTabInfoMetrics(tabInfo = webCompatInfo.tabInfo)
                setTabFrameworksMetrics(frameworks = webCompatInfo.frameworks)
            }

            setTabAntiTrackingMetrics(
                antiTracking = webCompatInfo.antitracking,
                sendBlockedUrls = store.state.includeEtpBlockedUrls,
                sendTabSpecificData = sendTabSpecificData,
            )

            setAppMetrics(appInfo = webCompatInfo.app)
            setBrowserInfoMetrics(browserInfo = webCompatInfo.browserInfo)
            setGraphicsMetrics(graphicsInfo = webCompatInfo.graphics)
            setPrefsMetrics(prefsInfo = webCompatInfo.prefs)
            setSystemMetrics(systemInfo = webCompatInfo.system)
        }
        setUrlMetrics(url = store.state.enteredUrl)
        setReasonMetrics(reason = store.state.reason)
        setDescriptionMetrics(description = store.state.problemDescription)
        setExperimentMetrics()

        Pings.brokenSiteReport.submit()
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

    private fun generatePreviewJSON(
        state: WebCompatReporterState,
        webCompatInfo: WebCompatInfoDto?,
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

        if (webCompatInfo == null) {
            return preview
        }

        val tabUrlHost = webCompatInfo.tabInfo.url.value.toUri().host
        val enteredUrlHost = state.enteredUrl.toUri().host
        val noTabSpecificData = tabUrlHost != enteredUrlHost

        val webCompatPreview = preview.addWebCompatInfo(webCompatInfo, noTabSpecificData)

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

    private fun setTabAntiTrackingMetrics(
        antiTracking: WebCompatInfoDto.WebCompatAntiTrackingDto,
        sendBlockedUrls: Boolean,
        sendTabSpecificData: Boolean,
    ) {
        BrokenSiteReportTabInfoAntitracking.blockList.set(antiTracking.blockList.value)
        BrokenSiteReportTabInfoAntitracking.etpCategory.set(antiTracking.etpCategory.value)
        if (sendTabSpecificData) {
            if (sendBlockedUrls) {
                BrokenSiteReportTabInfoAntitracking.blockedOrigins.set(antiTracking.blockedOrigins.value)
            }
            BrokenSiteReportTabInfoAntitracking.btpHasPurgedSite.set(antiTracking.btpHasPurgedSite.value)
            BrokenSiteReportTabInfoAntitracking.hasMixedActiveContentBlocked.set(
                antiTracking.hasMixedActiveContentBlocked.value,
            )
            BrokenSiteReportTabInfoAntitracking.hasMixedDisplayContentBlocked.set(
                antiTracking.hasMixedDisplayContentBlocked.value,
            )
            BrokenSiteReportTabInfoAntitracking.hasTrackingContentBlocked.set(
                antiTracking.hasTrackingContentBlocked.value,
            )
            BrokenSiteReportTabInfoAntitracking.isPrivateBrowsing.set(antiTracking.isPrivateBrowsing.value)
        }
    }

    private fun setAppMetrics(appInfo: WebCompatInfoDto.WebCompatAppDto) {
        BrokenSiteReportBrowserInfoApp.defaultUseragentString.set(appInfo.defaultUseragentString.value)
        BrokenSiteReportBrowserInfoApp.defaultLocales.set(appInfo.defaultLocales.value)
        BrokenSiteReportBrowserInfoApp.fissionEnabled.set(appInfo.fissionEnabled.value)
    }

    private fun setBrowserInfoMetrics(browserInfo: WebCompatInfoDto.WebCompatBrowserInfoDto) {
        val addons = BrokenSiteReportBrowserInfo.AddonsObject()
        for (addon in browserInfo.addons.value) {
            addons.add(
                BrokenSiteReportBrowserInfo.AddonsObjectItem(
                    id = addon.id,
                    name = addon.name,
                    temporary = addon.temporary,
                    version = addon.version,
                ),
            )
        }
        BrokenSiteReportBrowserInfo.addons.set(addons)
    }

    private fun setGraphicsMetrics(graphicsInfo: WebCompatInfoDto.WebCompatGraphicsDto) {
        graphicsInfo.devices.value?.let { devices ->
            BrokenSiteReportBrowserInfoGraphics.devicesJson.set(devices.toString())
        }

        BrokenSiteReportBrowserInfoGraphics.devicePixelRatio.set(graphicsInfo.devicePixelRatio.value.toString())

        BrokenSiteReportBrowserInfoGraphics.driversJson.set(graphicsInfo.drivers.value.toString())

        graphicsInfo.features.value?.let { features ->
            BrokenSiteReportBrowserInfoGraphics.featuresJson.set(features.toString())
        }

        graphicsInfo.hasTouchScreen.value?.let { hasTouchScreen ->
            BrokenSiteReportBrowserInfoGraphics.hasTouchScreen.set(hasTouchScreen)
        }

        graphicsInfo.monitors.value?.let { monitors ->
            BrokenSiteReportBrowserInfoGraphics.monitorsJson.set(monitors.toString())
        }
    }

    @Suppress("MaxLineLength")
    private fun setPrefsMetrics(prefsInfo: WebCompatInfoDto.WebCompatPrefsDto) {
        BrokenSiteReportBrowserInfoPrefs.cookieBehavior.set(prefsInfo.cookieBehavior.value)
        BrokenSiteReportBrowserInfoPrefs.forcedAcceleratedLayers.set(prefsInfo.forcedAcceleratedLayers.value)
        BrokenSiteReportBrowserInfoPrefs.globalPrivacyControlEnabled.set(prefsInfo.globalPrivacyControlEnabled.value)
        BrokenSiteReportBrowserInfoPrefs.installtriggerEnabled.set(prefsInfo.installtriggerEnabled.value)
        BrokenSiteReportBrowserInfoPrefs.opaqueResponseBlocking.set(prefsInfo.opaqueResponseBlocking.value)
        BrokenSiteReportBrowserInfoPrefs.resistFingerprintingEnabled.set(prefsInfo.resistFingerprintingEnabled.value)
        BrokenSiteReportBrowserInfoPrefs.softwareWebrender.set(prefsInfo.softwareWebrender.value)
        BrokenSiteReportBrowserInfoPrefs.thirdPartyCookieBlockingEnabled.set(prefsInfo.thirdPartyCookieBlockingEnabled.value)
        BrokenSiteReportBrowserInfoPrefs.thirdPartyCookieBlockingEnabledInPbm.set(prefsInfo.thirdPartyCookieBlockingEnabledInPbm.value)
    }

    private fun setSystemMetrics(systemInfo: WebCompatInfoDto.WebCompatSystemDto) {
        BrokenSiteReportBrowserInfoSystem.memory.set(systemInfo.memory.value)
        BrokenSiteReportBrowserInfoSystem.isTablet.set(systemInfo.isTablet.value)
    }

    private fun setTabFrameworksMetrics(frameworks: WebCompatInfoDto.WebCompatFrameworksDto) {
        BrokenSiteReportTabInfoFrameworks.fastclick.set(frameworks.fastclick.value)
        BrokenSiteReportTabInfoFrameworks.marfeel.set(frameworks.marfeel.value)
        BrokenSiteReportTabInfoFrameworks.mobify.set(frameworks.mobify.value)
    }

    private fun setTabInfoMetrics(tabInfo: WebCompatInfoDto.WebCompatTabInfoDto) {
        BrokenSiteReportTabInfo.languages.set(tabInfo.languages.value)
        BrokenSiteReportTabInfo.useragentString.set(tabInfo.useragentString.value)
    }

    private fun setUrlMetrics(url: String) {
        BrokenSiteReport.url.set(url)
    }

    private fun setReasonMetrics(reason: WebCompatReporterState.BrokenSiteReason?) {
        reason?.let {
            BrokenSiteReport.breakageCategory.set(reason.name.lowercase())
        }
    }

    private fun setDescriptionMetrics(description: String) {
        BrokenSiteReport.description.set(description)
    }

    private fun setExperimentMetrics() {
        val items = mutableListOf<BrokenSiteReportBrowserInfo.ExperimentsObjectItem>()
        nimbusExperimentsProvider.activeExperiments.mapTo(items) { experiment ->
            BrokenSiteReportBrowserInfo.ExperimentsObjectItem(
                branch = nimbusExperimentsProvider.getExperimentBranch(experiment.slug),
                slug = experiment.slug,
                kind = "nimbusExperiment",
            )
        }

        BrokenSiteReportBrowserInfo.experiments.set(
            BrokenSiteReportBrowserInfo.ExperimentsObject(items),
        )
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
