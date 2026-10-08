/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import mozilla.components.compose.base.text.Text
import mozilla.components.compose.menu.data.StandardMenuItem
import mozilla.components.compose.menu.ui.MenuItemIconRes
import mozilla.components.ui.icons.R as iconsR
import org.junit.Test
import org.mozilla.fenix.NavGraphDirections
import org.mozilla.fenix.R
import org.mozilla.fenix.components.menu.fake.FakeMenuHost
import org.mozilla.fenix.components.menu.fake.reachableEvents
import org.mozilla.fenix.components.menu.store.MenuAction

class CustomizeHomepageMenuItemProviderTest {
    @Test
    fun `WHEN providing the item THEN offer customizing the homepage`() {
        val provider = CustomizeHomepageMenuItemProvider()

        assertEquals(
            StandardMenuItem(
                title = Text.Resource(R.string.browser_menu_customize_homepage),
                icon = MenuItemIconRes(iconsR.drawable.mozac_ic_home_24),
                onClickEvent = MenuAction.Navigate.CustomizeHomepage,
            ),
            provider.itemFlow.value,
        )
    }

    @Test
    fun `WHEN clicking the item THEN show the homepage settings in place of the menu`() {
        val menu = FakeMenuHost()

        CustomizeHomepageMenuItemProvider().onEvent(MenuAction.Navigate.CustomizeHomepage, menu)

        assertEquals(NavGraphDirections.actionGlobalHomeSettingsFragment(), menu.directions)
    }

    @Test
    fun `WHEN building the item THEN handle all events it can dispatch and no others`() {
        val provider = CustomizeHomepageMenuItemProvider()

        val events = requireNotNull(provider.itemFlow.value).reachableEvents()

        assertTrue(events.all { provider.handles(it) }, "Not all of $events are handled")
        assertFalse(provider.handles(MenuAction.Navigate.Settings))
    }
}
