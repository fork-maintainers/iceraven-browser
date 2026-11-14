/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.webcompat

import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.createTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.compose.base.text.Text
import mozilla.components.compose.menu.data.MenuItemTitle
import mozilla.components.compose.menu.data.StandardMenuItem
import mozilla.components.compose.menu.ui.MenuItemIconRes
import mozilla.components.compose.menu.ui.MenuItemState
import mozilla.components.ui.icons.R as iconsR
import org.junit.Test
import org.mozilla.fenix.R
import org.mozilla.fenix.components.AppStore
import org.mozilla.fenix.components.appstate.AppState
import org.mozilla.fenix.components.menu.MenuFragmentDirections
import org.mozilla.fenix.components.menu.MenuTarget
import org.mozilla.fenix.components.menu.fake.FakeMenuHost
import org.mozilla.fenix.components.menu.fake.reachableEvents
import org.mozilla.fenix.components.menu.store.MenuAction
import org.mozilla.fenix.components.usecases.FenixBrowserUseCases
import org.mozilla.fenix.utils.Settings

@OptIn(ExperimentalCoroutinesApi::class)
class ReportBrokenSiteMenuItemProviderTest {
    private val settings: Settings = mockk()
    private val webCompatReporterMoreInfoSender: WebCompatReporterMoreInfoSender = mockk(relaxed = true)
    private val appStore: AppStore = mockk { every { state } returns AppState() }
    private val fenixBrowserUseCases: FenixBrowserUseCases = mockk(relaxed = true)
    private val menu = FakeMenuHost()

    @Test
    fun `GIVEN a webpage is shown WHEN building the menu item THEN allow reporting it as broken`() = runTest {
        val provider = provider(browserStore(url = "https://mozilla.org"))

        assertEquals(expectedItem(isEnabled = true), provider.itemFlow.value)
    }

    @Test
    fun `GIVEN an about page is shown WHEN building the menu item THEN don't allow reporting it as broken`() = runTest {
        val provider = provider(browserStore(url = "about:config"))

        assertEquals(expectedItem(isEnabled = false), provider.itemFlow.value)
    }

    @Test
    fun `GIVEN a content page is shown WHEN building the menu item THEN don't allow reporting it as broken`() =
        runTest {
            val provider = provider(browserStore(url = "content://media/external/file/1"))

            assertEquals(expectedItem(isEnabled = false), provider.itemFlow.value)
        }

    @Test
    fun `GIVEN there is no selected tab WHEN building the menu item THEN don't show it`() = runTest {
        val provider = provider(BrowserStore())

        assertNull(provider.itemFlow.value)
    }

    @Test
    fun `GIVEN telemetry is allowed WHEN clicking the item THEN show the reporter for the current page instead`() =
        runTest {
            every { settings.isTelemetryEnabled } returns true
            val provider = provider(browserStore(url = TEST_URL))

            provider.onEvent(MenuAction.Navigate.WebCompatReporter, menu)

            assertEquals(
                MenuFragmentDirections.actionMenuFragmentToWebCompatReporterFragment(tabUrl = TEST_URL),
                menu.directions,
            )
        }

    @Test
    fun `GIVEN telemetry is not allowed WHEN clicking the item THEN send the details and open webcompat`() = runTest {
        every { settings.isTelemetryEnabled } returns false
        val provider = provider(browserStore(url = TEST_URL))

        provider.onEvent(MenuAction.Navigate.WebCompatReporter, menu)
        runCurrent()

        coVerify {
            webCompatReporterMoreInfoSender.sendMoreWebCompatInfo(
                reason = null,
                problemDescription = null,
                enteredUrl = null,
                tabUrl = TEST_URL,
                engineSession = null,
            )
        }
        assertTrue(menu.isDismissed)
        verify {
            fenixBrowserUseCases.loadUrlOrSearch(
                searchTermOrURL = "$WEB_COMPAT_REPORTER_URL$TEST_URL",
                newTab = true,
                private = false,
            )
        }
    }

    @Test
    fun `GIVEN no selected tab WHEN clicking the item THEN keep the menu open`() = runTest {
        val provider = provider(BrowserStore())

        provider.onEvent(MenuAction.Navigate.WebCompatReporter, menu)
        runCurrent()

        assertFalse(menu.isUsed)
    }

    @Test
    fun `WHEN the page can be reported or not THEN handle all events the item can dispatch and no others`() = runTest {
        listOf(TEST_URL, "about:config").forEach { url ->
            val provider = provider(browserStore(url = url))

            val events = requireNotNull(provider.itemFlow.value).reachableEvents()

            assertTrue(events.all { provider.handles(it) }, "Not all of $events are handled")
            assertFalse(provider.handles(MenuAction.Navigate.Settings))
        }
    }

    // The item is kept up to date on a scope that runTest cancels at the end of each test.
    private fun TestScope.provider(
        browserStore: BrowserStore,
        target: MenuTarget = MenuTarget.BrowserTab,
    ) =
        ReportBrokenSiteMenuItemProvider(
            browserStore = browserStore,
            target = target,
            settings = settings,
            webCompatReporterMoreInfoSender = webCompatReporterMoreInfoSender,
            appStore = appStore,
            fenixBrowserUseCases = fenixBrowserUseCases,
            scope = backgroundScope,
        )

    private fun browserStore(url: String) =
        BrowserStore(
            BrowserState(
                tabs = listOf(createTab(url = url, id = TAB_ID)),
                selectedTabId = TAB_ID,
            )
        )

    private fun expectedItem(isEnabled: Boolean) =
        StandardMenuItem(
            title = MenuItemTitle(Text.Resource(R.string.browser_menu_webcompat_reporter_2)),
            icon = MenuItemIconRes(iconsR.drawable.mozac_ic_lightbulb_24),
            onClickEvent = MenuAction.Navigate.WebCompatReporter,
            state =
                when (isEnabled) {
                    true -> MenuItemState.DEFAULT
                    else -> MenuItemState.DISABLED
                },
        )

    private companion object {
        const val TAB_ID = "tab1"
        const val TEST_URL = "https://mozilla.org"
    }
}
