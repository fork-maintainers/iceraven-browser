/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.bookmarks

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import mozilla.appservices.places.BookmarkRoot
import mozilla.components.compose.base.text.Text
import mozilla.components.compose.menu.data.MenuItemTitle
import mozilla.components.compose.menu.data.StandardMenuItem
import mozilla.components.compose.menu.ui.MenuItemIconRes
import mozilla.components.ui.icons.R as iconsR
import org.junit.Test
import org.mozilla.fenix.NavGraphDirections
import org.mozilla.fenix.R
import org.mozilla.fenix.components.menu.fake.FakeMenuHost
import org.mozilla.fenix.components.menu.fake.reachableEvents
import org.mozilla.fenix.components.menu.store.MenuAction

class BookmarksScreenMenuItemProviderTest {
    @Test
    fun `WHEN building the menu item THEN provide the item for opening the bookmarks screen`() {
        val provider = BookmarksScreenMenuItemProvider()

        assertEquals(
            StandardMenuItem(
                title = MenuItemTitle(Text.Resource(R.string.library_bookmarks)),
                icon = MenuItemIconRes(iconsR.drawable.mozac_ic_bookmark_tray_fill_24),
                onClickEvent = MenuAction.Navigate.Bookmarks,
            ),
            provider.itemFlow.value,
        )
    }

    @Test
    fun `WHEN clicking the item THEN show the mobile bookmarks in place of the menu`() {
        val menu = FakeMenuHost()

        BookmarksScreenMenuItemProvider().onEvent(MenuAction.Navigate.Bookmarks, menu)

        assertEquals(NavGraphDirections.actionGlobalBookmarkFragment(BookmarkRoot.Mobile.id), menu.directions)
    }

    @Test
    fun `WHEN building the item THEN handle all events it can dispatch and no others`() {
        val provider = BookmarksScreenMenuItemProvider()

        val events = requireNotNull(provider.itemFlow.value).reachableEvents()

        assertTrue(events.all { provider.handles(it) }, "Not all of $events are handled")
        assertFalse(provider.handles(MenuAction.Navigate.History))
    }
}
