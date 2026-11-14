/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components

import io.mockk.mockk
import io.mockk.verify
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import mozilla.components.compose.base.text.Text
import mozilla.components.compose.menu.data.MenuItemTitle
import mozilla.components.compose.menu.data.StandardMenuItem
import mozilla.components.compose.menu.ui.MenuItemIconRes
import mozilla.components.ui.icons.R as iconsR
import org.junit.Test
import org.mozilla.fenix.R
import org.mozilla.fenix.components.appstate.AppAction.FindInPageAction
import org.mozilla.fenix.components.menu.fake.FakeMenuHost
import org.mozilla.fenix.components.menu.fake.reachableEvents
import org.mozilla.fenix.components.menu.store.MenuAction

class FindInPageMenuItemProviderTest {
    private val appStore: AppStore = mockk(relaxed = true)
    private val menu = FakeMenuHost()

    @Test
    fun `WHEN building the menu item THEN provide the item for searching the current page`() {
        val provider = FindInPageMenuItemProvider(appStore)

        assertEquals(
            StandardMenuItem(
                title = MenuItemTitle(Text.Resource(R.string.browser_menu_find_in_page)),
                icon = MenuItemIconRes(iconsR.drawable.mozac_ic_search_24),
                onClickEvent = MenuAction.FindInPage,
            ),
            provider.itemFlow.value,
        )
    }

    @Test
    fun `WHEN clicking the item THEN close the menu and start searching the current page`() {
        val provider = FindInPageMenuItemProvider(appStore)

        provider.onEvent(MenuAction.FindInPage, menu)

        assertTrue(menu.isDismissed)
        verify { appStore.dispatch(FindInPageAction.FindInPageStarted) }
    }

    @Test
    fun `WHEN building the item THEN handle all events it can dispatch and no others`() {
        val provider = FindInPageMenuItemProvider(appStore)

        val events = requireNotNull(provider.itemFlow.value).reachableEvents()

        assertTrue(events.all { provider.handles(it) }, "Not all of $events are handled")
        assertFalse(provider.handles(MenuAction.CustomizeReaderView))
    }
}
