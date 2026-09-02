/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix

import android.net.Uri
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import mozilla.components.browser.errorpages.ErrorPages
import mozilla.components.browser.errorpages.ErrorType
import mozilla.components.browser.state.search.SearchEngine
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.SearchState
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.engine.request.RequestInterceptor
import mozilla.components.concept.engine.utils.ABOUT_HOME_URL
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.AppRequestInterceptor.Companion.HIGH_RISK_ERROR_PAGES
import org.mozilla.fenix.AppRequestInterceptor.Companion.LOW_AND_MEDIUM_RISK_ERROR_PAGES
import org.mozilla.fenix.GleanMetrics.ErrorPage
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.helpers.FenixGleanTestRule
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertIs

@RunWith(RobolectricTestRunner::class)
class AppRequestInterceptorTest {

    @get:Rule
    val gleanTestRule = FenixGleanTestRule(testContext)

    private lateinit var interceptor: RequestInterceptor
    private lateinit var navigationController: NavController

    @Before
    fun setUp() {
        every { testContext.components.settings } returns mockk(relaxed = true)

        navigationController = mockk(relaxed = true)
        interceptor = spyk(
            AppRequestInterceptor(testContext).also {
                it.setNavigationController(navigationController)
            },
        )

        every { (interceptor as AppRequestInterceptor).isConnected() } returns true
    }

    @Test
    fun `GIVEN request to ABOUT_HOME WHEN request is intercepted THEN return a null interception response and navigate to the homepage`() {
        val result = interceptor.onLoadRequest(
            engineSession = mockk(),
            uri = ABOUT_HOME_URL,
            lastUri = ABOUT_HOME_URL,
            hasUserGesture = true,
            isSameDomain = true,
            isDirectNavigation = false,
            isRedirect = false,
            isSubframeRequest = false,
        )

        assertNull(result)

        verify {
            navigationController.navigate(NavGraphDirections.actionGlobalHome())
        }
    }

    @Test
    fun `GIVEN homepage is currently shown and a request to ABOUT_HOME WHEN request is intercepted THEN return a null interception response and do not navigate to the homepage`() {
        val mockDestination: NavDestination = mockk(relaxed = true)
        every { mockDestination.id } returns R.id.homeFragment
        every { navigationController.currentDestination } returns mockDestination

        val result = interceptor.onLoadRequest(
            engineSession = mockk(),
            uri = ABOUT_HOME_URL,
            lastUri = ABOUT_HOME_URL,
            hasUserGesture = true,
            isSameDomain = true,
            isDirectNavigation = false,
            isRedirect = false,
            isSubframeRequest = false,
        )

        assertNull(result)

        verify(exactly = 0) {
            navigationController.navigate(NavGraphDirections.actionGlobalHome())
        }
    }

    @Test
    fun `GIVEN onboarding is currently shown and a request to ABOUT_HOME WHEN request is intercepted THEN return a null interception response and do not navigate to the homepage`() {
        val mockDestination: NavDestination = mockk(relaxed = true)
        every { mockDestination.id } returns R.id.onboardingFragment
        every { navigationController.currentDestination } returns mockDestination

        val result = interceptor.onLoadRequest(
            engineSession = mockk(),
            uri = ABOUT_HOME_URL,
            lastUri = ABOUT_HOME_URL,
            hasUserGesture = true,
            isSameDomain = true,
            isDirectNavigation = false,
            isRedirect = false,
            isSubframeRequest = false,
        )

        assertNull(result)

        verify(exactly = 0) {
            navigationController.navigate(NavGraphDirections.actionGlobalHome())
        }
    }

    @Test
    fun `onErrorRequest results in correct error page for low risk level error`() {
        setOf(
            ErrorType.UNKNOWN,
            ErrorType.ERROR_NET_INTERRUPT,
            ErrorType.ERROR_NET_TIMEOUT,
            ErrorType.ERROR_CONNECTION_REFUSED,
            ErrorType.ERROR_UNKNOWN_SOCKET_TYPE,
            ErrorType.ERROR_REDIRECT_LOOP,
            ErrorType.ERROR_OFFLINE,
            ErrorType.ERROR_NET_RESET,
            ErrorType.ERROR_UNSAFE_CONTENT_TYPE,
            ErrorType.ERROR_CORRUPTED_CONTENT,
            ErrorType.ERROR_CONTENT_CRASHED,
            ErrorType.ERROR_INVALID_CONTENT_ENCODING,
            ErrorType.ERROR_UNKNOWN_HOST,
            ErrorType.ERROR_MALFORMED_URI,
            ErrorType.ERROR_FILE_NOT_FOUND,
            ErrorType.ERROR_FILE_ACCESS_DENIED,
            ErrorType.ERROR_PROXY_CONNECTION_REFUSED,
            ErrorType.ERROR_UNKNOWN_PROXY_HOST,
            ErrorType.ERROR_UNKNOWN_PROTOCOL,
        ).forEach { error ->
            val actualPage = createActualErrorPage(error)
            val expectedPage = createExpectedErrorPage(
                error = error,
                html = LOW_AND_MEDIUM_RISK_ERROR_PAGES,
            )

            assertEquals(expectedPage, actualPage)
            // Check if the error metric was recorded
            assertEquals(
                error.name,
                ErrorPage.visitedError.testGetValue()!!.last().extra?.get("error_type"),
            )
        }
    }

    @Test
    fun `onErrorRequest results in correct error page for medium risk level error`() {
        setOf(
            ErrorType.ERROR_SECURITY_BAD_CERT,
            ErrorType.ERROR_SECURITY_SSL,
            ErrorType.ERROR_PORT_BLOCKED,
        ).forEach { error ->
            val actualPage = createActualErrorPage(error)
            val expectedPage = createExpectedErrorPage(
                error = error,
                html = LOW_AND_MEDIUM_RISK_ERROR_PAGES,
            )

            assertEquals(expectedPage, actualPage)
            // Check if the error metric was recorded
            assertEquals(
                error.name,
                ErrorPage.visitedError.testGetValue()!!.last().extra?.get("error_type"),
            )
        }
    }

    @Test
    fun `onErrorRequest results in correct error page for high risk level error`() {
        setOf(
            ErrorType.ERROR_SAFEBROWSING_HARMFUL_URI,
            ErrorType.ERROR_SAFEBROWSING_MALWARE_URI,
            ErrorType.ERROR_SAFEBROWSING_PHISHING_URI,
            ErrorType.ERROR_SAFEBROWSING_UNWANTED_URI,
            ErrorType.ERROR_HARMFULADDON_URI,
        ).forEach { error ->
            val actualPage = createActualErrorPage(error)
            val expectedPage = createExpectedErrorPage(
                error = error,
                html = HIGH_RISK_ERROR_PAGES,
            )

            assertEquals(expectedPage, actualPage)
            // Check if the error metric was recorded
            assertEquals(
                error.name,
                ErrorPage.visitedError.testGetValue()!!.last().extra?.get("error_type"),
            )
        }
    }

    @Test
    fun `GIVEN a private session WHEN onError is called THEN return ErrorResponse with isPrivate = true`() {
        val isPrivateForSession: (EngineSession) -> Boolean = { true }
        val interceptor = AppRequestInterceptor(testContext, isPrivateForSession)

        val response = interceptor.onErrorRequest(mockk(), ErrorType.ERROR_UNSAFE_CONTENT_TYPE, "localhost")

        assertTrue(response.uri.contains("isPrivate=true"))
    }

    @Test
    fun `GIVEN a normal session WHEN onError is called THEN return ErrorResponse with isPrivate = true`() {
        val isPrivateForSession: (EngineSession) -> Boolean = { false }
        val interceptor = AppRequestInterceptor(testContext, isPrivateForSession)

        val response = interceptor.onErrorRequest(mockk(), ErrorType.ERROR_UNSAFE_CONTENT_TYPE, "localhost")

        assertTrue(response.uri.contains("isPrivate=false"))
    }

    @Test
    fun `GIVEN a search error-action url WHEN intercepted THEN returns a default-engine search url`() {
        val searchEngine = SearchEngine(
            id = "test",
            name = "Test Engine",
            icon = mockk(relaxed = true),
            type = SearchEngine.Type.BUNDLED,
            resultUrls = listOf("https://example.org/?q={searchTerms}"),
        )
        val store = BrowserStore(
            BrowserState(search = SearchState(regionSearchEngines = listOf(searchEngine))),
        )
        every { testContext.components.core.store } returns store

        val result = interceptor.onLoadRequest(
            engineSession = mockk(),
            uri = "${AppRequestInterceptor.ERROR_PAGE_ACTION_SCHEME}://search?q=example.com%2Fpath",
            lastUri = null,
            hasUserGesture = true,
            isSameDomain = false,
            isRedirect = false,
            isDirectNavigation = false,
            isSubframeRequest = false,
        )

        val url = assertIs<RequestInterceptor.InterceptionResponse.Url>(result).url
        assertTrue(url.startsWith("https://example.org/?q="))
        assertTrue(url.contains("example.com"))
        assertEquals(1, ErrorPage.archiveSearchWebSelected.testGetValue()?.size)
    }

    @Test
    fun `GIVEN an open error-action url WHEN intercepted THEN returns the archive url`() {
        val archiveUrl = "https://web.archive.org/web/2020/https://example.com/"
        val result = interceptor.onLoadRequest(
            engineSession = mockk(),
            uri = "${AppRequestInterceptor.ERROR_PAGE_ACTION_SCHEME}://open?url=${Uri.encode(archiveUrl)}",
            lastUri = null,
            hasUserGesture = true,
            isSameDomain = false,
            isRedirect = false,
            isDirectNavigation = false,
            isSubframeRequest = false,
        )

        assertEquals(archiveUrl, assertIs<RequestInterceptor.InterceptionResponse.Url>(result).url)
        assertEquals(1, ErrorPage.archivedVersionOpened.testGetValue()?.size)
    }

    @Test
    fun `GIVEN an attempt error-action url WHEN intercepted THEN the click is recorded and denied`() {
        val result = interceptor.onLoadRequest(
            engineSession = mockk(),
            uri = "${AppRequestInterceptor.ERROR_PAGE_ACTION_SCHEME}://attempt",
            lastUri = null,
            hasUserGesture = true,
            isSameDomain = false,
            isRedirect = false,
            isDirectNavigation = false,
            isSubframeRequest = false,
        )

        assertIs<RequestInterceptor.InterceptionResponse.Deny>(result)
        assertEquals(1, ErrorPage.archiveButtonClicked.testGetValue()?.size)
    }

    @Test
    fun `GIVEN wayback enabled WHEN onError for an archivable type THEN page carries archive params`() {
        every { testContext.components.settings.isWaybackMachineEnabled } returns true

        val response = interceptor.onErrorRequest(
            session = mockk(),
            errorType = ErrorType.ERROR_UNKNOWN_HOST,
            uri = "https://example.com/",
        )

        assertTrue(response!!.uri.contains("&archiveUrl="))
    }

    @Test
    fun `GIVEN wayback disabled WHEN onError for an archivable type THEN page carries no archive params`() {
        every { testContext.components.settings.isWaybackMachineEnabled } returns false

        val response = interceptor.onErrorRequest(
            session = mockk(),
            errorType = ErrorType.ERROR_UNKNOWN_HOST,
            uri = "https://example.com/",
        )

        assertFalse(response!!.uri.contains("&archiveUrl="))
    }

    @Test
    fun `GIVEN wayback enabled WHEN onError for a non-archivable type THEN page carries no archive params`() {
        every { testContext.components.settings.isWaybackMachineEnabled } returns true

        val response = interceptor.onErrorRequest(
            session = mockk(),
            errorType = ErrorType.ERROR_SECURITY_SSL,
            uri = "https://example.com/",
        )

        assertFalse(response!!.uri.contains("&archiveUrl="))
    }

    @Test
    fun `GIVEN wayback enabled WHEN onError for each archivable type THEN page carries archive params`() {
        every { testContext.components.settings.isWaybackMachineEnabled } returns true

        setOf(
            ErrorType.ERROR_UNKNOWN_HOST,
            ErrorType.ERROR_CONNECTION_REFUSED,
            ErrorType.ERROR_NET_TIMEOUT,
            ErrorType.ERROR_NET_RESET,
            ErrorType.ERROR_NET_INTERRUPT,
            ErrorType.ERROR_REDIRECT_LOOP,
            ErrorType.ERROR_FILE_NOT_FOUND,
            ErrorType.ERROR_PROXY_CONNECTION_REFUSED,
            ErrorType.ERROR_UNKNOWN_PROXY_HOST,
            ErrorType.ERROR_CORRUPTED_CONTENT,
            ErrorType.ERROR_INVALID_CONTENT_ENCODING,
            ErrorType.ERROR_UNSAFE_CONTENT_TYPE,
        ).forEach { error ->
            val response = interceptor.onErrorRequest(
                session = mockk(),
                errorType = error,
                uri = "https://example.com/",
            )

            assertTrue("Expected archive params for $error", response!!.uri.contains("&archiveUrl="))
        }
    }

    private fun createActualErrorPage(error: ErrorType): String {
        val errorPage = interceptor.onErrorRequest(session = mockk(), errorType = error, uri = null)
            as RequestInterceptor.ErrorResponse
        return errorPage.uri
    }

    private fun createExpectedErrorPage(error: ErrorType, html: String): String {
        return ErrorPages.createUrlEncodedErrorPage(
            context = testContext,
            errorType = error,
            htmlResource = html,
        )
    }
}
