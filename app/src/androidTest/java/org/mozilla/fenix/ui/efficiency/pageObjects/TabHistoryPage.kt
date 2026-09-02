/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.pageObjects

import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import org.mozilla.fenix.helpers.HomeActivityIntentTestRule
import org.mozilla.fenix.helpers.TestHelper.mDevice
import org.mozilla.fenix.ui.efficiency.helpers.BasePage
import org.mozilla.fenix.ui.efficiency.helpers.Selector
import org.mozilla.fenix.ui.efficiency.navigation.NavigationRegistry
import org.mozilla.fenix.ui.efficiency.navigation.NavigationStep
import org.mozilla.fenix.ui.efficiency.selectors.HomeSelectors
import org.mozilla.fenix.ui.efficiency.selectors.MainMenuSelectors
import org.mozilla.fenix.ui.efficiency.selectors.SearchBarSelectors
import org.mozilla.fenix.ui.efficiency.selectors.TabHistorySelectors
import org.mozilla.fenix.ui.efficiency.selectors.ToolbarSelectors

class TabHistoryPage(composeRule: AndroidComposeTestRule<HomeActivityIntentTestRule, *>) : BasePage(composeRule) {
    override val pageName = "TabHistoryPage"

    init {
        // The tab history sheet is opened by long-pressing the main menu Back button, but that
        // button is disabled (and the long-press is a no-op) unless the tab has back-history. The
        // incoming BrowserPage edge only loads a single page, so load a second, distinct page here
        // to create a back entry before opening the menu.
        NavigationRegistry.register(
            from = "BrowserPage",
            to = pageName,
            steps = listOf(
                NavigationStep.Click(ToolbarSelectors.TOOLBAR_URL_BOX_UIAUTOMATOR),
                NavigationStep.EnterTextValue(SearchBarSelectors.TOOLBAR_IN_EDIT_MODE, "example.org"),
                NavigationStep.PressEnter(SearchBarSelectors.TOOLBAR_IN_EDIT_MODE),
                NavigationStep.Click(HomeSelectors.MAIN_MENU_BUTTON_UIAUTOMATOR),
                NavigationStep.LongClick(MainMenuSelectors.BACK_BUTTON),
            ),
        )
    }

    override fun navigateToPage(url: String, forceNavigation: Boolean): TabHistoryPage {
        super.navigateToPage(url = url.ifBlank { "example.com" }, forceNavigation = forceNavigation)
        return this
    }

    /**
     * Dismisses the tab-history bottom sheet.
     *
     * Call this at the end of any test that opens it. The sheet is app UI the test deliberately opened,
     * so it is the test's to clean up — not something OverlayRegistry should handle, since that is for
     * system surfaces the test never asked for and only fires on a locate miss.
     *
     * This keeps the test self-contained rather than handing the next one an open sheet. Note it was NOT
     * the cause of the CustomTabsTest retry-pass on verifyDownloadInACustomTabTest: adding this left the
     * class result byte-identical, so that leak is still unidentified. Do not read this as the fix for it.
     */
    fun dismissTabHistorySheet(): TabHistoryPage {
        mDevice.pressBack()
        mozVerifyElementAbsent(TabHistorySelectors.TAB_HISTORY_LIST_UIAUTOMATOR)
        return this
    }

    override fun mozGetSelectorsByGroup(group: String): List<Selector> {
        return TabHistorySelectors.all.filter { it.groups.contains(group) }
    }
}
