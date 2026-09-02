/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.webcompat.middleware

import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import mozilla.components.support.test.middleware.CaptureActionsMiddleware
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.experiments.nimbus.internal.EnrolledExperiment
import org.mozilla.fenix.components.AppStore
import org.mozilla.fenix.components.appstate.AppAction
import org.mozilla.fenix.webcompat.GleanBrokenSiteReportSender
import org.mozilla.fenix.webcompat.middleware.WebCompatReporterSubmissionMiddleware.Companion.addActiveExperimentsToWebCompatInfo
import org.mozilla.fenix.webcompat.middleware.WebCompatReporterSubmissionMiddleware.Companion.addWebCompatInfo
import org.mozilla.fenix.webcompat.store.WebCompatReporterAction
import org.mozilla.fenix.webcompat.store.WebCompatReporterState
import org.mozilla.fenix.webcompat.store.WebCompatReporterStore
import org.mozilla.fenix.webcompat.testdata.WebCompatTestData
import org.robolectric.RobolectricTestRunner

class TestGleanBrokenSiteReportSender() : GleanBrokenSiteReportSender {
    internal var lastSentDetails: JSONObject? = null
    internal var lastSentDescription: String? = null
    internal var lastSentReason: WebCompatReporterState.BrokenSiteReason? = null
    internal var lastSentUrl: String? = null
    internal var lastSentSendTabSpecificInfo: Boolean? = null
    internal var lastSentSendBlockedUrls: Boolean? = null

    override suspend fun sendGleanBrokenSiteReport(
        details: JSONObject?,
        description: String?,
        reason: WebCompatReporterState.BrokenSiteReason,
        url: String,
        sendTabSpecificInfo: Boolean,
        sendBlockedUrls: Boolean,
    ) {
        lastSentDetails = details
        lastSentDescription = description
        lastSentReason = reason
        lastSentUrl = url
        lastSentSendTabSpecificInfo = sendTabSpecificInfo
        lastSentSendBlockedUrls = sendBlockedUrls
    }
}

@RunWith(RobolectricTestRunner::class) // Otherwise JSONObject doesn't work properly
class WebCompatReporterSubmissionMiddlewareTest {
    private val appStore: AppStore = mockk(relaxed = true)

    private val gleanBrokenSiteReportSender: TestGleanBrokenSiteReportSender = TestGleanBrokenSiteReportSender()

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

        store.dispatch(WebCompatReporterAction.SendReportClicked)
        testScheduler.advanceUntilIdle()

        assertLastSentBasicInfoMatchesExpectations()

        val expected = FakeWebCompatReporterRetrievalService().retrieveInfo()?.addActiveExperimentsToWebCompatInfo(nimbusExperimentsProvider)
        assertEquals(expected.toString(), gleanBrokenSiteReportSender.lastSentDetails.toString())
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

        store.dispatch(WebCompatReporterAction.SendReportClicked)
        testScheduler.advanceUntilIdle()

        assertLastSentBasicInfoMatchesExpectations(sentUrl = store.state.enteredUrl, shouldSendTabSpecificInfo = false)

        val expected = FakeWebCompatReporterRetrievalService().retrieveInfo()?.addActiveExperimentsToWebCompatInfo(nimbusExperimentsProvider)
        assertEquals(expected.toString(), gleanBrokenSiteReportSender.lastSentDetails.toString())
    }

    @Test
    fun `GIVEN WebCompatInfo is not retrieved successfully THEN only the form fields and active experiments are submitted`() = runTest {
        val webCompatReporterRetrievalService = object : WebCompatReporterRetrievalService {
            override suspend fun retrieveInfo(): JSONObject? = null
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

        store.dispatch(WebCompatReporterAction.SendReportClicked)
        testScheduler.advanceUntilIdle()

        assertLastSentBasicInfoMatchesExpectations(shouldSendTabSpecificInfo = false)
        assertEquals(null, gleanBrokenSiteReportSender.lastSentDetails)
    }

    @Test
    fun `GIVEN Nimbus has no active experiments WHEN submitting a report THEN the experiments metric is empty`() = runTest {
        val nimbusExperimentsProvider: NimbusExperimentsProvider = FakeNimbusExperimentsProvider(activeExperiments = emptyList())

        val store = createStore(
            service = FakeWebCompatReporterRetrievalService(),
            scope = this,
            nimbusExperimentsProvider = nimbusExperimentsProvider,
        )

        store.dispatch(WebCompatReporterAction.SendReportClicked)
        testScheduler.advanceUntilIdle()

        assertLastSentBasicInfoMatchesExpectations()

        val sentDetails = gleanBrokenSiteReportSender.lastSentDetails
        val sentExperiments = sentDetails?.optJSONObject("browserInfo")?.optJSONObject("experiments")?.optJSONArray("value")
        assertEquals("[]", sentExperiments.toString())
    }

    @Test
    fun `WHEN open preview is clicked AND enteredUrl matches tab url THEN preview contains full raw JSON plus form fields`() = runTest {
        val capture = CaptureActionsMiddleware<WebCompatReporterState, WebCompatReporterAction>()

        val webCompatReporterSubmissionMiddleware = createMiddleware(
            scope = this,
            gleanBrokenSiteReportSender = gleanBrokenSiteReportSender,
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
            gleanBrokenSiteReportSender = gleanBrokenSiteReportSender,
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
            gleanBrokenSiteReportSender = gleanBrokenSiteReportSender,
            service = FakeWebCompatReporterRetrievalService(),
        )

        val result = middleware.parseWebCompatPreviewJson(json)

        assertTrue(result.isEmpty())
    }

    private fun assertLastSentBasicInfoMatchesExpectations(
      sentUrl: String = "https://www.mozilla.org",
      shouldSendTabSpecificInfo: Boolean = true,
      shouldSendBlockedURLs: Boolean = false,
    ) {
        assertEquals(gleanBrokenSiteReportSender.lastSentDescription, "")
        assertEquals(gleanBrokenSiteReportSender.lastSentReason, WebCompatReporterState.BrokenSiteReason.Slow)
        assertEquals(gleanBrokenSiteReportSender.lastSentUrl, sentUrl)
        assertEquals(gleanBrokenSiteReportSender.lastSentSendTabSpecificInfo, shouldSendTabSpecificInfo)
        assertEquals(gleanBrokenSiteReportSender.lastSentSendBlockedUrls, shouldSendBlockedURLs)
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
                    gleanBrokenSiteReportSender = gleanBrokenSiteReportSender,
                    nimbusExperimentsProvider = nimbusExperimentsProvider,
                    scope = scope,
                ),
            ),
        )
    }

    @Test
    fun `GIVEN a JSONObject with not-to-be-previewed items, WHEN addWebCompatInfo is called THEN it ignores those items`() = runTest {
        val toAdd = buildJsonObject {
            put(
                "category",
                buildJsonObject {
                    put(
                        "included_item1",
                        buildJsonObject {
                            put("value", "included1")
                        },
                    )
                    put(
                        "included_item2",
                        buildJsonObject {
                            put("doNotPreview", false)
                            put("value", "included2")
                        },
                    )
                    put(
                        "ignored_item",
                        buildJsonObject {
                            put("doNotPreview", true)
                            put("value", "included")
                        },
                    )
                },
            )
        }

        val result = buildJsonObject { }.addWebCompatInfo(JSONObject(toAdd.toString()))

        val expected = buildJsonObject {
            put(
                "category",
                buildJsonObject {
                    put("included_item1", "included1")
                    put("included_item2", "included2")
                },
            )
        }

        assertEquals(expected, result)
    }

    private fun createMiddleware(
        service: WebCompatReporterRetrievalService,
        gleanBrokenSiteReportSender: GleanBrokenSiteReportSender,
        scope: CoroutineScope,
        nimbusExperimentsProvider: NimbusExperimentsProvider = FakeNimbusExperimentsProvider(),
    ) = WebCompatReporterSubmissionMiddleware(
        appStore = appStore,
        webCompatReporterRetrievalService = service,
        gleanBrokenSiteReportSender = gleanBrokenSiteReportSender,
        scope = scope,
        nimbusExperimentsProvider = nimbusExperimentsProvider,
    )

    private class FakeWebCompatReporterRetrievalService : WebCompatReporterRetrievalService {

        override suspend fun retrieveInfo(): JSONObject? =
            WebCompatTestData.createTestObject(blockedOrigins = listOf("https://exampleBlockedURLByETP.com"))
    }
}
