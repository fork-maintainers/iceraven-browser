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
import org.mozilla.fenix.ui.efficiency.selectors.MainMenuSelectors
import org.mozilla.fenix.ui.efficiency.selectors.WebCompatReporterSelectors

class WebCompatReporterPage(composeRule: AndroidComposeTestRule<HomeActivityIntentTestRule, *>) : BasePage(composeRule) {
    override val pageName = "WebCompatReporterPage"

    init {
        // "Report broken site" lives behind the main menu's More submenu, so the edge expands More
        // first. It is browser-only — there is no entry from the home-page main menu.
        NavigationRegistry.register(
            from = "MainMenuPage",
            to = pageName,
            steps = listOf(
                NavigationStep.Click(MainMenuSelectors.MORE_BUTTON),
                NavigationStep.Click(MainMenuSelectors.REPORT_BROKEN_SITE_BUTTON),
            ),
        )
    }

    override fun mozGetSelectorsByGroup(group: String): List<Selector> {
        return WebCompatReporterSelectors.all.filter { it.groups.contains(group) }
    }
}
