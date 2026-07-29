/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.pageObjects

import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import org.mozilla.fenix.helpers.HomeActivityIntentTestRule
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

    override fun mozGetSelectorsByGroup(group: String): List<Selector> {
        return TabHistorySelectors.all.filter { it.groups.contains(group) }
    }
}
