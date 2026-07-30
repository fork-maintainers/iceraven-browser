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
import org.mozilla.fenix.ui.efficiency.selectors.SettingsHomepageSelectors
import org.mozilla.fenix.ui.efficiency.selectors.SettingsSelectors

class SettingsHomepagePage(composeRule: AndroidComposeTestRule<HomeActivityIntentTestRule, *>) : BasePage(composeRule) {
    override val pageName = "SettingsHomepagePage"

    init {
        NavigationRegistry.register(
            from = pageName,
            to = "SettingsPage",
            steps = listOf(NavigationStep.Click(SettingsSelectors.GO_BACK_BUTTON)),
        )

        NavigationRegistry.register(
            from = pageName,
            to = "HomePage",
            steps = listOf(
                NavigationStep.Click(SettingsSelectors.GO_BACK_BUTTON),
                NavigationStep.Click(SettingsSelectors.GO_BACK_BUTTON),
            ),
        )
    }

    override fun mozGetSelectorsByGroup(group: String): List<Selector> {
        return SettingsHomepageSelectors.all.filter { it.groups.contains(group) }
    }
}
