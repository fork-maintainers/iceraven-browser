/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.pageObjects

import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import org.mozilla.fenix.helpers.AppAndSystemHelper
import org.mozilla.fenix.helpers.HomeActivityIntentTestRule
import org.mozilla.fenix.helpers.TestAssetHelper.waitingTime
import org.mozilla.fenix.ui.efficiency.helpers.BasePage
import org.mozilla.fenix.ui.efficiency.navigation.NavigationGraph
import org.mozilla.fenix.ui.efficiency.navigation.NavigationOptions
import org.mozilla.fenix.ui.efficiency.navigation.NavigationStep
import org.mozilla.fenix.ui.efficiency.selectors.BrowserPageSelectors
import org.mozilla.fenix.ui.efficiency.selectors.DownloadsSelectors
import org.mozilla.fenix.ui.efficiency.selectors.HomeSelectors
import org.mozilla.fenix.ui.efficiency.selectors.MainMenuSelectors

class DownloadsPage(composeRule: AndroidComposeTestRule<HomeActivityIntentTestRule, *>) : BasePage(composeRule) {
    override val pageName = "DownloadsPage"

    internal override fun registerNavigation(builder: NavigationGraph.Builder) {
        builder.register(
            from = "HomePage",
            to = pageName,
            steps =
                listOf(
                    NavigationStep.Click(HomeSelectors.MAIN_MENU_BUTTON),
                    NavigationStep.Click(MainMenuSelectors.DOWNLOADS_BUTTON),
                ),
        )

        // The downloads manager is also reachable from the browser's own three-dot menu, which is how
        // a download test gets there (routing browser -> home -> downloads would lose the loaded page).
        builder.register(
            from = "BrowserPage",
            to = pageName,
            steps =
                listOf(
                    NavigationStep.Click(BrowserPageSelectors.MAIN_MENU_BUTTON),
                    NavigationStep.Click(MainMenuSelectors.DOWNLOADS_BUTTON),
                ),
        )

        builder.register(
            from = pageName,
            to = "BrowserPage",
            steps = listOf(NavigationStep.Click(DownloadsSelectors.NAVIGATE_BACK_TOOLBAR_BUTTON)),
        )
    }

    override val selectorCatalog = DownloadsSelectors

    // Narrow the return type to this page so callers can chain page-specific helpers off
    // navigateToPage() (see SettingsAutofillPage for the same pattern).
    override fun navigateToPage(
        url: String,
        forceNavigation: Boolean,
        navigationOptions: NavigationOptions,
    ): DownloadsPage {
        super.navigateToPage(url = url, forceNavigation = forceNavigation, navigationOptions = navigationOptions)
        return this
    }

    /** Assert the downloads manager lists a completed download for [fileName]. */
    fun verifyDownloadedFileExistsInDownloadsList(fileName: String): DownloadsPage {
        mozVerify(DownloadsSelectors.DOWNLOADED_FILE_LIST_ITEM(fileName), timeout = waitingTime)
        return this
    }

    /** Open [fileName]'s row overflow menu, choose Delete, and confirm the delete dialog if one appears. */
    fun deleteDownloadedFile(fileName: String): DownloadsPage {
        mozClick(DownloadsSelectors.DOWNLOADED_FILE_LIST_ITEM_MENU(fileName))
        mozClick(DownloadsSelectors.DELETE_DOWNLOAD_ITEM_MENU_OPTION)
        mozClickIfPresent(DownloadsSelectors.DELETE_DOWNLOAD_DIALOG_CONFIRM_BUTTON)
        return this
    }

    /** Undo the most recent download deletion via the snackbar's Undo action. */
    fun clickUndoDeleteSnackbarButton(): DownloadsPage {
        mozClick(DownloadsSelectors.UNDO_DELETE_SNACKBAR_BUTTON)
        return this
    }

    /** Assert [fileName] has been removed from the device's public Downloads directory. */
    fun verifyDownloadedFileIsDeletedFromStorage(fileName: String): DownloadsPage {
        AppAndSystemHelper.assertDownloadedFileIsDeletedFromStorage(fileName)
        return this
    }

    /** Assert [fileName] still exists in the device's public Downloads directory (not removed from disk). */
    fun verifyDownloadedFileStillExistsOnStorage(fileName: String): DownloadsPage {
        AppAndSystemHelper.assertDownloadedFileExistsOnStorage(fileName)
        return this
    }
}
