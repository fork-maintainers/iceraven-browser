/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui

import org.junit.Rule
import org.junit.Test
import org.mozilla.fenix.customannotations.Converted
import org.mozilla.fenix.customannotations.SmokeTest
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.helpers.FenixTestRule
import org.mozilla.fenix.helpers.HomeActivityIntentTestRule
import org.mozilla.fenix.helpers.MockBrowserDataHelper
import org.mozilla.fenix.helpers.TestAssetHelper.genericAssets
import org.mozilla.fenix.helpers.TestAssetHelper.getGenericAsset
import org.mozilla.fenix.helpers.TestHelper.appContext
import org.mozilla.fenix.helpers.TestHelper.clickSnackbarButton
import org.mozilla.fenix.helpers.TestHelper.closeApp
import org.mozilla.fenix.helpers.TestHelper.mDevice
import org.mozilla.fenix.helpers.TestHelper.restartApp
import org.mozilla.fenix.helpers.TestHelper.verifySnackBarText
import org.mozilla.fenix.helpers.TestHelper.waitForAppWindowToBeUpdated
import org.mozilla.fenix.helpers.TestHelper.waitUntilSnackbarGone
import org.mozilla.fenix.helpers.perf.DetectMemoryLeaksRule
import org.mozilla.fenix.ui.robots.browserScreen
import org.mozilla.fenix.ui.robots.homeScreen
import org.mozilla.fenix.ui.robots.navigationToolbar
import org.mozilla.fenix.ui.robots.notificationShade
import androidx.compose.ui.test.junit4.v2.AndroidComposeTestRule as AndroidComposeTestRuleV2

/**
 *  Tests for verifying basic functionality of tabbed browsing
 *
 *  Including:
 *  - Opening a tab
 *  - Opening a private tab
 *  - Verifying tab list
 *  - Closing all tabs
 *  - Close tab
 *  - Swipe to close tab (temporarily disabled)
 *  - Undo close tab
 *  - Close private tabs persistent notification
 *  - Empty tab tray state
 *  - Tab tray details
 *  - Shortcut context menu navigation
 */

class TabbedBrowsingTest {
    @get:Rule(order = 0)
    val fenixTestRule: FenixTestRule = FenixTestRule()

    private val mockWebServer get() = fenixTestRule.mockWebServer

    @get:Rule(order = 1)
    val composeTestRule =
        AndroidComposeTestRuleV2(
            HomeActivityIntentTestRule.withDefaultSettingsOverrides(),
        ) { it.activity }

    @get:Rule(order = 2)
    val memoryLeaksRule = DetectMemoryLeaksRule(composeTestRule = { composeTestRule })

    // @Rule(order = 2)
    // @JvmField
    // val retryTestRule = RetryTestRule(3)

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/903599
    @Test
    fun closeAllTabsTest() {
        val defaultWebPage = mockWebServer.getGenericAsset(1)

        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(defaultWebPage.url) {
        }.openTabDrawer(composeTestRule) {
            verifyNormalTabsList()
        }.openThreeDotMenu {
            verifyCloseAllTabsButton()
            verifySelectTabsButton()
        }.closeAllTabs {
            verifyTabCounter("0")
        }

        // Repeat for Private Tabs
        homeScreen(composeTestRule) {
        }.togglePrivateBrowsingMode()

        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(defaultWebPage.url) {
        }.openTabDrawer(composeTestRule) {
            verifyPrivateTabsList()
        }.openThreeDotMenu {
            verifyCloseAllTabsButton()
        }.closeAllTabs {
            verifyTabCounter("0", isPrivateBrowsingEnabled = true)
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/2349580
    @Test
    fun closingTabsTest() {
        val genericURL = mockWebServer.getGenericAsset(1)

        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(genericURL.url) {
        }.openTabDrawer(composeTestRule) {
            verifyExistingOpenTabs("Test_Page_1")
            closeTab()
            verifySnackBarText(composeTestRule, "Tab closed")
            clickSnackbarButton(composeTestRule, "UNDO")
        }
        browserScreen(composeTestRule) {
            verifyTabCounter("1")
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/903604
    @Test
    fun swipeToCloseTabsTest() {
        val webPages = mockWebServer.genericAssets

        MockBrowserDataHelper.createTabItem(webPages[0].url.toString())
        MockBrowserDataHelper.createTabItem(webPages[1].url.toString())

        homeScreen(composeTestRule) {
        }.openTabDrawer {
            verifyExistingOpenTabs(webPages[0].title)
            verifyExistingOpenTabs(webPages[1].title)
            swipeTabRight(webPages[0].title)
            verifySnackBarText("Tab closed")
            waitForAppWindowToBeUpdated()
            clickSnackbarButton(composeTestRule, "UNDO")
            waitForAppWindowToBeUpdated()
            verifyExistingOpenTabs(webPages[0].title)
            verifyExistingOpenTabs(webPages[1].title)
            swipeTabRight(webPages[0].title)
            verifySnackBarText("Tab closed")
            verifyNoExistingOpenTabs(webPages[0].title)
            verifyExistingOpenTabs(webPages[1].title)
            swipeTabLeft(webPages[1].title)
            verifySnackBarText("Tab closed")
            waitForAppWindowToBeUpdated()
            clickSnackbarButton(composeTestRule, "UNDO")
            waitForAppWindowToBeUpdated()
        }
        browserScreen(composeTestRule) {
            verifyPageContent(webPages[1].content)
        }.openTabDrawer(composeTestRule) {
            verifyExistingOpenTabs(webPages[1].title)
            swipeTabLeft(webPages[1].title)
            verifySnackBarText("Tab closed")
        }
        homeScreen(composeTestRule) {
            verifyTabCounter("0")
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/903591
    @Test
    fun closingPrivateTabsTest() {
        val genericURL = mockWebServer.getGenericAsset(1)

        homeScreen(composeTestRule) {
        }.togglePrivateBrowsingMode(switchPBModeOn = true)
        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(genericURL.url) {
        }.openTabDrawer(composeTestRule) {
            verifyExistingOpenTabs("Test_Page_1")
            closeTab()
            verifySnackBarText(composeTestRule, "Private tab closed")
            clickSnackbarButton(composeTestRule, "UNDO")
        }
        browserScreen(composeTestRule) {
            verifyTabCounter("1", isPrivateBrowsingEnabled = true)
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/903592
    @SmokeTest
    @Test
    fun verifyCloseAllPrivateTabsNotificationTest() {
        val defaultWebPage = mockWebServer.getGenericAsset(1)

        homeScreen(composeTestRule) {
        }.togglePrivateBrowsingMode()

        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(defaultWebPage.url) {
            mDevice.openNotification()
        }

        notificationShade {
            verifyPrivateTabsNotification()
        }.clickClosePrivateTabsNotification(composeTestRule) {
            verifyHomeScreen()
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/903600
    @Test
    fun verifyEmptyTabTray() {
        homeScreen(composeTestRule) {
        }.openTabDrawer {
            verifyNormalBrowsingButtonIsSelected()
            verifyPrivateBrowsingButtonIsSelected(false)
            verifyTabGroupsButtonIsSelected(false)
            verifySyncedTabsButtonIsSelected(false)
            verifyNoOpenTabsInNormalBrowsing()
            verifyFab()
            verifyThreeDotButton()
        }.openThreeDotMenu {
            verifyTabSettingsButton()
            verifyRecentlyClosedTabsButton()
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/903585
    @Test
    fun verifyEmptyPrivateTabsTrayTest() {
        homeScreen(composeTestRule) {
        }.openTabDrawer {
        }.toggleToPrivateTabs {
            verifyNormalBrowsingButtonIsSelected(false)
            verifyPrivateBrowsingButtonIsSelected(true)
            verifyTabGroupsButtonIsSelected(false)
            verifySyncedTabsButtonIsSelected(false)
            verifyNoOpenTabsInPrivateBrowsing()
            verifyFab()
            verifyThreeDotButton()
        }.openThreeDotMenu {
            verifyTabSettingsButton()
            verifyRecentlyClosedTabsButton()
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/903601
    @Test
    fun verifyTabsTrayWithOpenTabTest() {
        val defaultWebPage = mockWebServer.getGenericAsset(1)

        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(defaultWebPage.url) {
        }.openTabDrawer(composeTestRule) {
            verifyNormalBrowsingButtonIsSelected()
            verifyPrivateBrowsingButtonIsSelected(isSelected = false)
            verifyTabGroupsButtonIsSelected(isSelected = false)
            verifySyncedTabsButtonIsSelected(isSelected = false)
            verifyThreeDotButton()
            verifyNormalTabsList()
            verifyFab()
            verifyTabThumbnail()
            verifyExistingOpenTabs(defaultWebPage.title)
            verifyTabCloseButton()
        }.openTab(defaultWebPage.title) {
            verifyUrl(defaultWebPage.url.toString())
            verifyTabCounter("1")
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/903587
    @Converted(
        replacedBy = ["org.mozilla.fenix.ui.efficiency.tests.TabbedBrowsingTest#verifyPrivateTabsTrayWithOpenTabTest"],
        bug = 2043491,
        since = "2026-05",
    )
    @SmokeTest
    @Test
    fun verifyPrivateTabsTrayWithOpenTabTest() {
        val website = mockWebServer.getGenericAsset(1)

        homeScreen(composeTestRule) {
        }.openTabDrawer {
        }.toggleToPrivateTabs {
        }.openNewTab {
        }.submitQuery(website.url.toString()) {
        }.openTabDrawer(composeTestRule) {
            verifyNormalBrowsingButtonIsSelected(false)
            verifyPrivateBrowsingButtonIsSelected(true)
            verifyTabGroupsButtonIsSelected(false)
            verifySyncedTabsButtonIsSelected(false)
            verifyThreeDotButton()
            verifyPrivateTabsList()
            verifyExistingOpenTabs(website.title)
            verifyTabCloseButton()
            verifyTabThumbnail()
            verifyFab()
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/927314
    @Test
    fun tabsCounterShortcutMenuCloseTabTest() {
        val firstWebPage = mockWebServer.getGenericAsset(1)
        val secondWebPage = mockWebServer.getGenericAsset(2)

        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(firstWebPage.url) {
            waitForPageToLoad()
        }.goToHomescreen(isPrivateModeEnabled = false) {
        }
        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(secondWebPage.url) {
            waitForPageToLoad()
        }
        navigationToolbar(composeTestRule) {
        }.openTabButtonShortcutsMenu {
            verifyTabButtonShortcutMenuItems()
        }.closeTabFromShortcutsMenu {
            browserScreen(composeTestRule) {
                verifyTabCounter("1")
                verifyPageContent(firstWebPage.content)
            }
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/2343663
    @Test
    fun tabsCounterShortcutMenuNewPrivateTabTest() {
        val defaultWebPage = mockWebServer.getGenericAsset(1)

        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(defaultWebPage.url) {}
        navigationToolbar(composeTestRule) {
        }.openTabButtonShortcutsMenu {
        }.openNewPrivateTabFromShortcutsMenu {
            verifySearchBarPlaceholder("Search or enter address")
        }.dismissSearchBar {
            verifyIfInPrivateOrNormalMode(privateBrowsingEnabled = true)
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/2343662
    @Test
    fun tabsCounterShortcutMenuNewTabTest() {
        val defaultWebPage = mockWebServer.getGenericAsset(1)

        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(defaultWebPage.url) {}
        navigationToolbar(composeTestRule) {
        }.openTabButtonShortcutsMenu {
        }.openNewTabFromShortcutsMenu {
            verifySearchBarPlaceholder("Search or enter address")
        }.dismissSearchBar {
            verifyIfInPrivateOrNormalMode(privateBrowsingEnabled = false)
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/927315
    @Test
    fun privateTabsCounterShortcutMenuCloseTabTest() {
        val firstWebPage = mockWebServer.getGenericAsset(1)
        val secondWebPage = mockWebServer.getGenericAsset(2)

        homeScreen(composeTestRule) {
        }.togglePrivateBrowsingMode(switchPBModeOn = true)
        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(firstWebPage.url) {
            waitForPageToLoad()
        }.goToHomescreen(isPrivateModeEnabled = true) {
        }
        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(secondWebPage.url) {
            waitForPageToLoad()
        }
        navigationToolbar(composeTestRule) {
        }.openTabButtonShortcutsMenu {
            verifyTabButtonShortcutMenuItems()
        }.closeTabFromShortcutsMenu {
            browserScreen(composeTestRule) {
                verifyTabCounter("1", isPrivateBrowsingEnabled = true)
                verifyPageContent(firstWebPage.content)
            }
        }.openTabButtonShortcutsMenu {
        }.closeTabFromShortcutsMenu {
            homeScreen(composeTestRule) {
                verifyIfInPrivateOrNormalMode(privateBrowsingEnabled = true)
            }
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/2344199
    @Test
    fun privateTabsCounterShortcutMenuNewPrivateTabTest() {
        val defaultWebPage = mockWebServer.getGenericAsset(1)

        homeScreen(composeTestRule) {
        }.togglePrivateBrowsingMode(switchPBModeOn = true)
        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(defaultWebPage.url) {
            waitForPageToLoad()
        }
        navigationToolbar(composeTestRule) {
        }.openTabButtonShortcutsMenu {
        }.openNewPrivateTabFromShortcutsMenu {
            verifySearchBarPlaceholder("Search or enter address")
        }.dismissSearchBar {
            verifyIfInPrivateOrNormalMode(privateBrowsingEnabled = true)
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/2344198
    @Test
    fun privateTabsCounterShortcutMenuNewTabTest() {
        val defaultWebPage = mockWebServer.getGenericAsset(1)

        homeScreen(composeTestRule) {
        }.togglePrivateBrowsingMode(switchPBModeOn = true)
        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(defaultWebPage.url) {
            verifyPageContent(defaultWebPage.content)
        }
        navigationToolbar(composeTestRule) {
        }.openTabButtonShortcutsMenu {
        }.openNewTabFromShortcutsMenu {
            verifySearchToolbar(isDisplayed = true)
        }.dismissSearchBar {
            verifyIfInPrivateOrNormalMode(privateBrowsingEnabled = false)
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/1046683
    @Converted(
        replacedBy = ["org.mozilla.fenix.ui.efficiency.tests.TabbedBrowsingTest#verifySyncedTabsWhenUserIsNotSignedInTest"],
        bug = 2039245,
        since = "2026-05",
    )
    @Test
    fun verifySyncedTabsWhenUserIsNotSignedInTest() {
        homeScreen(composeTestRule) {
        }.openTabDrawer {
            verifySyncedTabsButtonIsSelected(isSelected = false)
        }.toggleToSyncedTabs {
            verifySyncedTabsButtonIsSelected(isSelected = true)
            verifySyncedTabsListWhenUserIsNotSignedIn()
        }.clickSignInToSyncButton {
            verifyTurnOnSyncMenu()
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/903598
    @SmokeTest
    @Test
    fun shareTabsFromTabsTrayTest() {
        val firstWebsite = mockWebServer.getGenericAsset(1)
        val secondWebsite = mockWebServer.getGenericAsset(2)
        val firstWebsiteTitle = firstWebsite.title
        val secondWebsiteTitle = secondWebsite.title
        val sharingApp = "Gmail"
        val sharedUrlsString = "${firstWebsite.url}\n\n${secondWebsite.url}"

        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(firstWebsite.url) {
            verifyPageContent(firstWebsite.content)
        }.openTabDrawer(composeTestRule) {
        }.openNewTab {
        }.submitQuery(secondWebsite.url.toString()) {
            verifyPageContent(secondWebsite.content)
        }.openTabDrawer(composeTestRule) {
            verifyExistingOpenTabs("Test_Page_1")
            verifyExistingOpenTabs("Test_Page_2")
        }.openThreeDotMenu {
            verifySelectTabsButton()
        }.clickSelectTabsButton {
            selectTab("Test_Page_1", 1)
            selectTab("Test_Page_2", 2)
        }.openThreeDotMenu {
            verifyShareTabsButton()
        }.clickShareTabsButton {
            verifyShareTabsOverlay(firstWebsiteTitle, secondWebsiteTitle)
            verifySharingWithSelectedApp(
                sharingApp,
                sharedUrlsString,
                "$firstWebsiteTitle, $secondWebsiteTitle",
            )
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/526244
    @Test
    fun privateModeStaysAsDefaultAfterRestartTest() {
        val defaultWebPage = mockWebServer.getGenericAsset(1)

        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(defaultWebPage.url) {
        }.goToHomescreen {
        }.togglePrivateBrowsingMode()

        closeApp(composeTestRule.activityRule)
        restartApp(composeTestRule.activityRule)

        homeScreen(composeTestRule) {
            verifyPrivateBrowsingHomeScreenItems()
        }.openTabDrawer {
        }.toggleToNormalTabs {
            verifyExistingOpenTabs(defaultWebPage.title)
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/2228470
    @Converted(
        replacedBy = ["org.mozilla.fenix.ui.efficiency.tests.TabbedBrowsingTest#privateTabsDoNotPersistAfterClosingAppTest"],
        bug = 2058121,
        since = "2026-07",
    )
    @SmokeTest
    @Test
    fun privateTabsDoNotPersistAfterClosingAppTest() {
        val firstWebPage = mockWebServer.getGenericAsset(1)
        val secondWebPage = mockWebServer.getGenericAsset(2)

        homeScreen(composeTestRule) {
        }.togglePrivateBrowsingMode()

        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(firstWebPage.url) {
        }.openTabDrawer(composeTestRule) {
        }.openNewTab {
        }.submitQuery(secondWebPage.url.toString()) {
        }
        closeApp(composeTestRule.activityRule)
        restartApp(composeTestRule.activityRule)
        homeScreen(composeTestRule) {
            verifyPrivateBrowsingHomeScreenItems()
        }.openTabDrawer {
            verifyNoOpenTabsInPrivateBrowsing()
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/3024942
    @Test
    fun verifyTabsTrayListViewTest() {
        appContext.components.settings.gridTabView = false
        appContext.components.settings.tabGroupsOnboardingEnabled = false

        val webPages = mockWebServer.genericAssets

        MockBrowserDataHelper.createTabItem(webPages[0].url.toString())
        MockBrowserDataHelper.createTabItem(webPages[1].url.toString())
        MockBrowserDataHelper.createTabItem(webPages[2].url.toString())
        MockBrowserDataHelper.createTabItem(webPages[3].url.toString())

        homeScreen(composeTestRule) {
        }.openTabDrawer {
            verifyNormalTabsList()
        }.closeTabDrawer {}
        homeScreen(composeTestRule) {
        }.openTabDrawer {
            verifyOpenTabsOrder(title = webPages[0].title, position = 1, isListViewEnabled = true)
            verifyOpenTabsOrder(title = webPages[1].title, position = 2, isListViewEnabled = true)
            verifyOpenTabsOrder(title = webPages[2].title, position = 3, isListViewEnabled = true)
            verifyOpenTabsOrder(title = webPages[3].title, position = 4, isListViewEnabled = true)
            swipeTabLeft(title = webPages[0].title, isListViewEnabled = true)
            verifySnackBarText("Tab closed")
            waitUntilSnackbarGone()
            verifyNoExistingOpenTabs(webPages[0].title)
            verifyOpenTabsOrder(title = webPages[1].title, position = 1, isListViewEnabled = true)
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/1126911
    @Test
    fun verifyTabsTrayGridViewTest() {
        appContext.components.settings.gridTabView = true
        appContext.components.settings.tabGroupsOnboardingEnabled = false

        val webPages = mockWebServer.genericAssets

        MockBrowserDataHelper.createTabItem(webPages[0].url.toString())
        MockBrowserDataHelper.createTabItem(webPages[1].url.toString())
        MockBrowserDataHelper.createTabItem(webPages[2].url.toString())
        MockBrowserDataHelper.createTabItem(webPages[3].url.toString())

        homeScreen(composeTestRule) {
        }.openTabDrawer {
            verifyNormalTabsList()
        }.closeTabDrawer {}
        homeScreen(composeTestRule) {
        }.openTabDrawer {
            verifyOpenTabsOrder(title = webPages[0].title, position = 1)
            verifyOpenTabsOrder(title = webPages[1].title, position = 2)
            verifyOpenTabsOrder(title = webPages[2].title, position = 3)
            verifyOpenTabsOrder(title = webPages[3].title, position = 4)
            swipeTabLeft(title = webPages[0].title)
            verifyOpenTabsOrder(title = webPages[1].title, position = 1)
        }
    }

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/3968085
    @SmokeTest
    @Test
    fun verifyTheSearchTabsFunctionalityTest() {
        val firstWebPage = mockWebServer.getGenericAsset(1)
        val secondWebPage = mockWebServer.getGenericAsset(2)

        navigationToolbar(composeTestRule) {
        }.enterURLAndEnterToBrowser(firstWebPage.url) {
            waitForPageToLoad()
            verifyPageContent(firstWebPage.content)
        }.openTabDrawer(composeTestRule) {
        }.openNewTab {
        }.submitQuery(secondWebPage.url.toString()) {
            waitForPageToLoad()
            verifyPageContent(secondWebPage.content)
        }.openTabDrawer(composeTestRule) {
            clickSearchTabsButton()
            searchTab("android")
            verifyNoTabsFoundScreen()
            clickClearTabSearchButton()
            searchTab("localhost")
            verifySearchedTabIsDisplayed(firstWebPage.title)
        }.clickSearchedTab(firstWebPage.title) {
            verifyPageContent(firstWebPage.content)
            verifyTabCounter("2")
        }
    }
}
