/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.telemetry

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build
import mozilla.components.browser.state.action.AwesomeBarAction
import mozilla.components.browser.state.action.BrowserAction
import mozilla.components.browser.state.action.ContentAction
import mozilla.components.browser.state.action.DownloadAction
import mozilla.components.browser.state.action.EngineAction
import mozilla.components.browser.state.action.ExtensionsProcessAction
import mozilla.components.browser.state.action.TabListAction
import mozilla.components.browser.state.action.TranslationsAction
import mozilla.components.browser.state.selector.findTab
import mozilla.components.browser.state.selector.findTabOrCustomTab
import mozilla.components.browser.state.selector.normalTabs
import mozilla.components.browser.state.selector.privateTabs
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.SessionState
import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.concept.base.crash.CrashReporting
import mozilla.components.concept.engine.translate.TranslationOperation
import mozilla.components.lib.state.Middleware
import mozilla.components.lib.state.Store
import mozilla.components.support.base.log.logger.Logger
import mozilla.telemetry.glean.private.NoExtras
import org.mozilla.fenix.Config
import org.mozilla.fenix.GleanMetrics.Addons
import org.mozilla.fenix.GleanMetrics.Events
import org.mozilla.fenix.GleanMetrics.Metrics
import org.mozilla.fenix.GleanMetrics.Translations
import org.mozilla.fenix.GleanMetrics.Urlbar
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.components.metrics.MetricController
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.utils.Settings
import org.mozilla.fenix.GleanMetrics.EngineTab as EngineMetrics

/**
 * [Middleware] to record telemetry in response to [BrowserAction]s.
 *
 * @param context An Android [Context].
 * @param settings reference to the application [Settings].
 * @param metrics [MetricController] to pass events that have been mapped from actions.
 * @param crashReporting An instance of [CrashReporting] to report caught exceptions.
 * @param currentTimeMillis provider for the current time in milliseconds, injectable for testing.
 */
class TelemetryMiddleware(
    private val context: Context,
    private val settings: Settings,
    private val metrics: MetricController,
    private val crashReporting: CrashReporting? = null,
    private val currentTimeMillis: () -> Long = { System.currentTimeMillis() },
) : Middleware<BrowserState, BrowserAction> {

    private val logger = Logger("TelemetryMiddleware")

    // ApplicationExitInfo, which we need to distinguish user-requested exits from unexpected
    // process kills, is only available on API 30+. We skip all engine tab telemetry on older
    // versions to avoid recording false positives.
    private val androidVersionSupportsEngineTabTelemetry = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    // Tab IDs populated from TabListAction.RestoreAction (full app session restore).
    // Used to distinguish session-restored creates from content-process-kill creates.
    private val sessionRestoredTabIds = mutableSetOf<String>()

    private enum class ReloadReason(val value: String) {
        ContentProcessKill("content_process_kill"),
        AppSessionRestore("app_session_restore"),
    }

    @Suppress("TooGenericExceptionCaught", "CognitiveComplexMethod", "NestedBlockDepth", "LongMethod", "CyclomaticComplexMethod")
    override fun invoke(
        store: Store<BrowserState, BrowserAction>,
        next: (BrowserAction) -> Unit,
        action: BrowserAction,
    ) {
        // Pre process actions

        when (action) {
            is TabListAction.RestoreAction -> {
                if (androidVersionSupportsEngineTabTelemetry && !wasLastExitUserRequested()) {
                    sessionRestoredTabIds.addAll(action.tabs.map { it.state.id })
                }
            }
            is ContentAction.UpdateLoadingStateAction -> {
                store.state.findTab(action.sessionId)?.let { tab ->
                    val hasFinishedLoading = tab.content.loading && !action.loading

                    // Record UriOpened event when a non-private page finishes loading
                    if (hasFinishedLoading) {
                        Events.normalAndPrivateUriCount.add()
                    }
                }
            }
            is DownloadAction.AddDownloadAction -> { /* NOOP */ }
            is EngineAction.KillEngineSessionAction -> {
                val tab = store.state.findTabOrCustomTab(action.tabId)
                onEngineSessionKilled(store.state, tab)
            }
            is EngineAction.CreateEngineSessionAction -> {
                val tab = store.state.findTabOrCustomTab(action.tabId)
                onEngineSessionCreated(store.state, tab)
            }
            is ContentAction.CheckForFormDataExceptionAction -> {
                Events.formDataFailure.record(NoExtras())
                if (Config.channel.isNightlyOrDebug) {
                    crashReporting?.submitCaughtException(action.throwable)
                }
                return
            }
            is EngineAction.LoadUrlAction -> {
                metrics.track(Event.GrowthData.ConversionEvent3)
            }
            else -> {
                // no-op
            }
        }

        next(action)

        // Post process actions
        when (action) {
            is TabListAction.AddTabAction,
            is TabListAction.AddMultipleTabsAction,
            is TabListAction.RemoveTabAction,
            is TabListAction.RemoveAllNormalTabsAction,
            is TabListAction.RemoveAllTabsAction,
            is TabListAction.RestoreAction,
            -> {
                // Update/Persist tabs count whenever it changes
                settings.openTabsCount = store.state.normalTabs.count()
                settings.openPrivateTabsCount = store.state.privateTabs.count()
                if (store.state.normalTabs.isNotEmpty()) {
                    Metrics.hasOpenTabs.set(true)
                } else {
                    Metrics.hasOpenTabs.set(false)
                }
            }
            is ExtensionsProcessAction.EnabledAction -> {
                Addons.extensionsProcessUiRetry.add()
            }
            is ExtensionsProcessAction.DisabledAction -> {
                Addons.extensionsProcessUiDisable.add()
            }
            is AwesomeBarAction.EngagementFinished -> {
                if (action.abandoned) {
                    Urlbar.abandonment.record()
                } else {
                    Urlbar.engagement.record()
                }
            }
            is TranslationsAction.TranslateAction -> {
                Translations.translateRequested.record(
                    Translations.TranslateRequestedExtra(
                        fromLanguage = action.fromLanguage,
                        toLanguage = action.toLanguage,
                    ),
                )
            }
            is TranslationsAction.TranslateSuccessAction -> {
                if (action.operation == TranslationOperation.TRANSLATE) {
                    Translations.translateSuccess.record()
                }
            }
            is TranslationsAction.TranslateExceptionAction -> {
                if (action.operation == TranslationOperation.TRANSLATE) {
                    Translations.translateFailed.record(
                        Translations.TranslateFailedExtra(action.translationError.errorName),
                    )
                }
            }
            is TranslationsAction.SetEngineSupportedAction -> {
                if (!action.isEngineSupported) {
                    Translations.engineUnsupported.record()
                }
            }
            else -> {
                // no-op
            }
        }
    }

    /**
     * Collecting some engine-specific (GeckoView) telemetry.
     * https://github.com/mozilla-mobile/android-components/issues/9366
     */
    private fun onEngineSessionKilled(state: BrowserState, tab: SessionState?) {
        if (!androidVersionSupportsEngineTabTelemetry) return

        if (tab == null) {
            logger.debug("Could not find tab for killed engine session")
            return
        }

        val isSelected = tab.id == state.selectedTabId

        // Increment the counter of killed foreground/background tabs
        EngineMetrics.tabKilled.record(
            EngineMetrics.TabKilledExtra(
                foregroundTab = isSelected,
                appForeground = context.components.appStore.state.isForeground,
                hadFormData = tab.content.hasFormData,
            ),
        )
    }

    // When the exit buffer is empty (e.g. fresh install, or cleared after a device reboot),
    // firstOrNull() returns null and ?.reason == REASON_USER_REQUESTED evaluates to false.
    // This means we cannot confirm the exit was user-requested, so we assume it was unexpected
    // and fire the metric — a conservative default that prefers occasional false positives over
    // silently dropping genuine unexpected-kill restores.
    // Known false positive: if the user explicitly closes Firefox and the device reboots before
    // the next launch, the OS clears the exit buffer, and we lose the REASON_USER_REQUESTED signal,
    // causing us to incorrectly record an app_session_restore. These are hard to detect since the
    // evidence is gone by the time we check.
    @SuppressLint("NewApi") // Only called when supportsEngineTabTelemetry is true (API >= 30).
    private fun wasLastExitUserRequested(): Boolean =
        (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
            .getHistoricalProcessExitReasons(null, 0, 0)
            .firstOrNull { ":" !in it.processName }
            ?.reason == ApplicationExitInfo.REASON_USER_REQUESTED

    private fun computeDurationSinceLastVisible(tab: SessionState): Int {
        val lastVisibleAt = (tab as? TabSessionState)?.lastVisibleAt?.takeIf { it != 0L } ?: return -1
        val elapsed = currentTimeMillis() - lastVisibleAt
        return (elapsed / 1000L).coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
    }

    /**
     * Collecting some engine-specific (GeckoView) telemetry.
     */
    private fun onEngineSessionCreated(state: BrowserState, tab: SessionState?) {
        if (!androidVersionSupportsEngineTabTelemetry) return

        if (tab == null) {
            logger.debug("Could not find tab for created engine session")
            return
        }

        val isFromSessionRestore = sessionRestoredTabIds.remove(tab.id)
        val isFromProcessKill = state.recentlyKilledTabs.contains(tab.id)

        val reason = when {
            isFromProcessKill -> ReloadReason.ContentProcessKill
            isFromSessionRestore -> ReloadReason.AppSessionRestore
            else -> null
        }

        if (reason != null) {
            EngineMetrics.reloaded.record(
                EngineMetrics.ReloadedExtra(
                    durationSinceLastVisibleSeconds = computeDurationSinceLastVisible(tab),
                    reason = reason.value,
                ),
            )
        }
    }
}
