/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.browser.applinks

import io.mockk.mockk
import io.mockk.verify
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import mozilla.components.compose.base.text.Text
import mozilla.components.compose.menu.data.StandardMenuItem
import mozilla.components.compose.menu.ui.MenuItemIconRes
import mozilla.components.ui.icons.R as iconsR
import org.junit.Test
import org.mozilla.fenix.R
import org.mozilla.fenix.components.AppStore
import org.mozilla.fenix.components.appstate.AppAction
import org.mozilla.fenix.components.menu.fake.FakeMenuHost
import org.mozilla.fenix.components.menu.store.MenuAction

class OpenInFirefoxMenuItemProviderTest {
    private val appStore: AppStore = mockk(relaxed = true)
    private val menu = FakeMenuHost()

    @Test
    fun `WHEN building the item THEN offer opening in the application`() = runTest {
        val provider = OpenInFirefoxMenuItemProvider(appName = APP_NAME, appStore = appStore)

        assertEquals(
            StandardMenuItem(
                title = Text.Resource(R.string.browser_menu_open_in_fenix, listOf(APP_NAME)),
                icon = MenuItemIconRes(iconsR.drawable.mozac_ic_external_link_24),
                onClickEvent = MenuAction.OpenInFirefox,
            ),
            provider.itemFlow.value,
        )
    }

    @Test
    fun `WHEN clicking the item THEN close the menu and request opening in Firefox`() = runTest {
        val provider = OpenInFirefoxMenuItemProvider(appName = APP_NAME, appStore = appStore)

        provider.onEvent(MenuAction.OpenInFirefox, menu)

        assertTrue(menu.isDismissed)
        verify { appStore.dispatch(AppAction.OpenInFirefoxStarted) }
    }

    @Test
    fun `WHEN asked about the events of other items THEN don't handle them`() = runTest {
        val provider = OpenInFirefoxMenuItemProvider(appName = APP_NAME, appStore = appStore)

        assertFalse(provider.handles(MenuAction.OpenInApp))
        assertFalse(provider.handles(MenuAction.Navigate.Settings))
    }

    private companion object {
        const val APP_NAME = "Firefox"
    }
}
