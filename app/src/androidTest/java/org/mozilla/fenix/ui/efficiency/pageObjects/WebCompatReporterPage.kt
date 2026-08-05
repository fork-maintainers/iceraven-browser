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
import org.mozilla.fenix.ui.efficiency.selectors.BrowserPageSelectors
import org.mozilla.fenix.ui.efficiency.selectors.MainMenuSelectors
import org.mozilla.fenix.ui.efficiency.selectors.WebCompatReporterSelectors

class WebCompatReporterPage(composeRule: AndroidComposeTestRule<HomeActivityIntentTestRule, *>) : BasePage(composeRule) {
    override val pageName = "WebCompatReporterPage"

    init {
        // "Report broken site" lives behind the browser main menu's More submenu, so the edge opens the
        // menu, expands More, then picks the item. Rooted at BrowserPage rather than MainMenuPage: that
        // node stands for both the home and browser menus and is reachable from HomePage in one step, so
        // an edge from it let the planner route through the home menu, which has no Report broken site.
        NavigationRegistry.register(
            from = "BrowserPage",
            to = pageName,
            steps = listOf(
                NavigationStep.Click(BrowserPageSelectors.MAIN_MENU_BUTTON),
                NavigationStep.Click(MainMenuSelectors.MORE_BUTTON),
                NavigationStep.Click(MainMenuSelectors.REPORT_BROKEN_SITE_BUTTON),
            ),
        )
    }

    // Reporting a broken site needs a site, so an empty url still has to load one.
    override fun navigateToPage(url: String, forceNavigation: Boolean): WebCompatReporterPage {
        super.navigateToPage(url = url.ifBlank { "example.com" }, forceNavigation = forceNavigation)
        return this
    }

    override fun mozGetSelectorsByGroup(group: String): List<Selector> {
        return WebCompatReporterSelectors.all.filter { it.groups.contains(group) }
    }
}
