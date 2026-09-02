/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.devtools

import android.util.Log
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mozilla.fenix.ui.efficiency.helpers.BasePage
import org.mozilla.fenix.ui.efficiency.helpers.BaseTest
import org.mozilla.fenix.ui.efficiency.helpers.PageContext

/**
 * NavigationReachabilityParameterizedTest
 *
 * Purpose:
 * - Verify that each selected page/component can be reached via `navigateToPage()`
 *   from a fresh test setup.
 *
 * Why this shape:
 * - Matches the same parameterized structure already proven to work in this project.
 * - Keeps each navigation target as its own test case.
 * - Allows the case list to be pasted in from a helper/generator so maintenance stays low.
 *
 * Future direction:
 * - The static `listOf(...)` below can be replaced or regenerated from a helper that reflects
 *   over PageContext and prints `Case(...)` boilerplate.
 */
@RunWith(Parameterized::class)
class NavigationReachabilityParameterizedTest(
    private val case: Case,
) : BaseTest() {

    data class Case(
        val label: String,
        val testRailId: String,
        val page: PageContext.() -> BasePage,
        val state: String = "",
    ) {
        override fun toString(): String =
            "$label ($testRailId)${if (state.isNotBlank()) " — $state" else ""}"
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{index}: {0}")
        fun data(): List<Any> {
            val runState = System.getProperty("testRunState")?.takeIf { it.isNotBlank() } ?: ""

            val cases = listOf(
                // pageName=BookmarksPage, property=bookmarks, paths=5
                Case(
                    label = "BookmarksPage",
                    testRailId = "TBD",
                    page = { bookmarks },
                    state = runState.ifBlank { "Navigation Reachability" },
                ),
// pageName=BrowserPage, property=browserPage, paths=2
                Case(
                    label = "BrowserPage",
                    testRailId = "TBD",
                    page = { browserPage },
                    state = runState.ifBlank { "Navigation Reachability" },
                ),
// pageName=CollectionsPage, property=collections, paths=1
                Case(
                    label = "CollectionsPage",
                    testRailId = "TBD",
                    page = { collections },
                    state = runState.ifBlank { "Navigation Reachability" },
                ),
// pageName=DownloadsPage, property=downloads, paths=3
                Case(
                    label = "DownloadsPage",
                    testRailId = "TBD",
                    page = { downloads },
                    state = runState.ifBlank { "Navigation Reachability" },
                ),
// pageName=FindInPagePage, property=findInPage, paths=2
                Case(
                    label = "FindInPagePage",
                    testRailId = "TBD",
                    page = { findInPage },
                    state = runState.ifBlank { "Navigation Reachability" },
                ),
// pageName=HistoryPage, property=history, paths=5
                Case(
                    label = "HistoryPage",
                    testRailId = "TBD",
                    page = { history },
                    state = runState.ifBlank { "Navigation Reachability" },
                ),
// pageName=HomePage, property=home, paths=1
                Case(
                    label = "HomePage",
                    testRailId = "TBD",
                    page = { home },
                    state = runState.ifBlank { "Navigation Reachability" },
                ),
// pageName=MainMenuPage, property=mainMenu, paths=2
                Case(
                    label = "MainMenuPage",
                    testRailId = "TBD",
                    page = { mainMenu },
                    state = runState.ifBlank { "Navigation Reachability" },
                ),
// pageName=MicrosurveysPage, property=microsurveys, paths=1
                Case(
                    label = "MicrosurveysPage",
                    testRailId = "TBD",
                    page = { microsurveys },
                    state = runState.ifBlank { "Navigation Reachability" },
                ),
// pageName=RecentlyClosedTabsPage, property=recentlyClosedTabs, paths=1
                Case(
                    label = "RecentlyClosedTabsPage",
                    testRailId = "TBD",
                    page = { recentlyClosedTabs },
                    state = runState.ifBlank { "Navigation Reachability" },
                ),
// pageName=SearchBarComponent, property=searchBar, paths=2
                Case(
                    label = "SearchBarComponent",
                    testRailId = "TBD",
                    page = { searchBar },
                    state = runState.ifBlank { "Navigation Reachability" },
                ),
// pageName=SettingsPage, property=settings, paths=4
                Case(
                    label = "SettingsPage",
                    testRailId = "TBD",
                    page = { settings },
                    state = runState.ifBlank { "Navigation Reachability" },
                ),
// pageName=SettingsAboutPage, property=settingsAbout, paths=4
                Case(
                    label = "SettingsAboutPage",
                    testRailId = "TBD",
                    page = { settingsAbout },
                    state = runState.ifBlank { "Navigation Reachability" },
                ),
// pageName=SettingsAccessibilityPage, property=settingsAccessibility, paths=4
                Case(
                    label = "SettingsAccessibilityPage",
                    testRailId = "TBD",
                    page = { settingsAccessibility },
                    state = runState.ifBlank { "Navigation Reachability" },
                ),
// pageName=SettingsAddonsManagerPage, property=settingsAddonsManager, paths=2
                Case(
                    label = "SettingsAddonsManagerPage",
                    testRailId = "TBD",
                    page = { settingsAddonsManager },
                    state = runState.ifBlank { "Navigation Reachability" },
                ),
// pageName=SettingsAutofillPage, property=settingsAutofill, paths=4
                Case(
                    label = "SettingsAutofillPage",
                    testRailId = "TBD",
                    page = { settingsAutofill },
                    state = runState.ifBlank { "Navigation Reachability" },
                ),
// pageName=SettingsCustomizePage, property=settingsCustomize, paths=4
                Case(
                    label = "SettingsCustomizePage",
                    testRailId = "TBD",
                    page = { settingsCustomize },
                    state = runState.ifBlank { "Navigation Reachability" },
                ),
// pageName=SettingsDataCollectionPage, property=settingsDataCollection, paths=1
                Case(
                    label = "SettingsDataCollectionPage",
                    testRailId = "TBD",
                    page = { settingsDataCollection },
                    state = runState.ifBlank { "Navigation Reachability" },
                ),
// pageName=SettingsDeleteBrowsingDataPage, property=settingsDeleteBrowsingData, paths=1
                Case(
                    label = "SettingsDeleteBrowsingDataPage",
                    testRailId = "TBD",
                    page = { settingsDeleteBrowsingData },
                    state = runState.ifBlank { "Navigation Reachability" },
                ),
// pageName=SettingsDeleteBrowsingDataOnQuitPage, property=settingsDeleteBrowsingDataOnQuit, paths=1
                Case(
                    label = "SettingsDeleteBrowsingDataOnQuitPage",
                    testRailId = "TBD",
                    page = { settingsDeleteBrowsingDataOnQuit },
                    state = runState.ifBlank { "Navigation Reachability" },
                ),
// pageName=SettingsEnhancedTrackingProtectionPage, property=settingsEnhancedTrackingProtection, paths=1
                Case(
                    label = "SettingsEnhancedTrackingProtectionPage",
                    testRailId = "TBD",
                    page = { settingsEnhancedTrackingProtection },
                    state = runState.ifBlank { "Navigation Reachability" },
                ),
// pageName=SettingsEnhancedTrackingProtectionExceptionsPage, property=settingsEnhancedTrackingProtectionExceptions, paths=1
                Case(
                    label = "SettingsEnhancedTrackingProtectionExceptionsPage",
                    testRailId = "TBD",
                    page = { settingsEnhancedTrackingProtectionExceptions },
                    state = runState.ifBlank { "Navigation Reachability" },
                ),
// pageName=SettingsExperimentsPage, property=settingsExperiments, paths=1
                Case(
                    label = "SettingsExperimentsPage",
                    testRailId = "TBD",
                    page = { settingsExperiments },
                    state = runState.ifBlank { "Navigation Reachability" },
                ),
// pageName=SettingsHTTPSOnlyModePage, property=settingsHTTPSOnlyMode, paths=1
                Case(
                    label = "SettingsHTTPSOnlyModePage",
                    testRailId = "TBD",
                    page = { settingsHTTPSOnlyMode },
                    state = runState.ifBlank { "Navigation Reachability" },
                ),
// pageName=SettingsHomepagePage, property=settingsHomepage, paths=8
                Case(
                    label = "SettingsHomepagePage",
                    testRailId = "TBD",
                    page = { settingsHomepage },
                    state = runState.ifBlank { "Navigation Reachability" },
                ),
// pageName=SettingsLanguagePage, property=settingsLanguage, paths=1
                Case(
                    label = "SettingsLanguagePage",
                    testRailId = "TBD",
                    page = { settingsLanguage },
                    state = runState.ifBlank { "Navigation Reachability" },
                ),
// pageName=SettingsOpenLinksInAppsPage, property=settingsOpenLinksInApps, paths=1
                Case(
                    label = "SettingsOpenLinksInAppsPage",
                    testRailId = "TBD",
                    page = { settingsOpenLinksInApps },
                    state = runState.ifBlank { "Navigation Reachability" },
                ),
// pageName=SettingsPasswordsPage, property=settingsPasswords, paths=4
                Case(
                    label = "SettingsPasswordsPage",
                    testRailId = "TBD",
                    page = { settingsPasswords },
                    state = runState.ifBlank { "Navigation Reachability" },
                ),
// pageName=SettingsPrivateBrowsingPage, property=settingsPrivateBrowsing, paths=1
                Case(
                    label = "SettingsPrivateBrowsingPage",
                    testRailId = "TBD",
                    page = { settingsPrivateBrowsing },
                    state = runState.ifBlank { "Navigation Reachability" },
                ),
// pageName=SettingsSavePasswordsPage, property=settingsSavePasswords, paths=1
                Case(
                    label = "SettingsSavePasswordsPage",
                    testRailId = "TBD",
                    page = { settingsSavePasswords },
                    state = runState.ifBlank { "Navigation Reachability" },
                ),
// pageName=SettingsSavedPasswordsPage, property=settingsSavedPasswords, paths=2
                Case(
                    label = "SettingsSavedPasswordsPage",
                    testRailId = "TBD",
                    page = { settingsSavedPasswords },
                    state = runState.ifBlank { "Navigation Reachability" },
                ),
// pageName=SettingsSearchPage, property=settingsSearch, paths=4
                Case(
                    label = "SettingsSearchPage",
                    testRailId = "TBD",
                    page = { settingsSearch },
                    state = runState.ifBlank { "Navigation Reachability" },
                ),
// pageName=SettingsSearchAddSearchEnginePage, property=settingsSearchAddSearchEngine
                Case(
                    label = "SettingsSearchAddSearchEnginePage",
                    testRailId = "TBD",
                    page = { settingsSearchAddSearchEngine },
                    state = runState.ifBlank { "Navigation Reachability" },
                ),
// pageName=SettingsSearchManageShortcutsPage, property=settingsSearchManageShortcuts
                Case(
                    label = "SettingsSearchManageShortcutsPage",
                    testRailId = "TBD",
                    page = { settingsSearchManageShortcuts },
                    state = runState.ifBlank { "Navigation Reachability" },
                ),
// pageName=SettingsSiteSettingsPage, property=settingsSiteSettings, paths=1
                Case(
                    label = "SettingsSiteSettingsPage",
                    testRailId = "TBD",
                    page = { settingsSiteSettings },
                    state = runState.ifBlank { "Navigation Reachability" },
                ),
// pageName=SettingsSiteSettingsExceptionsPage, property=settingsSiteSettingsExceptions, paths=1
                Case(
                    label = "SettingsSiteSettingsExceptionsPage",
                    testRailId = "TBD",
                    page = { settingsSiteSettingsExceptions },
                    state = runState.ifBlank { "Navigation Reachability" },
                ),
// pageName=SettingsTabsPage, property=settingsTabs, paths=4
                Case(
                    label = "SettingsTabsPage",
                    testRailId = "TBD",
                    page = { settingsTabs },
                    state = runState.ifBlank { "Navigation Reachability" },
                ),
// pageName=SettingsTurnOnSyncPage, property=settingsTurnOnSync, paths=1
                Case(
                    label = "SettingsTurnOnSyncPage",
                    testRailId = "TBD",
                    page = { settingsTurnOnSync },
                    state = runState.ifBlank { "Navigation Reachability" },
                ),
// pageName=ShareOverlayPage, property=shareOverlay, paths=2
                Case(
                    label = "ShareOverlayPage",
                    testRailId = "TBD",
                    page = { shareOverlay },
                    state = runState.ifBlank { "Navigation Reachability" },
                ),
// pageName=ShortcutsPage, property=shortcuts, paths=1
                Case(
                    label = "ShortcutsPage",
                    testRailId = "TBD",
                    page = { shortcuts },
                    state = runState.ifBlank { "Navigation Reachability" },
                ),
// pageName=TabDrawerPage, property=tabDrawer, paths=1
                Case(
                    label = "TabDrawerPage",
                    testRailId = "TBD",
                    page = { tabDrawer },
                    state = runState.ifBlank { "Navigation Reachability" },
                ),
// pageName=ToolbarComponent, property=toolbar, paths=3
                Case(
                    label = "ToolbarComponent",
                    testRailId = "TBD",
                    page = { toolbar },
                    state = runState.ifBlank { "Navigation Reachability" },
                ),
            )

            return cases.map { it as Any }
        }
    }

    @Test
    fun verifyNavigationReachability() {
        Log.i(
            "NavigationReachabilityTest",
            "TestRail=${case.testRailId} Page=${case.label} State=${case.state}",
        )
        println("TestRail=${case.testRailId} Page=${case.label} State=${case.state}")

        val pageObj: BasePage = case.page(on)
        pageObj.navigateToPage()

        // Add optional page-specific assertions later if needed.
    }
}
