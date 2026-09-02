/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.pageObjects

import android.os.SystemClock
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import org.mozilla.fenix.helpers.HomeActivityIntentTestRule
import org.mozilla.fenix.ui.efficiency.helpers.BasePage
import org.mozilla.fenix.ui.efficiency.helpers.Selector
import org.mozilla.fenix.ui.efficiency.navigation.NavigationRegistry
import org.mozilla.fenix.ui.efficiency.navigation.NavigationStep
import org.mozilla.fenix.ui.efficiency.selectors.HomeSelectors
import org.mozilla.fenix.ui.efficiency.selectors.MainMenuSelectors
import org.mozilla.fenix.ui.efficiency.selectors.SettingsDeleteBrowsingDataSelectors
import org.mozilla.fenix.ui.efficiency.selectors.SettingsSelectors

class SettingsDeleteBrowsingDataPage(composeRule: AndroidComposeTestRule<HomeActivityIntentTestRule, *>) : BasePage(composeRule) {
    override val pageName = "SettingsDeleteBrowsingDataPage"

    init {
        NavigationRegistry.register(
            from = "HomePage",
            to = pageName,
            steps = listOf(
                NavigationStep.Click(HomeSelectors.MAIN_MENU_BUTTON),
                NavigationStep.Click(MainMenuSelectors.SETTINGS_BUTTON),
                NavigationStep.Swipe(SettingsSelectors.DELETE_BROWSING_DATA_BUTTON),
                NavigationStep.Click(SettingsSelectors.DELETE_BROWSING_DATA_BUTTON),
            ),
        )

        // Reachable from the Settings screen too, so the page can be opened after browsing (Browser ->
        // MainMenu -> Settings -> here) and not only straight from Home.
        NavigationRegistry.register(
            from = "SettingsPage",
            to = pageName,
            steps = listOf(
                NavigationStep.Swipe(SettingsSelectors.DELETE_BROWSING_DATA_BUTTON),
                NavigationStep.Click(SettingsSelectors.DELETE_BROWSING_DATA_BUTTON),
            ),
        )

        // Back out to Settings so the graph can route onward (e.g. to Home, the tab drawer or History)
        // after a deletion, the way the legacy tests do with goBack()/exitMenu().
        NavigationRegistry.register(
            from = pageName,
            to = "SettingsPage",
            steps = listOf(NavigationStep.Click(SettingsSelectors.GO_BACK_BUTTON)),
        )
    }

    override fun mozGetSelectorsByGroup(group: String): List<Selector> {
        return SettingsDeleteBrowsingDataSelectors.all.filter { it.groups.contains(group) }
    }

    override fun navigateToPage(url: String, forceNavigation: Boolean): SettingsDeleteBrowsingDataPage {
        super.navigateToPage(url, forceNavigation)
        return this
    }

    fun verifyAllCheckBoxesAreChecked(): SettingsDeleteBrowsingDataPage {
        verifyCheckBox(SettingsDeleteBrowsingDataSelectors.OPEN_TABS_CHECKBOX, checked = true)
        verifyCheckBox(SettingsDeleteBrowsingDataSelectors.BROWSING_HISTORY_CHECKBOX, checked = true)
        verifyCheckBox(SettingsDeleteBrowsingDataSelectors.COOKIES_CHECKBOX, checked = true)
        verifyCheckBox(SettingsDeleteBrowsingDataSelectors.CACHED_FILES_CHECKBOX, checked = true)
        verifyCheckBox(SettingsDeleteBrowsingDataSelectors.SITE_PERMISSIONS_CHECKBOX, checked = true)
        verifyCheckBox(SettingsDeleteBrowsingDataSelectors.DOWNLOADS_CHECKBOX, checked = true)
        return this
    }

    /**
     * Assert a checkbox's state, polling to a deadline. These rows are native MaterialCheckBoxes that
     * inflate unchecked and get their real state applied a beat later as the fragment binds; the bottom
     * "Delete browsing data" arrival anchor can be present before that settles, so a single-shot read
     * races the bind and flakes (~1/50 on Firebase). Retrying the (single-shot) BasePage assertion keeps
     * the wait local to this screen instead of changing the shared verb. Returns as soon as the state
     * matches; a genuinely wrong state still fails once the deadline passes.
     */
    private fun verifyCheckBox(selector: Selector, checked: Boolean): SettingsDeleteBrowsingDataPage {
        val deadline = SystemClock.uptimeMillis() + CHECKBOX_STATE_TIMEOUT_MS
        while (true) {
            try {
                if (checked) mozVerifyElementIsChecked(selector) else mozVerifyElementIsNotChecked(selector)
                return this
            } catch (e: AssertionError) {
                if (SystemClock.uptimeMillis() >= deadline) throw e
                SystemClock.sleep(CHECKBOX_STATE_POLL_MS)
            }
        }
    }

    fun verifyOpenTabsCheckBox(checked: Boolean) =
        verifyCheckBox(SettingsDeleteBrowsingDataSelectors.OPEN_TABS_CHECKBOX, checked)

    fun verifyCookiesCheckBox(checked: Boolean) =
        verifyCheckBox(SettingsDeleteBrowsingDataSelectors.COOKIES_CHECKBOX, checked)

    /** Legacy verifyBrowsingHistoryDetails(Boolean): asserts the check box state. */
    fun verifyBrowsingHistoryCheckBox(checked: Boolean) =
        verifyCheckBox(SettingsDeleteBrowsingDataSelectors.BROWSING_HISTORY_CHECKBOX, checked)

    /** Legacy verifyBrowsingHistoryDetails(String): asserts the "<n> addresses" summary. */
    fun verifyBrowsingHistoryDetails(addresses: String): SettingsDeleteBrowsingDataPage {
        mozVerify(SettingsDeleteBrowsingDataSelectors.BROWSING_HISTORY_DETAILS(addresses))
        return this
    }

    fun verifyOpenTabsDetails(count: String): SettingsDeleteBrowsingDataPage {
        mozVerify(SettingsDeleteBrowsingDataSelectors.OPEN_TABS_DETAILS(count))
        return this
    }

    /** Uncheck everything except "Open tabs", asserting each toggle took, as the legacy robot did. */
    fun selectOnlyOpenTabsCheckBox(): SettingsDeleteBrowsingDataPage {
        mozClick(SettingsDeleteBrowsingDataSelectors.BROWSING_HISTORY_CHECKBOX)
        verifyCheckBox(SettingsDeleteBrowsingDataSelectors.BROWSING_HISTORY_CHECKBOX, checked = false)
        mozClick(SettingsDeleteBrowsingDataSelectors.COOKIES_CHECKBOX)
        verifyCheckBox(SettingsDeleteBrowsingDataSelectors.COOKIES_CHECKBOX, checked = false)
        mozClick(SettingsDeleteBrowsingDataSelectors.CACHED_FILES_CHECKBOX)
        verifyCheckBox(SettingsDeleteBrowsingDataSelectors.CACHED_FILES_CHECKBOX, checked = false)
        mozClick(SettingsDeleteBrowsingDataSelectors.SITE_PERMISSIONS_CHECKBOX)
        verifyCheckBox(SettingsDeleteBrowsingDataSelectors.SITE_PERMISSIONS_CHECKBOX, checked = false)
        mozClick(SettingsDeleteBrowsingDataSelectors.DOWNLOADS_CHECKBOX)
        verifyCheckBox(SettingsDeleteBrowsingDataSelectors.DOWNLOADS_CHECKBOX, checked = false)
        verifyCheckBox(SettingsDeleteBrowsingDataSelectors.OPEN_TABS_CHECKBOX, checked = true)
        return this
    }

    /** Uncheck everything except "Browsing history", asserting each toggle took. */
    fun selectOnlyBrowsingHistoryCheckBox(): SettingsDeleteBrowsingDataPage {
        mozClick(SettingsDeleteBrowsingDataSelectors.OPEN_TABS_CHECKBOX)
        verifyCheckBox(SettingsDeleteBrowsingDataSelectors.OPEN_TABS_CHECKBOX, checked = false)
        mozClick(SettingsDeleteBrowsingDataSelectors.COOKIES_CHECKBOX)
        verifyCheckBox(SettingsDeleteBrowsingDataSelectors.COOKIES_CHECKBOX, checked = false)
        mozClick(SettingsDeleteBrowsingDataSelectors.CACHED_FILES_CHECKBOX)
        verifyCheckBox(SettingsDeleteBrowsingDataSelectors.CACHED_FILES_CHECKBOX, checked = false)
        mozClick(SettingsDeleteBrowsingDataSelectors.SITE_PERMISSIONS_CHECKBOX)
        verifyCheckBox(SettingsDeleteBrowsingDataSelectors.SITE_PERMISSIONS_CHECKBOX, checked = false)
        mozClick(SettingsDeleteBrowsingDataSelectors.DOWNLOADS_CHECKBOX)
        verifyCheckBox(SettingsDeleteBrowsingDataSelectors.DOWNLOADS_CHECKBOX, checked = false)
        verifyCheckBox(SettingsDeleteBrowsingDataSelectors.BROWSING_HISTORY_CHECKBOX, checked = true)
        return this
    }

    /** Uncheck everything except "Cookies and site data", asserting each toggle took. */
    fun selectOnlyCookiesCheckBox(): SettingsDeleteBrowsingDataPage {
        mozClick(SettingsDeleteBrowsingDataSelectors.OPEN_TABS_CHECKBOX)
        verifyCheckBox(SettingsDeleteBrowsingDataSelectors.OPEN_TABS_CHECKBOX, checked = false)
        verifyCheckBox(SettingsDeleteBrowsingDataSelectors.COOKIES_CHECKBOX, checked = true)
        mozClick(SettingsDeleteBrowsingDataSelectors.CACHED_FILES_CHECKBOX)
        verifyCheckBox(SettingsDeleteBrowsingDataSelectors.CACHED_FILES_CHECKBOX, checked = false)
        mozClick(SettingsDeleteBrowsingDataSelectors.SITE_PERMISSIONS_CHECKBOX)
        verifyCheckBox(SettingsDeleteBrowsingDataSelectors.SITE_PERMISSIONS_CHECKBOX, checked = false)
        mozClick(SettingsDeleteBrowsingDataSelectors.DOWNLOADS_CHECKBOX)
        verifyCheckBox(SettingsDeleteBrowsingDataSelectors.DOWNLOADS_CHECKBOX, checked = false)
        mozClick(SettingsDeleteBrowsingDataSelectors.BROWSING_HISTORY_CHECKBOX)
        verifyCheckBox(SettingsDeleteBrowsingDataSelectors.BROWSING_HISTORY_CHECKBOX, checked = false)
        return this
    }

    fun clickDeleteBrowsingDataButton(): SettingsDeleteBrowsingDataPage {
        mozClick(SettingsDeleteBrowsingDataSelectors.DELETE_BROWSING_DATA_BUTTON)
        return this
    }

    fun verifyDeleteBrowsingDataDialog(): SettingsDeleteBrowsingDataPage {
        mozVerify(SettingsDeleteBrowsingDataSelectors.DELETE_DIALOG_MESSAGE)
        mozVerify(SettingsDeleteBrowsingDataSelectors.DELETE_DIALOG_CANCEL_BUTTON)
        mozVerify(SettingsDeleteBrowsingDataSelectors.DELETE_DIALOG_DELETE_BUTTON)
        return this
    }

    fun clickDialogCancelButton(): SettingsDeleteBrowsingDataPage {
        mozClick(SettingsDeleteBrowsingDataSelectors.DELETE_DIALOG_CANCEL_BUTTON)
        return this
    }

    /** Confirm deletion and wait for the transient "Browsing data deleted" snackbar to clear. */
    fun confirmDeletionAndAssertSnackbar(): SettingsDeleteBrowsingDataPage {
        mozClick(SettingsDeleteBrowsingDataSelectors.DELETE_DIALOG_DELETE_BUTTON)
        mozWaitUntilAbsent(SettingsDeleteBrowsingDataSelectors.DELETION_SNACKBAR)
        return this
    }

    private companion object {
        const val CHECKBOX_STATE_TIMEOUT_MS = 5_000L
        const val CHECKBOX_STATE_POLL_MS = 250L
    }
}
