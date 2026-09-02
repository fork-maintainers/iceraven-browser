/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui

import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.mozilla.fenix.R
import org.mozilla.fenix.customannotations.Converted
import org.mozilla.fenix.customannotations.SkipLeaks
import org.mozilla.fenix.customannotations.SmokeTest
import org.mozilla.fenix.helpers.AppAndSystemHelper.registerAndCleanupIdlingResources
import org.mozilla.fenix.helpers.FenixTestRule
import org.mozilla.fenix.helpers.HomeActivityIntentTestRule
import org.mozilla.fenix.helpers.RecyclerViewIdlingResource
import org.mozilla.fenix.helpers.TestAssetHelper.getGenericAsset
import org.mozilla.fenix.helpers.TestHelper.longTapSelectItem
import org.mozilla.fenix.helpers.TestHelper.mDevice
import org.mozilla.fenix.helpers.perf.DetectMemoryLeaksRule
import org.mozilla.fenix.ui.robots.browserScreen
import org.mozilla.fenix.ui.robots.homeScreen
import org.mozilla.fenix.ui.robots.navigationToolbar
import androidx.compose.ui.test.junit4.v2.AndroidComposeTestRule as AndroidComposeTestRuleV2

/**
 *  Tests for verifying basic functionality of recently closed tabs history
 *
 */
class RecentlyClosedTabsTest {
    @get:Rule(order = 0)
    val fenixTestRule: FenixTestRule = FenixTestRule()

    private val mockWebServer get() = fenixTestRule.mockWebServer

    @get:Rule(order = 1)
    val composeTestRule = AndroidComposeTestRuleV2(
        HomeActivityIntentTestRule.withDefaultSettingsOverrides(),
    ) { it.activity }

    @get:Rule(order = 2)
    val memoryLeaksRule = DetectMemoryLeaksRule(composeTestRule = { composeTestRule })

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/1065414
    // Verifies that a recently closed item is properly opened
    @Converted(
        replacedBy = ["org.mozilla.fenix.ui.efficiency.tests.RecentlyClosedTabsTest#openRecentlyClosedItemTest"],
        bug = 2057411,
        since = "2026-07",
    )
    @SmokeTest
    @Test
    @SkipLeaks(reasons = ["https://bugzilla.mozilla.org/show_bug.cgi?id=1956220"])
    fun openRecentlyClosedItemTest() {
        val website = mockWebServer.getGenericAsset(1)

        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(website.url) {
            mDevice.waitForIdle()
        }.openTabDrawer(composeTestRule) {
            closeTab()
        }
        homeScreen(composeTestRule) {
        }.openThreeDotMenu {
        }.clickHistoryButton {
        }.openRecentlyClosedTabs {
            waitForListToExist()
            registerAndCleanupIdlingResources(
                RecyclerViewIdlingResource(composeTestRule.activity.findViewById(R.id.recently_closed_list), 1),
            ) {
                verifyRecentlyClosedTabsMenuView()
                verifyRecentlyClosedTabsPageTitle("Test_Page_1")
                verifyRecentlyClosedTabsUrl(website.url)
            }
        }.clickRecentlyClosedItem(composeTestRule, "Test_Page_1") {
            verifyUrl(website.url.toString())
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/2195812
    // Verifies that tapping the "x" button removes a recently closed item from the list
    @Converted(
        replacedBy = ["org.mozilla.fenix.ui.efficiency.tests.RecentlyClosedTabsTest#deleteRecentlyClosedTabsItemTest"],
        bug = 2057411,
        since = "2026-07",
    )
    @SmokeTest
    @Test
    @SkipLeaks(reasons = ["https://bugzilla.mozilla.org/show_bug.cgi?id=1956220"])
    fun deleteRecentlyClosedTabsItemTest() {
        val website = mockWebServer.getGenericAsset(1)

        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(website.url) {
            mDevice.waitForIdle()
        }.openTabDrawer(composeTestRule) {
            closeTab()
        }
        homeScreen(composeTestRule) {
        }.openThreeDotMenu {
        }.clickHistoryButton {
        }.openRecentlyClosedTabs {
            waitForListToExist()
            registerAndCleanupIdlingResources(
                RecyclerViewIdlingResource(composeTestRule.activity.findViewById(R.id.recently_closed_list), 1),
            ) {
                verifyRecentlyClosedTabsMenuView()
            }
            clickDeleteRecentlyClosedTabs()
            verifyEmptyRecentlyClosedTabsList()
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/1605515
    @Test
    fun openMultipleRecentlyClosedTabsTest() {
        val firstPage = mockWebServer.getGenericAsset(1)
        val secondPage = mockWebServer.getGenericAsset(2)

        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(firstPage.url) {
            waitForPageToLoad()
        }.openTabDrawer(composeTestRule) {
        }.openNewTab {
        }.submitQuery(secondPage.url.toString()) {
            waitForPageToLoad()
        }.openTabDrawer(composeTestRule) {
        }.openThreeDotMenu {
        }.closeAllTabs {
        }.openThreeDotMenu {
        }.clickHistoryButton {
        }.openRecentlyClosedTabs {
            waitForListToExist()
            longTapSelectItem(firstPage.url)
            longTapSelectItem(secondPage.url)
            openActionBarOverflowOrOptionsMenu(composeTestRule.activity)
        }.clickOpenInNewTab(composeTestRule) {
            // URL verification to be removed once https://bugzilla.mozilla.org/show_bug.cgi?id=1839179 is fixed.
            browserScreen(composeTestRule) {
                verifyPageContent(secondPage.content)
                verifyUrl(secondPage.url.toString())
            }.openTabDrawer(composeTestRule) {
                verifyNormalBrowsingButtonIsSelected(true)
                verifyExistingOpenTabs(firstPage.title, secondPage.title)
            }
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/2198690
    // Couldn't enable it due to the bug I've discovered: https://bugzilla.mozilla.org/show_bug.cgi?id=2015388
    @Ignore("disabled - https://bugzilla.mozilla.org/show_bug.cgi?id=1989405")
    @Test
    fun openRecentlyClosedTabsInPrivateBrowsingTest() {
        val firstPage = mockWebServer.getGenericAsset(1)
        val secondPage = mockWebServer.getGenericAsset(2)

        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(firstPage.url) {
            waitForPageToLoad()
        }.openTabDrawer(composeTestRule) {
        }.openNewTab {
        }.submitQuery(secondPage.url.toString()) {
            waitForPageToLoad()
        }.openTabDrawer(composeTestRule) {
        }.openThreeDotMenu {
        }.closeAllTabs {
        }.openThreeDotMenu {
        }.clickHistoryButton {
        }.openRecentlyClosedTabs {
            waitForListToExist()
            longTapSelectItem(firstPage.url)
            longTapSelectItem(secondPage.url)
            openActionBarOverflowOrOptionsMenu(composeTestRule.activity)
        }.clickOpenInPrivateTab(composeTestRule) {
            // URL verification to be removed once https://bugzilla.mozilla.org/show_bug.cgi?id=1839179 is fixed.
            browserScreen(composeTestRule) {
                verifyPageContent(secondPage.content)
                verifyUrl(secondPage.url.toString())
            }.openTabDrawer(composeTestRule) {
                verifyPrivateBrowsingButtonIsSelected(true)
                verifyExistingOpenTabs(firstPage.title, secondPage.title)
            }
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/1605514
    @Test
    @SkipLeaks(reasons = ["https://bugzilla.mozilla.org/show_bug.cgi?id=1956220"])
    fun shareMultipleRecentlyClosedTabsTest() {
        val firstPage = mockWebServer.getGenericAsset(1)
        val secondPage = mockWebServer.getGenericAsset(2)
        val sharingApp = "Gmail"
        val urlString = "${firstPage.url}\n\n${secondPage.url}"

        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(firstPage.url) {
            waitForPageToLoad()
        }.openTabDrawer(composeTestRule) {
        }.openNewTab {
        }.submitQuery(secondPage.url.toString()) {
            waitForPageToLoad()
        }.openTabDrawer(composeTestRule) {
        }.openThreeDotMenu {
        }.closeAllTabs {
        }.openThreeDotMenu {
        }.clickHistoryButton {
        }.openRecentlyClosedTabs {
            waitForListToExist()
            longTapSelectItem(firstPage.url)
            longTapSelectItem(secondPage.url)
        }.clickShare {
            verifyShareTabsOverlay(firstPage.title, secondPage.title)
            verifySharingWithSelectedApp(sharingApp, urlString, "${firstPage.title}, ${secondPage.title}")
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/1065438
    @Test
    @SkipLeaks(reasons = ["https://bugzilla.mozilla.org/show_bug.cgi?id=1956220"])
    fun closedPrivateTabsAreNotSavedInRecentlyClosedTabsTest() {
        val firstPage = mockWebServer.getGenericAsset(1)
        val secondPage = mockWebServer.getGenericAsset(2)

        homeScreen(composeTestRule) {
        }.togglePrivateBrowsingMode()

        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(firstPage.url) {
            waitForPageToLoad()
        }.openTabDrawer(composeTestRule) {
        }.openNewTab {
        }.submitQuery(secondPage.url.toString()) {
            waitForPageToLoad()
        }.openTabDrawer(composeTestRule) {
        }.openThreeDotMenu {
        }.closeAllTabs {
        }.openThreeDotMenu {
        }.clickHistoryButton {
        }.openRecentlyClosedTabs {
            verifyEmptyRecentlyClosedTabsList()
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/1065439
    @Test
    @SkipLeaks(reasons = ["https://bugzilla.mozilla.org/show_bug.cgi?id=1956220"])
    fun deletingBrowserHistoryClearsRecentlyClosedTabsListTest() {
        val firstPage = mockWebServer.getGenericAsset(1)
        val secondPage = mockWebServer.getGenericAsset(2)

        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(firstPage.url) {
            waitForPageToLoad()
        }.openTabDrawer(composeTestRule) {
        }.openNewTab {
        }.submitQuery(secondPage.url.toString()) {
            waitForPageToLoad()
        }.openTabDrawer(composeTestRule) {
        }.openThreeDotMenu {
        }.closeAllTabs {
        }.openThreeDotMenu {
        }.clickHistoryButton {
        }.openRecentlyClosedTabs {
            waitForListToExist()
        }.goBackToHistoryMenu(composeTestRule) {
            clickDeleteAllHistoryButton()
            selectEverythingOption()
            confirmDeleteAllHistory()
            verifyEmptyHistoryView()
        }.openRecentlyClosedTabs {
            verifyEmptyRecentlyClosedTabsList()
        }
    }
}
