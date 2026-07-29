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
import org.mozilla.fenix.ui.efficiency.selectors.CustomTabsSelectors
import org.mozilla.fenix.ui.efficiency.selectors.FindInPageSelectors
import org.mozilla.fenix.ui.efficiency.selectors.HomeSelectors
import org.mozilla.fenix.ui.efficiency.selectors.MainMenuSelectors

class FindInPagePage(composeRule: AndroidComposeTestRule<HomeActivityIntentTestRule, *>) : BasePage(composeRule) {
    override val pageName = "FindInPagePage"

    init {
        NavigationRegistry.register(
            from = "BrowserPage",
            to = pageName,
            steps = listOf(
                NavigationStep.Click(HomeSelectors.MAIN_MENU_BUTTON_UIAUTOMATOR),
                NavigationStep.Click(MainMenuSelectors.FIND_IN_PAGE_BUTTON),
            ),
        )

        // Open Find in page from a custom tab's own menu.
        NavigationRegistry.register(
            from = "CustomTabsPage",
            to = pageName,
            steps = listOf(
                NavigationStep.Click(CustomTabsSelectors.MAIN_MENU_BUTTON),
                NavigationStep.Click(CustomTabsSelectors.MENU_FIND_IN_PAGE),
            ),
        )
    }

    override fun navigateToPage(url: String, forceNavigation: Boolean): FindInPagePage {
        super.navigateToPage(url = url.ifBlank { "example.com" }, forceNavigation = forceNavigation)
        return this
    }

    fun verifyFindInPageElement(query: String, count: Int): FindInPagePage {
        mozClearAndEnterText(query, FindInPageSelectors.FIND_IN_PAGE_QUERY)
        for (i in 1..count) {
            mozVerify(FindInPageSelectors.RESULT_COUNTER("$i/$count"))
            if (i < count) mozClick(FindInPageSelectors.FIND_IN_PAGE_NEXT_BUTTON)
        }
        for (i in count - 1 downTo 1) {
            mozClick(FindInPageSelectors.FIND_IN_PAGE_PREV_BUTTON)
            mozVerify(FindInPageSelectors.RESULT_COUNTER("$i/$count"))
        }
        mozClick(FindInPageSelectors.FIND_IN_PAGE_CLOSE_BUTTON)
        return this
    }

    override fun mozGetSelectorsByGroup(group: String): List<Selector> {
        return FindInPageSelectors.all.filter { it.groups.contains(group) }
    }
}
