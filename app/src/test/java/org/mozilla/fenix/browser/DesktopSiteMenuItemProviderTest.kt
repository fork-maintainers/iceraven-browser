/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.browser

import androidx.compose.ui.semantics.Role
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.ContentState
import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.browser.state.state.createCustomTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.compose.base.text.Text
import mozilla.components.compose.menu.data.MenuItemBadge
import mozilla.components.compose.menu.data.MenuItemTitle
import mozilla.components.compose.menu.data.StandardMenuItem
import mozilla.components.compose.menu.ui.MenuItemIconRes
import mozilla.components.compose.menu.ui.MenuItemState
import mozilla.components.feature.session.SessionUseCases
import mozilla.components.ui.icons.R as iconsR
import org.junit.Test
import org.mozilla.fenix.R
import org.mozilla.fenix.components.menu.MenuTarget
import org.mozilla.fenix.components.menu.fake.FakeMenuHost
import org.mozilla.fenix.components.menu.fake.reachableEvents
import org.mozilla.fenix.components.menu.store.MenuAction

class DesktopSiteMenuItemProviderTest {
    private val requestDesktopSite: SessionUseCases.RequestDesktopSiteUseCase = mockk(relaxed = true)
    private val menu = FakeMenuHost()

    @Test
    fun `GIVEN there is no selected tab WHEN building the desktop site menu item THEN return null`() = runTest {
        val provider = provider(BrowserStore())

        assertNull(provider.itemFlow.value)
    }

    @Test
    fun `GIVEN the mobile version is shown WHEN building the desktop site menu item THEN offer switching to desktop mode`() =
        runTest {
            val provider = provider(browserStore(desktopMode = false))

            assertEquals(expectedItem(isDesktopMode = false), provider.itemFlow.value)
        }

    @Test
    fun `GIVEN a custom tab WHEN building the desktop site menu item THEN offer switching to desktop mode`() = runTest {
        val customTab = createCustomTab(url = "https://mozilla.org", id = "customTabId", desktopMode = false)
        val browserStore = BrowserStore(BrowserState(customTabs = listOf(customTab)))
        val provider = provider(browserStore, target = MenuTarget.CustomTab("customTabId"))

        assertEquals(expectedItem(isDesktopMode = false), provider.itemFlow.value)
    }

    @Test
    fun `GIVEN the desktop version is shown WHEN building the desktop site menu item THEN offer switching to mobile mode`() =
        runTest {
            val provider = provider(browserStore(desktopMode = true))

            assertEquals(expectedItem(isDesktopMode = true), provider.itemFlow.value)
        }

    @Test
    fun `GIVEN the current page is a PDF WHEN building the desktop site menu item THEN show it as disabled and without a badge`() =
        runTest {
            val provider = provider(browserStore(isPdf = true))

            assertEquals(
                StandardMenuItem(
                    title = MenuItemTitle(Text.Resource(R.string.browser_menu_desktop_site)),
                    icon = MenuItemIconRes(iconsR.drawable.mozac_ic_device_desktop_24),
                    onClickEvent = MenuAction.RequestDesktopSite,
                    badge = null,
                    state = MenuItemState.DISABLED,
                    role = Role.Switch,
                ),
                provider.itemFlow.value,
            )
        }

    // The item is kept up to date on a scope that runTest cancels at the end of each test.
    @Test
    fun `GIVEN the mobile version is shown WHEN clicking the item THEN close the menu and show the desktop one`() =
        runTest {
            val provider = provider(browserStore(desktopMode = false))

            provider.onEvent(MenuAction.RequestDesktopSite, menu)

            assertTrue(menu.isDismissed)
            verify { requestDesktopSite(enable = true, tabId = TAB_ID) }
        }

    @Test
    fun `GIVEN a custom tab WHEN tapping desktop site THEN switch mode using the custom tab id`() = runTest {
        val customTab = createCustomTab(url = "https://mozilla.org", id = CUSTOM_TAB_ID, desktopMode = false)
        val store = BrowserStore(BrowserState(customTabs = listOf(customTab)))
        val provider = provider(store, target = MenuTarget.CustomTab(CUSTOM_TAB_ID))

        provider.onEvent(MenuAction.RequestDesktopSite, menu)

        assertTrue(menu.isDismissed)
        verify { requestDesktopSite(enable = true, tabId = CUSTOM_TAB_ID) }
    }

    @Test
    fun `GIVEN the desktop version is shown WHEN clicking the item THEN close the menu and show the mobile one`() =
        runTest {
            val provider = provider(browserStore(desktopMode = true))

            provider.onEvent(MenuAction.RequestMobileSite, menu)

            assertTrue(menu.isDismissed)
            verify { requestDesktopSite(enable = false, tabId = TAB_ID) }
        }

    @Test
    fun `GIVEN no selected tab WHEN clicking the item THEN keep the menu open`() = runTest {
        val provider = provider(BrowserStore())

        provider.onEvent(MenuAction.RequestDesktopSite, menu)

        assertFalse(menu.isUsed)
        verify(exactly = 0) { requestDesktopSite(any(), any()) }
    }

    @Test
    fun `WHEN either version is shown THEN handle all events the item can dispatch`() = runTest {
        val stores =
            listOf(browserStore(desktopMode = false), browserStore(desktopMode = true), browserStore(isPdf = true))

        stores.forEach { store ->
            val provider = provider(store)

            val events = requireNotNull(provider.itemFlow.value).reachableEvents()

            assertTrue(events.all { provider.handles(it) }, "Not all of $events are handled")
        }
    }

    @Test
    fun `WHEN asked about the events of other items THEN don't handle them`() = runTest {
        assertFalse(provider(browserStore()).handles(MenuAction.FindInPage))
    }

    private fun TestScope.provider(
        browserStore: BrowserStore,
        target: MenuTarget = MenuTarget.BrowserTab,
    ) =
        DesktopSiteMenuItemProvider(
            browserStore = browserStore,
            target = target,
            requestDesktopSite = requestDesktopSite,
            scope = backgroundScope,
        )

    private fun browserStore(
        desktopMode: Boolean = false,
        isPdf: Boolean = false,
    ) =
        BrowserStore(
            BrowserState(
                tabs =
                    listOf(
                        TabSessionState(
                            id = TAB_ID,
                            content =
                                ContentState(
                                    url = "https://mozilla.org",
                                    desktopMode = desktopMode,
                                    isPdf = isPdf,
                                ),
                        )
                    ),
                selectedTabId = TAB_ID,
            )
        )

    private fun expectedItem(isDesktopMode: Boolean) =
        StandardMenuItem(
            title = MenuItemTitle(Text.Resource(R.string.browser_menu_desktop_site)),
            icon = MenuItemIconRes(iconsR.drawable.mozac_ic_device_desktop_24),
            onClickEvent =
                when (isDesktopMode) {
                    true -> MenuAction.RequestMobileSite
                    else -> MenuAction.RequestDesktopSite
                },
            badge =
                MenuItemBadge(
                    text =
                        Text.Resource(
                            when (isDesktopMode) {
                                true -> R.string.browser_feature_desktop_site_on
                                else -> R.string.browser_feature_desktop_site_off
                            }
                        ),
                    state = if (isDesktopMode) MenuItemState.ACTIVE else MenuItemState.DEFAULT,
                ),
            state = if (isDesktopMode) MenuItemState.ACTIVE else MenuItemState.DEFAULT,
            role = Role.Switch,
        )

    private companion object {
        const val TAB_ID = "tab1"
        const val CUSTOM_TAB_ID = "customTabId"
    }
}
