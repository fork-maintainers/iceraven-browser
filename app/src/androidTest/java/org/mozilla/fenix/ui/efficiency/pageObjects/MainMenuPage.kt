/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.pageObjects

import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import org.mozilla.fenix.helpers.DataGenerationHelper.getRecommendedExtensionTitle
import org.mozilla.fenix.helpers.HomeActivityIntentTestRule
import org.mozilla.fenix.helpers.TestAssetHelper.waitingTimeLong
import org.mozilla.fenix.ui.efficiency.helpers.BasePage
import org.mozilla.fenix.ui.efficiency.helpers.Selector
import org.mozilla.fenix.ui.efficiency.navigation.NavigationRegistry
import org.mozilla.fenix.ui.efficiency.navigation.NavigationStep
import org.mozilla.fenix.ui.efficiency.selectors.BrowserPageSelectors
import org.mozilla.fenix.ui.efficiency.selectors.HomeSelectors
import org.mozilla.fenix.ui.efficiency.selectors.MainMenuSelectors
import org.mozilla.fenix.ui.efficiency.selectors.SettingsAddonsManagerSelectors

class MainMenuPage(composeRule: AndroidComposeTestRule<HomeActivityIntentTestRule, *>) : BasePage(composeRule) {
    override val pageName = "MainMenuPage"

    init {
        NavigationRegistry.register(
            from = "HomePage",
            to = pageName,
            steps = listOf(
                NavigationStep.Click(HomeSelectors.MAIN_MENU_BUTTON),
            ),
        )

        NavigationRegistry.register(
            from = "BrowserPage",
            to = pageName,
            steps = listOf(
                NavigationStep.Click(BrowserPageSelectors.MAIN_MENU_BUTTON),
            ),
        )

        NavigationRegistry.register(
            from = pageName,
            to = "BrowserPage",
            steps = listOf(),
        )
    }

    override fun mozGetSelectorsByGroup(group: String): List<Selector> {
        return MainMenuSelectors.all.filter { it.groups.contains(group) }
    }

    /**
     * Installs the first recommended extension shown in the expanded Extensions submenu and returns
     * its name. The submenu must already be expanded (click [MainMenuSelectors.EXTENSIONS_BUTTON_UIAUTOMATOR])
     * before calling. Which extension AMO recommends is server-driven, so the name is discovered at
     * runtime and threaded back to the caller for the later removal step.
     */
    fun installFirstRecommendedExtension(): String {
        val addonTitle = getRecommendedExtensionTitle(composeRule)
        mozClick(MainMenuSelectors.RECOMMENDED_ADDON_INSTALL_BUTTON(addonTitle))
        mozVerify(SettingsAddonsManagerSelectors.ADDON_PERMISSION_PROMPT_TITLE(addonTitle), timeout = waitingTimeLong)
        // The permission dialog disables its Add button for ~1s after appearing, so wait for it to
        // become enabled before clicking (mirrors the legacy allowPermissionToInstall).
        mozClickWhenEnabled(SettingsAddonsManagerSelectors.ADDON_PERMISSION_ALLOW_BUTTON)
        mozVerify(SettingsAddonsManagerSelectors.ADDON_INSTALL_COMPLETED_TITLE(addonTitle), timeout = waitingTimeLong)
        // Some extensions (e.g. NoScript) auto-open an onboarding tab on install, which dismisses the
        // "<addon> was added" dialog out from under us. Closing it is therefore best-effort: if the tab
        // already tore the dialog down, clicking OK is a no-op rather than a "Failed to click UiObject"
        // failure. Mirrors the legacy closeAddonInstallCompletePrompt, which ignored the click result.
        mozClickIfPresent(SettingsAddonsManagerSelectors.ADDON_INSTALL_COMPLETED_OK_BUTTON)
        return addonTitle
    }
}
