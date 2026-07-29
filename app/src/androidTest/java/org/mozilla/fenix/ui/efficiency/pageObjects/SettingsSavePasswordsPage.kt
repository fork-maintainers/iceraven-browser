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
import org.mozilla.fenix.ui.efficiency.selectors.SettingsPasswordsSelectors
import org.mozilla.fenix.ui.efficiency.selectors.SettingsSavePasswordsSelectors
import org.mozilla.fenix.ui.efficiency.selectors.SettingsSelectors

class SettingsSavePasswordsPage(composeRule: AndroidComposeTestRule<HomeActivityIntentTestRule, *>) : BasePage(composeRule) {
    override val pageName = "SettingsSavePasswordsPage"

    init {
        NavigationRegistry.register(
            from = "HomePage",
            to = pageName,
            steps = listOf(
                NavigationStep.Click(HomeSelectors.MAIN_MENU_BUTTON),
                NavigationStep.Click(MainMenuSelectors.SETTINGS_BUTTON),
                NavigationStep.Click(SettingsSelectors.PASSWORDS_BUTTON),
                NavigationStep.Click(SettingsPasswordsSelectors.SAVE_PASSWORDS_OPTION),
            ),
        )

        // Back out of settings to Home (each "Navigate up" pops one level: save-passwords -> passwords ->
        // settings -> home). Lets tests that set a passwords option then load a page route home -> browser.
        NavigationRegistry.register(
            from = pageName,
            to = "HomePage",
            steps = listOf(
                NavigationStep.ClickIfPresent(SettingsSelectors.GO_BACK_BUTTON),
                NavigationStep.ClickIfPresent(SettingsSelectors.GO_BACK_BUTTON),
                NavigationStep.ClickIfPresent(SettingsSelectors.GO_BACK_BUTTON),
            ),
        )
    }

    fun clickNeverSaveOption(): SettingsSavePasswordsPage {
        mozClick(SettingsSavePasswordsSelectors.NEVER_SAVE_OPTION)
        return this
    }

    override fun mozGetSelectorsByGroup(group: String): List<Selector> {
        return SettingsSavePasswordsSelectors.all.filter { it.groups.contains(group) }
    }
}
