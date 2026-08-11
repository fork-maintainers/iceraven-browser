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
import org.mozilla.fenix.ui.efficiency.selectors.HomeSelectors
import org.mozilla.fenix.ui.efficiency.selectors.MainMenuSelectors
import org.mozilla.fenix.ui.efficiency.selectors.SettingsAddonsManagerSelectors

class SettingsAddonsManagerPage(composeRule: AndroidComposeTestRule<HomeActivityIntentTestRule, *>) : BasePage(composeRule) {
    override val pageName = "SettingsAddonsManagerPage"

    init {
        NavigationRegistry.register(
            from = "HomePage",
            to = pageName,
            steps = listOf(
                NavigationStep.Click(HomeSelectors.MAIN_MENU_BUTTON),
                NavigationStep.Click(MainMenuSelectors.EXTENSIONS_BUTTON),
            ),
        )

        NavigationRegistry.register(
            from = "HomePage",
            to = pageName,
            steps = listOf(
                NavigationStep.Click(HomeSelectors.MAIN_MENU_BUTTON),
                NavigationStep.Click(MainMenuSelectors.EXTENSIONS_BUTTON),
                // Click the add-on to be able to open the details
            ),
        )

        NavigationRegistry.register(
            from = pageName,
            to = "HomePage",
            steps = listOf(
                NavigationStep.Click(SettingsAddonsManagerSelectors.NAVIGATE_BACK_TOOLBAR_BUTTON),
            ),
        )
    }

    override fun mozGetSelectorsByGroup(group: String): List<Selector> {
        return SettingsAddonsManagerSelectors.all.filter { it.groups.contains(group) }
    }

    /**
     * Opens the detail screen for the installed [addonTitle] and removes it, returning to the add-ons
     * manager list. Mirrors the legacy openDetailedMenuForAddon + removeAddon flow.
     */
    fun removeInstalledExtension(addonTitle: String): SettingsAddonsManagerPage {
        mozVerify(SettingsAddonsManagerSelectors.INSTALLED_ADDON_ITEM(addonTitle))
        mozClick(SettingsAddonsManagerSelectors.INSTALLED_ADDON_ITEM(addonTitle))
        mozVerify(SettingsAddonsManagerSelectors.REMOVE_ADDON_BUTTON)
        mozClick(SettingsAddonsManagerSelectors.REMOVE_ADDON_BUTTON)
        return this
    }

    /**
     * Opens the detail screen for the installed [addonTitle], toggles it off, and returns to the
     * add-ons manager list. Mirrors the legacy openDetailedMenuForAddon + disableExtension +
     * waitUntilSnackbarGone + goBack flow.
     */
    fun disableInstalledExtension(addonTitle: String): SettingsAddonsManagerPage {
        mozVerify(SettingsAddonsManagerSelectors.INSTALLED_ADDON_ITEM(addonTitle))
        mozClick(SettingsAddonsManagerSelectors.INSTALLED_ADDON_ITEM(addonTitle))
        mozVerify(SettingsAddonsManagerSelectors.ENABLE_OR_DISABLE_EXTENSION_TOGGLE)
        mozClick(SettingsAddonsManagerSelectors.ENABLE_OR_DISABLE_EXTENSION_TOGGLE)
        // The "extension disabled" snackbar overlays the toolbar; wait it out before navigating back,
        // mirroring the legacy waitUntilSnackbarGone.
        mozWaitUntilAbsent(BrowserPageSelectors.SNACKBAR)
        mozClick(SettingsAddonsManagerSelectors.NAVIGATE_BACK_TOOLBAR_BUTTON)
        return this
    }
}
