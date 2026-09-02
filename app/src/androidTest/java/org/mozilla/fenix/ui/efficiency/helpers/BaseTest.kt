/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.helpers

import android.os.SystemClock
import android.util.Log
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.test.espresso.Espresso
import androidx.test.espresso.IdlingResourceTimeoutException
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.base.DefaultFailureHandler
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.test.uiautomator.UiObjectNotFoundException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import leakcanary.NoLeakAssertionFailedError
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TestRule
import org.junit.runners.model.Statement
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.helpers.AppAndSystemHelper.deleteBookmarksStorage
import org.mozilla.fenix.helpers.AppAndSystemHelper.deletePinnedSitesStorage
import org.mozilla.fenix.helpers.FenixTestRule
import org.mozilla.fenix.helpers.HomeActivityIntentTestRule
import org.mozilla.fenix.helpers.IdlingResourceHelper.unregisterAllIdlingResources
import org.mozilla.fenix.helpers.TestHelper.appContext
import org.mozilla.fenix.helpers.TestHelper.exitMenu
import org.mozilla.fenix.ui.efficiency.logging.LoggingBridge
import org.mozilla.fenix.ui.efficiency.logging.TestLogging
import org.mozilla.fenix.ui.efficiency.navigation.LaunchConfig
import org.mozilla.fenix.ui.efficiency.navigation.NavigationRegistry
import org.mozilla.fenix.ui.efficiency.navigation.PageCatalog
import androidx.compose.ui.test.junit4.v2.AndroidComposeTestRule as AndroidComposeTestRuleV2

/**
 * BaseTest
 *
 * Why BaseTest wires up the structured logger:
 * - Tests should only describe "what is being tested".
 * - The harness (BaseTest/BasePage/helpers) owns "how it runs": navigation, retries, selectors,
 *   and therefore it owns observability for those behaviors.
 *
 * Why we print to stdout:
 * - Instrumentation captures System.out into logcat, which can be filtered into a clean stream.
 * - This gives us a single location for human debugging locally and in CI artifacts without
 *   requiring additional infrastructure during early iteration.
 *
 * Long-term intent:
 * - This structured log stream becomes a source-of-truth execution trace that remains useful
 *   even when tests are dynamically generated (factories, reflection, CI-driven permutations).
 * - Later we can route the same events into richer sinks (files/JSON/XML) and unify with the
 *   existing Feature.spec / factory logging pipeline.
 */
abstract class BaseTest(
    private val skipOnboarding: Boolean = true,
    private val isPageLoadTranslationsPromptEnabled: Boolean = false,
    private val isPocketEnabled: Boolean = true,
    private val isRecentlyVisitedFeatureEnabled: Boolean = true,
    private val shouldUseExpandedToolbar: Boolean = false,
    private val isTabStripEnabled: Boolean = false,
) {

    // Default launch built from the constructor args (back-compat for every existing subclass).
    private val defaultLaunchConfig = LaunchConfig(
        skipOnboarding = skipOnboarding,
        isPageLoadTranslationsPromptEnabled = isPageLoadTranslationsPromptEnabled,
        isPocketEnabled = isPocketEnabled,
        isRecentlyVisitedFeatureEnabled = isRecentlyVisitedFeatureEnabled,
        shouldUseExpandedToolbar = shouldUseExpandedToolbar,
        isTabStripEnabled = isTabStripEnabled,
    )

    /** Override to vary the launch per run/case (e.g. the reachability shard uses the case's config). */
    protected open fun launchConfig(): LaunchConfig = defaultLaunchConfig

    @get:Rule(order = 0)
    val fenixTestRule: FenixTestRule = FenixTestRule()

    // Backing property so composeRule can be re-created fresh on each retry attempt.
    // AndroidComposeTestRule holds a TestScope that can only be entered once — re-creating
    // the rule per attempt ensures a clean TestScope every time.
    private var _composeRule: AndroidComposeTestRule<HomeActivityIntentTestRule, *>? = null
    val composeRule get() = _composeRule!!

    // Combines retry and compose rule creation into a single rule. We cannot reuse
    // RetryTestRule here because the retry logic must own the creation of composeRule —
    // a separate RetryTestRule has no way to replace an already-constructed @get:Rule.
    // Re-creates composeRule on each attempt so its internal TestScope is never re-entered,
    // which would otherwise throw:
    // "Only a single call to `runTest` can be performed during one test."
    @get:Rule(order = 1)
    val retryWithCompose: TestRule = TestRule { base, description ->
        object : Statement() {
            override fun evaluate() {
                repeat(1 + MAX_RETRIES) { attempt ->
                    val cfg = launchConfig()
                    _composeRule = AndroidComposeTestRuleV2(
                        HomeActivityIntentTestRule(
                            skipOnboarding = cfg.skipOnboarding,
                            isPageLoadTranslationsPromptEnabled = cfg.isPageLoadTranslationsPromptEnabled,
                            isPocketEnabled = cfg.isPocketEnabled,
                            isRecentlyVisitedFeatureEnabled = cfg.isRecentlyVisitedFeatureEnabled,
                            shouldUseExpandedToolbar = cfg.shouldUseExpandedToolbar,
                            isTabStripEnabled = cfg.isTabStripEnabled,
                        ),
                    ) { it.activity }
                    try {
                        Log.i("BaseTest", "RetryTestRule: Started try #${attempt + 1}.")
                        runBlocking {
                            deleteBookmarksStorage()
                            deletePinnedSitesStorage()
                            withContext(Dispatchers.IO) {
                                appContext.components.core.sessionStorage.clear()
                                // Clear saved autofill addresses so every attempt starts from a clean
                                // screen. A leftover address (e.g. from a failed first attempt) changes
                                // the Autofill settings layout and can push "Add address" off-screen,
                                // turning a one-off failure into a retry that fails differently.
                                // Best-effort: a storage error must not fail the attempt on its own, but
                                // it is logged — a silent failure here looks identical to a state leak.
                                runCatching {
                                    val autofill = appContext.components.core.autofillStorage
                                    autofill.getAllAddresses().forEach { autofill.deleteAddress(it.guid) }
                                    // Same for cards: a leftover card replaces "Add card" with "Manage
                                    // cards" on the Autofill screen, so a retry of a card test starts on
                                    // a different screen than the first attempt did.
                                    autofill.getAllCreditCards().forEach { autofill.deleteCreditCard(it.guid) }
                                }.onFailure {
                                    Log.i("BaseTest", "RetryTestRule: autofill clear failed: ${it.message}")
                                }
                                // Clear saved logins for the same reason (and so a retry doesn't inherit
                                // logins the previous attempt saved — a re-submit of the same credentials
                                // shows no save prompt, which reads as a spurious failure).
                                runCatching {
                                    appContext.components.core.passwordsStorage.wipeLocal()
                                }.onFailure {
                                    Log.i("BaseTest", "RetryTestRule: logins clear failed: ${it.message}")
                                }
                            }
                        }
                        appContext.components.useCases.tabsUseCases.removeAllTabs()
                        _composeRule!!.apply(base, description).evaluate()
                        return // success, exit early
                    } catch (t: NoLeakAssertionFailedError) {
                        Log.i("BaseTest", "RetryTestRule: NoLeakAssertionFailedError caught, not retrying.")
                        cleanup(removeTabs = true)
                        throw t
                    } catch (t: Throwable) {
                        if (!t.isRetryable() || attempt >= MAX_RETRIES) throw t
                        Log.i("BaseTest", "RetryTestRule: ${t::class.simpleName} caught, retrying.")
                        cleanup(removeTabs = true)
                        finishLeftoverActivities()
                    }
                }
            }
        }
    }

    // get() ensures this always delegates to the current composeRule instance,
    // not a stale one captured at class construction time.
    protected val on: PageContext get() = PageContext(composeRule)

    /**
     * Reporter lifecycle:
     * - Create once if missing.
     * - Reset at the start of every test so summaries and counters are per-test.
     *
     * Why reset instead of re-creating:
     * - Keeps construction cheap and avoids wiring churn if we later attach file sinks.
     * - Makes it easier to evolve toward a more formal "test context" object later.
     */
    @Before
    fun setUp() {
        // Disable Espresso's screenshot-on-failure locally.
        //
        // Why: Espresso's DefaultFailureHandler captures a screenshot when an interaction fails.
        // On a debug build on a REAL device, that bitmap capture (DeviceCapture ->
        // takeScreenshotOnNextFrame) trips Fenix's StrictMode penaltyDeath and KILLS the process
        // before the real assertion error is reported — so the genuine failure is swallowed and the
        // test looks like an opaque crash. It does not reproduce on Firebase (no penaltyDeath /
        // different capture path), which is why these only fail locally. Installing the default
        // handler with captureScreenshotOnFailure = false keeps failure messages intact without the
        // fatal screenshot. We still get the real error (and our own ScreenDump) for debugging.
        // Second arg is captureScreenshotOnFailure = false.
        Espresso.setFailureHandler(DefaultFailureHandler(appContext, false))

        if (TestLogging.reporter == null) {
            TestLogging.reporter = LoggingBridge.createReporter()
        }
        TestLogging.reporter?.reset()
        if (java.lang.Boolean.getBoolean("logNavigationSummary")) {
            NavigationRegistry.logPathSummary()
        }
        if (java.lang.Boolean.getBoolean("logPageCatalog")) {
            val pages = PageCatalog.discoverPages()

            Log.i("PageCatalog", "Discovered ${pages.size} pages from PageContext")

            pages.forEachIndexed { index, pageRef ->
                val page = pageRef.getter(on)

                Log.i(
                    "PageCatalog",
                    "   ${index + 1}. ${page.pageName} (property=${pageRef.propertyName})",
                )
            }
        }

        // State tracker is a lightweight breadcrumb used by navigation helpers.
        // Source-of-truth remains selector-based verification (mozIsOnPageNow / mozWaitForPageToLoad).
        PageStateTracker.currentPageName = "AppEntry"
    }

    /**
     * Print a short per-test summary to stdout.
     *
     * Why:
     * - Helps spot where time was spent and which layer failed most often (STEP/CMD/LOC).
     * - Provides immediate value in CI where you may only have the log artifact.
     *
     * Note:
     * - "Wall time" is overall elapsed real-world time for the test (start -> end).
     * - STEP/CMD/LOC totals sum only the instrumented scopes.
     */
    @After
    fun tearDown() {
        appContext.components.useCases.tabsUseCases.removeAllTabs()
    }

    @After
    fun tearDownLogging() {
        try {
            TestLogging.reporter?.printSummary()
        } catch (_: Throwable) {
            // Logging must never fail a test.
        }
    }

    private companion object {
        /**
         * Number of retry attempts to do, if the test fails.
         */
        const val MAX_RETRIES = 1
    }
}

/**
 * Finish whatever the failed attempt left running, and wait for it to actually be gone.
 *
 * The next attempt launches a fresh HomeActivity, and that launch is what breaks if the previous
 * attempt's instance is still alive: HomeActivity is launchMode="singleTask", so the intent is
 * delivered to the existing instance instead of creating one, MonitoringInstrumentation never sees a
 * newly launched activity reach RESUMED, and the attempt dies after 45s on "Could not launch intent
 * ... HomeActivity". That error names HomeActivity, so it hides whatever actually failed first.
 *
 * The activity rule's own teardown is not enough to rely on here — the retry runs immediately after
 * the failure, and the previous attempt's HomeActivity has been observed still RESUMED at that point.
 * Best-effort: this must never turn a retryable failure into a different one, so errors are logged
 * and swallowed, and the wait is bounded.
 */
private fun finishLeftoverActivities() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val deadline = SystemClock.uptimeMillis() + LEFTOVER_ACTIVITY_TIMEOUT
    while (SystemClock.uptimeMillis() < deadline) {
        var remaining = emptyList<String>()
        runCatching {
            instrumentation.runOnMainSync {
                val monitor = ActivityLifecycleMonitorRegistry.getInstance()
                val live = Stage.values()
                    .filter { it != Stage.DESTROYED }
                    .flatMap { monitor.getActivitiesInStage(it) }
                    .distinct()
                remaining = live.map { it.javaClass.simpleName }
                live.forEach { it.finish() }
            }
        }.onFailure {
            Log.i("BaseTest", "RetryTestRule: could not inspect leftover activities: ${it.message}")
            return
        }
        if (remaining.isEmpty()) return
        Log.i("BaseTest", "RetryTestRule: finishing leftover activities: $remaining")
        SystemClock.sleep(LEFTOVER_ACTIVITY_POLL)
    }
    Log.i("BaseTest", "RetryTestRule: leftover activities outlived ${LEFTOVER_ACTIVITY_TIMEOUT}ms")
}

private const val LEFTOVER_ACTIVITY_TIMEOUT = 5_000L
private const val LEFTOVER_ACTIVITY_POLL = 200L

private fun cleanup(removeTabs: Boolean = false) {
    unregisterAllIdlingResources()
    if (removeTabs) {
        appContext.components.useCases.tabsUseCases.removeAllTabs()
    }
    exitMenu()
}

private fun Throwable.isRetryable(): Boolean = when (this) {
    is AssertionError,
    is junit.framework.AssertionFailedError,
    is UiObjectNotFoundException,
    is NoMatchingViewException,
    is IdlingResourceTimeoutException,
    is RuntimeException,
    is NullPointerException,
    -> true
    else -> false
}
