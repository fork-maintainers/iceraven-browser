/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.tests

import org.junit.Test
import org.mozilla.fenix.customannotations.Critical
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.helpers.AppAndSystemHelper.bringAppToForeground
import org.mozilla.fenix.helpers.AppAndSystemHelper.putAppToBackground
import org.mozilla.fenix.helpers.TestAssetHelper.enhancedTrackingProtectionAsset
import org.mozilla.fenix.helpers.TestAssetHelper.getGenericAsset
import org.mozilla.fenix.helpers.TestHelper.appContext
import org.mozilla.fenix.helpers.TestHelper.closeApp
import org.mozilla.fenix.helpers.TestHelper.restartApp
import org.mozilla.fenix.ui.efficiency.helpers.BaseTest
import org.mozilla.fenix.ui.efficiency.pageObjects.ProtectionsDashboardPage
import org.mozilla.fenix.ui.efficiency.selectors.ProtectionsDashboardSelectors
import org.mozilla.fenix.ui.efficiency.selectors.SearchBarSelectors
import org.mozilla.fenix.ui.efficiency.selectors.TabDrawerSelectors

class ProtectionsDashboardTest : BaseTest() {

    // Transient overlay kept out of PageContext (like ReaderViewPage), so instantiate it locally.
    private val protectionsDashboard
        get() = ProtectionsDashboardPage(composeRule)

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/4227311
    @Critical
    @Test
    fun verifyBlockedTrackersDashboardWithTrackersBlockedInTabsTrayTest() {
        appContext.components.settings.setStrictETP()
        val genericPage = mockWebServer.getGenericAsset(1)
        val trackingProtectionUrl = mockWebServer.enhancedTrackingProtectionAsset.url.toString()

        on.browserPage.navigateToPage(genericPage.url.toString())
        on.browserPage.verifyPageContent(genericPage.content)

        on.browserPage.navigateToPage(trackingProtectionUrl)
        on.browserPage.verifyPageContentWithReload(trackingProtectionUrl, "social blocked")
        on.browserPage.verifyPageContentWithReload(trackingProtectionUrl, "ads blocked")
        on.browserPage.verifyPageContentWithReload(trackingProtectionUrl, "analytics blocked")
        on.browserPage.verifyPageContentWithReload(trackingProtectionUrl, "Fingerprinting blocked")

        // Second tab, same tracker URL: the dashboard aggregates blocked counts across all tabs, not just the current
        // one.
        on.tabDrawer.navigateToPage().mozClick(TabDrawerSelectors.FAB)
        on.searchBar
            .mozEnterText(trackingProtectionUrl, SearchBarSelectors.TOOLBAR_IN_EDIT_MODE)
            .mozPressEnter(SearchBarSelectors.TOOLBAR_IN_EDIT_MODE)
        on.browserPage.navigateToPage()
        on.browserPage.verifyPageContentWithReload(trackingProtectionUrl, "social blocked")

        // Open the tray and the dashboard; verify its elements.
        on.tabDrawer.navigateToPage().openTrackersBlockedDashboard()
        protectionsDashboard.verifyDashboardElements()

        // Still shown after backgrounding.
        putAppToBackground()
        bringAppToForeground()
        protectionsDashboard.mozVerify(ProtectionsDashboardSelectors.DASHBOARD_CLOSE_HANDLE)

        // Dismissed by swiping down.
        protectionsDashboard.swipeToDismiss()

        // Dismissed by tapping outside.
        on.tabDrawer.openTrackersBlockedDashboard()
        protectionsDashboard.mozVerify(ProtectionsDashboardSelectors.DASHBOARD_CLOSE_HANDLE)
        protectionsDashboard.tapOutsideToDismiss()

        // Not shown after a restart.
        on.tabDrawer.openTrackersBlockedDashboard()
        protectionsDashboard.mozVerify(ProtectionsDashboardSelectors.DASHBOARD_CLOSE_HANDLE)
        closeApp(composeRule.activityRule)
        restartApp(composeRule.activityRule)
        protectionsDashboard.mozWaitUntilAbsent(ProtectionsDashboardSelectors.DASHBOARD_CLOSE_HANDLE)
    }
}
