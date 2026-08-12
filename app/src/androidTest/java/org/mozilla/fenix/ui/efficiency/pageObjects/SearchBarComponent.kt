/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.pageObjects

import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import org.mozilla.fenix.helpers.HomeActivityIntentTestRule
import org.mozilla.fenix.ui.efficiency.helpers.BasePage
import org.mozilla.fenix.ui.efficiency.helpers.Selector
import org.mozilla.fenix.ui.efficiency.helpers.SelectorStrategy
import org.mozilla.fenix.ui.efficiency.navigation.NavigationRegistry
import org.mozilla.fenix.ui.efficiency.navigation.NavigationStep
import org.mozilla.fenix.ui.efficiency.selectors.SearchBarSelectors
import org.mozilla.fenix.ui.efficiency.selectors.ToolbarSelectors

class SearchBarComponent(composeRule: AndroidComposeTestRule<HomeActivityIntentTestRule, *>) : BasePage(composeRule) {
    override val pageName = "SearchBarComponent"

    init {
        // Click empty Search bar to enter a URL
        NavigationRegistry.register(
            from = "HomePage",
            to = pageName,
            steps = listOf(NavigationStep.Click(ToolbarSelectors.TOOLBAR_URL_BOX)),
        )

        // Click search bar to edit or replace a URL
        // Use UIAutomator selector to avoid Compose sync hanging when GeckoView is active.
        NavigationRegistry.register(
            from = "BrowserPage",
            to = pageName,
            steps = listOf(NavigationStep.Click(ToolbarSelectors.TOOLBAR_URL_BOX_UIAUTOMATOR)),
        )

        // Dismiss the search bar (edit mode) back to Home. Without an outbound edge the graph can enter
        // the search bar but never route out of it.
        NavigationRegistry.register(
            from = pageName,
            to = "HomePage",
            steps = listOf(NavigationStep.PressBack),
        )
    }

    override fun mozGetSelectorsByGroup(group: String): List<Selector> {
        return SearchBarSelectors.all.filter { it.groups.contains(group) }
    }

    override fun navigateToPage(url: String, forceNavigation: Boolean): SearchBarComponent {
        super.navigateToPage(url, forceNavigation = forceNavigation)
        return this
    }

    fun verifyUrl(url: String): SearchBarComponent {
        mozVerify(
            Selector(
                strategy = SelectorStrategy.UIAUTOMATOR_WITH_TEXT_CONTAINS,
                value = url,
                description = "URL bar contains '$url'",
                groups = listOf(),
            ),
        )
        return this
    }
}
