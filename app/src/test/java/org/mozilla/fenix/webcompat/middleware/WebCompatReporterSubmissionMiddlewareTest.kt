/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.webcompat.middleware

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.createTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.EngineSession
import mozilla.components.support.test.middleware.CaptureActionsMiddleware
import mozilla.components.support.test.robolectric.testContext
import mozilla.telemetry.glean.private.NoReasonCodes
import mozilla.telemetry.glean.private.PingType
import mozilla.telemetry.glean.testing.GleanTestRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.experiments.nimbus.internal.EnrolledExperiment
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
import org.mozilla.fenix.webcompat.WebCompatReporterMoreInfoSender
import org.mozilla.fenix.webcompat.fake.FakeWebCompatReporterMoreInfoSender
import org.mozilla.fenix.webcompat.middleware.WebCompatInfoDto.Companion.addWebCompatInfo
import org.mozilla.fenix.webcompat.store.WebCompatReporterAction
import org.mozilla.fenix.webcompat.store.WebCompatReporterState
import org.mozilla.fenix.webcompat.store.WebCompatReporterStore
import org.mozilla.fenix.webcompat.testdata.WebCompatTestData
import kotlin.test.assertNotNull

@RunWith(AndroidJUnit4::class) // for GleanTestRule
class WebCompatReporterSubmissionMiddlewareTest {
    private val appStore: AppStore = mockk(relaxed = true)

    @get:Rule
    val gleanTestRule = GleanTestRule(testContext)

    @Before
    fun setUp() {
        // TODO(bug 1934931): Glean currently does not re-register custom pings
        // when the GleanTestRule automatically resets Glean after each test.
        // We do it manually here for the tests to work.
        val brokenSiteReport: PingType<NoReasonCodes> = PingType<NoReasonCodes>(
            name = "broken-site-report",
            includeClientId = false,
            sendIfEmpty = false,
            preciseTimestamps = true,
            includeInfoSections = true,
            enabled = true,
            schedulesPings = listOf(),
            reasonCodes = listOf(),
            followsCollectionEnabled = true,
            uploaderCapabilities = listOf(),
        )
    }

    @Test
    fun `GIVEN the URL is not changed WHEN WebCompatInfo is retrieved successfully THEN all report broken site pings are submitted`() = runTest {
        val nimbusExperimentsProvider: NimbusExperimentsProvider = FakeNimbusExperimentsProvider(
            activeExperiments = listOf(
                EnrolledExperiment(
                    featureIds = listOf(),
                    slug = "slug1",
                    userFacingName = "",
                    userFacingDescription = "",
                    branchSlug = "",
                    isRollout = false,
                ),
                EnrolledExperiment(
                    featureIds = listOf(),
                    slug = "slug2",
                    userFacingName = "",
                    userFacingDescription = "",
                    branchSlug = "",
                    isRollout = false,
                ),
            ),
            experimentBranchLambda = { it },
        )

        val store = createStore(
            enteredUrl = "https://www.mozilla.org",
            nimbusExperimentsProvider = nimbusExperimentsProvider,
            scope = this,
        )

        val job = Pings.brokenSiteReport.testBeforeNextSubmit {
            assertEquals(
                "basic",
                BrokenSiteReportTabInfoAntitracking.blockList.testGetValue(),
            )
            assertEquals(
                false,
                BrokenSiteReportTabInfoAntitracking.btpHasPurgedSite.testGetValue(),
            )
            assertEquals(
                "standard",
                BrokenSiteReportTabInfoAntitracking.etpCategory.testGetValue(),
            )
            assertEquals(
                false,
                BrokenSiteReportTabInfoAntitracking.hasMixedActiveContentBlocked.testGetValue(),
            )
            assertEquals(
                false,
                BrokenSiteReportTabInfoAntitracking.hasMixedDisplayContentBlocked.testGetValue(),
            )
            assertEquals(
                false,
                BrokenSiteReportTabInfoAntitracking.hasTrackingContentBlocked.testGetValue(),
            )
            assertEquals(
                false,
                BrokenSiteReportTabInfoAntitracking.isPrivateBrowsing.testGetValue(),
            )

            assertEquals(
                buildJsonArray {
                    addJsonObject {
                        put("id", "id.temp")
                        put("name", "name1")
                        put("temporary", true)
                        put("version", "version1")
                    }

                    addJsonObject {
                        put("id", "id.perm")
                        put("name", "name2")
                        put("temporary", false)
                        put("version", "version2")
                    }
                },
                BrokenSiteReportBrowserInfo.addons.testGetValue(),
            )

            assertEquals(
                buildJsonArray {
                    addJsonObject {
                        put("branch", "slug1")
                        put("kind", "nimbusExperiment")
                        put("slug", "slug1")
                    }

                    addJsonObject {
                        put("branch", "slug2")
                        put("kind", "nimbusExperiment")
                        put("slug", "slug2")
                    }
                },
                BrokenSiteReportBrowserInfo.experiments.testGetValue(),
            )

            assertEquals(
                "testDefaultUserAgent",
                BrokenSiteReportBrowserInfoApp.defaultUseragentString.testGetValue(),
            )

            assertEquals(
                """[{"id":"device1"},{"id":"device2"},{"id":"device3"}]""",
                BrokenSiteReportBrowserInfoGraphics.devicesJson.testGetValue(),
            )
            assertEquals(
                """[{"id":"driver1"},{"id":"driver2"},{"id":"driver3"}]""",
                BrokenSiteReportBrowserInfoGraphics.driversJson.testGetValue(),
            )
            assertEquals(
                """{"id":"feature1"}""",
                BrokenSiteReportBrowserInfoGraphics.featuresJson.testGetValue(),
            )
            assertEquals(
                true,
                BrokenSiteReportBrowserInfoGraphics.hasTouchScreen.testGetValue(),
            )
            assertEquals(
                """[{"id":"monitor1"},{"id":"monitor2"},{"id":"monitor3"}]""",
                BrokenSiteReportBrowserInfoGraphics.monitorsJson.testGetValue(),
            )

            assertEquals(
                listOf("en-CA", "en-US"),
                BrokenSiteReportBrowserInfoApp.defaultLocales.testGetValue(),
            )

            assertEquals(
                false,
                BrokenSiteReportBrowserInfoApp.fissionEnabled.testGetValue(),
            )
            assertEquals(
                1L,
                BrokenSiteReportBrowserInfoSystem.memory.testGetValue(),
            )

            assertEquals(
                false,
                BrokenSiteReportBrowserInfoPrefs.opaqueResponseBlocking.testGetValue(),
            )
            assertEquals(
                false,
                BrokenSiteReportBrowserInfoPrefs.installtriggerEnabled.testGetValue(),
            )
            assertEquals(
                false,
                BrokenSiteReportBrowserInfoPrefs.softwareWebrender.testGetValue(),
            )
            assertEquals(
                1L,
                BrokenSiteReportBrowserInfoPrefs.cookieBehavior.testGetValue(),
            )
            assertEquals(
                false,
                BrokenSiteReportBrowserInfoPrefs.globalPrivacyControlEnabled.testGetValue(),
            )
            assertEquals(
                false,
                BrokenSiteReportBrowserInfoPrefs.resistFingerprintingEnabled.testGetValue(),
            )

            assertEquals(
                "1.5",
                BrokenSiteReportBrowserInfoGraphics.devicePixelRatio.testGetValue(),
            )

            assertEquals(
                true,
                BrokenSiteReportTabInfoFrameworks.fastclick.testGetValue(),
            )
            assertEquals(
                true,
                BrokenSiteReportTabInfoFrameworks.marfeel.testGetValue(),
            )
            assertEquals(
                true,
                BrokenSiteReportTabInfoFrameworks.mobify.testGetValue(),
            )

            assertEquals(
                listOf("en-CA", "en-US"),
                BrokenSiteReportTabInfo.languages.testGetValue(),
            )

            assertEquals(
                "testUserAgent",
                BrokenSiteReportTabInfo.useragentString.testGetValue(),
            )

            assertEquals(store.state.enteredUrl, BrokenSiteReport.url.testGetValue())
            assertEquals(
                store.state.reason?.name?.lowercase(),
                BrokenSiteReport.breakageCategory.testGetValue(),
            )
            assertEquals(
                store.state.problemDescription,
                BrokenSiteReport.description.testGetValue(),
            )
        }

        store.dispatch(WebCompatReporterAction.SendReportClicked)
        testScheduler.advanceUntilIdle()
        job.join()
    }

    @Test
    fun `WHEN the report is sent successfully THEN appState is updated`() = runTest {
        val store = createStore(scope = this)

        store.dispatch(WebCompatReporterAction.SendReportClicked)
        testScheduler.advanceUntilIdle()

        verify { appStore.dispatch(AppAction.WebCompatAction.WebCompatReportSent) }
    }

    @Test
    fun `GIVEN the URL is changed WHEN WebCompatInfo is retrieved successfully THEN only non tab related report broken site pings are submitted`() = runTest {
        val nimbusExperimentsProvider: NimbusExperimentsProvider = FakeNimbusExperimentsProvider(
            activeExperiments = listOf(
                EnrolledExperiment(
                    featureIds = listOf(),
                    slug = "slug1",
                    userFacingName = "",
                    userFacingDescription = "",
                    branchSlug = "",
                    isRollout = false,
                ),
                EnrolledExperiment(
                    featureIds = listOf(),
                    slug = "slug2",
                    userFacingName = "",
                    userFacingDescription = "",
                    branchSlug = "",
                    isRollout = false,
                ),
            ),
            experimentBranchLambda = { it },
        )

        val store = createStore(
            enteredUrl = "https://example.com",
            scope = this,
            nimbusExperimentsProvider = nimbusExperimentsProvider,
        )

        val job = Pings.brokenSiteReport.testBeforeNextSubmit {
            assertEquals(
                "basic",
                BrokenSiteReportTabInfoAntitracking.blockList.testGetValue(),
            )
            assertEquals(
                "standard",
                BrokenSiteReportTabInfoAntitracking.etpCategory.testGetValue(),
            )
            assertNull(BrokenSiteReportTabInfoAntitracking.btpHasPurgedSite.testGetValue())
            assertNull(BrokenSiteReportTabInfoAntitracking.hasMixedActiveContentBlocked.testGetValue())
            assertNull(BrokenSiteReportTabInfoAntitracking.hasMixedDisplayContentBlocked.testGetValue())
            assertNull(BrokenSiteReportTabInfoAntitracking.hasTrackingContentBlocked.testGetValue())
            assertNull(BrokenSiteReportTabInfoAntitracking.isPrivateBrowsing.testGetValue())

            assertEquals(
                buildJsonArray {
                    addJsonObject {
                        put("id", "id.temp")
                        put("name", "name1")
                        put("temporary", true)
                        put("version", "version1")
                    }

                    addJsonObject {
                        put("id", "id.perm")
                        put("name", "name2")
                        put("temporary", false)
                        put("version", "version2")
                    }
                },
                BrokenSiteReportBrowserInfo.addons.testGetValue(),
            )

            assertEquals(
                buildJsonArray {
                    addJsonObject {
                        put("branch", "slug1")
                        put("kind", "nimbusExperiment")
                        put("slug", "slug1")
                    }

                    addJsonObject {
                        put("branch", "slug2")
                        put("kind", "nimbusExperiment")
                        put("slug", "slug2")
                    }
                },
                BrokenSiteReportBrowserInfo.experiments.testGetValue(),
            )

            assertEquals(
                "testDefaultUserAgent",
                BrokenSiteReportBrowserInfoApp.defaultUseragentString.testGetValue(),
            )

            assertEquals(
                """[{"id":"device1"},{"id":"device2"},{"id":"device3"}]""",
                BrokenSiteReportBrowserInfoGraphics.devicesJson.testGetValue(),
            )
            assertEquals(
                """[{"id":"driver1"},{"id":"driver2"},{"id":"driver3"}]""",
                BrokenSiteReportBrowserInfoGraphics.driversJson.testGetValue(),
            )
            assertEquals(
                """{"id":"feature1"}""",
                BrokenSiteReportBrowserInfoGraphics.featuresJson.testGetValue(),
            )
            assertEquals(
                true,
                BrokenSiteReportBrowserInfoGraphics.hasTouchScreen.testGetValue(),
            )
            assertEquals(
                """[{"id":"monitor1"},{"id":"monitor2"},{"id":"monitor3"}]""",
                BrokenSiteReportBrowserInfoGraphics.monitorsJson.testGetValue(),
            )

            assertEquals(
                listOf("en-CA", "en-US"),
                BrokenSiteReportBrowserInfoApp.defaultLocales.testGetValue(),
            )

            assertEquals(
                false,
                BrokenSiteReportBrowserInfoApp.fissionEnabled.testGetValue(),
            )
            assertEquals(
                1L,
                BrokenSiteReportBrowserInfoSystem.memory.testGetValue(),
            )

            assertEquals(
                false,
                BrokenSiteReportBrowserInfoPrefs.opaqueResponseBlocking.testGetValue(),
            )
            assertEquals(
                false,
                BrokenSiteReportBrowserInfoPrefs.installtriggerEnabled.testGetValue(),
            )
            assertEquals(
                false,
                BrokenSiteReportBrowserInfoPrefs.softwareWebrender.testGetValue(),
            )
            assertEquals(
                1L,
                BrokenSiteReportBrowserInfoPrefs.cookieBehavior.testGetValue(),
            )
            assertEquals(
                false,
                BrokenSiteReportBrowserInfoPrefs.globalPrivacyControlEnabled.testGetValue(),
            )
            assertEquals(
                false,
                BrokenSiteReportBrowserInfoPrefs.resistFingerprintingEnabled.testGetValue(),
            )

            assertEquals(
                "1.5",
                BrokenSiteReportBrowserInfoGraphics.devicePixelRatio.testGetValue(),
            )

            assertNull(BrokenSiteReportTabInfoFrameworks.fastclick.testGetValue())
            assertNull(BrokenSiteReportTabInfoFrameworks.marfeel.testGetValue())
            assertNull(BrokenSiteReportTabInfoFrameworks.mobify.testGetValue())

            assertNull(BrokenSiteReportTabInfo.languages.testGetValue())

            assertNotEquals(store.state.tabUrl, BrokenSiteReport.url.testGetValue())
            assertEquals(store.state.enteredUrl, BrokenSiteReport.url.testGetValue())
            assertEquals(
                store.state.reason?.name?.lowercase(),
                BrokenSiteReport.breakageCategory.testGetValue(),
            )
            assertEquals(
                store.state.problemDescription,
                BrokenSiteReport.description.testGetValue(),
            )

            assertNull(BrokenSiteReportTabInfo.useragentString.testGetValue())
        }

        store.dispatch(WebCompatReporterAction.SendReportClicked)
        testScheduler.advanceUntilIdle()

        job.join()
    }

    @Test
    fun `GIVEN WebCompatInfo is not retrieved successfully THEN only the form fields and active experiments are submitted`() = runTest {
        val webCompatReporterRetrievalService = object : WebCompatReporterRetrievalService {
            override suspend fun retrieveInfo(): WebCompatInfoDto? = null
        }

        val nimbusExperimentsProvider: NimbusExperimentsProvider = FakeNimbusExperimentsProvider(
            activeExperiments = listOf(
                EnrolledExperiment(
                    featureIds = listOf(),
                    slug = "slug1",
                    userFacingName = "",
                    userFacingDescription = "",
                    branchSlug = "",
                    isRollout = false,
                ),
            ),
            experimentBranchLambda = { it },
        )

        val store = createStore(
            service = webCompatReporterRetrievalService,
            scope = this,
            nimbusExperimentsProvider = nimbusExperimentsProvider,
        )

        val job = Pings.brokenSiteReport.testBeforeNextSubmit {
            assertNull(BrokenSiteReportTabInfoAntitracking.blockList.testGetValue())
            assertNull(BrokenSiteReportTabInfoAntitracking.btpHasPurgedSite.testGetValue())
            assertNull(BrokenSiteReportTabInfoAntitracking.etpCategory.testGetValue())
            assertNull(BrokenSiteReportTabInfoAntitracking.hasMixedActiveContentBlocked.testGetValue())
            assertNull(BrokenSiteReportTabInfoAntitracking.hasMixedDisplayContentBlocked.testGetValue())
            assertNull(BrokenSiteReportTabInfoAntitracking.hasTrackingContentBlocked.testGetValue())
            assertNull(BrokenSiteReportTabInfoAntitracking.isPrivateBrowsing.testGetValue())

            assertNull(BrokenSiteReportBrowserInfo.addons.testGetValue())

            assertNull(BrokenSiteReportBrowserInfoApp.defaultUseragentString.testGetValue())

            assertNull(BrokenSiteReportBrowserInfoGraphics.devicesJson.testGetValue())
            assertNull(BrokenSiteReportBrowserInfoGraphics.driversJson.testGetValue())
            assertNull(BrokenSiteReportBrowserInfoGraphics.featuresJson.testGetValue())
            assertNull(BrokenSiteReportBrowserInfoGraphics.hasTouchScreen.testGetValue())
            assertNull(BrokenSiteReportBrowserInfoGraphics.monitorsJson.testGetValue())
            assertNull(BrokenSiteReportBrowserInfoApp.defaultLocales.testGetValue())

            assertNull(BrokenSiteReportBrowserInfoApp.fissionEnabled.testGetValue())
            assertNull(BrokenSiteReportBrowserInfoSystem.memory.testGetValue())

            assertNull(BrokenSiteReportBrowserInfoPrefs.opaqueResponseBlocking.testGetValue())
            assertNull(BrokenSiteReportBrowserInfoPrefs.installtriggerEnabled.testGetValue())
            assertNull(BrokenSiteReportBrowserInfoPrefs.softwareWebrender.testGetValue())
            assertNull(BrokenSiteReportBrowserInfoPrefs.cookieBehavior.testGetValue())
            assertNull(BrokenSiteReportBrowserInfoPrefs.globalPrivacyControlEnabled.testGetValue())
            assertNull(BrokenSiteReportBrowserInfoPrefs.resistFingerprintingEnabled.testGetValue())

            assertNull(BrokenSiteReportBrowserInfoGraphics.devicePixelRatio.testGetValue())

            assertNull(BrokenSiteReportTabInfoFrameworks.fastclick.testGetValue())
            assertNull(BrokenSiteReportTabInfoFrameworks.marfeel.testGetValue())
            assertNull(BrokenSiteReportTabInfoFrameworks.mobify.testGetValue())

            assertNull(BrokenSiteReportTabInfo.languages.testGetValue())

            assertEquals(store.state.enteredUrl, BrokenSiteReport.url.testGetValue())
            assertEquals(
                store.state.reason?.name?.lowercase(),
                BrokenSiteReport.breakageCategory.testGetValue(),
            )
            assertEquals(
                store.state.problemDescription,
                BrokenSiteReport.description.testGetValue(),
            )

            assertEquals(
                buildJsonArray {
                    addJsonObject {
                        put("branch", "slug1")
                        put("kind", "nimbusExperiment")
                        put("slug", "slug1")
                    }
                },
                BrokenSiteReportBrowserInfo.experiments.testGetValue(),
            )

            assertNull(BrokenSiteReportTabInfo.useragentString.testGetValue())
        }

        store.dispatch(WebCompatReporterAction.SendReportClicked)
        testScheduler.advanceUntilIdle()
        job.join()
    }

    @Test
    fun `GIVEN Nimbus has no active experiments WHEN submitting a report THEN the experiments metric is empty`() = runTest {
        val nimbusExperimentsProvider: NimbusExperimentsProvider = FakeNimbusExperimentsProvider(activeExperiments = emptyList())

        val store = createStore(
            service = FakeWebCompatReporterRetrievalService(),
            scope = this,
            nimbusExperimentsProvider = nimbusExperimentsProvider,
        )

        val job = Pings.brokenSiteReport.testBeforeNextSubmit {
            val experiments = BrokenSiteReportBrowserInfo.experiments.testGetValue()

            assertNotNull(experiments)

            assertEquals(
                buildJsonArray { },
                experiments,
            )
        }

        store.dispatch(WebCompatReporterAction.SendReportClicked)
        testScheduler.advanceUntilIdle()

        job.join()
    }

    @Test
    fun `WHEN open preview is clicked AND enteredUrl matches tab url THEN preview contains full raw JSON plus form fields`() = runTest {
        val capture = CaptureActionsMiddleware<WebCompatReporterState, WebCompatReporterAction>()

        val webCompatReporterSubmissionMiddleware = createMiddleware(
            scope = this,
            service = FakeWebCompatReporterRetrievalService(),
        )

        val store = WebCompatReporterStore(
            initialState = WebCompatReporterState(
                tabUrl = "https://www.mozilla.org",
                enteredUrl = "https://www.mozilla.org",
                reason = WebCompatReporterState.BrokenSiteReason.Slow,
                problemDescription = "",
                includeEtpBlockedUrls = true,
            ),
            middleware = listOf(
                capture,
                webCompatReporterSubmissionMiddleware,
            ),
        )

        store.dispatch(WebCompatReporterAction.OpenPreviewClicked)
        testScheduler.advanceUntilIdle()

        val actual = store.state.previewReporterItems
        val webCompatInfo = FakeWebCompatReporterRetrievalService().retrieveInfo()
        val expectedJson = buildJsonObject {
            put(
                "basic",
                buildJsonObject {
                    put("description", "")
                    put("reason", WebCompatReporterState.BrokenSiteReason.Slow.name)
                    put("url", "https://www.mozilla.org")
                },
            )
        }.addWebCompatInfo(webCompatInfo)

        val expected = webCompatReporterSubmissionMiddleware.parseWebCompatPreviewJson(expectedJson)

        assertEquals(expected, actual)
    }

    @Test
    fun `GIVEN a complex JsonObject WHEN parseWebCompatPreviewJson is called THEN it returns a list of PreviewReporterItem`() = runTest {
        val json = buildJsonObject {
            put(
                "group1",
                buildJsonObject {
                    put("key1", "value1")
                    put("key2", 2)
                    put("key3", true)
                },
            )
            put(
                "group2",
                buildJsonObject {
                    put("key4", "value4")
                },
            )
            put("invalid", "not an object")
        }

        val middleware = createMiddleware(
            scope = this,
            service = FakeWebCompatReporterRetrievalService(),
        )
        val result = middleware.parseWebCompatPreviewJson(json)

        assertEquals(2, result.size)

        assertEquals("group1", result[0].title)
        assertEquals(3, result[0].data.size)
        assertEquals("value1", result[0].data["key1"])
        assertEquals("2", result[0].data["key2"])
        assertEquals("true", result[0].data["key3"])

        assertEquals("group2", result[1].title)
        assertEquals(1, result[1].data.size)
        assertEquals("value4", result[1].data["key4"])
    }

    @Test
    fun `GIVEN an empty JsonObject WHEN parseWebCompatPreviewJson is called THEN it returns an empty list`() = runTest {
        val json = buildJsonObject { }
        val middleware = createMiddleware(
            scope = this,
            service = FakeWebCompatReporterRetrievalService(),
        )

        val result = middleware.parseWebCompatPreviewJson(json)

        assertTrue(result.isEmpty())
    }

    private fun createStore(
        enteredUrl: String = "https://www.mozilla.org",
        service: WebCompatReporterRetrievalService = FakeWebCompatReporterRetrievalService(),
        nimbusExperimentsProvider: NimbusExperimentsProvider = FakeNimbusExperimentsProvider(),
        scope: CoroutineScope,
    ): WebCompatReporterStore {
        return WebCompatReporterStore(
            initialState = WebCompatReporterState(
                tabUrl = "",
                enteredUrl = enteredUrl,
                reason = WebCompatReporterState.BrokenSiteReason.Slow,
                problemDescription = "",
            ),
            middleware = listOf(
                createMiddleware(
                    service = service,
                    nimbusExperimentsProvider = nimbusExperimentsProvider,
                    scope = scope,
                ),
            ),
        )
    }

    private fun createMiddleware(
        service: WebCompatReporterRetrievalService,
        scope: CoroutineScope,
        nimbusExperimentsProvider: NimbusExperimentsProvider = FakeNimbusExperimentsProvider(),
    ) = WebCompatReporterSubmissionMiddleware(
        appStore = appStore,
        webCompatReporterRetrievalService = service,
        scope = scope,
        nimbusExperimentsProvider = nimbusExperimentsProvider,
    )

    private class FakeWebCompatReporterRetrievalService : WebCompatReporterRetrievalService {

        override suspend fun retrieveInfo(): WebCompatInfoDto =
            WebCompatTestData.createTestObject(blockedOrigins = listOf("https://exampleBlockedURLByETP.com"))
    }
}
