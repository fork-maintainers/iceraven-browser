/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.pageObjects

import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import org.mozilla.fenix.helpers.HomeActivityIntentTestRule
import org.mozilla.fenix.helpers.TestAssetHelper.waitingTime
import org.mozilla.fenix.ui.efficiency.helpers.BasePage
import org.mozilla.fenix.ui.efficiency.helpers.Selector
import org.mozilla.fenix.ui.efficiency.navigation.NavigationRegistry
import org.mozilla.fenix.ui.efficiency.navigation.NavigationStep
import org.mozilla.fenix.ui.efficiency.selectors.BrowserPageSelectors
import org.mozilla.fenix.ui.efficiency.selectors.DownloadsSelectors
import org.mozilla.fenix.ui.efficiency.selectors.HomeSelectors
import org.mozilla.fenix.ui.efficiency.selectors.MainMenuSelectors

class DownloadsPage(composeRule: AndroidComposeTestRule<HomeActivityIntentTestRule, *>) : BasePage(composeRule) {
    override val pageName = "DownloadsPage"

    init {
        NavigationRegistry.register(
            from = "HomePage",
            to = pageName,
            steps = listOf(
                NavigationStep.Click(HomeSelectors.MAIN_MENU_BUTTON),
                NavigationStep.Click(MainMenuSelectors.DOWNLOADS_BUTTON),
            ),
        )

        // The downloads manager is also reachable from the browser's own three-dot menu, which is how
        // a download test gets there (routing browser -> home -> downloads would lose the loaded page).
        NavigationRegistry.register(
            from = "BrowserPage",
            to = pageName,
            steps = listOf(
                NavigationStep.Click(BrowserPageSelectors.MAIN_MENU_BUTTON),
                NavigationStep.Click(MainMenuSelectors.DOWNLOADS_BUTTON),
            ),
        )

        NavigationRegistry.register(
            from = pageName,
            to = "BrowserPage",
            steps = listOf(
                NavigationStep.Click(DownloadsSelectors.NAVIGATE_BACK_TOOLBAR_BUTTON),
            ),
        )
    }

    override fun mozGetSelectorsByGroup(group: String): List<Selector> {
        return DownloadsSelectors.all.filter { it.groups.contains(group) }
    }

    // Narrow the return type to this page so callers can chain page-specific helpers off
    // navigateToPage() (see SettingsAutofillPage for the same pattern).
    override fun navigateToPage(url: String, forceNavigation: Boolean): DownloadsPage {
        super.navigateToPage(url = url, forceNavigation = forceNavigation)
        return this
    }

    /** Assert the downloads manager lists a completed download for [fileName]. */
    fun verifyDownloadedFileExistsInDownloadsList(fileName: String): DownloadsPage {
        mozVerify(DownloadsSelectors.DOWNLOADED_FILE_LIST_ITEM(fileName), timeout = waitingTime)
        return this
    }
}
