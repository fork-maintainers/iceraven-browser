/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.browser

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.ReaderState
import mozilla.components.browser.state.state.createCustomTab
import mozilla.components.browser.state.state.createTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.compose.base.text.Text
import mozilla.components.compose.menu.data.StandardMenuItem
import mozilla.components.compose.menu.ui.MenuItemIconRes
import mozilla.components.concept.engine.prompt.ShareData
import mozilla.components.ui.icons.R as iconsR
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.R
import org.mozilla.fenix.components.menu.MenuTarget
import org.mozilla.fenix.components.menu.fake.FakeMenuHost
import org.mozilla.fenix.components.menu.fake.reachableEvents
import org.mozilla.fenix.components.menu.store.MenuAction
import org.mozilla.fenix.components.share.ShareSource
import org.mozilla.fenix.components.usecases.ShareUseCases
import org.mozilla.fenix.share.ShareFragmentArgs

@RunWith(AndroidJUnit4::class)
class ShareMenuItemProviderTest {
    private val shareUseCases: ShareUseCases = mockk(relaxed = true)
    private val menu = FakeMenuHost()

    @Test
    fun `WHEN building the share menu item THEN return correct configuration`() {
        val provider = createProvider()

        assertEquals(
            StandardMenuItem(
                title = Text.Resource(R.string.browser_menu_share),
                icon = MenuItemIconRes(iconsR.drawable.mozac_ic_share_android_24),
                onClickEvent = MenuAction.Navigate.Share,
            ),
            provider.itemFlow.value,
        )
    }

    @Test
    fun `GIVEN a selected tab WHEN clicking the item THEN share its page`() {
        val provider = createProvider()

        provider.onEvent(MenuAction.Navigate.Share, menu)

        verify {
            shareUseCases.shareUrl(
                id = TAB_ID,
                url = TEST_URL,
                title = TEST_TITLE,
                source = ShareSource.BROWSER_MENU,
                isPrivate = false,
                isCustomTab = false,
                navigateToShareFragment = any(),
            )
        }
    }

    @Test
    fun `GIVEN a custom tab WHEN clicking the item THEN share its page with custom tab source`() {
        val customTab = createCustomTab(url = TEST_URL, title = TEST_TITLE, id = TAB_ID)
        val browserStore = BrowserStore(BrowserState(customTabs = listOf(customTab)))
        val provider = createProvider(browserStore = browserStore, target = MenuTarget.CustomTab(TAB_ID))

        provider.onEvent(MenuAction.Navigate.Share, menu)

        verify {
            shareUseCases.shareUrl(
                id = TAB_ID,
                url = TEST_URL,
                title = TEST_TITLE,
                source = ShareSource.CUSTOM_TAB_MENU,
                isPrivate = false,
                isCustomTab = true,
                navigateToShareFragment = any(),
            )
        }
    }

    @Test
    fun `GIVEN the page is shown in reader view WHEN clicking the item THEN share the original page`() {
        val tab =
            createTab(
                url = "https://mozilla.org/reader",
                title = TEST_TITLE,
                id = TAB_ID,
                readerState = ReaderState(active = true, activeUrl = TEST_URL),
            )
        val provider = createProvider(BrowserStore(BrowserState(tabs = listOf(tab), selectedTabId = TAB_ID)))

        provider.onEvent(MenuAction.Navigate.Share, menu)

        verify {
            shareUseCases.shareUrl(
                id = TAB_ID,
                url = TEST_URL,
                title = any(),
                source = any(),
                isPrivate = any(),
                navigateToShareFragment = any(),
            )
        }
    }

    @Test
    fun `GIVEN the page can be shared by another app WHEN clicking the item THEN close the menu`() {
        val provider = createProvider()

        provider.onEvent(MenuAction.Navigate.Share, menu)

        assertTrue(menu.isDismissed)
        assertNull(menu.directions)
    }

    @Test
    fun `GIVEN no other app can share the page WHEN clicking the item THEN show the share screen instead`() {
        every {
            shareUseCases.shareUrl(any(), any(), any(), any(), any(), any(), any(), any(), any())
        } answers { lastArg<() -> Unit>().invoke() }
        val provider = createProvider()

        provider.onEvent(MenuAction.Navigate.Share, menu)

        assertFalse(menu.isDismissed)
        val directions = requireNotNull(menu.directions)
        assertEquals(R.id.action_global_shareFragment, directions.actionId)
        val args = ShareFragmentArgs.fromBundle(directions.arguments)
        assertEquals(listOf(ShareData(title = TEST_TITLE, url = TEST_URL, private = false)), args.data.toList())
        assertTrue(args.showPage)
        assertEquals(TAB_ID, args.sessionId)
    }

    @Test
    fun `GIVEN no selected tab WHEN clicking the item THEN keep the menu open`() {
        val provider = createProvider(BrowserStore())

        provider.onEvent(MenuAction.Navigate.Share, menu)

        assertFalse(menu.isUsed)
        verify(exactly = 0) { shareUseCases.shareUrl(any(), any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `WHEN building the item THEN handle all events it can dispatch and no others`() {
        val provider = createProvider()

        val events = requireNotNull(provider.itemFlow.value).reachableEvents()

        assertTrue(events.all { provider.handles(it) }, "Not all of $events are handled")
        assertFalse(provider.handles(MenuAction.Navigate.Settings))
    }

    private fun createProvider(
        browserStore: BrowserStore =
            BrowserStore(
                BrowserState(
                    tabs = listOf(createTab(url = TEST_URL, title = TEST_TITLE, id = TAB_ID)),
                    selectedTabId = TAB_ID,
                )
            ),
        target: MenuTarget = MenuTarget.BrowserTab,
    ) = ShareMenuItemProvider(browserStore = browserStore, target = target, shareUseCases = shareUseCases)

    private companion object {
        const val TAB_ID = "tab1"
        const val TEST_URL = "https://mozilla.org"
        const val TEST_TITLE = "Mozilla"
    }
}
