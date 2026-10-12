/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.menu

import kotlin.test.assertEquals
import kotlin.test.assertNull
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.createCustomTab
import mozilla.components.browser.state.state.createTab
import org.junit.Test

class MenuTargetTest {
    private val selectedTab = createTab(url = "https://mozilla.org", id = "selectedTab")
    private val state =
        BrowserState(tabs = listOf(createTab(url = "https://example.org"), selectedTab), selectedTabId = selectedTab.id)

    @Test
    fun `GIVEN the menu was opened from home WHEN getting its tab THEN return null`() {
        assertNull(MenuTarget.Home.browserSessionFrom(state))
    }

    @Test
    fun `GIVEN the menu was opened from the browser WHEN getting its tab THEN get the selected tab`() {
        assertEquals(selectedTab, MenuTarget.BrowserTab.browserSessionFrom(state))
    }

    @Test
    fun `GIVEN no tab is selected WHEN getting the tab of the browser menu THEN return null`() {
        assertNull(MenuTarget.BrowserTab.browserSessionFrom(state.copy(selectedTabId = null)))
    }

    @Test
    fun `GIVEN the menu was opened from a custom tab WHEN getting its tab THEN get the custom tab`() {
        val customTab = createCustomTab(url = "https://custom.org", id = "customTabId")
        val customState = state.copy(customTabs = listOf(customTab))
        assertEquals(customTab, MenuTarget.CustomTab("customTabId").browserSessionFrom(customState))
    }
}
